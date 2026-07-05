package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <h2>处方药品明细实体类</h2>
 *
 * <p>映射数据库表 {@code prescription_drug}，用于存储处方中每种药品的详细信息。
 * 这是处方（{@link Prescription}）与药品（{@link Drug}）之间的关联表，
 * 记录每种药品的数量、用法、用量和频次。</p>
 *
 * <p>继承自 {@link BaseEntity}，自动获得创建时间（createTime）、
 * 更新时间（updateTime）和逻辑删除标识（deleted）三个公共字段。</p>
 *
 * <h3>表关系说明</h3>
 * <p>一张处方（Prescription）可以包含多条处方药品明细（PrescriptionDrug），
 * 每条明细关联一种药品（Drug）。这是一对多的关系：</p>
 * <pre>
 * Prescription (1) ──→ PrescriptionDrug (N) ──→ Drug (1)
 * </pre>
 *
 * <h3>用药信息</h3>
 * <ul>
 *   <li><b>usageMethod</b> — 用法，如：口服、外用、静脉注射等</li>
 *   <li><b>dosage</b> — 用量，如：每次 1 片、每次 10ml 等</li>
 *   <li><b>frequency</b> — 频次，如：每日三次、每日一次等</li>
 * </ul>
 *
 * <h3>数据库表结构对应关系</h3>
 * <table border="1">
 *   <tr><th>Java 字段</th><th>数据库列名</th><th>数据类型</th><th>说明</th></tr>
 *   <tr><td>id</td><td>id</td><td>BIGINT</td><td>主键，自增</td></tr>
 *   <tr><td>prescriptionId</td><td>prescription_id</td><td>BIGINT</td><td>处方ID，外键关联 prescription 表</td></tr>
 *   <tr><td>drugId</td><td>drug_id</td><td>BIGINT</td><td>药品ID，外键关联 drug 表</td></tr>
 *   <tr><td>quantity</td><td>quantity</td><td>INT</td><td>药品数量</td></tr>
 *   <tr><td>usageMethod</td><td>usage_method</td><td>VARCHAR</td><td>用法（口服、外用等）</td></tr>
 *   <tr><td>dosage</td><td>dosage</td><td>VARCHAR</td><td>用量</td></tr>
 *   <tr><td>frequency</td><td>frequency</td><td>VARCHAR</td><td>用药频次</td></tr>
 * </table>
 *
 * @author clinic
 * @version 1.0
 * @see BaseEntity
 * @see Prescription
 * @see Drug
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prescription_drug")
public class PrescriptionDrug extends BaseEntity {

    /**
     * 处方药品明细ID，主键，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 处方ID，外键关联 {@link Prescription} 表的主键，
     * 标识该药品明细属于哪张处方
     */
    private Long prescriptionId;

    /**
     * 药品ID，外键关联 {@link Drug} 表的主键，
     * 标识处方中开出的是哪种药品
     */
    private Long drugId;

    /**
     * 药品数量，如开具 2 盒、3 瓶等，
     * 收费时用于计算药费：数量 × 药品单价
     */
    private Integer quantity;

    /**
     * 用法，描述药品的使用方式，如：
     * <ul>
     *   <li>口服 — 通过口腔服用</li>
     *   <li>外用 — 涂抹于皮肤表面</li>
     *   <li>静脉注射 — 通过静脉给药</li>
     *   <li>肌肉注射 — 通过肌肉给药</li>
     * </ul>
     */
    private String usageMethod;

    /**
     * 用量，描述每次使用的剂量，如：
     * "每次 1 片"、"每次 10ml"、"每次 1 粒"
     */
    private String dosage;

    /**
     * 用药频次，描述每天用药的次数或间隔，如：
     * "每日一次 (qd)"、"每日两次 (bid)"、"每日三次 (tid)"、"必要时 (prn)"
     */
    private String frequency;
}
