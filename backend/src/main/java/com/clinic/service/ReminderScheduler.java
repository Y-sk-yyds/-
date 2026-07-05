package com.clinic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clinic.entity.Reminder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 复诊提醒定时任务——每日扫描到期提醒并标记为已提醒
 * <p>
 * 提醒数据的持久化存储由 Reminder 表 + ReminderService 管理，
 * 定时任务只负责每天将到期的待提醒记录状态从 0→1。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderService reminderService;

    /**
     * 每天上午 8:00，将当天到期的待提醒记录批量标记为"已提醒"
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkReminders() {
        log.info("开始扫描复诊提醒...");
        reminderService.update()
                .set("status", 1)
                .eq("status", 0)
                .eq("remind_date", java.time.LocalDate.now())
                .update();
        log.info("复诊提醒扫描完成");
    }
}
