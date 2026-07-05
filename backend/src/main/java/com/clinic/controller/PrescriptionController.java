package com.clinic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.clinic.common.Result;
import com.clinic.dto.PrescriptionCreateRequest;
import com.clinic.entity.Consultation;
import com.clinic.entity.Doctor;
import com.clinic.entity.Prescription;
import com.clinic.mapper.DoctorMapper;
import com.clinic.security.SecurityUtils;
import com.clinic.service.BillService;
import com.clinic.service.ConsultationService;
import com.clinic.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 处方管理控制器 —— 处理处方的创建和查询 HTTP 请求。
 *
 * <p>
 * 本控制器管理医生的处方开具流程，核心功能包括：
 * </p>
 * <ol>
 *   <li>医生在完成就诊后，为患者开具处方（{@link #create(PrescriptionCreateRequest)}）</li>
 *   <li>处方中可包含多种药品及其用量、用法</li>
 *   <li>开具处方时，系统<b>自动生成对应的账单</b>，
 *       方便后续收费流程</li>
 *   <li>分页查询处方列表，支持按医生筛选</li>
 * </ol>
 *
 * <p>
 * <b>权限控制</b>：创建处方时，系统会从安全上下文获取当前登录用户，
 * 验证其是否为医生角色，并自动设置 doctorId，防止前端伪造医生身份。
 * </p>
 *
 * <h3>请求路径前缀</h3>
 * <p>所有接口均位于 {@code /api/prescriptions} 路径下。</p>
 *
 * @see PrescriptionService 处方服务层，封装处方创建与查询的业务逻辑
 * @see BillService 账单服务，处方创建时自动调用生成账单
 * @see Prescription 处方实体类，映射 prescriptions 数据库表
 * @see PrescriptionCreateRequest 创建处方的请求 DTO
 */
@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    /**
     * 处方服务 —— 由 Spring 通过 {@code @RequiredArgsConstructor} 自动注入。
     */
    private final PrescriptionService prescriptionService;

    /**
     * 医生数据访问层 —— 用于根据当前登录用户查询对应的医生记录。
     */
    private final DoctorMapper doctorMapper;

    /**
     * 就诊服务 —— 用于在开具处方时查询关联的就诊信息。
     */
    private final ConsultationService consultationService;

    /**
     * 账单服务 —— 用于在开具处方时自动生成账单。
     */
    private final BillService billService;

    /**
     * 分页查询处方列表。
     *
     * <h4>请求方式</h4>
     * <pre>{@code GET /api/prescriptions?page=1&pageSize=10&doctorId=5}</pre>
     *
     * <h4>请求参数（Query String）</h4>
     * <ul>
     *   <li><b>page</b> (Integer, 可选, 默认 1) — 页码</li>
     *   <li><b>pageSize</b> (Integer, 可选, 默认 10) — 每页记录数</li>
     *   <li><b>doctorId</b> (Long, 可选) — 医生ID，筛选指定医生的处方</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <ul>
     *   <li>成功时返回 {@link Result}{@code <IPage<Prescription>>}，data 的 records 为处方列表</li>
     * </ul>
     *
     * @param page     页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @param doctorId 医生ID（可选）
     * @return 处方分页数据的统一响应结果
     */
    @GetMapping
    public Result<IPage<Prescription>> list(@RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                             @RequestParam(required = false) Long doctorId) {
        // 委托 PrescriptionService 执行带条件的分页查询
        return Result.ok(prescriptionService.pageQuery(page, pageSize, doctorId));
    }

    /**
     * 创建处方（开具处方）。
     *
     * <p>
     * 医生在完成就诊后调用此接口为患者开具处方。
     * 该接口的核心逻辑：
     * </p>
     * <ol>
     *   <li>从 Spring Security 上下文获取当前登录用户 ID</li>
     *   <li>查询对应的医生记录，验证当前用户是否为医生</li>
     *   <li>将 doctorId 强制设置为当前登录医生（安全措施：防止前端伪造）</li>
     *   <li>调用 {@link PrescriptionService#createPrescription(PrescriptionCreateRequest)} 创建处方</li>
     *   <li>查询关联的就诊记录，获取 registrationId</li>
     *   <li><b>自动调用 {@link BillService#createBill(Long, Long)} 生成账单</b>，
     *       将处方费用计入账单</li>
     * </ol>
     *
     * <h4>请求方式</h4>
     * <pre>{@code POST /api/prescriptions}</pre>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>consultationId</b> (Long, 必填) — 就诊记录 ID，标识该处方属于哪次就诊</li>
     *   <li><b>doctorId</b> (Long) — 医生ID（会被系统自动覆盖为当前登录医生）</li>
     *   <li><b>items</b> (Array) — 处方明细列表，每条包含：
     *     <ul>
     *       <li><b>drugId</b> — 药品ID</li>
     *       <li><b>drugName</b> — 药品名称</li>
     *       <li><b>quantity</b> — 数量</li>
     *       <li><b>usage</b> — 用法（如"口服"、"外用"）</li>
     *       <li><b>dosage</b> — 用量（如"每次 2 片"）</li>
     *       <li><b>frequency</b> — 频次（如"每日 3 次"）</li>
     *       <li><b>days</b> — 天数</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <ul>
     *   <li>成功时返回 {@link Result}{@code <Prescription>}，data 为新创建的处方记录</li>
     *   <li>当前用户不是医生时返回错误</li>
     * </ul>
     *
     * @param request 创建处方的请求对象
     * @return 包含新创建处方记录的统一响应结果
     * @throws IllegalArgumentException 如果当前用户不是医生
     */
    @PostMapping
    public Result<Prescription> create(@RequestBody PrescriptionCreateRequest request) {
        // 从当前登录用户获取医生ID（前端可能未传或不正确，强制覆盖以确保安全）
        Long userId = SecurityUtils.getCurrentUserId();
        Doctor doctor = doctorMapper.selectOne(
                new LambdaQueryWrapper<Doctor>().eq(Doctor::getUserId, userId));
        if (doctor == null) throw new IllegalArgumentException("当前用户不是医生");
        // 强制设置 doctorId，防止前端伪造
        request.setDoctorId(doctor.getId());

        // 委托 PrescriptionService 创建处方，包含处方明细的保存
        Prescription prescription = prescriptionService.createPrescription(request);

        // 处方开具后自动生成账单，将处方费用纳入收费流程
        Consultation consultation = consultationService.getById(prescription.getConsultationId());
        if (consultation != null && consultation.getRegistrationId() != null) {
            // 根据挂号ID和处方ID自动创建账单
            billService.createBill(consultation.getRegistrationId(), prescription.getId());
        }

        return Result.ok(prescription);
    }
}
