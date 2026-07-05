package com.clinic.mq;

import com.clinic.config.RabbitMQConfig;
import com.clinic.dto.ReminderMessage;
import com.clinic.entity.Reminder;
import com.clinic.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 复诊提醒消费者——监听 RabbitMQ 队列，将消息落库为 reminder 记录
 * <p>
 * 消费队列: clinic.reminder.queue（配置在 RabbitMQConfig 中）
 * 异步解耦：就诊完成不阻塞，消息异步处理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderConsumer {

    private final ReminderService reminderService;

    /**
     * 监听复诊提醒队列，接收到消息后创建 reminder 记录
     *
     * @param message 来自就诊完成事件的提醒消息
     */
    @RabbitListener(queues = RabbitMQConfig.REMINDER_QUEUE)
    public void handleReminder(ReminderMessage message) {
        log.info("收到复诊提醒消息: consultationId={}, patientId={}", message.getConsultationId(), message.getPatientId());

        Reminder reminder = new Reminder();
        reminder.setPatientId(message.getPatientId());
        reminder.setConsultationId(message.getConsultationId());
        reminder.setDoctorId(message.getDoctorId());
        reminder.setRemindDate(message.getRemindDate());
        String diagnosisInfo = message.getDiagnosis() != null ? "诊断: " + message.getDiagnosis() + "。" : "";
        String adviceInfo = message.getAdvice() != null ? "医嘱: " + message.getAdvice() : "请按时复诊";
        reminder.setContent(diagnosisInfo + adviceInfo);
        reminder.setStatus(0);

        reminderService.save(reminder);
        log.info("复诊提醒已入库: id={}, remindDate={}", reminder.getId(), message.getRemindDate());
    }
}
