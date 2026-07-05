package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <h2>患者实体类</h2>
 *
 * <p>映射数据库表 {@code patient}，用于存储就诊患者的基本信息。
 * 患者是诊所业务的核心对象，关联挂号、就诊、病历、收费等全部业务流程。</p>
 *
 * <p>继承自 {@link BaseEntity}，自动获得创建时间（createTime）、
 * 更新时间（updateTime）和逻辑删除标识（deleted）三个公共字段。</p>
 *
 * <h3>核心字段说明</h3>
 * <ul>
 *   <li><b>idCard</b> — 身份证号，用于唯一标识患者身份</li>
 *   <li><b>allergyHistory</b> — 过敏史，医生开药时的重要参考信息</li>
 *   <li><b>gender</b> — 性别，M 表示男性，F 表示女性</li>
 * </ul>
 *
 * <h3>数据库表结构对应关系</h3>
 * <table border="1">
 *   <tr><th>Java 字段</th><th>数据库列名</th><th>数据类型</th><th>说明</th></tr>
 *   <tr><td>id</td><td>id</td><td>BIGINT</td><td>主键，自增</td></tr>
 *   <tr><td>name</td><td>name</td><td>VARCHAR</td><td>患者姓名</td></tr>
 *   <tr><td>idCard</td><td>id_card</td><td>VARCHAR</td><td>身份证号</td></tr>
 *   <tr><td>phone</td><td>phone</td><td>VARCHAR</td><td>联系电话</td></tr>
 *   <tr><td>age</td><td>age</td><td>INT</td><td>年龄</td></tr>
 *   <tr><td>gender</td><td>gender</td><td>VARCHAR</td><td>性别：M-男，F-女</td></tr>
 *   <tr><td>allergyHistory</td><td>allergy_history</td><td>VARCHAR</td><td>过敏史</td></tr>
 *   <tr><td>address</td><td>address</td><td>VARCHAR</td><td>家庭住址</td></tr>
 * </table>
 *
 * @author clinic
 * @version 1.0
 * @see BaseEntity
 * @see Registration
 * @see MedicalRecord
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("patient")
public class Patient extends BaseEntity {

    /**
     * 患者ID，主键，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 患者姓名
     */
    private String name;

    /**
     * 身份证号，18位公民身份号码，用于患者身份唯一性校验
     */
    private String idCard;

    /**
     * 联系电话，用于预约通知、随访联系等
     */
    private String phone;

    /**
     * 年龄（岁），用于辅助诊断和用药剂量参考
     */
    private Integer age;

    /**
     * 性别
     * <ul>
     *   <li>M — 男性（Male）</li>
     *   <li>F — 女性（Female）</li>
     * </ul>
     */
    private String gender;

    /**
     * 过敏史，记录患者已知的药物或食物过敏情况。
     * 医生在开具处方时应首先查看此信息，避免开出过敏药物。
     */
    private String allergyHistory;

    /**
     * 家庭住址，用于建档和必要时与患者联系
     */
    private String address;
}
