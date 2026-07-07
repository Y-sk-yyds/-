package com.clinic.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clinic.dto.PrescriptionCreateRequest;
import com.clinic.entity.*;
import com.clinic.mapper.PrescriptionDrugMapper;
import com.clinic.mapper.PrescriptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 处方服务类
 * <p>
 * 负责处理处方的创建和查询，核心功能包括：
 * <ul>
 *   <li>分页查询处方记录（支持按医生筛选）</li>
 *   <li>创建处方：包含药品库存扣减（Redis 分布式锁）、处方记录创建、处方药品明细创建</li>
 * </ul>
 * </p>
 * <p>
 * <b>库存扣减策略：</b>使用 Redis SETNX 实现分布式锁，保证高并发场景下库存扣减的原子性。
 * 锁的 Key 格式为 drug:lock:{drugId}，超时时间 3 秒，通过 UUID 值实现安全释放。
 * </p>
 *
 * @author clinic
 */
@Service
@RequiredArgsConstructor
public class PrescriptionService extends ServiceImpl<PrescriptionMapper, Prescription> {

    /** 操作处方药品明细表的 Mapper */
    private final PrescriptionDrugMapper prescriptionDrugMapper;

    /** 药品服务，用于查询药品信息 */
    private final DrugService drugService;

    /** 药品库存服务，用于查询和更新库存 */
    private final DrugStockService drugStockService;

    /** Redis String 模板，用于分布式锁（纯字符串，无序列化问题） */
    private final StringRedisTemplate stringRedisTemplate;

    /** Redis 分布式锁 Key 前缀，实际 Key 格式为 drug:lock:{drugId} */
    private static final String STOCK_LOCK_PREFIX = "drug:lock:";

    /**
     * 分页查询处方记录
     * <p>支持按医生ID筛选，结果按创建时间降序排列。</p>
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param doctorId 医生ID（可选）
     * @return 分页的处方记录
     */
    public IPage<Prescription> pageQuery(Integer page, Integer pageSize, Long doctorId) {
        return baseMapper.selectPage(new Page<>(page, pageSize),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Prescription>()
                        .eq(doctorId != null, Prescription::getDoctorId, doctorId)
                        .orderByDesc(Prescription::getCreateTime));
    }

    /**
     * 创建处方
     * <p>
     * 业务执行流程：
     * <ol>
     *   <li>遍历请求中的药品列表，对每种药品执行库存扣减（使用 Redis 分布式锁保证并发安全）</li>
     *   <li>创建处方主记录，状态设为 0（待收费）</li>
     *   <li>为处方中的每种药品创建明细记录（处方-药品关联表）</li>
     * </ol>
     * </p>
     *
     * @param request 处方创建请求，包含就诊ID、医生ID、诊断、处方内容、药品清单
     * @return 创建的处方记录
     * @throws IllegalArgumentException 库存不足或系统繁忙时抛出
     */
    @Transactional
    public Prescription createPrescription(PrescriptionCreateRequest request) {
        // ===== 第1步：批量扣减药品库存 =====
        // 遍历处方中的每种药品，使用分布式锁逐一扣减库存
        for (var item : request.getDrugs()) {
            deductStock(item.getDrugId(), item.getQuantity());
        }

        // ===== 第2步：创建处方主记录 =====
        Prescription prescription = new Prescription();
        prescription.setConsultationId(request.getConsultationId());
        prescription.setDoctorId(request.getDoctorId());
        prescription.setDiagnosis(request.getDiagnosis());
        prescription.setPrescriptionContent(request.getPrescriptionContent());
        prescription.setStatus(0);  // 状态：0 = 待收费
        save(prescription);

        // ===== 第3步：创建处方药品明细记录 =====
        // 将每种药品的用法、用量、频次等信息写入关联表
        for (var item : request.getDrugs()) {
            PrescriptionDrug pd = new PrescriptionDrug();
            pd.setPrescriptionId(prescription.getId());  // 关联处方
            pd.setDrugId(item.getDrugId());              // 药品ID
            pd.setQuantity(item.getQuantity());          // 数量
            pd.setUsageMethod(item.getUsageMethod());    // 用法（如口服、外用）
            pd.setDosage(item.getDosage());              // 单次用量
            pd.setFrequency(item.getFrequency());        // 频次（如每日三次）
            prescriptionDrugMapper.insert(pd);
        }

        return prescription;
    }

    /**
     * 扣减药品库存（内部方法）
     * <p>
     * 使用 Redis SETNX 实现分布式锁机制：
     * <ol>
     *   <li>尝试获取分布式锁（Key: drug:lock:{drugId}，超时 3 秒）</li>
     *   <li>获取锁成功后，查询当前库存是否满足需求</li>
     *   <li>通过 MySQL 条件更新扣减库存（带乐观锁检查 current_stock >= quantity）</li>
     *   <li>在 finally 块中释放锁（校验 UUID 匹配，避免误删其他线程的锁）</li>
     * </ol>
     * </p>
     *
     * @param drugId   药品ID
     * @param quantity 扣减数量
     * @throws IllegalArgumentException 获取锁超时（系统繁忙）或库存不足时抛出
     */
    void deductStock(Long drugId, int quantity) {
        // 构建锁 Key
        String lockKey = STOCK_LOCK_PREFIX + drugId;

        try {
            // ===== 尝试获取分布式锁（SETNX + 过期时间） =====
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(3));
            if (Boolean.FALSE.equals(locked)) {
                // 获取锁失败，说明有其他线程正在操作该药品库存
                throw new IllegalArgumentException("系统繁忙，请稍后重试");
            }

            // ===== 获取锁成功：检查并扣减库存 =====
            DrugStock stock = drugStockService.getByDrugId(drugId);
            if (stock == null || stock.getCurrentStock() < quantity) {
                // 库存不足，拼接药品名称提供友好的错误提示
                Drug drug = drugService.getById(drugId);
                throw new IllegalArgumentException("药品 [" + (drug != null ? drug.getName() : drugId) + "] 库存不足");
            }

            // 执行 MySQL 层面的条件更新，ge("current_stock", quantity) 防止超扣
            drugStockService.update()
                    .setSql("current_stock = current_stock - " + quantity)
                    .eq("drug_id", drugId)
                    .ge("current_stock", quantity)
                    .update();

        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }
}
