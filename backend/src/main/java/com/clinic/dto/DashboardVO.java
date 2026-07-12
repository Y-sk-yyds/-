package com.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 仪表盘数据 VO——聚合首页统计/图表/待办
 * <p>
 * GET /api/dashboard 返回体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardVO {

    /** 今日挂号数 */
    private Long todayRegistrations;
    /** 待就诊数（已挂号未接诊） */
    private Long waitingConsultations;
    /** 本日已完成就诊数 */
    private Long completedToday;
    /** 待收费账单数 */
    private Long unpaidBills;

    /** 今日挂号趋势（今日 vs 昨日变化百分比，null 表示无对比数据） */
    private Integer todayRegTrend;
    /** 待就诊趋势 */
    private Integer waitingTrend;
    /** 已完成趋势 */
    private Integer completedTrend;
    /** 待收费趋势 */
    private Integer unpaidTrend;

    /** 近7日门诊趋势 */
    private List<TrendItem> weeklyTrend;
    /** 科室就诊分布 */
    private List<DeptItem> departmentDistribution;
    /** 待办提醒 */
    private List<TodoItem> todos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendItem {
        private String date;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeptItem {
        private String name;
        private Long value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodoItem {
        private Long id;
        private String text;
        private String color;
        private String time;
    }
}
