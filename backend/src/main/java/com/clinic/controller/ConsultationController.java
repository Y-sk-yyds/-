package com.clinic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.clinic.common.ForbiddenException;
import com.clinic.common.Result;
import com.clinic.config.RabbitMQConfig;
import com.clinic.dto.ReminderMessage;
import com.clinic.entity.Consultation;
import com.clinic.entity.Doctor;
import com.clinic.entity.Registration;
import com.clinic.mapper.DoctorMapper;
import com.clinic.security.SecurityUtils;
import com.clinic.service.ConsultationService;
import com.clinic.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 就诊管理控制器 —— 处理就诊记录的开始、完成和查询 HTTP 请求。
 *
 * <p>
 * 本控制器管理患者的就诊流程，是诊所核心业务流程之一：
 * </p>
 * <ol>
 *   <li>患者挂号后，医生可以<b>开始就诊</b>（{@link #start(Long)}），
 *       系统创建就诊记录并将挂号状态更新为"就诊中"</li>
 *   <li>医生完成诊断后，调用<b>完成就诊</b>（{@link #complete(Long, Map)}），
 *       填写主诉、诊断意见和治疗建议</li>
 *   <li>查询就诊记录时，医生角色只能查看自己的就诊记录（数据隔离）</li>
 * </ol>
 *
 * <h3>请求路径前缀</h3>
 * <p>所有接口均位于 {@code /api/consultations} 路径下。</p>
 *
 * @see ConsultationService 就诊服务层，封装就诊流程的核心业务逻辑
 * @see Consultation 就诊实体类，映射 consultations 数据库表
 */
@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    /**
     * 就诊服务 —— 由 Spring 通过 {@code @RequiredArgsConstructor} 自动注入。
     */
    private final ConsultationService consultationService;
    private final DoctorMapper doctorMapper;
    private final RegistrationService registrationService;
    private final RabbitTemplate rabbitTemplate;

    private void requireRole(String... roles) {
        String currentRole = SecurityUtils.getCurrentRole();
        for (String role : roles) if (role.equals(currentRole)) return;
        throw new ForbiddenException("权限不足，当前角色：" + currentRole);
    }

    /**
     * 分页查询就诊列表。
     *
     * <p>
     * <b>权限控制</b>：系统自动从安全上下文获取当前登录用户，
     * 查询其对应的医生记录，并强制将 doctorId 设置为该医生的 ID。
     * 这意味着无论前端传入什么 doctorId，医生登录后只能看到自己的就诊记录，
     * 实现了严格的数据隔离。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code GET /api/consultations?page=1&pageSize=10&status=1}</pre>
     *
     * <h4>请求参数（Query String）</h4>
     * <ul>
     *   <li><b>page</b> (Integer, 可选, 默认 1) — 页码</li>
     *   <li><b>pageSize</b> (Integer, 可选, 默认 10) — 每页记录数</li>
     *   <li><b>doctorId</b> (Long, 可选) — 医生ID（会被系统自动覆盖为当前登录医生）</li>
     *   <li><b>status</b> (Integer, 可选) — 就诊状态（0-就诊中, 1-已完成）</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <ul>
     *   <li>成功时返回 {@link Result}{@code <IPage<Consultation>>}，data 的 records 为就诊记录列表</li>
     * </ul>
     *
     * @param page     页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @param doctorId 医生ID（可选，会被自动覆盖）
     * @param status   就诊状态（可选）
     * @return 就诊分页数据的统一响应结果
     */
    @GetMapping
    public Result<IPage<Consultation>> list(@RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                             @RequestParam(required = false) Long doctorId,
                                             @RequestParam(required = false) Integer status) {
        requireRole("ADMIN", "DOCTOR");
        // 当前医生只能看自己的就诊记录——强制使用当前登录医生ID覆盖参数
        Long userId = SecurityUtils.getCurrentUserId();
        Doctor doctor = doctorMapper.selectOne(
                new LambdaQueryWrapper<Doctor>().eq(Doctor::getUserId, userId));
        if (doctor != null) {
            // 强制覆盖 doctorId，确保数据隔离
            doctorId = doctor.getId();
        }
        // 委托 ConsultationService 执行带条件的分页查询
        return Result.ok(consultationService.pageQuery(page, pageSize, doctorId, status));
    }

    /**
     * 开始就诊。
     *
     * <p>
     * 医生根据挂号记录 ID 开始对患者进行诊疗。
     * 系统会校验当前登录用户是否为医生角色，非医生用户调用会抛出异常。
     * 就诊开始后，对应的挂号状态会被更新为"就诊中"。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code PUT /api/consultations/{registrationId}/start}</pre>
     *
     * <h4>路径参数</h4>
     * <ul>
     *   <li><b>registrationId</b> (Long) — 挂号记录 ID，标识本次就诊对应的挂号</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <ul>
     *   <li>成功时返回 {@link Result}{@code <Consultation>}，data 为新创建的就诊记录</li>
     *   <li>当前用户不是医生时返回错误</li>
     * </ul>
     *
     * @param registrationId 挂号记录ID，来自 URL 路径变量
     * @return 包含新创建就诊记录的统一响应结果
     * @throws IllegalArgumentException 如果当前用户不是医生
     */
    @PutMapping("/{registrationId}/start")
    public Result<Consultation> start(@PathVariable Long registrationId) {
        // 获取当前登录用户ID，并查询对应的医生记录
        Long userId = SecurityUtils.getCurrentUserId();
        Doctor doctor = doctorMapper.selectOne(
                new LambdaQueryWrapper<Doctor>().eq(Doctor::getUserId, userId));
        // 非医生用户禁止进行就诊操作
        if (doctor == null) throw new IllegalArgumentException("当前用户不是医生");
        // 委托 ConsultationService 创建就诊记录并更新挂号状态
        return Result.ok(consultationService.startConsultation(registrationId, doctor.getId()));
    }

    /**
     * 完成就诊。
     *
     * <p>
     * 医生结束诊疗后，填写主诉（complaint）、诊断意见（diagnosis）和治疗建议（advice）。
     * 就诊状态将被更新为"已完成"。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code PUT /api/consultations/{id}/complete}</pre>
     *
     * <h4>路径参数</h4>
     * <ul>
     *   <li><b>id</b> (Long) — 就诊记录 ID</li>
     * </ul>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>complaint</b> (String) — 主诉，患者描述的症状</li>
     *   <li><b>diagnosis</b> (String) — 诊断意见，医生的诊断结论</li>
     *   <li><b>advice</b> (String) — 治疗建议，医生给出的治疗方案</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param id   就诊记录ID，来自 URL 路径变量
     * @param body 请求体 Map，包含主诉、诊断意见和治疗建议
     * @return 统一响应结果
     */
    /** 匹配医嘱中的数字+时间单位，支持中英文数字，用于推算复诊日期 */
    private static final Pattern FOLLOW_UP_PATTERN =
            Pattern.compile("(\\d+|[一二三四五六七八九十]|半)\\s*(天|周|月|日|分钟|小时)");

    /** 中文数字 → 阿拉伯数字映射 */
    private static final Map<String, Integer> CN_NUM = Map.of(
            "一", 1, "两", 2, "三", 3, "四", 4, "五", 5,
            "六", 6, "七", 7, "八", 8, "九", 9, "十", 10);

    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id, @RequestBody Map<String, String> body) {
        requireRole("ADMIN", "DOCTOR");
        String complaint = body.get("complaint");
        String diagnosis = body.get("diagnosis");
        String advice = body.get("advice");
        consultationService.completeConsultation(id, complaint, diagnosis, advice);

        // 异步发送复诊提醒到 RabbitMQ
        Consultation consultation = consultationService.getById(id);
        if (consultation != null) {
            Registration reg = registrationService.getById(consultation.getRegistrationId());
            Long patientId = reg != null ? reg.getPatientId() : null;
            LocalDate remindDate = parseRemindDate(advice);
            if (remindDate != null && patientId != null) {
                ReminderMessage msg = new ReminderMessage(id, patientId,
                        consultation.getDoctorId(), diagnosis, advice, remindDate);
                rabbitTemplate.convertAndSend(RabbitMQConfig.REMINDER_EXCHANGE,
                        RabbitMQConfig.REMINDER_ROUTING_KEY, msg);
            }
        }
        return Result.ok();
    }

    /** 从医嘱文本中提取复诊时间，返回推算日期；无匹配则返回 null */
    private LocalDate parseRemindDate(String advice) {
        if (advice == null) return null;
        // 特殊词快捷匹配："今天/今日复诊" → 当天，"明天/明日复诊" → 明天
        if (advice.contains("今天") || advice.contains("今日")) return LocalDate.now();
        if (advice.contains("明天") || advice.contains("明日")) return LocalDate.now().plusDays(1);
        Matcher m = FOLLOW_UP_PATTERN.matcher(advice);
        if (m.find()) {
            String numStr = m.group(1);
            String unit = m.group(2);
            // 识别数字：阿拉伯数字直接解析，中文数字查映射表，"半"=0.5天
            int num;
            if ("半".equals(numStr)) {
                num = 0; // 半周 = 3.5天，取整为3
            } else if (CN_NUM.containsKey(numStr)) {
                num = CN_NUM.get(numStr);
            } else {
                num = Integer.parseInt(numStr);
            }
            // 根据时间单位计算日期
            return switch (unit) {
                case "分钟", "小时" -> LocalDate.now(); // 小时/分钟级当天提醒
                case "天", "日" -> LocalDate.now().plusDays(num);
                case "周" -> "半".equals(numStr)
                        ? LocalDate.now().plusDays(3)  // 半周 ≈ 3天后
                        : LocalDate.now().plusWeeks(num);
                case "月" -> "半".equals(numStr)
                        ? LocalDate.now().plusDays(15) // 半月 ≈ 15天后
                        : LocalDate.now().plusMonths(num);
                default -> null;
            };
        }
        return null;
    }
}
