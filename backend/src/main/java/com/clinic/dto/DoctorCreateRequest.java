package com.clinic.dto;

import lombok.Data;

/**
 * <h2>医生创建请求 DTO</h2>
 *
 * <p>管理员在系统中新增医生时，前端提交的请求数据结构。
 * 包含医生的基本信息，如姓名、科室、职称、联系电话和个人简介。</p>
 *
 * <p>注意：此 DTO 仅包含用户可填写的业务信息，
 * 不包含系统自动生成的字段（如 ID、创建时间、逻辑删除标识等）。</p>
 *
 * <h3>请求示例</h3>
 * <pre>{@code
 * POST /api/doctors
 * Content-Type: application/json
 *
 * {
 *   "name": "李医生",
 *   "department": "内科",
 *   "title": "主任医师",
 *   "phone": "13800138000",
 *   "introduction": "从事内科临床工作20年，擅长心脑血管疾病的诊治。"
 * }
 * }</pre>
 *
 * @author clinic
 * @version 1.0
 */
@Data
public class DoctorCreateRequest {

    /**
     * 医生姓名
     */
    private String name;

    /**
     * 所属科室
     * <p>如"内科"、"外科"、"儿科"、"妇产科"等</p>
     */
    private String department;

    /**
     * 职称
     * <p>如"主任医师"、"副主任医师"、"主治医师"、"住院医师"等</p>
     */
    private String title;

    /**
     * 联系电话
     * <p>用于患者联系或系统通知</p>
     */
    private String phone;

    /**
     * 个人简介
     * <p>医生的专业背景、擅长领域等介绍信息，在医生详情页面展示</p>
     */
    private String introduction;
}
