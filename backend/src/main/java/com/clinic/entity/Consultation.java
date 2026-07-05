package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * <h2>就诊记录实体类</h2>
 *
 * <p>映射数据库表 {@code consultation}，用于存储医生对患者的诊疗过程记录。
 * 每当医生开始接诊一位患者，系统创建一条就诊记录，记录主诉、诊断结果、
 * 医嘱以及就诊的起止时间。</p>
 *
 * <p>继承自 {@link BaseEntity}，自动获得创建时间（createTime）、
 * 更新时间（updateTime）和逻辑删除标识（deleted）三个公共字段。</p>
 *
 * <h3>就诊状态流转</h3>
 * <pre>
 * 待诊(0) ──→ 就诊中(1) ──→ 已完成(2)
 * </pre>
 * <ul>
 *   <li><b>待诊(0)</b> — 挂号后尚未被医生接诊</li>
 *   <li><b>就诊中(1)</b> — 医生已开始接诊，记录 {@code startTime}</li>
 *   <li><b>已完成(2)</b> — 就诊结束，记录 {@code endTime}、诊断和医嘱</li>
 * </ul>
 *
 * <h3>与其他实体的关系</h3>
 * <ul>
 *   <li>通过 {@code registrationId} 关联挂号记录</li>
 *   <li>通过 {@code doctorId} 关联接诊医生</li>
 *   <li>就诊完成后生成处方（{@link Prescription}）和病历（{@link MedicalRecord}）</li>
 * </ul>
 *
 * <h3>数据库表结构对应关系</h3>
 * <table border="1">
 *   <tr><th>Java 字段</th><th>数据库列名</th><th>数据类型</th><th>说明</th></tr>
 *   <tr><td>id</td><td>id</td><td>BIGINT</td><td>主键，自增</td></tr>
 *   <tr><td>registrationId</td><td>registration_id</td><td>BIGINT</td><td>挂号ID，外键关联 registration 表</td></tr>
 *   <tr><td>doctorId</td><td>doctor_id</td><td>BIGINT</td><td>医生ID，外键关联 doctor 表</td></tr>
 *   <tr><td>complaint</td><td>complaint</td><td>VARCHAR</td><td>主诉：患者自述症状</td></tr>
 *   <tr><td>diagnosis</td><td>diagnosis</td><td>VARCHAR</td><td>诊断结果</td></tr>
 *   <tr><td>advice</td><td>advice</td><td>VARCHAR</td><td>医嘱：医生给出的治疗建议</td></tr>
 *   <tr><td>status</td><td>status</td><td>INT</td><td>状态：0-待诊，1-就诊中，2-已完成</td></tr>
 *   <tr><td>startTime</td><td>start_time</td><td>DATETIME</td><td>就诊开始时间</td></tr>
 *   <tr><td>endTime</td><td>end_time</td><td>DATETIME</td><td>就诊结束时间</td></tr>
 * </table>
 *
 * @author clinic
 * @version 1.0
 * @see BaseEntity
 * @see Registration
 * @see Doctor
 * @see Prescription
 * @see MedicalRecord
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("consultation")
public class Consultation extends BaseEntity {

    /**
     * 就诊记录ID，主键，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 挂号记录ID，外键关联 {@link Registration} 表的主键，
     * 每个挂号记录对应一次就诊
     */
    private Long registrationId;

    /**
     * 接诊医生ID，外键关联 {@link Doctor} 表的主键
     */
    private Long doctorId;

    /**
     * 主诉，记录患者自述的主要症状和不适，
     * 如"咳嗽三天，伴有发热"，是医生诊断的重要参考
     */
    private String complaint;

    /**
     * 诊断结果，医生根据主诉、检查等信息得出的疾病判断，
     * 如"上呼吸道感染"
     */
    private String diagnosis;

    /**
     * 医嘱，医生给出的治疗建议和注意事项，
     * 包括用药指导、生活建议、复诊要求等
     */
    private String advice;

    /**
     * 就诊状态
     * <ul>
     *   <li>0 — 待诊，患者已挂号但医生尚未开始接诊</li>
     *   <li>1 — 就诊中，医生正在接诊</li>
     *   <li>2 — 已完成，本次就诊结束</li>
     * </ul>
     */
    private Integer status;

    /**
     * 就诊开始时间，医生点击"开始接诊"时记录
     */
    private LocalDateTime startTime;

    /**
     * 就诊结束时间，医生完成诊断并提交时记录。
     * 与 {@code startTime} 的差值即为本次就诊时长。
     */
    private LocalDateTime endTime;
}
