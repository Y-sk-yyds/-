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
     * 创建账单（兼容入口，按阶段自动分流）
     *
     * <p><b>为什么要分流：</b>挂号费在挂号那一刻就要缴清，药费要等医生开完处方才产生。
     * 若两者混在同一张账单里，患者先付掉挂号费后，开方时把药费追加进来，
     * 账单金额变了但状态仍是"已支付"——药费会被白送。因此拆成两张独立的账单。</p>
     *
     * <ul>
     *   <li>{@code prescriptionId == null} —— 挂号阶段，走 {@link #createRegistrationBill(Long)}</li>
     *   <li>{@code prescriptionId != null} —— 开方阶段，走 {@link #createDrugBill(Long, Long)}</li>
     * </ul>
     *
     * @param registrationId 挂号记录ID
     * @param prescriptionId 处方ID（纯挂号无处方时传 null）
     * @return 创建或复用的账单记录
     */
    @Transactional
    public Bill createBill(Long registrationId, Long prescriptionId) {
        return prescriptionId == null
                ? createRegistrationBill(registrationId)
                : createDrugBill(registrationId, prescriptionId);
    }

    /**
     * 创建/复用「挂号费单」
     *
     * <p>挂号费单的识别方式：{@code registration_id = 本次挂号} 且
     * {@code prescription_id IS NULL}。挂号费在挂号那一刻按出诊医生定价固化写入，
     * 此后医生调价不影响本账单。</p>
     *
     * <p>同一挂号只会有一张挂号费单：重复调用（如重复提交）直接复用已有记录，
     * 不会重复计费。</p>
     *
     * @param registrationId 挂号记录ID
     * @return 挂号费单（新建或已存在的）
     */
    @Transactional
    public Bill createRegistrationBill(Long registrationId) {
        // 同一挂号只应存在一张挂号费单（prescription_id 为 NULL 的那张）
        // 第二个参数 false：即使历史数据异常出现多条也不抛异常
        Bill bill = getOne(new LambdaQueryWrapper<Bill>()
                .eq(Bill::getRegistrationId, registrationId)
                .isNull(Bill::getPrescriptionId), false);
        if (bill != null) {
            return bill;
        }

        bill = new Bill();
        bill.setRegistrationId(registrationId);
        // 挂号费按出诊医生的定价确定，在挂号那一刻固化写入账单。
        // 此后医生调价不影响本账单，保证患者按挂号当时的价格结算。
        bill.setRegistrationFee(resolveRegistrationFee(registrationId));
        bill.setDrugFee(BigDecimal.ZERO);          // 挂号阶段还没有药费
        bill.setTotalAmount(bill.getRegistrationFee());
        bill.setStatus(0);                          // 0 = 待支付

        save(bill);
        return bill;
    }

    /**
     * 创建/更新「药费单」
     *
     * <p>药费单的识别方式：{@code registration_id = 本次挂号} 且
     * {@code prescription_id = 本次处方}。药费单的 {@code registration_fee} 恒为 0，
     * 总金额只等于药费——挂号费绝不混入本单，因此挂号费的支付状态与药费互不影响。</p>
     *
     * @param registrationId 挂号记录ID
     * @param prescriptionId 处方ID（必填）
     * @return 药费单（新建或更新后的）
     */
    @Transactional
    public Bill createDrugBill(Long registrationId, Long prescriptionId) {
        Bill bill = getOne(new LambdaQueryWrapper<Bill>()
                .eq(Bill::getRegistrationId, registrationId)
                .eq(Bill::getPrescriptionId, prescriptionId), false);

        // 药费单已收费则不再改金额（防止处方重复提交导致金额漂移）
        if (bill != null && bill.getStatus() != null && bill.getStatus() == 1) {
            return bill;
        }

        BigDecimal drugFee = calcDrugFee(prescriptionId);

        if (bill == null) {
            bill = new Bill();
            bill.setRegistrationId(registrationId);
            bill.setPrescriptionId(prescriptionId);
            bill.setRegistrationFee(BigDecimal.ZERO);   // 药费单不含挂号费
            bill.setStatus(0);
        }

        bill.setDrugFee(drugFee);
        bill.setTotalAmount(drugFee);                   // 总金额 = 药费（不含挂号费）
        saveOrUpdate(bill);
        return bill;
    }

    /**
     * 计算某张处方的药费合计：∑(药品单价 × 数量)
     *
     * @param prescriptionId 处方ID
     * @return 药费合计（元），无明细时为 0
     */
    private BigDecimal calcDrugFee(Long prescriptionId) {
        BigDecimal drugFee = BigDecimal.ZERO;
        if (prescriptionId == null) {
            return drugFee;
        }
        List<PrescriptionDrug> items = prescriptionDrugMapper.selectList(
                new LambdaQueryWrapper<PrescriptionDrug>().eq(PrescriptionDrug::getPrescriptionId, prescriptionId));
        for (PrescriptionDrug item : items) {
            Drug drug = drugService.getById(item.getDrugId());
            if (drug == null || drug.getUnitPrice() == null || item.getQuantity() == null) {
                continue;
            }
            drugFee = drugFee.add(drug.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return drugFee;
    }

    /**
     * 推断账单的费用类型（不依赖额外的数据库列，由金额字段推导）
     *
     * <ul>
     *   <li>{@code REGISTRATION} —— 挂号费单（只有挂号费，药费为 0）</li>
     *   <li>{@code DRUG} —— 药费单（只有药费，挂号费为 0）</li>
     *   <li>{@code MIXED} —— 混合单（历史数据：挂号费与药费在同一笔账单里）</li>
     * </ul>
     *
     * @param bill 账单
     * @return 费用类型代码
     */
    public static String resolveFeeType(Bill bill) {
        boolean hasRegistration = bill.getRegistrationFee() != null
                && bill.getRegistrationFee().compareTo(BigDecimal.ZERO) > 0;
        boolean hasDrug = bill.getDrugFee() != null
                && bill.getDrugFee().compareTo(BigDecimal.ZERO) > 0;
        if (hasRegistration && hasDrug) return "MIXED";
        if (hasDrug) return "DRUG";
        return "REGISTRATION";
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
