package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <h2>病历实体类</h2>
 *
 * <p>映射数据库表 {@code medical_record}，用于存储患者每次就诊的病历记录。
 * 病历是患者医疗档案的核心组成部分，记录诊断结果和治疗方案，
 * 并为患者提供历史就诊信息查询。</p>
 *
 * <p>继承自 {@link BaseEntity}，自动获得创建时间（createTime）、
 * 更新时间（updateTime）和逻辑删除标识（deleted）三个公共字段。</p>
 *
 * <h3>安全说明</h3>
 * <p><b>病历内容（content）采用 AES 加密存储</b>，确保患者隐私数据安全。
 * 写入数据库前进行加密，读取后进行解密，防止数据库泄露导致病历信息暴露。</p>
 *
 * <h3>与其他实体的关系</h3>
 * <pre>
 * Patient (1) ──→ MedicalRecord (N)
 * Consultation (1) ──→ MedicalRecord (1)
 * Doctor (1) ──→ MedicalRecord (N)
 * </pre>
 * <ul>
 *   <li>一个患者可以有多次病历记录（一对多）</li>
 *   <li>一次就诊对应一份病历（一对一）</li>
 *   <li>一个医生可以编写多份病历（一对多）</li>
 * </ul>
 *
 * <h3>数据库表结构对应关系</h3>
 * <table border="1">
 *   <tr><th>Java 字段</th><th>数据库列名</th><th>数据类型</th><th>说明</th></tr>
 *   <tr><td>id</td><td>id</td><td>BIGINT</td><td>主键，自增</td></tr>
 *   <tr><td>patientId</td><td>patient_id</td><td>BIGINT</td><td>患者ID，外键关联 patient 表</td></tr>
 *   <tr><td>consultationId</td><td>consultation_id</td><td>BIGINT</td><td>就诊ID，外键关联 consultation 表</td></tr>
 *   <tr><td>content</td><td>content</td><td>VARCHAR</td><td>病历内容（AES加密存储）</td></tr>
 *   <tr><td>diagnosis</td><td>diagnosis</td><td>VARCHAR</td><td>诊断结果</td></tr>
 *   <tr><td>treatmentPlan</td><td>treatment_plan</td><td>VARCHAR</td><td>治疗方案</td></tr>
 *   <tr><td>doctorId</td><td>doctor_id</td><td>BIGINT</td><td>接诊医生ID</td></tr>
 * </table>
 *
 * @author clinic
 * @version 1.0
 * @see BaseEntity
 * @see Patient
 * @see Consultation
 * @see Doctor
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("medical_record")
public class MedicalRecord extends BaseEntity {

    /**
     * 病历ID，主键，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 患者ID，外键关联 {@link Patient} 表的主键，
     * 标识该病历属于哪位患者。
     * 通过该字段可以查询患者的所有历史病历。
     */
    private Long patientId;

    /**
     * 就诊记录ID，外键关联 {@link Consultation} 表的主键。
     * 一次就诊对应一份病历，形成一对一关系。
     */
    private Long consultationId;

    /**
     * 病历内容，记录医生撰写的详细病历信息，
     * 包括主诉、现病史、既往史、体格检查、辅助检查等。
     *
     * <p><b>安全说明：</b>此字段在存入数据库前经过 AES 加密处理，
     * 从数据库读取后需解密还原，以确保患者隐私数据安全。</p>
     */
    private String content;

    /**
     * 诊断结果，医生对患者疾病的判断结论，
     * 与就诊记录（{@link Consultation}）中的诊断保持一致
     */
    private String diagnosis;

    /**
     * 治疗方案，医生制定的治疗计划，
     * 包括用药方案、治疗措施、复诊建议等
     */
    private String treatmentPlan;

    /**
     * 接诊医生ID，外键关联 {@link Doctor} 表的主键，
     * 记录是哪位医生编写的该份病历
     */
    private Long doctorId;
}
