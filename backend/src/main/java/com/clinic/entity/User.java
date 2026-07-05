package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <h2>系统用户实体类</h2>
 *
 * <p>映射数据库表 {@code sys_user}，用于存储诊所管理系统的登录用户信息。
 * 支持三种角色：管理员（ADMIN）、医生（DOCTOR）、收银员（CASHIER）。</p>
 *
 * <p>继承自 {@link BaseEntity}，自动获得创建时间（createTime）、
 * 更新时间（updateTime）和逻辑删除标识（deleted）三个公共字段。</p>
 *
 * <h3>角色说明</h3>
 * <ul>
 *   <li><b>ADMIN</b> — 系统管理员，拥有最高权限</li>
 *   <li><b>DOCTOR</b> — 医生，可进行诊疗操作</li>
 *   <li><b>CASHIER</b> — 收银员，负责收费和退费操作</li>
 * </ul>
 *
 * <h3>数据库表结构对应关系</h3>
 * <table border="1">
 *   <tr><th>Java 字段</th><th>数据库列名</th><th>数据类型</th><th>说明</th></tr>
 *   <tr><td>id</td><td>id</td><td>BIGINT</td><td>主键，自增</td></tr>
 *   <tr><td>username</td><td>username</td><td>VARCHAR</td><td>登录用户名</td></tr>
 *   <tr><td>password</td><td>password</td><td>VARCHAR</td><td>登录密码（加密存储）</td></tr>
 *   <tr><td>realName</td><td>real_name</td><td>VARCHAR</td><td>真实姓名</td></tr>
 *   <tr><td>phone</td><td>phone</td><td>VARCHAR</td><td>联系电话</td></tr>
 *   <tr><td>role</td><td>role</td><td>VARCHAR</td><td>角色：ADMIN / DOCTOR / CASHIER</td></tr>
 *   <tr><td>status</td><td>status</td><td>INT</td><td>状态：1-启用，0-禁用</td></tr>
 * </table>
 *
 * @author clinic
 * @version 1.0
 * @see BaseEntity
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

    /**
     * 用户ID，主键，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 登录用户名，用于系统登录认证
     */
    private String username;

    /**
     * 登录密码，需经过加密（如 BCrypt）后再存储，不允许明文保存
     */
    private String password;

    /**
     * 用户真实姓名，用于界面显示和业务关联
     */
    private String realName;

    /**
     * 联系电话，可用于找回密码或接收系统通知
     */
    private String phone;

    /**
     * 用户角色，决定系统权限范围
     * <ul>
     *   <li>ADMIN — 系统管理员</li>
     *   <li>DOCTOR — 医生</li>
     *   <li>CASHIER — 收银员</li>
     * </ul>
     */
    private String role;

    /**
     * 账号状态
     * <ul>
     *   <li>1 — 启用，允许正常登录</li>
     *   <li>0 — 禁用，禁止登录系统</li>
     * </ul>
     */
    private Integer status;
}
