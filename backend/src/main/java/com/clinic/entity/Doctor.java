package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <h2>医生实体类</h2>
 *
 * <p>映射数据库表 {@code doctor}，用于存储诊所医生的专业信息。
 * 每个医生关联一个系统用户（{@link User}），通过 {@code userId}
 * 实现医生信息与登录账号的绑定。</p>
 *
 * <p>继承自 {@link BaseEntity}，自动获得创建时间（createTime）、
 * 更新时间（updateTime）和逻辑删除标识（deleted）三个公共字段。</p>
 *
 * <h3>与用户的关联关系</h3>
 * <p>一个医生对应一个系统用户（{@code userId}），该用户的角色应为 DOCTOR。
 * 医生登录系统后，通过自身的 ID 查询关联的排班、就诊记录等业务数据。</p>
 *
 * <h3>数据库表结构对应关系</h3>
 * <table border="1">
 *   <tr><th>Java 字段</th><th>数据库列名</th><th>数据类型</th><th>说明</th></tr>
 *   <tr><td>id</td><td>id</td><td>BIGINT</td><td>主键，自增</td></tr>
 *   <tr><td>name</td><td>name</td><td>VARCHAR</td><td>医生姓名</td></tr>
 *   <tr><td>department</td><td>department</td><td>VARCHAR</td><td>所属科室</td></tr>
 *   <tr><td>title</td><td>title</td><td>VARCHAR</td><td>职称</td></tr>
 *   <tr><td>phone</td><td>phone</td><td>VARCHAR</td><td>联系电话</td></tr>
 *   <tr><td>introduction</td><td>introduction</td><td>VARCHAR</td><td>个人简介</td></tr>
 *   <tr><td>userId</td><td>user_id</td><td>BIGINT</td><td>关联系统用户ID</td></tr>
 * </table>
 *
 * @author clinic
 * @version 1.0
 * @see BaseEntity
 * @see User
 * @see Schedule
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("doctor")
public class Doctor extends BaseEntity {

    /**
     * 医生ID，主键，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 医生姓名，用于排班表显示和患者挂号选择
     */
    private String name;

    /**
     * 所属科室，如：内科、外科、儿科、妇科、中医科等
     */
    private String department;

    /**
     * 职称，如：主任医师、副主任医师、主治医师、住院医师
     */
    private String title;

    /**
     * 联系电话，用于排班调整等内部沟通
     */
    private String phone;

    /**
     * 个人简介，展示医生的专业背景、擅长领域等信息，
     * 供患者在挂号时作为选择医生的参考
     */
    private String introduction;

    /**
     * 关联的系统用户ID，指向 {@link User} 表的主键。
     * 该用户的角色应为 DOCTOR，用于医生登录和权限控制。
     */
    private Long userId;
}
