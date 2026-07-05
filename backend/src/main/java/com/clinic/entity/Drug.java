package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.baomidou.mybatisplus.annotation.TableField;

import java.math.BigDecimal;

/**
 * <h2>药品实体类</h2>
 *
 * <p>映射数据库表 {@code drug}，用于存储诊所药品目录信息。
 * 记录药品的基本信息，包括名称、规格、单位、单价和分类等。
 * 药品是处方（{@link Prescription}）和库存管理（{@link DrugStock}）的核心基础数据。</p>
 *
 * <p>继承自 {@link BaseEntity}，自动获得创建时间（createTime）、
 * 更新时间（updateTime）和逻辑删除标识（deleted）三个公共字段。</p>
 *
 * <h3>非数据库字段说明</h3>
 * <p>{@code currentStock} 和 {@code warningThreshold} 字段通过
 * {@code @TableField(exist = false)} 标记，表示它们不存在于数据库表中，
 * 而是在查询时从 {@link DrugStock} 表动态关联获取的库存信息。</p>
 *
 * <h3>药品管理流程</h3>
 * <pre>
 * 药品入库 ──→ 更新库存 ──→ 医生开处方 ──→ 收费扣减库存
 * </pre>
 *
 * <h3>数据库表结构对应关系</h3>
 * <table border="1">
 *   <tr><th>Java 字段</th><th>数据库列名</th><th>数据类型</th><th>说明</th></tr>
 *   <tr><td>id</td><td>id</td><td>BIGINT</td><td>主键，自增</td></tr>
 *   <tr><td>name</td><td>name</td><td>VARCHAR</td><td>药品名称</td></tr>
 *   <tr><td>specification</td><td>specification</td><td>VARCHAR</td><td>规格（如 0.25g×12片）</td></tr>
 *   <tr><td>unit</td><td>unit</td><td>VARCHAR</td><td>单位（如 盒、瓶、支）</td></tr>
 *   <tr><td>unitPrice</td><td>unit_price</td><td>DECIMAL</td><td>单价</td></tr>
 *   <tr><td>category</td><td>category</td><td>VARCHAR</td><td>药品分类</td></tr>
 *   <tr><td>status</td><td>status</td><td>INT</td><td>状态：1-启用，0-停用</td></tr>
 * </table>
 *
 * @author clinic
 * @version 1.0
 * @see BaseEntity
 * @see DrugStock
 * @see PrescriptionDrug
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("drug")
public class Drug extends BaseEntity {

    /**
     * 药品ID，主键，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 药品名称，如"阿莫西林胶囊"、"布洛芬片"等
     */
    private String name;

    /**
     * 药品规格，描述药品的剂型和含量，如：
     * "0.25g×12片/盒"、"100ml/瓶"
     */
    private String specification;

    /**
     * 药品单位，用于计价和库存计量的最小单位，如：
     * "盒"、"瓶"、"支"、"袋"
     */
    private String unit;

    /**
     * 药品单价（元），使用 {@link BigDecimal} 确保金额计算的精度，
     * 避免浮点数运算误差
     */
    private BigDecimal unitPrice;

    /**
     * 药品分类，用于药品管理和统计，如：
     * "抗生素"、"解热镇痛"、"中成药"、"心血管"等
     */
    private String category;

    /**
     * 药品状态
     * <ul>
     *   <li>1 — 启用，可正常开药</li>
     *   <li>0 — 停用，已下架不可开药</li>
     * </ul>
     */
    private Integer status;

    // ===================== 以下为非数据库字段 =====================

    /**
     * 当前库存数量（非数据库字段）
     *
     * <p>此字段通过 {@code @TableField(exist = false)} 标注，
     * 表示在数据库 {@code drug} 表中不存在该列。
     * 实际数据从 {@link DrugStock} 表中查询关联后填充，
     * 用于前端展示药品的实时库存信息。</p>
     */
    @TableField(exist = false)
    private Integer currentStock;

    /**
     * 库存预警阈值（非数据库字段）
     *
     * <p>此字段通过 {@code @TableField(exist = false)} 标注，
     * 表示在数据库 {@code drug} 表中不存在该列。
     * 实际数据从 {@link DrugStock} 表中查询关联后填充。
     * 当 {@code currentStock} 低于此阈值时，系统进行库存预警提示。</p>
     */
    @TableField(exist = false)
    private Integer warningThreshold;
}
