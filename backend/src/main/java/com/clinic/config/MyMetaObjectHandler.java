package com.clinic.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 元对象字段自动填充处理器 —— 在插入和更新操作时自动填充公共字段。
 *
 * <p>核心职责：</p>
 * <ul>
 *   <li><b>插入时</b>自动填充：创建时间（createTime）、更新时间（updateTime）、逻辑删除标记（deleted）</li>
 *   <li><b>更新时</b>自动填充：更新时间（updateTime）</li>
 * </ul>
 *
 * <p>使用前提：实体类中对应的字段需要标注 {@code @TableField(fill = FieldFill.INSERT)} 或
 * {@code @TableField(fill = FieldFill.UPDATE)} 注解，本处理器才会自动填充。</p>
 *
 * <p>使用 {@code strictInsertFill} / {@code strictUpdateFill} 方法（严格填充模式）：
 * 只有当实体字段值为 {@code null} 时才会填充，如果已有值则保留原值。</p>
 *
 * @author clinic
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时自动填充 —— 在执行 insert 操作时被 MyBatis-Plus 回调。
     *
     * <p>自动填充以下字段：</p>
     * <ul>
     *   <li><b>createTime</b>：当前时间（记录创建时间）</li>
     *   <li><b>updateTime</b>：当前时间（首次插入时与 createTime 相同）</li>
     *   <li><b>deleted</b>：0（逻辑删除标记，0 表示未删除）</li>
     * </ul>
     *
     * @param metaObject MyBatis 元对象，包含当前正在操作的实体信息
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 严格插入填充：仅当 createTime 字段为 null 时才设置当前时间
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        // 严格插入填充：仅当 updateTime 字段为 null 时才设置当前时间
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        // 严格插入填充：仅当 deleted 字段为 null 时才设置默认值 0（未删除）
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    /**
     * 更新时自动填充 —— 在执行 update 操作时被 MyBatis-Plus 回调。
     *
     * <p>自动填充以下字段：</p>
     * <ul>
     *   <li><b>updateTime</b>：当前时间（记录最近一次更新时间）</li>
     * </ul>
     *
     * <p>注意：此处使用 {@code strictUpdateFill}，如果前端已传入 updateTime 值则不会被覆盖。</p>
     *
     * @param metaObject MyBatis 元对象，包含当前正在操作的实体信息
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 严格更新填充：仅当 updateTime 字段为 null 时才设置当前时间
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
