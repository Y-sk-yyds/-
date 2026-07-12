package com.clinic.common;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
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
 * @author clinic
 */
@Data
public abstract class BaseEntity {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
