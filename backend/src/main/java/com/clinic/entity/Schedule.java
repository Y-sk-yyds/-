package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * <h2>排班实体类</h2>
 *
 * <p>映射数据库表 {@code schedule}，用于管理医生的出诊排班信息。
 * 每个排班记录定义了某位医生在特定日期的上午或下午时段的号源情况。</p>
 *
 * <p>继承自 {@link BaseEntity}，自动获得创建时间（createTime）、
 * 更新时间（updateTime）和逻辑删除标识（deleted）三个公共字段。</p>
 *
 * <h3>号源管理逻辑</h3>
 * <ul>
 *   <li>{@code totalNum} — 该时段的总号源数，由管理员在排班时设定</li>
 *   <li>{@code remainNum} — 剩余可挂号数，患者每次挂号成功后减 1</li>
 *   <li>当 {@code remainNum} 为 0 时，患者无法再挂该时段的号</li>
 * </ul>
 *
 * <h3>排班状态</h3>
 * <ul>
 *   <li><b>1</b> — 正常出诊，患者可正常挂号</li>
 *   <li><b>0</b> — 停诊，该时段不可挂号</li>
 * </ul>
 *
 * <h3>数据库表结构对应关系</h3>
 * <table border="1">
 *   <tr><th>Java 字段</th><th>数据库列名</th><th>数据类型</th><th>说明</th></tr>
 *   <tr><td>id</td><td>id</td><td>BIGINT</td><td>主键，自增</td></tr>
 *   <tr><td>doctorId</td><td>doctor_id</td><td>BIGINT</td><td>医生ID，外键关联 doctor 表</td></tr>
 *   <tr><td>workDate</td><td>work_date</td><td>DATE</td><td>出诊日期</td></tr>
 *   <tr><td>period</td><td>period</td><td>VARCHAR</td><td>时段：AM-上午，PM-下午</td></tr>
 *   <tr><td>totalNum</td><td>total_num</td><td>INT</td><td>总号源数量</td></tr>
 *   <tr><td>remainNum</td><td>remain_num</td><td>INT</td><td>剩余号源数量</td></tr>
 *   <tr><td>status</td><td>status</td><td>INT</td><td>状态：1-正常，0-停诊</td></tr>
 * </table>
 *
 * @author clinic
 * @version 1.0
 * @see BaseEntity
 * @see Doctor
 * @see Registration
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("schedule")
public class Schedule extends BaseEntity {

    /**
     * 排班ID，主键，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 医生ID，外键关联 {@link Doctor} 表的主键，
     * 标识该排班属于哪位医生
     */
    private Long doctorId;

    /**
     * 出诊日期，精确到天。
     * 与 {@code period} 字段组合，构成一个唯一的出诊时段。
     */
    private LocalDate workDate;

    /**
     * 出诊时段
     * <ul>
     *   <li>AM — 上午（如 08:00-12:00）</li>
     *   <li>PM — 下午（如 14:00-18:00）</li>
     * </ul>
     */
    private String period;

    /**
     * 总号源数，管理员在排班时设定的该时段最大可挂号人数
     */
    private Integer totalNum;

    /**
     * 剩余号源数，实时反映当前还可挂号的数量。
     * 患者成功挂号后减 1，取消挂号后加 1。
     * 当剩余号源为 0 时，系统拒绝该时段的挂号请求。
     */
    private Integer remainNum;

    /**
     * 排班状态
     * <ul>
     *   <li>1 — 正常出诊，患者可正常挂号</li>
     *   <li>0 — 停诊，该时段不可挂号（如医生临时请假）</li>
     * </ul>
     */
    private Integer status;
}
