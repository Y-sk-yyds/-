package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <h2>处方实体类</h2>
 *
 * <p>映射数据库表 {@code prescription}，用于存储医生在就诊结束时开具的处方信息。
 * 每张处方关联一次就诊（{@link Consultation}），记录诊断结果和处方内容，
 * 并在收费完成后更新状态。</p>
 *
 * <p>继承自 {@link BaseEntity}，自动获得创建时间（createTime）、
 * 更新时间（updateTime）和逻辑删除标识（deleted）三个公共字段。</p>
 *
 * <h3>处方状态流转</h3>
 * <pre>
 * 待收费(0) ──→ 已收费(1)
 * </pre>
 * <ul>
 *   <li><b>待收费(0)</b> — 医生已开具处方，患者尚未缴费</li>
 *   <li><b>已收费(1)</b> — 收银员完成收费确认</li>
 * </ul>
 *
 * <h3>与药品明细的关系</h3>
 * <p>一张处方可以包含多种药品，具体药品明细存储在
 * {@link PrescriptionDrug} 表中，通过 {@code prescriptionId} 关联。</p>
 *
 * <h3>数据库表结构对应关系</h3>
 * <table border="1">
 *   <tr><th>Java 字段</th><th>数据库列名</th><th>数据类型</th><th>说明</th></tr>
 *   <tr><td>id</td><td>id</td><td>BIGINT</td><td>主键，自增</td></tr>
 *   <tr><td>consultationId</td><td>consultation_id</td><td>BIGINT</td><td>就诊ID，外键关联 consultation 表</td></tr>
 *   <tr><td>diagnosis</td><td>diagnosis</td><td>VARCHAR</td><td>诊断结果（与就诊记录中的诊断一致）</td></tr>
 *   <tr><td>prescriptionContent</td><td>prescription_content</td><td>VARCHAR</td><td>处方内容，用于描述整体用药方案</td></tr>
 *   <tr><td>status</td><td>status</td><td>INT</td><td>状态：0-待收费，1-已收费</td></tr>
 *   <tr><td>doctorId</td><td>doctor_id</td><td>BIGINT</td><td>开处方的医生ID</td></tr>
 * </table>
 *
 * @author clinic
 * @version 1.0
 * @see BaseEntity
 * @see Consultation
 * @see PrescriptionDrug
 * @see Bill
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prescription")
public class Prescription extends BaseEntity {

    /**
     * 处方ID，主键，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 就诊记录ID，外键关联 {@link Consultation} 表的主键，
     * 标识该处方是在哪次就诊中开具的
     */
    private Long consultationId;

    /**
     * 诊断结果，记录医生对患者疾病的判断，
     * 作为开具处方的医学依据，与就诊记录中的诊断保持同步
     */
    private String diagnosis;

    /**
     * 处方内容，用于描述整体的用药方案和治疗说明。
     * 具体的药品明细（药品名称、数量、用法等）存储在
     * {@link PrescriptionDrug} 表中。
     */
    private String prescriptionContent;

    /**
     * 处方状态
     * <ul>
     *   <li>0 — 待收费，患者尚未缴纳药费</li>
     *   <li>1 — 已收费，收银员已完成收费确认</li>
     * </ul>
     */
    private Integer status;

    /**
     * 开处方医生ID，外键关联 {@link Doctor} 表的主键，
     * 记录是哪位医生开具的处方
     */
    private Long doctorId;
}
