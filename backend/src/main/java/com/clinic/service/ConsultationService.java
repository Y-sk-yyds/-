package com.clinic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clinic.entity.Consultation;
import com.clinic.entity.Registration;
import com.clinic.entity.Schedule;
import com.clinic.mapper.ConsultationMapper;
import com.clinic.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 就诊/接诊服务类
 * <p>
 * 负责处理医生接诊和完成就诊的核心业务流程，包括：
 * <ul>
 *   <li>分页查询就诊记录（支持按医生、状态筛选）</li>
 *   <li>开始接诊：校验挂号归属、更新挂号状态、创建就诊记录</li>
 *   <li>完成就诊：填写主诉/诊断/医嘱、自动生成病历</li>
 * </ul>
 * </p>
 * <p>
 * <b>状态流转：</b>挂号状态 1(已挂号) → 2(已就诊)；就诊状态 1(就诊中) → 2(已完成)
 * </p>
 *
 * @author clinic
 */
@Service
@RequiredArgsConstructor
public class ConsultationService extends ServiceImpl<ConsultationMapper, Consultation> {

    /** 操作就诊表的 Mapper */
    private final ConsultationMapper consultationMapper;

    /** 挂号服务，用于查询和更新挂号记录 */
    private final RegistrationService registrationService;

    /** 排班服务，用于校验挂号归属医生 */
    private final ScheduleService scheduleService;

    /** 病历服务，用于就诊完成时自动生成病历 */
    private final MedicalRecordService medicalRecordService;

    /**
     * 分页查询就诊记录
     * <p>
     * 支持按医生ID和就诊状态筛选，结果按状态升序（待处理的优先）和创建时间降序排列。
     * </p>
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param doctorId 医生ID（可选，筛选特定医生的就诊）
     * @param status   就诊状态（可选，1=就诊中, 2=已完成）
     * @return 分页的就诊记录
     */
    public IPage<Consultation> pageQuery(Integer page, Integer pageSize, Long doctorId, Integer status) {
        LambdaQueryWrapper<Consultation> wrapper = new LambdaQueryWrapper<>();

        // 按医生ID筛选（可选参数）
        if (doctorId != null) {
            wrapper.eq(Consultation::getDoctorId, doctorId);
        }
        // 按就诊状态筛选（可选参数）
        if (status != null) {
            wrapper.eq(Consultation::getStatus, status);
        }

        // 按状态升序+创建时间降序排列：让"就诊中"的记录排在最前面
        wrapper.orderByAsc(Consultation::getStatus).orderByDesc(Consultation::getCreateTime);
        return baseMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    /**
     * 开始接诊
     * <p>
     * 医生在叫号后点击"开始接诊"触发此方法，业务流程：
     * <ol>
     *   <li>校验挂号记录是否存在且状态为"已挂号"(status=1)</li>
     *   <li>通过挂号关联的排班信息，校验该挂号是否属于当前医生</li>
     *   <li>将挂号状态更新为"已就诊"(status=2)</li>
     *   <li>创建就诊记录，状态设为"就诊中"(status=1)，记录接诊开始时间</li>
     * </ol>
     * </p>
     *
     * @param registrationId 挂号记录ID
     * @param doctorId       当前医生的ID
     * @return 创建的就诊记录
     * @throws IllegalArgumentException 挂号状态异常或不属于该医生时抛出
     */
    @Transactional
    public Consultation startConsultation(Long registrationId, Long doctorId) {
        // ===== 第1步：校验挂号记录状态 =====
        Registration reg = registrationService.getById(registrationId);
        if (reg == null || reg.getStatus() != 1) {
            throw new IllegalArgumentException("挂号状态异常，无法接诊");
        }

        // ===== 第2步：校验挂号归属 —— 该挂号是否属于当前医生的排班 =====
        Schedule schedule = scheduleService.getById(reg.getScheduleId());
        if (schedule == null || !schedule.getDoctorId().equals(doctorId)) {
            throw new IllegalArgumentException("该挂号不属于您的排班，无法接诊");
        }

        // ===== 第3步：更新挂号状态为"已就诊" =====
        reg.setStatus(2);
        registrationService.updateById(reg);

        // ===== 第4步：创建就诊记录 =====
        Consultation consultation = new Consultation();
        consultation.setRegistrationId(registrationId);
        consultation.setDoctorId(doctorId);
        consultation.setStatus(1);                  // 状态：1 = 就诊中
        consultation.setStartTime(LocalDateTime.now()); // 记录接诊开始时间
        save(consultation);

        return consultation;
    }

    /**
     * 完成就诊
     * <p>
     * 医生填写完主诉、诊断和医嘱后点击"完成就诊"，业务流程：
     * <ol>
     *   <li>校验就诊记录是否存在且状态为"就诊中"(status=1)</li>
     *   <li>更新就诊记录：填写主诉、诊断、医嘱，状态改为"已完成"(status=2)，记录结束时间</li>
     *   <li>自动生成病历记录，将主诉、诊断、医嘱拼接为病历内容</li>
     * </ol>
     * </p>
     *
     * @param id        就诊记录ID
     * @param complaint 主诉（患者描述的症状）
     * @param diagnosis 诊断结果
     * @param advice    医嘱/治疗建议
     * @throws IllegalArgumentException 就诊状态异常时抛出
     */
    @Transactional
    public void completeConsultation(Long id, String complaint, String diagnosis, String advice) {
        // ===== 第1步：校验就诊记录状态 =====
        Consultation consultation = getById(id);
        if (consultation == null || consultation.getStatus() != 1) {
            throw new IllegalArgumentException("就诊状态异常");
        }

        // ===== 第2步：完善就诊信息并标记完成 =====
        consultation.setComplaint(complaint);          // 主诉
        consultation.setDiagnosis(diagnosis);          // 诊断
        consultation.setAdvice(advice);                // 医嘱
        consultation.setStatus(2);                     // 状态：2 = 已完成
        consultation.setEndTime(LocalDateTime.now());  // 记录就诊结束时间
        updateById(consultation);

        // ===== 第3步：自动生成病历 =====
        // 通过就诊记录关联查询挂号，获取患者ID
        Registration reg = registrationService.getById(consultation.getRegistrationId());
        if (reg != null) {
            // 将主诉、诊断、医嘱拼接为结构化的病历内容
            String content = (complaint != null ? "主诉: " + complaint + "\n" : "")
                    + (diagnosis != null ? "诊断: " + diagnosis + "\n" : "")
                    + (advice != null ? "医嘱: " + advice : "");

            // 创建病历记录，内容会经过 AES 加密后存储
            medicalRecordService.create(reg.getPatientId(), consultation.getId(),
                    consultation.getDoctorId(), content, diagnosis, advice);
        }
    }
}
