package com.clinic.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.clinic.common.Result;
import com.clinic.entity.Schedule;
import com.clinic.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 排班管理控制器 —— 处理医生排班信息的增删改查 HTTP 请求。
 *
 * <p>
 * 本控制器管理医生的出诊排班，包括排班日期、时段、可挂号人数等。
 * 排班是患者挂号的基础：只有当医生有对应排班时，患者才能挂号。
 * 每条排班记录包含总号源数（totalNum）和剩余号源数（remainNum），
 * 创建排班时剩余号源自动等于总号源，挂号后 remainNum 递减。
 * </p>
 *
 * <h3>请求路径前缀</h3>
 * <p>所有接口均位于 {@code /api/schedules} 路径下。</p>
 *
 * @see ScheduleService 排班服务层
 * @see Schedule 排班实体类，映射 schedules 数据库表
 */
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    /**
     * 排班服务 —— 由 Spring 通过 {@code @RequiredArgsConstructor} 自动注入。
     */
    private final ScheduleService scheduleService;

    /**
     * 分页查询排班列表。
     *
     * <p>
     * 支持按医生 ID、日期范围进行筛选。
     * 常用于查看某位医生在某段时间内的出诊安排。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code GET /api/schedules?page=1&pageSize=10&doctorId=5&startDate=2025-01-01&endDate=2025-01-31}</pre>
     *
     * <h4>请求参数（Query String）</h4>
     * <ul>
     *   <li><b>page</b> (Integer, 可选, 默认 1) — 页码</li>
     *   <li><b>pageSize</b> (Integer, 可选, 默认 10) — 每页记录数</li>
     *   <li><b>doctorId</b> (Long, 可选) — 医生ID，筛选指定医生的排班</li>
     *   <li><b>startDate</b> (String, 可选, 格式 yyyy-MM-dd) — 排班开始日期</li>
     *   <li><b>endDate</b> (String, 可选, 格式 yyyy-MM-dd) — 排班结束日期</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <ul>
     *   <li>成功时返回 {@link Result}{@code <IPage<Schedule>>}，data 的 records 为排班列表</li>
     * </ul>
     *
     * @param page      页码，默认第 1 页
     * @param pageSize  每页条数，默认 10 条
     * @param doctorId  医生ID（可选）
     * @param startDate 排班起始日期（可选）
     * @param endDate   排班结束日期（可选）
     * @return 排班分页数据的统一响应结果
     */
    @GetMapping
    public Result<IPage<Schedule>> list(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                         @RequestParam(required = false) Long doctorId,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        // 委托 ScheduleService 执行带条件的分页查询
        return Result.ok(scheduleService.pageQuery(page, pageSize, doctorId, startDate, endDate));
    }

    /**
     * 新增排班信息。
     *
     * <p>
     * 创建一条排班记录。关键逻辑：创建时自动将 <b>剩余号源数（remainNum）</b>
     * 设置为与 <b>总号源数（totalNum）</b> 相同，表示初始状态所有号源均可用。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code POST /api/schedules}</pre>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>doctorId</b> (Long) — 医生ID</li>
     *   <li><b>scheduleDate</b> (String, yyyy-MM-dd) — 排班日期</li>
     *   <li><b>timeSlot</b> (String) — 时段（如 "上午"、"下午"）</li>
     *   <li><b>totalNum</b> (Integer) — 总号源数</li>
     *   <li><b>fee</b> (BigDecimal) — 挂号费</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param schedule 排班实体对象，由请求体 JSON 自动反序列化
     * @return 统一响应结果
     */
    @PostMapping
    public Result<Void> create(@RequestBody Schedule schedule) {
        // 创建排班时，剩余号源数初始化为总号源数
        schedule.setRemainNum(schedule.getTotalNum());
        // 调用 MyBatis-Plus 的 save 方法持久化
        scheduleService.save(schedule);
        return Result.ok();
    }

    /**
     * 更新排班信息。
     *
     * <p>
     * 根据传入排班对象的 ID 定位目标记录，更新排班信息。
     * 注意：如果总号源数被修改，可能需要同步调整剩余号源数，
     * 当前实现仅执行简单更新，不自动处理号源数一致性。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code PUT /api/schedules}</pre>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>id</b> (Long, 必填) — 排班ID</li>
     *   <li>其它需要更新的 {@link Schedule} 字段</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param schedule 排班实体对象，必须包含有效的 ID
     * @return 统一响应结果
     */
    @PutMapping
    public Result<Void> update(@RequestBody Schedule schedule) {
        // 调用 MyBatis-Plus 的 updateById 方法按主键更新
        scheduleService.updateById(schedule);
        return Result.ok();
    }

    /**
     * 删除排班信息。
     *
     * <p>
     * 根据排班 ID 删除对应记录。
     * 注意：如果该排班下已有患者挂号记录，删除操作可能失败。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code DELETE /api/schedules/{id}}</pre>
     *
     * <h4>路径参数</h4>
     * <ul>
     *   <li><b>id</b> (Long) — 要删除的排班 ID</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param id 排班ID，来自 URL 路径变量
     * @return 统一响应结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 调用 MyBatis-Plus 的 removeById 按主键删除
        scheduleService.removeById(id);
        return Result.ok();
    }
}
