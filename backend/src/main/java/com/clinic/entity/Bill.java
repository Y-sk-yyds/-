package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * <h2>账单/费用实体类</h2>
 *
 * <p>映射数据库表 {@code bill}，用于存储患者的缴费记录。
 * 每张账单关联一次挂号（{@link Registration}）和一张处方（{@link Prescription}），
 * 包含挂号费和药费两部分费用，以及支付方式和支付状态。</p>
 *
 * <p>继承自 {@link BaseEntity}，自动获得创建时间（createTime）、
 * 更新时间（updateTime）和逻辑删除标识（deleted）三个公共字段。</p>
 *
 * <h3>费用构成</h3>
 * <pre>
 * 总金额(totalAmount) = 挂号费(registrationFee) + 药费(drugFee)
 * </pre>
 *
 * <h3>支付状态流转</h3>
 * <pre>
 * 未支付(0) ──→ 已支付(1) ──→ 已退款(2)
 * </pre>
 *
 * <h3>支持的支付方式</h3>
 * <ul>
 *   <li><b>CASH</b> — 现金支付</li>
 *   <li><b>WECHAT</b> — 微信支付</li>
 *   <li><b>ALIPAY</b> — 支付宝支付</li>
 *   <li><b>CARD</b> — 银行卡支付</li>
 * </ul>
 *
 * <h3>数据库表结构对应关系</h3>
 * <table border="1">
 *   <tr><th>Java 字段</th><th>数据库列名</th><th>数据类型</th><th>说明</th></tr>
 *   <tr><td>id</td><td>id</td><td>BIGINT</td><td>主键，自增</td></tr>
 *   <tr><td>registrationId</td><td>registration_id</td><td>BIGINT</td><td>挂号ID，外键关联 registration 表</td></tr>
 *   <tr><td>prescriptionId</td><td>prescription_id</td><td>BIGINT</td><td>处方ID，外键关联 prescription 表</td></tr>
 *   <tr><td>registrationFee</td><td>registration_fee</td><td>DECIMAL</td><td>挂号费（元）</td></tr>
 *   <tr><td>drugFee</td><td>drug_fee</td><td>DECIMAL</td><td>药费（元）</td></tr>
 *   <tr><td>totalAmount</td><td>total_amount</td><td>DECIMAL</td><td>总金额（元）</td></tr>
 *   <tr><td>paymentMethod</td><td>payment_method</td><td>VARCHAR</td><td>支付方式</td></tr>
 *   <tr><td>status</td><td>status</td><td>INT</td><td>状态：0-未支付，1-已支付，2-已退款</td></tr>
 * </table>
 *
 * @author clinic
 * @version 1.0
 * @see BaseEntity
 * @see Registration
 * @see Prescription
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bill")
public class Bill extends BaseEntity {

    /**
     * 账单ID，主键，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 挂号记录ID，外键关联 {@link Registration} 表的主键。
     * 通过挂号记录可追溯到患者信息和就诊时段。
     */
    private Long registrationId;

    /**
     * 处方ID，外键关联 {@link Prescription} 表的主键。
     * 通过处方可查询药品明细和计算药费。
     */
    private Long prescriptionId;

    /**
     * 挂号费（元），患者挂号时缴纳的费用。
     * 使用 {@link BigDecimal} 确保金额计算精度。
     */
    private BigDecimal registrationFee;

    /**
     * 药费（元），处方中所有药品的总费用。
     * 计算公式：∑(药品单价 × 数量)。
     * 使用 {@link BigDecimal} 确保金额计算精度。
     */
    private BigDecimal drugFee;

    /**
     * 总金额（元），挂号费与药费之和。
     * 使用 {@link BigDecimal} 确保金额计算精度。
     */
    private BigDecimal totalAmount;

    /**
     * 支付方式
     * <ul>
     *   <li>CASH — 现金支付</li>
     *   <li>WECHAT — 微信支付</li>
     *   <li>ALIPAY — 支付宝支付</li>
     *   <li>CARD — 银行卡支付</li>
     * </ul>
     */
    private String paymentMethod;

    /**
     * 支付状态
     * <ul>
     *   <li>0 — 未支付，账单已生成但患者尚未缴费</li>
     *   <li>1 — 已支付，收银员确认收款完成</li>
     *   <li>2 — 已退款，费用已退还给患者</li>
     * </ul>
     */
    private Integer status;
}
