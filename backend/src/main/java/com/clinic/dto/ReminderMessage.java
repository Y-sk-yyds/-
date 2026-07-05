package com.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 复诊提醒消息——通过 RabbitMQ 从就诊服务发到提醒消费者
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 就诊ID */
    private Long consultationId;
    /** 患者ID */
    private Long patientId;
    /** 医生ID */
    private Long doctorId;
    /** 诊断结果 */
    private String diagnosis;
    /** 医嘱（从中提取复诊日期建议） */
    private String advice;
    /** 应复诊日期（由消费者计算：就诊日 + 医嘱中的天数） */
    private LocalDate remindDate;
}
