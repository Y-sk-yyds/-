package com.clinic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clinic.dto.DashboardVO;
import com.clinic.entity.*;
import com.clinic.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 仪表盘聚合服务——从各业务表拉取统计数据拼装首页所需数据
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RegistrationMapper registrationMapper;
    private final ConsultationMapper consultationMapper;
    private final BillMapper billMapper;
    private final DrugMapper drugMapper;
    private final DrugStockMapper drugStockMapper;
    private final DoctorMapper doctorMapper;
    private final ReminderService reminderService;
    private final PatientMapper patientMapper;

    // ----------------------------------------------------------------
    // 核心统计
    // ----------------------------------------------------------------

    /**
     * 组装完整的仪表盘数据
     */
    public DashboardVO build() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        return DashboardVO.builder()
                .todayRegistrations(countTodayRegistrations(today))
                .waitingConsultations(countWaiting())
                .completedToday(countCompletedToday(today))
                .unpaidBills(countUnpaidBills())
                .todayRegTrend(calcTrend(countTodayRegistrations(today), countTodayRegistrations(yesterday)))
                .waitingTrend(calcTrend(countWaiting(), countWaitingOnDate(yesterday)))
                .completedTrend(calcTrend(countCompletedToday(today), countCompletedToday(yesterday)))
                .unpaidTrend(calcTrend(countUnpaidBills(), countUnpaidBillsOnDate(yesterday)))
                .weeklyTrend(buildWeeklyTrend(today))
                .departmentDistribution(buildDeptDistribution())
                .todos(buildTodos())
                .build();
    }

    // ================================================================
    // 四项统计卡片
    // ================================================================

    /**
     * 今日挂号数 —— registration 表当天创建且非已取消
     */
    private Long countTodayRegistrations(LocalDate today) {
        return registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getRegistrationDate, today)
                .ne(Registration::getStatus, 0));
    }

    /**
     * 待就诊数 —— registration.status = 1
     */
    private Long countWaiting() {
        return registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getStatus, 1));
    }

    /**
     * 今日已完成就诊数 —— consultation.status = 2，且 endTime 在今天
     */
    private Long countCompletedToday(LocalDate today) {
        return consultationMapper.selectCount(new LambdaQueryWrapper<Consultation>()
                .eq(Consultation::getStatus, 2)
                .ge(Consultation::getEndTime, today.atStartOfDay())
                .lt(Consultation::getEndTime, today.plusDays(1).atStartOfDay()));
    }

    /**
     * 待收费账单数 —— bill.status = 0
     */
    private Long countUnpaidBills() {
        return billMapper.selectCount(new LambdaQueryWrapper<Bill>()
                .eq(Bill::getStatus, 0));
    }

    /**
     * 计算趋势百分比（今日 vs 昨日）
     * @return null 表示无对比数据（两天均为0），否则为变化百分比
     */
    private Integer calcTrend(long today, long yesterday) {
        if (today == 0 && yesterday == 0) return null;
        if (yesterday == 0) return null;
        return (int) Math.round((today - yesterday) * 100.0 / yesterday);
    }

    /**
     * 指定日期的待就诊数 —— registration.status = 1 且 registration_date = date
     */
    private Long countWaitingOnDate(LocalDate date) {
        return registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getStatus, 1)
                .eq(Registration::getRegistrationDate, date));
    }

    /**
     * 指定日期的待收费账单数 —— bill.status = 0 且 create_time 在当天
     */
    private Long countUnpaidBillsOnDate(LocalDate date) {
        return billMapper.selectCount(new LambdaQueryWrapper<Bill>()
                .eq(Bill::getStatus, 0)
                .ge(Bill::getCreateTime, date.atStartOfDay())
                .lt(Bill::getCreateTime, date.plusDays(1).atStartOfDay()));
    }

    // ================================================================
    // 图表数据
    // ================================================================

    /**
     * 近7日门诊趋势 —— 按 registration_date 分组统计每天的挂号数
     */
    private List<DashboardVO.TrendItem> buildWeeklyTrend(LocalDate today) {
        List<DashboardVO.TrendItem> trend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            Long count = registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                    .eq(Registration::getRegistrationDate, d)
                    .ne(Registration::getStatus, 0));
            trend.add(DashboardVO.TrendItem.builder()
                    .date(d.format(fmt))
                    .count(count)
                    .build());
        }
        return trend;
    }

    /**
     * 科室就诊占比 —— 通过已完成的 consultation 关联 doctor.department 分组计数
     */
    private List<DashboardVO.DeptItem> buildDeptDistribution() {
        List<Consultation> list = consultationMapper.selectList(new LambdaQueryWrapper<Consultation>()
                .eq(Consultation::getStatus, 2)
                .select(Consultation::getId, Consultation::getDoctorId));
        if (list.isEmpty()) return defaultDeptItems();

        // 按 doctorId 分组计数 → 查 doctor 取 department → 按科室汇总
        Map<Long, Long> doctorConsultCount = list.stream()
                .collect(Collectors.groupingBy(Consultation::getDoctorId, Collectors.counting()));
        List<Long> doctorIds = new ArrayList<>(doctorConsultCount.keySet());
        List<Doctor> doctors = doctorMapper.selectBatchIds(doctorIds);

        Map<String, Long> deptTotal = new java.util.LinkedHashMap<>();
        for (Doctor d : doctors) {
            String dept = d.getDepartment() != null ? d.getDepartment() : "其他";
            deptTotal.merge(dept, doctorConsultCount.getOrDefault(d.getId(), 0L), Long::sum);
        }

        if (deptTotal.isEmpty()) return defaultDeptItems();
        return deptTotal.entrySet().stream()
                .map(e -> DashboardVO.DeptItem.builder().name(e.getKey()).value(e.getValue()).build())
                .collect(Collectors.toList());
    }

    private List<DashboardVO.DeptItem> defaultDeptItems() {
        return List.of(
                DashboardVO.DeptItem.builder().name("内科").value(0L).build(),
                DashboardVO.DeptItem.builder().name("儿科").value(0L).build(),
                DashboardVO.DeptItem.builder().name("外科").value(0L).build(),
                DashboardVO.DeptItem.builder().name("其他").value(0L).build()
        );
    }

    // ================================================================
    // 待办提醒
    // ================================================================

    /**
     * 待办事项——药品库存预警 + 药品效期提醒 + 待收费提醒 + 复诊提醒
     *
     * <p>待办列表为实时聚合结果，不单独建表存储：每项都是由各业务表当前状态
     * 现查现拼出来的，因此管理员处理完对应问题（补货、更新效期、收费）后，
     * 待办项会自动消失。</p>
     */
    private List<DashboardVO.TodoItem> buildTodos() {
        List<DashboardVO.TodoItem> todos = new ArrayList<>();
        long idGen = 1;
        LocalDate today = LocalDate.now();

        // 药品库存预警
        List<DrugStock> stocks = drugStockMapper.selectList(new LambdaQueryWrapper<DrugStock>()
                .apply("current_stock < warning_threshold"));
        for (DrugStock s : stocks) {
            Drug drug = drugMapper.selectById(s.getDrugId());
            String name = drug != null ? drug.getName() : "未知药品";
            todos.add(DashboardVO.TodoItem.builder()
                    .id(idGen++).text("库存预警: " + name + " 仅剩 " + s.getCurrentStock() + " 盒")
                    .color("#E74C3C").time("现在").build());
        }

        // 药品近效期与已过期：距到期不足 NEAR_EXPIRY_DAYS 天的药品已被定时任务自动下架，
        // 此处统一提醒管理员处理（退药、换药或更新效期）。
        // 不按 status 过滤：下架后仍需持续提醒，直到药品被清理或效期被更新。
        List<Drug> expiryDrugs = drugMapper.selectList(new LambdaQueryWrapper<Drug>()
                .isNotNull(Drug::getExpireDate)
                .le(Drug::getExpireDate, today.plusDays(DrugExpiryScheduler.NEAR_EXPIRY_DAYS))
                .orderByAsc(Drug::getExpireDate));
        for (Drug d : expiryDrugs) {
            long remainDays = ChronoUnit.DAYS.between(today, d.getExpireDate());
            boolean expired = remainDays < 0;
            long absDays = Math.abs(remainDays);
            todos.add(DashboardVO.TodoItem.builder()
                    .id(idGen++)
                    .text((expired ? "药品已过期: " : "药品近效期: ") + d.getName()
                            + " 有效期至 " + d.getExpireDate()
                            + (expired ? "（已逾期 " + absDays + " 天）" : "（剩 " + absDays + " 天）")
                            + "，已自动下架并停止开具，请及时"
                            + (expired ? "清理库存" : "处理"))
                    .color(expired ? "#C0392B" : "#F39C12")
                    .time(expired ? ("已逾期 " + absDays + " 天") : ("剩 " + absDays + " 天"))
                    .build());
        }

        // 待收费提醒
        long unpaid = countUnpaidBills();
        if (unpaid > 0) {
            todos.add(DashboardVO.TodoItem.builder()
                    .id(idGen++).text("有 " + unpaid + " 笔处方待收费")
                    .color("#F39C12").time("现在").build());
        }

        // 复诊提醒（从 reminder 表读取待提醒记录）
        List<Reminder> reminders = reminderService.findAllPending();
        for (Reminder r : reminders) {
            Patient patient = patientMapper.selectById(r.getPatientId());
            String patientName = patient != null ? patient.getName() : "患者" + r.getPatientId();
            String dateStr = r.getRemindDate() != null ? r.getRemindDate().toString() : "待定";
            Doctor remindDoctor = doctorMapper.selectById(r.getDoctorId());
            String doctorName = remindDoctor != null ? remindDoctor.getName() : "";
            String doctorHint = doctorName.isEmpty() ? "" : "，建议复诊医生：" + doctorName;
            todos.add(DashboardVO.TodoItem.builder()
                    .id(idGen++)
                    .text("复诊提醒: " + patientName + " 应于 " + dateStr + " 复诊" + doctorHint)
                    .color("#2775B6").time(dateStr).build());
        }

        return todos;
    }
}
