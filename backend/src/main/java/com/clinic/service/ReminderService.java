package com.clinic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clinic.entity.Reminder;
import com.clinic.mapper.ReminderMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 复诊提醒服务——供消费者落库和仪表盘查询
 */
@Service
public class ReminderService extends ServiceImpl<ReminderMapper, Reminder> {

    /**
     * 查当天到期的待提醒记录
     */
    public List<Reminder> findPendingToday() {
        return list(new LambdaQueryWrapper<Reminder>()
                .eq(Reminder::getRemindDate, LocalDate.now())
                .eq(Reminder::getStatus, 0));
    }

    /**
     * 查全部待提醒记录
     */
    public List<Reminder> findAllPending() {
        return list(new LambdaQueryWrapper<Reminder>()
                .eq(Reminder::getStatus, 0)
                .orderByAsc(Reminder::getRemindDate));
    }
}
