package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * <h2>挂号记录实体类</h2>
 *
 * <p>映射数据库表 {@code registration}，用于存储患者的挂号信息。
 * 挂号是患者就诊流程的入口，关联患者（{@link Patient}）和排班（{@link Schedule}），
 * 并生成排队序号，后续进入就诊（{@link Consultation}）环节。</p>
 *
 * <p>继承自 {@link BaseEntity}，自动获得创建时间（createTime）、
 * 更新时间（updateTime）和逻辑删除标识（deleted）三个公共字段。</p>
 *
 * <h3>挂号状态流转</h3>
 * <pre>
 * 已挂号(1) ──→ 已就诊(2) ──→ （正常结束）
 *     │
 *     ├──→ 已取消(0) — （患者主动取消）
 *     │
 *     └──→ 爽约(3)   — （挂号后未按时就诊）
 * </pre>
 *
 * <h3>排队序号</h3>
 * <p>{@code queueNum} 在同一排班时段内按挂号顺序递增，用于门诊叫号。</p>
 *
 * <h3>数据库表结构对应关系</h3>
 * <table border="1">
 *   <tr><th>Java 字段</th><th>数据库列名</th><th>数据类型</th><th>说明</th></tr>
 *   <tr><td>id</td><td>id</td><td>BIGINT</td><td>主键，自增</td></tr>
 *   <tr><td>patientId</td><td>patient_id</td><td>BIGINT</td><td>患者ID，外键关联 patient 表</td></tr>
 *   <tr><td>scheduleId</td><td>schedule_id</td><td>BIGINT</td><td>排班ID，外键关联 schedule 表</td></tr>
 *   <tr><td>queueNum</td><td>queue_num</td><td>INT</td><td>排队序号</td></tr>
 *   <tr><td>status</td><td>status</td><td>INT</td><td>状态：0-已取消，1-已挂号，2-已就诊，3-爽约</td></tr>
 *   <tr><td>registrationDate</td><td>registration_date</td><td>DATE</td><td>挂号日期</td></tr>
 *   <tr><td>remark</td><td>remark</td><td>VARCHAR</td><td>备注信息</td></tr>
 * </table>
 *
 * @author clinic
 * @version 1.0
 * @see BaseEntity
 * @see Patient
 * @see Schedule
 * @see Consultation
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("registration")
public class Registration extends BaseEntity {

    /**
     * 挂号记录ID，主键，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 患者ID，外键关联 {@link Patient} 表的主键，
     * 标识该挂号属于哪位患者
     */
    private Long patientId;

    /**
     * 排班ID，外键关联 {@link Schedule} 表的主键，
     * 标识挂号对应的医生出诊时段
     */
    private Long scheduleId;

    /**
     * 排队序号，在同一排班时段（同医生、同日期、同时段）内，
     * 按挂号先后顺序自动生成递增编号，用于门诊叫号。
     */
    private Integer queueNum;

    /**
     * 挂号状态
     * <ul>
     *   <li>0 — 已取消，患者主动取消挂号</li>
     *   <li>1 — 已挂号，等待就诊</li>
     *   <li>2 — 已就诊，医生已完成接诊</li>
     *   <li>3 — 爽约，挂号后未在规定时间内就诊</li>
     * </ul>
     */
    private Integer status;

    /**
     * 挂号日期，记录患者挂号的业务日期
     */
    private LocalDate registrationDate;

    /**
     * 备注信息，可记录挂号时的特殊说明
     */
    private String remark;
}
