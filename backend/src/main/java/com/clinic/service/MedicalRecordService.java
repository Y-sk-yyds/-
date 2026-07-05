package com.clinic.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clinic.entity.MedicalRecord;
import com.clinic.mapper.MedicalRecordMapper;
import com.clinic.utils.AesUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 病历服务类
 * <p>
 * 负责处理患者病历的管理，核心功能包括：
 * <ul>
 *   <li>分页查询病历记录（支持按患者ID筛选）</li>
 *   <li>创建病历：对敏感内容进行 AES 加密后存储</li>
 *   <li>查询解密病历：将加密内容解密后返回</li>
 * </ul>
 * </p>
 * <p>
 * <b>安全策略：</b>病历中的 content 字段（包含主诉、诊断、医嘱等敏感信息）
 * 在存储前使用 AES 对称加密，查询时需要解密后才能阅读。
 * 这样即使数据库被直接读取，也无法获取明文的病历内容。
 * </p>
 *
 * @author clinic
 */
@Service
@RequiredArgsConstructor
public class MedicalRecordService extends ServiceImpl<MedicalRecordMapper, MedicalRecord> {

    /** AES 加密工具类，用于病历内容的加解密 */
    private final AesUtils aesUtils;

    /**
     * 分页查询病历记录
     * <p>支持按患者ID筛选，结果按创建时间降序排列。</p>
     * <p><b>注意：</b>此方法返回的 content 字段是加密后的密文，
     * 如需明文内容请使用 {@link #getDecrypted(Long)} 方法。</p>
     *
     * @param page      页码
     * @param pageSize  每页大小
     * @param patientId 患者ID（可选）
     * @return 分页的病历记录
     */
    public IPage<MedicalRecord> pageQuery(Integer page, Integer pageSize, Long patientId) {
        return baseMapper.selectPage(new Page<>(page, pageSize),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MedicalRecord>()
                        .eq(patientId != null, MedicalRecord::getPatientId, patientId)
                        .orderByDesc(MedicalRecord::getCreateTime));
    }

    /**
     * 创建病历记录
     * <p>
     * 业务执行流程：
     * <ol>
     *   <li>构建病历实体对象，关联患者、就诊记录和医生</li>
     *   <li>将病历的 content 字段（包含主诉、诊断、医嘱等信息）通过 AES 加密</li>
     *   <li>保存加密后的病历记录到数据库</li>
     * </ol>
     * </p>
     * <p><b>加密说明：</b>content 字段使用 AES 对称加密算法加密，
     * 保证患者隐私数据在数据库中以密文形式存储。</p>
     *
     * @param patientId      患者ID
     * @param consultationId 就诊记录ID
     * @param doctorId       医生ID
     * @param content        病历内容（明文，将自动加密存储）
     * @param diagnosis      诊断结果（明文存储，用于统计查询）
     * @param treatmentPlan  治疗方案（明文存储，用于统计查询）
     * @return 创建的病历记录
     */
    @Transactional
    public MedicalRecord create(Long patientId, Long consultationId, Long doctorId,
                                 String content, String diagnosis, String treatmentPlan) {
        MedicalRecord record = new MedicalRecord();

        record.setPatientId(patientId);
        record.setConsultationId(consultationId);
        record.setDoctorId(doctorId);

        // ===== 敏感内容加密存储 =====
        // content 包含完整的主诉、诊断、医嘱等隐私信息，使用 AES 加密后存储
        record.setContent(aesUtils.encrypt(content));

        // diagnosis 和 treatmentPlan 为结构化字段，明文存储便于检索和统计
        record.setDiagnosis(diagnosis);
        record.setTreatmentPlan(treatmentPlan);

        save(record);
        return record;
    }

    /**
     * 查询并解密病历
     * <p>
     * 根据病历ID查询记录，并将 content 字段从密文解密为明文。
     * 此方法用于前端查看病历详情时调用。
     * </p>
     * <p><b>注意：</b>此方法会修改传入实体对象的 content 字段值，
     * 返回的对象中 content 已经是明文内容。</p>
     *
     * @param id 病历记录ID
     * @return 病历记录（content 已解密为明文），如果记录不存在或 content 为空则返回 null
     */
    public MedicalRecord getDecrypted(Long id) {
        MedicalRecord record = getById(id);
        if (record != null && record.getContent() != null) {
            // ===== 将密文 content 解密为明文 =====
            record.setContent(aesUtils.decrypt(record.getContent()));
        }
        return record;
    }
}
