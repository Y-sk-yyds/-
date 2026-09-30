package com.clinic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clinic.entity.*;
import com.clinic.mapper.BillMapper;
import com.clinic.mapper.DoctorMapper;
import com.clinic.mapper.PrescriptionDrugMapper;
import com.clinic.mapper.ScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 账单服务类
 * <p>
 * 负责处理账单的创建和支付，核心功能包括：
 * <ul>
 *   <li>分页查询账单记录（支持按支付状态筛选）</li>
 *   <li>创建账单：自动计算挂号费 + 药品费用 = 总金额</li>
 *   <li>支付账单：更新支付状态并同步更新处方状态</li>
 * </ul>
 * </p>
 * <p>
 * <b>费用构成：</b>总金额 = 挂号费（按出诊医生的定价，10.00 ~ 50.00 元） + 药品费（药品单价 × 数量累加）
 * <br>
 * <b>状态流转：</b>账单状态 0(待支付) → 1(已支付)；处方状态 0(待收费) → 1(已收费)
 * </p>
 *
 * @author clinic
 */
@Service
@RequiredArgsConstructor
public class BillService extends ServiceImpl<BillMapper, Bill> {

    /** 操作账单表的 Mapper */
    private final BillMapper billMapper;

    /** 操作处方药品明细表的 Mapper，用于计算药费 */
    private final PrescriptionDrugMapper prescriptionDrugMapper;

    /** 操作排班表的 Mapper，用于由挂号记录定位出诊医生 */
    private final ScheduleMapper scheduleMapper;

    /** 操作医生表的 Mapper，用于读取该医生的挂号费定价 */
    private final DoctorMapper doctorMapper;

    /** 药品服务，用于查询药品单价 */
    private final DrugService drugService;

    /** 挂号服务，用于关联查询挂号信息 */
    private final RegistrationService registrationService;

    /** 处方服务，用于支付时同步更新处方状态 */
    private final PrescriptionService prescriptionService;

    /**
     * 挂号费兜底值（元）
     * <p>当医生未配置挂号费（字段为 NULL）或配置为非法值（≤ 0）时使用，
     * 确保账单的挂号费与总金额永远不会出现 NULL 或负数。</p>
     */
    private static final BigDecimal DEFAULT_REGISTRATION_FEE = new BigDecimal("10.00");

    /**
     * 分页查询账单记录
     * <p>支持按支付状态筛选，结果按创建时间降序排列。</p>
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param status   支付状态（可选，0=待支付, 1=已支付）
     * @return 分页的账单记录
     */
    public IPage<Bill> pageQuery(Integer page, Integer pageSize, Integer status) {
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Bill::getStatus, status);
        }
        wrapper.orderByDesc(Bill::getCreateTime);
        return baseMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    /**
     * 创建账单
     * <p>
     * 业务执行流程：
     * <ol>
     *   <li>查询挂号记录，获取就诊基本信息</li>
     *   <li>按出诊医生定价确定挂号费：挂号 → 排班 → 医生 → registration_fee</li>
     *   <li>如果有处方，查询处方关联的所有药品明细，
     *       累加计算药品费用：∑(药品单价 × 数量)</li>
     *   <li>总金额 = 挂号费 + 药品费</li>
     *   <li>保存账单记录，状态设为 0（待支付）</li>
     * </ol>
     * </p>
     *
     * @param registrationId 挂号记录ID
     * @param prescriptionId 处方ID（可选，纯挂号无处方时可传 null）
     * @return 创建的账单记录
     */
    @Transactional
    public Bill createBill(Long registrationId, Long prescriptionId) {
        // 挂号时已生成账单，开处方时更新已有账单加药费
        Bill bill = getOne(new LambdaQueryWrapper<Bill>().eq(Bill::getRegistrationId, registrationId));
        boolean exists = bill != null;
        if (!exists) {
            bill = new Bill();
            bill.setRegistrationId(registrationId);
            // 挂号费按出诊医生的定价确定，在挂号那一刻固化写入账单。
            // 此后医生调价不影响本账单，保证患者按挂号当时的价格结算。
            bill.setRegistrationFee(resolveRegistrationFee(registrationId));
            bill.setStatus(0);
        }

        bill.setPrescriptionId(prescriptionId);

        // ===== 计算药费 =====
        BigDecimal drugFee = BigDecimal.ZERO;
        if (prescriptionId != null) {
            List<PrescriptionDrug> items = prescriptionDrugMapper.selectList(
                    new LambdaQueryWrapper<PrescriptionDrug>().eq(PrescriptionDrug::getPrescriptionId, prescriptionId));
            for (PrescriptionDrug item : items) {
                Drug drug = drugService.getById(item.getDrugId());
                drugFee = drugFee.add(drug.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        bill.setDrugFee(drugFee);

        // ===== 计算总金额 =====
        // 总金额 = 挂号费 + 药品费
        bill.setTotalAmount(bill.getRegistrationFee().add(drugFee));

        // 已存在则更新，不存在则插入
        saveOrUpdate(bill);

        return bill;
    }

    /**
     * 解析某次挂号应收的挂号费
     *
     * <p>取值链路：</p>
     * <pre>
     * registration.scheduleId → schedule.doctorId → doctor.registration_fee
     * </pre>
     *
     * <p>任一层查询不到、或医生未配置有效价格（NULL 或 ≤ 0）时，
     * 统一回退为 {@link #DEFAULT_REGISTRATION_FEE}（10.00 元），
     * 保证账单金额字段永不为 NULL。</p>
     *
     * @param registrationId 挂号记录ID
     * @return 该次挂号应收取的挂号费（元）
     */
    private BigDecimal resolveRegistrationFee(Long registrationId) {
        Registration registration = registrationService.getById(registrationId);
        if (registration == null || registration.getScheduleId() == null) {
            return DEFAULT_REGISTRATION_FEE;
        }

        Schedule schedule = scheduleMapper.selectById(registration.getScheduleId());
        if (schedule == null || schedule.getDoctorId() == null) {
            return DEFAULT_REGISTRATION_FEE;
        }

        Doctor doctor = doctorMapper.selectById(schedule.getDoctorId());
        if (doctor == null || doctor.getRegistrationFee() == null
                || doctor.getRegistrationFee().compareTo(BigDecimal.ZERO) <= 0) {
            return DEFAULT_REGISTRATION_FEE;
        }

        return doctor.getRegistrationFee();
    }

    /**
     * 支付账单
     * <p>
     * 业务执行流程：
     * <ol>
     *   <li>校验账单记录是否存在且状态为"待支付"(status=0)</li>
     *   <li>记录支付方式，更新账单状态为"已支付"(status=1)</li>
     *   <li>如果有处方，同步更新处方状态为"已收费"(status=1)</li>
     * </ol>
     * </p>
     *
     * @param billId        账单ID
     * @param paymentMethod 支付方式（如微信、支付宝、现金等）
     * @throws IllegalArgumentException 账单状态异常（不存在或已支付）时抛出
     */
    @Transactional
    public void pay(Long billId, String paymentMethod) {
        // ===== 第1步：校验账单状态 =====
        Bill bill = getById(billId);
        if (bill == null || bill.getStatus() != 0) {
            throw new IllegalArgumentException("账单状态异常");
        }

        // ===== 第2步：记录支付信息并更新状态 =====
        bill.setPaymentMethod(paymentMethod);  // 记录支付方式
        bill.setStatus(1);                     // 状态：1 = 已支付
        updateById(bill);

        // ===== 第3步：同步更新处方状态为"已收费" =====
        // 仅当账单关联了处方时才执行此步骤
        if (bill.getPrescriptionId() != null) {
            prescriptionService.update()
                    .set("status", 1)            // 状态：1 = 已收费
                    .eq("id", bill.getPrescriptionId())
                    .update();
        }
    }
}
