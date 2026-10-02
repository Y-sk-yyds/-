package com.clinic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clinic.common.Result;
import com.clinic.entity.*;
import com.clinic.mapper.ScheduleMapper;
import com.clinic.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 移动端患者服务控制器 —— 免登录，通过手机号验证患者身份
 */
@RestController
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
public class MobileController {

    private final ScheduleService scheduleService;
    /** 需要绕过逻辑删除取已归档排班（历史就诊回显就诊日期） */
    private final ScheduleMapper scheduleMapper;
    private final DoctorService doctorService;
    private final PatientService patientService;
    private final RegistrationService registrationService;
    private final BillService billService;
    private final MedicalRecordService medicalRecordService;

    /** 获取排班列表（含医生信息） */
    @GetMapping("/schedules")
    public Result<List<Map<String, Object>>> schedules(@RequestParam(required = false) String workDate) {
        LocalDate date = (workDate != null && !workDate.isBlank())
                ? LocalDate.parse(workDate) : LocalDate.now();
        LambdaQueryWrapper<Schedule> qw = new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getStatus, 1)
                .eq(Schedule::getWorkDate, date)
                .orderByAsc(Schedule::getPeriod);
        List<Schedule> list = scheduleService.list(qw);

        Map<Long, Doctor> doctorMap = doctorService.list().stream()
                .collect(Collectors.toMap(Doctor::getId, d -> d));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Schedule s : list) {
            Doctor doc = doctorMap.get(s.getDoctorId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("doctorId", s.getDoctorId());
            m.put("doctorName", doc != null ? doc.getName() : "");
            m.put("department", doc != null ? doc.getDepartment() : "");
            m.put("title", doc != null ? doc.getTitle() : "");
            // 挂号费：该医生定价，供移动端挂号前向患者明示费用
            m.put("registrationFee", doc != null ? doc.getRegistrationFee() : null);
            m.put("workDate", s.getWorkDate());
            m.put("period", s.getPeriod());
            m.put("remainNum", s.getRemainNum());
            result.add(m);
        }
        return Result.ok(result);
    }

    /**
     * 手机号挂号 —— 「先缴费，后挂号」一次调用完成
     *
     * <p>业务语义：挂号本身即含缴费动作，系统不提供"先挂号、稍后补缴"的中间态。
     * 执行顺序：</p>
     * <ol>
     *   <li>Redis Lua 原子扣减号源（超卖防护，论文核心点）</li>
     *   <li>生成挂号费单（金额取该医生定价）</li>
     *   <li>立即完成结算，账单置为已支付</li>
     * </ol>
     *
     * <p><b>失败回退</b>：第 2、3 步任一环节抛异常时，取消本次挂号并回补号源，
     * 保证不会出现"钱没收到、号却被占"或"已挂号但欠费"的脏数据。</p>
     *
     * <p>药费不在此处：需等医生开完处方才产生，届时另生成一张独立的药费单。</p>
     */
    @PostMapping("/registrations")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        Long scheduleId = Long.valueOf(body.get("scheduleId"));
        String paymentMethod = body.getOrDefault("paymentMethod", "WECHAT");

        Patient patient = patientService.getOne(
                new LambdaQueryWrapper<Patient>().eq(Patient::getPhone, phone));
        if (patient == null) throw new IllegalArgumentException("未找到该手机号对应的患者");

        Registration reg = registrationService.createRegistration(patient.getId(), scheduleId);
        Bill bill = null;
        try {
            bill = billService.createRegistrationBill(reg.getId());
            billService.pay(bill.getId(), paymentMethod);
        } catch (RuntimeException e) {
            // 缴费未完成 → 整体回退，不留下未缴费的挂号记录
            if (bill != null) billService.removeById(bill.getId());
            registrationService.cancelRegistration(reg.getId());
            throw e;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("queueNum", reg.getQueueNum() != null ? reg.getQueueNum() : 0);
        result.put("registrationId", reg.getId());
        result.put("billId", bill.getId());
        result.put("billAmount", bill.getTotalAmount());
        result.put("paymentMethod", paymentMethod);
        result.put("paid", true);
        return Result.ok(result);
    }

    /** 手机端患者自助建档 */
    @PostMapping("/patients")
    public Result<Map<String, Object>> createPatient(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String phone = body.get("phone");
        String gender = body.getOrDefault("gender", "M");
        String ageStr = body.getOrDefault("age", "0");

        if (name == null || name.isBlank() || phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("姓名和手机号为必填项");
        }

        Patient existing = patientService.getOne(
                new LambdaQueryWrapper<Patient>().eq(Patient::getPhone, phone));
        if (existing != null) {
            return Result.ok(Map.of("id", existing.getId(), "name", existing.getName(), "exists", true));
        }

        Patient patient = new Patient();
        patient.setName(name);
        patient.setPhone(phone);
        patient.setGender(gender);
        patient.setAge(Integer.parseInt(ageStr));
        patientService.save(patient);

        return Result.ok(Map.of("id", patient.getId(), "name", patient.getName(), "exists", false));
    }

    /** 我的全部数据：挂号列表 + 排队信息 + 账单 + 病历 */
    @GetMapping("/my-data")
    public Result<Map<String, Object>> myData(@RequestParam String phone) {
        Patient patient = patientService.getOne(
                new LambdaQueryWrapper<Patient>().eq(Patient::getPhone, phone));
        if (patient == null) return Result.error("未找到患者");

        Long patientId = patient.getId();
        LocalDate today = LocalDate.now();

        // ===== 1. 挂号列表 =====
        List<Registration> regs = registrationService.list(
                new LambdaQueryWrapper<Registration>()
                        .eq(Registration::getPatientId, patientId)
                        .orderByDesc(Registration::getCreateTime));

        // 批量取排班与医生，避免 N+1 查询
        Map<Long, Schedule> scheduleMap = new HashMap<>();
        Map<Long, Doctor> doctorMap = new HashMap<>();
        if (!regs.isEmpty()) {
            List<Long> scheduleIds = regs.stream()
                    .map(Registration::getScheduleId).distinct().collect(Collectors.toList());
            // ⚠️ 必须用 selectByIdsIncludeDeleted：历史挂号的排班已归档（deleted=1），
            //    普通 listByIds 会被逻辑删除拦截，导致就诊日期/时段/医生全部丢失
            List<Schedule> schedules = scheduleMapper.selectByIdsIncludeDeleted(scheduleIds);
            scheduleMap = schedules.stream().collect(Collectors.toMap(Schedule::getId, s -> s));
            List<Long> doctorIds = schedules.stream()
                    .map(Schedule::getDoctorId).distinct().collect(Collectors.toList());
            if (!doctorIds.isEmpty()) {
                doctorMap = doctorService.listByIds(doctorIds).stream()
                        .collect(Collectors.toMap(Doctor::getId, d -> d));
            }
        }

        // 账单：一次查出全部并按挂号 ID 分组
        // ⚠️ 不能用 getOne(registrationId)：同一挂号现在会有「挂号费单 + 药费单」两张，
        //    getOne 遇到多条结果会直接抛 TooManyResultsException
        Map<Long, List<Bill>> billMap = new HashMap<>();
        if (!regs.isEmpty()) {
            List<Long> regIds = regs.stream().map(Registration::getId).collect(Collectors.toList());
            List<Bill> allBills = billService.list(new LambdaQueryWrapper<Bill>()
                    .in(Bill::getRegistrationId, regIds)
                    .orderByAsc(Bill::getId));
            billMap = allBills.stream().collect(Collectors.groupingBy(Bill::getRegistrationId));
        }

        // ===== 2. 挂号视图：把"就诊日 / 时段 / 医生 / 缴费状态"一并交给移动端 =====
        // ⚠️ registration_date 是"挂号操作那天"，不等于"就诊那天"；
        //    移动端要按就诊日分组，必须用 schedule.work_date
        List<Map<String, Object>> regViews = new ArrayList<>();
        for (Registration r : regs) {
            Schedule s = scheduleMap.get(r.getScheduleId());
            Doctor doc = (s != null) ? doctorMap.get(s.getDoctorId()) : null;
            List<Bill> regBills = billMap.getOrDefault(r.getId(), Collections.emptyList());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("scheduleId", r.getScheduleId());
            m.put("queueNum", r.getQueueNum());
            m.put("status", r.getStatus());
            m.put("registrationDate", r.getRegistrationDate());
            m.put("workDate", s != null ? s.getWorkDate() : null);
            m.put("period", s != null ? s.getPeriod() : null);
            m.put("doctorName", doc != null ? doc.getName() : "");
            m.put("department", doc != null ? doc.getDepartment() : "");
            m.put("title", doc != null ? doc.getTitle() : "");
            // 逐笔账单（挂号费单 / 药费单），移动端分行展示、分别支付
            m.put("bills", regBills.stream().map(this::toBillView).collect(Collectors.toList()));

            // ---- 兼容字段（旧前端与分区判断仍会用）----
            // billAmount = 该挂号全部账单合计
            // billStatus = 只要还有未支付的单子就是 0（进「待缴费」），全付清才是 1
            // billId     = 优先指向未支付的那张，便于一键"去支付"
            final Bill firstUnpaid = regBills.stream()
                    .filter(b -> b.getStatus() != null && b.getStatus() == 0)
                    .findFirst().orElse(null);
            BigDecimal billSum = regBills.stream()
                    .map(b -> b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            m.put("billAmount", billSum);
            m.put("billStatus", firstUnpaid != null ? 0 : (regBills.isEmpty() ? null : 1));
            m.put("billId", firstUnpaid != null ? firstUnpaid.getId()
                    : (regBills.isEmpty() ? null : regBills.get(regBills.size() - 1).getId()));
            regViews.add(m);
        }

        // 排序：待就诊（今天及以后）优先且按日期升序；历史按日期倒序；同日 AM 在 PM 前
        regViews.sort((a, b) -> compareByDate((LocalDate) a.get("workDate"),
                (LocalDate) b.get("workDate"), a.get("period"), b.get("period"), today));

        // ===== 3. 病历 =====
        List<MedicalRecord> records = medicalRecordService.list(
                new LambdaQueryWrapper<MedicalRecord>()
                        .eq(MedicalRecord::getPatientId, patientId)
                        .orderByDesc(MedicalRecord::getCreateTime));

        // ===== 4. 排队看板：只取「今天」的待就诊排班 —— 上午(AM)/下午(PM)各一组 =====
        // 排队看板的场景是"当场等叫号"，历史与未来排班的队列既无参考价值、又会随挂号变化，
        // 因此后端只下发 work_date == 今天的组；患者其他日期的预约由前端另行提示。
        // 注意：scheduleMap 在上面被重新赋值过，lambda 里需引用一个 final 别名
        final Map<Long, Schedule> schedMap = scheduleMap;
        List<Long> activeScheduleIds = regs.stream()
                .filter(r -> r.getStatus() != null && r.getStatus() == 1)
                .map(Registration::getScheduleId)
                .distinct()
                .filter(sid -> {
                    Schedule s = schedMap.get(sid);
                    return s != null && today.equals(s.getWorkDate());
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> queueGroups = new ArrayList<>();
        if (!activeScheduleIds.isEmpty()) {
            // 一次查出这些排班下的全部非取消挂号（排队看板要显示同排班的其他患者）
            List<Registration> sameSched = registrationService.list(
                    new LambdaQueryWrapper<Registration>()
                            .in(Registration::getScheduleId, activeScheduleIds)
                            .ne(Registration::getStatus, 0));

            Map<Long, String> patientNames = patientService.listByIds(
                    sameSched.stream().map(Registration::getPatientId).distinct().collect(Collectors.toList()))
                    .stream().collect(Collectors.toMap(Patient::getId, Patient::getName));

            Map<Long, List<Registration>> grouped = sameSched.stream()
                    .collect(Collectors.groupingBy(Registration::getScheduleId));

            for (Long sid : activeScheduleIds) {
                Schedule s = scheduleMap.get(sid);
                if (s == null) continue;   // 排班已被删除时跳过
                Doctor doc = doctorMap.get(s.getDoctorId());

                List<Map<String, Object>> items = grouped.getOrDefault(sid, Collections.emptyList()).stream()
                        .sorted(Comparator.comparing(Registration::getQueueNum,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(r -> {
                            Map<String, Object> q = new LinkedHashMap<>();
                            q.put("queueNum", r.getQueueNum());
                            q.put("patientName", patientNames.getOrDefault(r.getPatientId(), ""));
                            q.put("isMe", r.getPatientId().equals(patientId));
                            q.put("status", r.getStatus());
                            return q;
                        })
                        .collect(Collectors.toList());

                Map<String, Object> g = new LinkedHashMap<>();
                g.put("scheduleId", sid);
                g.put("workDate", s.getWorkDate());
                g.put("period", s.getPeriod());
                g.put("doctorName", doc != null ? doc.getName() : "");
                g.put("department", doc != null ? doc.getDepartment() : "");
                g.put("title", doc != null ? doc.getTitle() : "");
                g.put("items", items);
                queueGroups.add(g);
            }

            queueGroups.sort((a, b) -> compareByDate((LocalDate) a.get("workDate"),
                    (LocalDate) b.get("workDate"), a.get("period"), b.get("period"), today));
        }

        // ===== 5. 组装结果 =====
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patient", Map.of("id", patient.getId(), "name", patient.getName()));
        result.put("registrations", regViews);
        result.put("bills", billMap);
        result.put("records", records);
        result.put("queueGroups", queueGroups);   // 替代原 queue：按排班分组
        result.put("today", today);               // 由服务端下发"今天"，避免移动端时区误差
        return Result.ok(result);
    }

    /**
     * 就诊排序规则：今天及以后优先（日期升序）→ 历史在后（日期倒序）；
     * 同一天内上午(AM)排在下午(PM)之前。日期缺失的一律排到最后。
     */
    private int compareByDate(LocalDate da, LocalDate db, Object pa, Object pb, LocalDate today) {
        if (da == null || db == null) {
            return da == null ? (db == null ? 0 : 1) : -1;
        }
        boolean aFuture = !da.isBefore(today);
        boolean bFuture = !db.isBefore(today);
        if (aFuture != bFuture) return aFuture ? -1 : 1;
        int cmp = aFuture ? da.compareTo(db) : db.compareTo(da);
        if (cmp != 0) return cmp;
        return periodOrder(pa) - periodOrder(pb);
    }

    /** 时段权重：AM=0, PM=1, 其他=2（用于同日排序） */
    private int periodOrder(Object period) {
        if ("AM".equals(period)) return 0;
        if ("PM".equals(period)) return 1;
        return 2;
    }

    /**
     * 账单视图对象：把费用类型与金额明细交给移动端
     *
     * <p>费用类型由 {@link BillService#resolveFeeType(Bill)} 依据金额字段推导，
     * 不依赖额外的数据库列：</p>
     * <ul>
     *   <li>{@code REGISTRATION} —— 挂号费单</li>
     *   <li>{@code DRUG} —— 药费单</li>
     *   <li>{@code MIXED} —— 混合单（历史数据）</li>
     * </ul>
     *
     * @param b 账单实体
     * @return 供前端渲染的账单视图
     */
    private Map<String, Object> toBillView(Bill b) {
        Map<String, Object> bm = new LinkedHashMap<>();
        bm.put("id", b.getId());
        bm.put("feeType", BillService.resolveFeeType(b));
        bm.put("registrationFee", b.getRegistrationFee());
        bm.put("drugFee", b.getDrugFee());
        bm.put("totalAmount", b.getTotalAmount());
        bm.put("status", b.getStatus());
        bm.put("paymentMethod", b.getPaymentMethod());
        return bm;
    }

    /** 手机端支付账单 */
    @PutMapping("/bills/{id}/pay")
    public Result<Void> payBill(@PathVariable Long id, @RequestBody Map<String, String> body) {
        billService.pay(id, body.getOrDefault("paymentMethod", "WECHAT"));
        return Result.ok();
    }
}
