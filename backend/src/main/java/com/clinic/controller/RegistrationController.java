package com.clinic.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.clinic.common.Result;
import com.clinic.entity.Doctor;
import com.clinic.entity.Registration;
import com.clinic.mapper.DoctorMapper;
import com.clinic.security.SecurityUtils;
import com.clinic.service.RegistrationService;
import com.clinic.vo.RegistrationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 挂号管理控制器 —— 处理患者挂号的创建、查询和取消 HTTP 请求。
 *
 * <p>
 * 本控制器管理患者挂号流程，核心功能包括：
 * </p>
 * <ul>
 *   <li>患者选择医生排班进行挂号</li>
 *   <li>分页查询挂号记录，支持按患者、状态、医生筛选</li>
 *   <li>取消挂号（退号）</li>
 * </ul>
 *
 * <p>
 * <b>权限控制</b>：查询挂号列表时，如果前端未传入 doctorId 参数，
 * 系统会根据当前登录用户自动推断医生身份（若当前用户是医生），
 * 从而实现"医生只能查看自己名下的挂号记录"的数据隔离。
 * </p>
 *
 * <h3>请求路径前缀</h3>
 * <p>所有接口均位于 {@code /api/registrations} 路径下。</p>
 *
 * @see RegistrationService 挂号服务层，封装挂号创建与取消的核心业务逻辑
 * @see Registration 挂号实体类，映射 registrations 数据库表
 * @see RegistrationVO 挂号视图对象，包含关联的患者、医生、排班信息
 */
@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    /**
     * 挂号服务 —— 由 Spring 通过 {@code @RequiredArgsConstructor} 自动注入。
     */
    private final RegistrationService registrationService;

    /**
     * 医生数据访问层 —— 用于根据当前登录用户查询对应的医生记录。
     */
    private final DoctorMapper doctorMapper;

    /**
     * 分页查询挂号列表。
     *
     * <p>
     * 支持按患者ID、挂号状态和医生ID进行筛选。
     * <b>权限逻辑</b>：如果请求中未传入 doctorId 参数，
     * 系统会自动从安全上下文中获取当前登录用户 ID，
     * 查询其对应的医生记录，并将 doctorId 设置为该医生的 ID。
     * 这确保了医生角色登录后只能看到自己排班的挂号记录。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code GET /api/registrations?page=1&pageSize=10&patientId=3&status=0&doctorId=5}</pre>
     *
     * <h4>请求参数（Query String）</h4>
     * <ul>
     *   <li><b>page</b> (Integer, 可选, 默认 1) — 页码</li>
     *   <li><b>pageSize</b> (Integer, 可选, 默认 10) — 每页记录数</li>
     *   <li><b>patientId</b> (Long, 可选) — 患者ID</li>
     *   <li><b>status</b> (Integer, 可选) — 挂号状态（0-待就诊, 1-已就诊, 2-已取消）</li>
     *   <li><b>doctorId</b> (Long, 可选) — 医生ID，不传时自动从当前登录用户推断</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <ul>
     *   <li>成功时返回 {@link Result}{@code <IPage<RegistrationVO>>}，data 的 records 为挂号视图对象列表</li>
     * </ul>
     *
     * @param page      页码，默认第 1 页
     * @param pageSize  每页条数，默认 10 条
     * @param patientId 患者ID（可选）
     * @param status    挂号状态（可选）
     * @param doctorId  医生ID（可选），不传时自动推断
     * @return 挂号分页数据的统一响应结果
     */
    @GetMapping
    public Result<IPage<RegistrationVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                             @RequestParam(required = false) Long patientId,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(required = false) Long doctorId) {
        // 医生角色数据隔离：如果前端未传 doctorId，则从当前登录用户推断
        if (doctorId == null) {
            // 从 Spring Security 上下文获取当前登录用户的 ID
            Long userId = SecurityUtils.getCurrentUserId();
            // 在 Doctor 表中查询 userId 对应的医生记录
            Doctor doctor = doctorMapper.selectOne(
                    new LambdaQueryWrapper<Doctor>().eq(Doctor::getUserId, userId));
            // 如果当前用户确实是医生，则只显示该医生的挂号
            if (doctor != null) doctorId = doctor.getId();
        }
        // 委托 RegistrationService 执行带条件的分页查询
        return Result.ok(registrationService.pageQuery(page, pageSize, patientId, status, doctorId));
    }

    /**
     * 创建挂号记录。
     *
     * <p>
     * 患者选择某个医生的排班时段进行挂号。
     * 系统会校验号源是否充足，挂号成功后自动扣减对应排班的剩余号源数。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code POST /api/registrations}</pre>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>patientId</b> (Long, 必填) — 患者ID</li>
     *   <li><b>scheduleId</b> (Long, 必填) — 排班ID，标识要挂号的医生排班时段</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <ul>
     *   <li>成功时返回 {@link Result}{@code <Registration>}，data 为新创建的挂号记录</li>
     *   <li>号源不足时返回错误信息</li>
     * </ul>
     *
     * @param body 请求体 Map，包含 patientId 和 scheduleId
     * @return 包含新创建挂号记录的统一响应结果
     */
    @PostMapping
    public Result<Registration> create(@RequestBody Map<String, Long> body) {
        // 从请求体中提取患者ID和排班ID
        Long patientId = body.get("patientId");
        Long scheduleId = body.get("scheduleId");
        // 委托 RegistrationService 执行挂号逻辑（含号源校验和扣减）
        return Result.ok(registrationService.createRegistration(patientId, scheduleId));
    }

    /**
     * 取消挂号（退号）。
     *
     * <p>
     * 将指定挂号记录的状态变更为"已取消"，
     * 同时恢复对应排班的剩余号源数，使号源可以被其他患者使用。
     * 通常只有待就诊状态的挂号可以取消，已就诊的无法取消。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code PUT /api/registrations/{id}/cancel}</pre>
     *
     * <h4>路径参数</h4>
     * <ul>
     *   <li><b>id</b> (Long) — 要取消的挂号记录 ID</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param id 挂号记录ID，来自 URL 路径变量
     * @return 统一响应结果
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        // 委托 RegistrationService 执行退号逻辑（状态变更 + 恢复号源）
        registrationService.cancelRegistration(id);
        return Result.ok();
    }
}
