package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <h2>药品库存实体类</h2>
 *
 * <p>映射数据库表 {@code drug_stock}，用于管理每种药品的库存数量。
 * 每种药品（{@link Drug}）对应一条库存记录，记录当前库存量和预警阈值。</p>
 *
 * <p>继承自 {@link BaseEntity}，自动获得创建时间（createTime）、
 * 更新时间（updateTime）和逻辑删除标识（deleted）三个公共字段。</p>
 *
 * <h3>库存管理规则</h3>
 * <ul>
 *   <li><b>当前库存（currentStock）</b> — 药品的实际可用数量</li>
 *   <li><b>预警阈值（warningThreshold）</b> — 当库存低于此值时触发预警</li>
 *   <li>医生开药时检查库存是否充足，不足时提示警告</li>
 *   <li>收费确认后自动扣减库存（减少 {@code currentStock}）</li>
 *   <li>退药/退货时恢复库存（增加 {@code currentStock}）</li>
 * </ul>
 *
 * <h3>库存预警机制</h3>
 * <pre>
 * 当 currentStock <= warningThreshold 时
 *     系统弹出库存不足预警
 *     提示管理员及时补货
 * </pre>
 *
 * <h3>数据库表结构对应关系</h3>
 * <table border="1">
 *   <tr><th>Java 字段</th><th>数据库列名</th><th>数据类型</th><th>说明</th></tr>
 *   <tr><td>id</td><td>id</td><td>BIGINT</td><td>主键，自增</td></tr>
 *   <tr><td>drugId</td><td>drug_id</td><td>BIGINT</td><td>药品ID，外键关联 drug 表</td></tr>
 *   <tr><td>currentStock</td><td>current_stock</td><td>INT</td><td>当前库存数量</td></tr>
 *   <tr><td>warningThreshold</td><td>warning_threshold</td><td>INT</td><td>库存预警阈值</td></tr>
 * </table>
 *
 * @author clinic
 * @version 1.0
 * @see BaseEntity
 * @see Drug
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("drug_stock")
public class DrugStock extends BaseEntity {

    /**
     * 库存记录ID，主键，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 药品ID，外键关联 {@link Drug} 表的主键。
     * 每种药品对应唯一一条库存记录，形成一对一关系。
     */
    private Long drugId;

    /**
     * 当前库存数量，表示该药品在药房中的实际可用数量。
     * 开药时扣减库存，退药/入库时增加库存。
     */
    private Integer currentStock;

    /**
     * 库存预警阈值，当 {@code currentStock} 低于或等于此值时，
     * 系统触发库存不足预警，提醒管理员及时采购补货。
     */
    private Integer warningThreshold;
}
