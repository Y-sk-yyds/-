package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

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
 *   <tr><td>produceDate</td><td>produce_date</td><td>DATE</td><td>生产日期</td></tr>
 *   <tr><td>expireDate</td><td>expire_date</td><td>DATE</td><td>有效期至</td></tr>
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
     * 生产日期
     *
     * <p>为该批次药品的生产日期，与 {@link #expireDate} 配合计算剩余效期。
     * 允许为 NULL —— 历史数据或未登记效期的药品不参与效期判断。</p>
     *
     * <p><b>更新策略：</b>使用 {@code FieldStrategy.IGNORED}，
     * 使 null 值也能写入数据库。MyBatis-Plus 默认策略为 NOT_NULL，
     * 若沿用默认值，管理员清空有效期时将静默失效（旧值残留），
     * 导致工作台的过期提醒无法消除。</p>
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDate produceDate;

    /**
     * 有效期至
     *
     * <p><b>效期语义：</b>「有效期至 2026-10-20」表示 10-20 当天仍可销售使用，
     * 次日起失效。因此过期判定条件为 {@code expire_date < 当前日期}（严格小于）。</p>
     *
     * <p>允许为 NULL —— 未登记效期的药品不参与过期判断，
     * 避免历史数据被误判为过期而下架。</p>
     *
     * <p>系统据此字段实现两级管理：</p>
     * <ul>
     *   <li><b>临期预警</b> —— 距到期 ≤ 30 天，工作台提示，药品仍可正常开具</li>
     *   <li><b>过期下架</b> —— 已过期，定时任务自动将 {@link #status} 置为 0，
     *       医生开方时被拒绝</li>
     * </ul>
     *
     * <p><b>更新策略：</b>同 {@link #produceDate}，使用 {@code FieldStrategy.IGNORED}
     * 以支持清空效期。</p>
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private LocalDate expireDate;

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

    /**
     * 距有效期剩余天数（非数据库字段）
     *
     * <p>由 {@code DrugService.pageQuery()} 依据 {@link #expireDate} 与当前日期实时计算，
     * 供前端直接展示，避免前后端各写一套效期阈值。未登记效期的药品为 {@code null}；
     * 已过期则为负数。</p>
     */
    @TableField(exist = false)
    private Long daysToExpire;

    /**
     * 效期预警级别（非数据库字段）
     *
     * <p>与 {@link #daysToExpire} 一同由服务层计算下发，取值：</p>
     * <ul>
     *   <li>{@code EXPIRED} —— 已过期（剩余 &lt; 0 天）</li>
     *   <li>{@code NEAR} —— 近效期（剩余 ≤ 30 天），已自动下架、禁止开具</li>
     *   <li>{@code WARN} —— 临期预警（剩余不足 3 个月），<b>可开具但需提醒医生</b></li>
     *   <li>{@code NORMAL} —— 正常</li>
     *   <li>{@code UNKNOWN} —— 未登记有效期</li>
     * </ul>
     */
    @TableField(exist = false)
    private String expiryLevel;
}
