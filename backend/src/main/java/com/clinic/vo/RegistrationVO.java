package com.clinic.vo;

import com.clinic.entity.Registration;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <h2>挂号记录展示对象（VO）</h2>
 *
 * <p>继承自实体类 {@link Registration}，在挂号记录基础字段之上
 * 额外补充前端展示所需的冗余字段，避免前端需要多次请求关联数据。</p>
 *
 * <p>本类采用"宽表"设计思路，将患者姓名、医生姓名、科室等
 * 联表信息直接平铺到 VO 中，使前端只需一次请求即可获取
 * 挂号记录的完整展示信息。</p>
 *
 * <h3>继承字段（来自 Registration 实体）</h3>
 * <ul>
 *   <li>基础字段：id、patientId、doctorId、scheduleId、status 等</li>
 *   <li>BaseEntity 公共字段：createTime、updateTime、deleted</li>
 * </ul>
 *
 * <h3>扩展字段</h3>
 * <ul>
 *   <li><b>patientName</b> — 患者姓名（冗余自患者表）</li>
 *   <li><b>doctorName</b> — 医生姓名（冗余自医生表）</li>
 *   <li><b>department</b> — 科室名称（冗余自医生表或排班表）</li>
 *   <li><b>workDateStr</b> — 出诊日期（格式化字符串，方便前端直接展示）</li>
 *   <li><b>periodLabel</b> — 时段标签（如"上午"、"下午"）</li>
 * </ul>
 *
 * @author clinic
 * @version 1.0
 * @see Registration 父类挂号实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RegistrationVO extends Registration {

    /**
     * 患者姓名
     * <p>从患者表（t_patient）关联查询获取，用于挂号记录列表展示患者身份</p>
     */
    private String patientName;

    /**
     * 医生姓名
     * <p>从医生表（t_doctor）关联查询获取，用于挂号记录列表展示接诊医生</p>
     */
    private String doctorName;

    /**
     * 科室名称
     * <p>从医生表或排班表关联查询获取，如"内科"、"外科"等</p>
     */
    private String department;

    /**
     * 出诊日期（格式化后的字符串）
     * <p>如 "2024-01-15"，从排班表关联查询获取后格式化为字符串，
     * 避免前端再做日期格式化处理</p>
     */
    private String workDateStr;

    /**
     * 时段标签
     * <p>如"上午"、"下午"，从排班表关联查询获取，方便前端直接展示</p>
     */
    private String periodLabel;
}
