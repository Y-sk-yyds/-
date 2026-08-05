package com.clinic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clinic.common.Result;
import com.clinic.entity.*;
import com.clinic.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("doctorId", s.getDoctorId());
            m.put("doctorName", doctorMap.getOrDefault(s.getDoctorId(), new Doctor()).getName());
            m.put("department", doctorMap.getOrDefault(s.getDoctorId(), new Doctor()).getDepartment());
            m.put("workDate", s.getWorkDate());
            m.put("period", s.getPeriod());
            m.put("remainNum", s.getRemainNum());
            result.add(m);
        }
        return Result.ok(result);
    }

    /** 手机号挂号 */
    @PostMapping("/registrations")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        Long scheduleId = Long.valueOf(body.get("scheduleId"));

        Patient patient = patientService.getOne(
                new LambdaQueryWrapper<Patient>().eq(Patient::getPhone, phone));
        if (patient == null) throw new IllegalArgumentException("未找到该手机号对应的患者");

        Registration reg = registrationService.createRegistration(patient.getId(), scheduleId);
        billService.createBill(reg.getId(), null);
        Map<String, Object> result = Map.of("queueNum", reg.getQueueNum() != null ? reg.getQueueNum() : 0);
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

        // 挂号列表
        List<Registration> regs = registrationService.list(
                new LambdaQueryWrapper<Registration>()
                        .eq(Registration::getPatientId, patientId)
                        .orderByDesc(Registration::getCreateTime));

        // 账单（按挂号 ID 查）
        Map<Long, Bill> billMap = new HashMap<>();
        for (Registration r : regs) {
            Bill bill = billService.getOne(
                    new LambdaQueryWrapper<Bill>().eq(Bill::getRegistrationId, r.getId()));
            if (bill != null) billMap.put(r.getId(), bill);
        }

        // 病历
        List<MedicalRecord> records = medicalRecordService.list(
                new LambdaQueryWrapper<MedicalRecord>()
                        .eq(MedicalRecord::getPatientId, patientId)
                        .orderByDesc(MedicalRecord::getCreateTime));

        // 排队（第一个有效挂号的排班）
        List<Map<String, Object>> queue = new ArrayList<>();
        Registration active = regs.stream().filter(r -> r.getStatus() == 1).findFirst().orElse(null);
        if (active == null && !regs.isEmpty()) active = regs.get(0);
        if (active != null) {
            List<Registration> sameSched = registrationService.list(
                    new LambdaQueryWrapper<Registration>()
                            .eq(Registration::getScheduleId, active.getScheduleId()));
            Map<Long, String> patientNames = patientService.listByIds(
                    sameSched.stream().map(Registration::getPatientId).distinct().collect(Collectors.toList()))
                    .stream().collect(Collectors.toMap(Patient::getId, Patient::getName));
            // 查排班 → 医生信息
            Schedule schedule = scheduleService.getById(active.getScheduleId());
            String doctorName = "";
            String department = "";
            if (schedule != null) {
                Doctor doc = doctorService.getById(schedule.getDoctorId());
                if (doc != null) { doctorName = doc.getName(); department = doc.getDepartment(); }
            }
            List<Registration> sorted = sameSched.stream()
                    .sorted(Comparator.comparing(Registration::getQueueNum, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
            for (Registration r : sorted) {
                Map<String, Object> q = new LinkedHashMap<>();
                q.put("queueNum", r.getQueueNum());
                q.put("patientName", patientNames.getOrDefault(r.getPatientId(), ""));
                q.put("isMe", r.getPatientId().equals(patientId));
                q.put("status", r.getStatus());
                q.put("doctorName", doctorName);
                q.put("department", department);
                queue.add(q);
            }
        }

        // 组装结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patient", Map.of("id", patient.getId(), "name", patient.getName()));
        result.put("registrations", regs);
        result.put("bills", billMap);
        result.put("records", records);
        result.put("queue", queue);
        return Result.ok(result);
    }

    /** 手机端支付账单 */
    @PutMapping("/bills/{id}/pay")
    public Result<Void> payBill(@PathVariable Long id, @RequestBody Map<String, String> body) {
        billService.pay(id, body.getOrDefault("paymentMethod", "WECHAT"));
        return Result.ok();
    }
}
