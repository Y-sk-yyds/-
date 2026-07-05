package com.clinic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.clinic.common.Result;
import com.clinic.entity.Registration;
import com.clinic.entity.Schedule;
import com.clinic.mapper.RegistrationMapper;
import com.clinic.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 排班管理控制器 —— 处理医生排班信息的增删改查 HTTP 请求。
 *
 * <p>
 * 本控制器管理医生的出诊排班，包括排班日期、时段、可挂号人数等。
 * 排班是患者挂号的基础：只有当医生有对应排班时，患者才能挂号。
 * 每条排班记录包含总号源数（totalNum）和剩余号源数（remainNum），
 * 创建排班时剩余号源自动等于总号源，挂号后 remainNum 递减。
 * </p>
 *
 * <h3>请求路径前缀</h3>
 * <p>所有接口均位于 {@code /api/schedules} 路径下。</p>
 *
 * @see ScheduleService 排班服务层
 * @see Schedule 排班实体类，映射 schedules 数据库表
 */
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final RegistrationMapper registrationMapper;

    /**
     * 分页查询排班列表。
     *
     * <p>
     * 支持按医生 ID、日期范围进行筛选。
     * 常用于查看某位医生在某段时间内的出诊安排。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code GET /api/schedules?page=1&pageSize=10&doctorId=5&startDate=2025-01-01&endDate=2025-01-31}</pre>
     *
     * <h4>请求参数（Query String）</h4>
     * <ul>
     *   <li><b>page</b> (Integer, 可选, 默认 1) — 页码</li>
     *   <li><b>pageSize</b> (Integer, 可选, 默认 10) — 每页记录数</li>
     *   <li><b>doctorId</b> (Long, 可选) — 医生ID，筛选指定医生的排班</li>
     *   <li><b>startDate</b> (String, 可选, 格式 yyyy-MM-dd) — 排班开始日期</li>
     *   <li><b>endDate</b> (String, 可选, 格式 yyyy-MM-dd) — 排班结束日期</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <ul>
     *   <li>成功时返回 {@link Result}{@code <IPage<Schedule>>}，data 的 records 为排班列表</li>
     * </ul>
     *
     * @param page      页码，默认第 1 页
     * @param pageSize  每页条数，默认 10 条
     * @param doctorId  医生ID（可选）
     * @param startDate 排班起始日期（可选）
     * @param endDate   排班结束日期（可选）
     * @return 排班分页数据的统一响应结果
     */
    @GetMapping
    public Result<IPage<Schedule>> list(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                         @RequestParam(required = false) Long doctorId,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        // 委托 ScheduleService 执行带条件的分页查询
        return Result.ok(scheduleService.pageQuery(page, pageSize, doctorId, startDate, endDate));
    }

    /**
     * 新增排班信息。
     *
     * <p>
     * 创建一条排班记录。创建前检查同一医生+日期+时段是否已存在排班，
     * 防止重复排班。关键逻辑：创建时自动将 <b>剩余号源数（remainNum）</b>
     * 设置为与 <b>总号源数（totalNum）</b> 相同，表示初始状态所有号源均可用。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code POST /api/schedules}</pre>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>doctorId</b> (Long) — 医生ID</li>
     *   <li><b>workDate</b> (String, yyyy-MM-dd) — 排班日期</li>
     *   <li><b>period</b> (String) — 时段（AM=上午, PM=下午）</li>
     *   <li><b>totalNum</b> (Integer) — 总号源数</li>
     *   <li><b>status</b> (Integer) — 状态（1=正常, 0=停诊）</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param schedule 排班实体对象，由请求体 JSON 自动反序列化
     * @return 统一响应结果
     */
    @PostMapping
    public Result<Void> create(@RequestBody Schedule schedule) {
        // 检查同一医生+日期+时段是否已有排班，防止重复
        long count = scheduleService.count(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getDoctorId, schedule.getDoctorId())
                .eq(Schedule::getWorkDate, schedule.getWorkDate())
                .eq(Schedule::getPeriod, schedule.getPeriod()));
        if (count > 0) {
            return Result.error("该医生该时段已有排班，请勿重复创建");
        }
        // 创建排班时，剩余号源数初始化为总号源数
        schedule.setRemainNum(schedule.getTotalNum());
        scheduleService.save(schedule);
        return Result.ok();
    }

    /**
     * 更新排班信息。
     *
     * <p>
     * 根据传入排班对象的 ID 定位目标记录，更新可编辑字段（医生、日期、时段、
     * 总号源数、状态）。剩余号源数（remainNum）不会被覆盖，而是根据总号源数
     * 的变化量自动调整：
     * </p>
     * <ul>
     *   <li>增加总号源 → remainNum 同步增加</li>
     *   <li>减少总号源 → remainNum 同步减少，但不低于 0</li>
     * </ul>
     *
     * <h4>请求方式</h4>
     * <pre>{@code PUT /api/schedules}</pre>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>id</b> (Long, 必填) — 排班ID</li>
     *   <li><b>doctorId</b> (Long) — 医生ID</li>
     *   <li><b>workDate</b> (String) — 排班日期</li>
     *   <li><b>period</b> (String) — 时段（AM/PM）</li>
     *   <li><b>totalNum</b> (Integer) — 总号源数</li>
     *   <li><b>status</b> (Integer) — 状态（1=正常, 0=停诊）</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param schedule 排班实体对象，必须包含有效的 ID
     * @return 统一响应结果
     */
    @PutMapping
    public Result<Void> update(@RequestBody Schedule schedule) {
        // 获取数据库中已有记录，保护 remainNum 不被覆盖
        Schedule existing = scheduleService.getById(schedule.getId());
        if (existing == null) {
            return Result.error("排班不存在");
        }
        // 合并可编辑字段到已有记录上
        existing.setDoctorId(schedule.getDoctorId());
        existing.setWorkDate(schedule.getWorkDate());
        existing.setPeriod(schedule.getPeriod());
        existing.setStatus(schedule.getStatus());
        // 处理总号源变化：remainNum 同步增减
        int delta = schedule.getTotalNum() - existing.getTotalNum();
        existing.setTotalNum(schedule.getTotalNum());
        existing.setRemainNum(Math.max(0, Math.min(existing.getRemainNum() + delta, schedule.getTotalNum())));
        // 更新全部字段（此时 remainNum 已正确）
        scheduleService.updateById(existing);
        return Result.ok();
    }

    /**
     * 删除排班信息。
     *
     * <p>
     * 根据排班 ID 删除对应记录。删除前校验该排班下是否存在已挂号记录，
     * 如有则拒绝删除，必须先取消或处理所有挂号记录。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code DELETE /api/schedules/{id}}</pre>
     *
     * <h4>路径参数</h4>
     * <ul>
     *   <li><b>id</b> (Long) — 要删除的排班 ID</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param id 排班ID，来自 URL 路径变量
     * @return 统一响应结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 检查该排班下是否有已挂号记录（status=1），有则拒绝删除
        long regCount = registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getScheduleId, id)
                .eq(Registration::getStatus, 1));
        if (regCount > 0) {
            return Result.error("该排班下存在 " + regCount + " 条已挂号记录，请先取消挂号后再删除");
        }
        scheduleService.removeById(id);
        return Result.ok();
    }
}
