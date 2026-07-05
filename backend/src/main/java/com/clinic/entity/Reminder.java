package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 复诊提醒实体——映射 reminder 表
 * <p>
 * 就诊完成时通过 RabbitMQ 异步创建，仪表盘定时查询展示。
 * remindDate 字段存储应复诊日期，status 跟踪提醒生命周期。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reminder")
public class Reminder extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 患者ID */
    private Long patientId;
    /** 就诊ID */
    private Long consultationId;
    /** 医生ID */
    private Long doctorId;
    /** 应复诊日期 */
    private LocalDate remindDate;
    /** 提醒内容（如"请于一周后复诊复查血压"） */
    private String content;
    /** 0待提醒 1已提醒 2已复诊 */
    private Integer status;
}
