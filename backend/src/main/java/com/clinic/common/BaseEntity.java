package com.clinic.common;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <h2>实体基类</h2>
 *
 * <p>所有数据库实体类（Entity）的抽象父类，用于提取公共字段，
 * 包括创建时间、更新时间和逻辑删除标识。</p>
 *
 * <p>通过 MyBatis-Plus 的 {@code @TableField} 自动填充机制，
 * 在插入和更新时自动设置对应时间戳，无需手动赋值。</p>
 *
 * <h3>自动填充字段说明</h3>
 * <ul>
 *   <li><b>createTime</b> — 插入时自动填充，记录数据创建时间</li>
 *   <li><b>updateTime</b> — 插入和更新时自动填充，记录最后修改时间</li>
 *   <li><b>deleted</b> — 插入时自动填充，配合 {@code @TableLogic} 实现逻辑删除</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <p>实体类继承本类即可自动获得以上三个字段：</p>
 * <pre>{@code
 *   @Data
 *   @TableName("t_user")
 *   public class User extends BaseEntity {
 *       private Long id;
 *       private String name;
 *   }
 * }</pre>
 *
 * @author clinic
 * @version 1.0
 */
@Data
public abstract class BaseEntity {

    /**
     * 记录创建时间
     * <p>由 MyBatis-Plus 自动填充，插入时自动设为当前时间</p>
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 记录最后更新时间
     * <p>由 MyBatis-Plus 自动填充，插入（INSERT）和更新（UPDATE）时均自动设为当前时间</p>
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标识
     *
     * <p>配合 {@link TableLogic @TableLogic} 注解实现逻辑删除：
     * 0 表示正常（未删除），1 表示已删除。
     * 查询时 MyBatis-Plus 会自动过滤已删除的记录。</p>
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
