package com.clinic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clinic.entity.Drug;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <h2>药品效期定时任务</h2>
 *
 * <p>负责按药品有效期自动管理上下架状态，是「药品效期管理」闭环的执行端。
 * 与 {@link ReminderScheduler}（复诊提醒）并列，同为系统的定时调度组件。</p>
 *
 * <h3>下架策略（近效期即下架）</h3>
 * <p>距有效期不足 {@value #NEAR_EXPIRY_DAYS} 天的药品不再等到过期当天，
 * 直接判定为「近效期」并自动下架，为退药、换药、补货留出处理时间。</p>
 * <table border="1">
 *   <tr><th>级别</th><th>判定条件</th><th>处理动作</th></tr>
 *   <tr>
 *     <td>近效期下架</td>
 *     <td>0 ≤ 距到期天数 ≤ {@value #NEAR_EXPIRY_DAYS}</td>
 *     <td>自动将 {@code status} 置为 0，工作台同步提示处理</td>
 *   </tr>
 *   <tr>
 *     <td>已过期</td>
 *     <td>距到期天数 &lt; 0</td>
 *     <td>此前已被下架，工作台以红色高亮持续提醒清理</td>
 *   </tr>
 * </table>
 *
 * <h3>效期语义</h3>
 * <p>「有效期至 2026-10-20」表示 10-20 当天仍可使用，次日起失效。
 * 下架判定采用 {@code expire_date <= 当前日期 + 30 天}（含两端）。</p>
 *
 * <h3>执行时机</h3>
 * <ul>
 *   <li><b>每日 00:30</b> —— {@link Scheduled} 定时扫描，处理夜间跨越有效期的药品</li>
 *   <li><b>应用启动就绪后</b> —— {@link EventListener} 补偿扫描一次，
 *       避免服务停机期间遗漏的过期药品一直处于在售状态</li>
 * </ul>
 *
 * <h3>NULL 值处理（重要）</h3>
 * <p>未登记有效期的药品（{@code expire_date IS NULL}）<b>不参与</b>过期判断，
 * 因此查询中必须显式添加 {@code isNotNull} 条件。若省略该条件，
 * 历史药品数据将被误判为过期并批量下架。</p>
 *
 * @author clinic
 * @version 1.0
 * @see Drug#getExpireDate()
 * @see Drug#getStatus()
 * @see DrugService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DrugExpiryScheduler {

    /** 药品服务，用于查询与更新药品状态 */
    private final DrugService drugService;

    /**
     * 近效期下架提前量（天）。
     * <p>距有效期不足该天数的药品即视为近效期，由本任务自动下架；
     * 工作台 {@link DashboardService} 复用同一常量生成待办提醒，
     * 调整阈值时两处必须保持一致。</p>
     */
    public static final int NEAR_EXPIRY_DAYS = 30;

    /**
     * 效期预警提前量（月）。
     *
     * <p>距有效期不足该月数的药品<b>仍可正常开具</b>，但医生开方界面会给出黄色提示
     * 「还有 X 天过期」，由医生自行判断是否使用。</p>
     *
     * <p>与 {@link #NEAR_EXPIRY_DAYS} 构成两级管控：<b>预警线只提示、不禁用；
     * 下架线才真正停用</b>。阈值统一由本类持有并在 {@code DrugService} 中复用，
     * 前端不重复定义数字。</p>
     */
    public static final int EXPIRY_WARN_MONTHS = 3;

    /**
     * 扫描并自动下架已过期药品
     *
     * <p>执行流程：</p>
     * <ol>
     *   <li>查询 {@code expire_date} 非空、距到期不足 {@value #NEAR_EXPIRY_DAYS} 天、且仍为启用状态（status=1）的药品</li>
     *   <li>若存在近效期药品，批量将其 {@code status} 置为 0（已停用/已下架）</li>
     *   <li>输出日志记录被下架的药品名称，便于运维追溯</li>
     * </ol>
     *
     * <p>下架后药品不会从数据库中删除，历史处方与账单不受影响；
     * 管理员在工作台看到提醒后，可更新效期信息并重新上架。</p>
     */
    @Scheduled(cron = "0 30 0 * * ?")
    public void autoOffShelfExpiredDrugs() {
        LocalDate today = LocalDate.now();
        // 下架线：有效期落在「今天 ~ 今天+30 天」区间内（含两端）即视为近效期
        LocalDate deadline = today.plusDays(NEAR_EXPIRY_DAYS);

        // 查询条件：已登记效期 + 已进入近效期 + 尚未下架
        // isNotNull 必不可少：未登记效期的药品不参与效期判断
        List<Drug> expired = drugService.list(new LambdaQueryWrapper<Drug>()
                .isNotNull(Drug::getExpireDate)
                .le(Drug::getExpireDate, deadline)
                .eq(Drug::getStatus, 1));

        if (expired.isEmpty()) {
            log.info("药品效期扫描完成：无近效期药品需要下架");
            return;
        }

        // 批量下架：状态置 0（已停用）
        List<Long> ids = expired.stream().map(Drug::getId).collect(Collectors.toList());
        drugService.update()
                .set("status", 0)
                .in("id", ids)
                .update();

        String names = expired.stream()
                .map(d -> d.getName() + "（有效期至 " + d.getExpireDate() + "）")
                .collect(Collectors.joining("、"));
        log.warn("药品效期扫描完成：已自动下架 {} 种近效期/过期药品 —— {}", expired.size(), names);
    }

    /**
     * 应用启动就绪后的补偿扫描
     *
     * <p>定时任务只在每日 00:30 触发，若服务在该时刻处于停机状态，
     * 当日的过期药品将不会被处理。此处监听 Spring 的
     * {@link ApplicationReadyEvent}，在容器完全就绪后立即补扫一次，
     * 保证任何时刻启动服务，过期药品都会被及时下架。</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void scanOnStartup() {
        log.info("应用启动完成，执行药品效期补偿扫描...");
        autoOffShelfExpiredDrugs();
    }
}
