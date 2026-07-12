package com.clinic.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.clinic.common.ForbiddenException;
import com.clinic.common.Result;
import com.clinic.dto.DoctorCreateRequest;
import com.clinic.entity.Doctor;
import com.clinic.security.SecurityUtils;
import com.clinic.service.DoctorService;
import com.clinic.vo.DoctorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 医生管理控制器 —— 处理医生信息的增删改查（CRUD）HTTP 请求。
 *
 * <p>
 * 本控制器管理医生的基本信息，包括姓名、科室、职称、简介等。
 * 创建医生时需同时关联系统用户账号（{@link DoctorCreateRequest}），
 * 确保每位医生都有对应的登录凭据。
 * 查询接口返回 {@link DoctorVO} 视图对象，其中包含了关联的用户信息。
 * </p>
 *
 * <h3>请求路径前缀</h3>
 * <p>所有接口均位于 {@code /api/doctors} 路径下。</p>
 *
 * @see DoctorService 医生服务层，封装医生创建、更新与删除的业务逻辑
 * @see Doctor 医生实体类，映射 doctors 数据库表
 * @see DoctorVO 医生视图对象，包含医生基本信息及关联的用户信息
 * @see DoctorCreateRequest 创建医生的请求 DTO，同时包含医生信息和用户账号信息
 */
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    /**
     * 医生服务 —— 由 Spring 通过 {@code @RequiredArgsConstructor} 自动注入。
     */
    private final DoctorService doctorService;

    private void requireRole(String... roles) {
        String currentRole = SecurityUtils.getCurrentRole();
        for (String role : roles) if (role.equals(currentRole)) return;
        throw new ForbiddenException("权限不足，当前角色：" + currentRole);
    }

    /**
     * 分页查询医生列表。
     *
     * <p>
     * 支持按姓名（模糊匹配）和科室进行筛选。
     * 返回 {@link DoctorVO} 视图对象列表，其中包含了医生的用户账号信息。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code GET /api/doctors?page=1&pageSize=10&name=李医生&department=内科}</pre>
     *
     * <h4>请求参数（Query String）</h4>
     * <ul>
     *   <li><b>page</b> (Integer, 可选, 默认 1) — 页码</li>
     *   <li><b>pageSize</b> (Integer, 可选, 默认 10) — 每页记录数</li>
     *   <li><b>name</b> (String, 可选) — 医生姓名，支持模糊搜索</li>
     *   <li><b>department</b> (String, 可选) — 科室名称，支持模糊搜索</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <ul>
     *   <li>成功时返回 {@link Result}{@code <IPage<DoctorVO>>}，data 的 records 为医生视图对象列表</li>
     * </ul>
     *
     * @param page       页码，默认第 1 页
     * @param pageSize   每页条数，默认 10 条
     * @param name       按医生姓名筛选（可选，模糊匹配）
     * @param department 按科室筛选（可选，模糊匹配）
     * @return 医生分页数据的统一响应结果
     */
    @GetMapping
    public Result<IPage<DoctorVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                         @RequestParam(required = false) String name,
                                         @RequestParam(required = false) String department) {
        // 委托 DoctorService 执行带条件的分页查询
        return Result.ok(doctorService.pageQuery(page, pageSize, name, department));
    }

    /**
     * 新增医生信息（同时创建关联的系统用户账号）。
     *
     * <p>
     * 该接口不仅创建医生基本信息记录，还会在系统用户表中创建一个对应的登录账号。
     * {@link DoctorCreateRequest} 中包含了医生的基本信息和用户账号信息
     * （用户名、密码等），由 {@link DoctorService#createDoctor(DoctorCreateRequest)}
     * 在一个事务中完成两张表的写入。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code POST /api/doctors}</pre>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>name</b> — 医生姓名</li>
     *   <li><b>department</b> — 所属科室</li>
     *   <li><b>title</b> — 职称</li>
     *   <li><b>specialty</b> — 专长描述</li>
     *   <li><b>introduction</b> — 个人简介</li>
     *   <li><b>username</b> — 登录用户名（系统自动或人工指定）</li>
     *   <li><b>password</b> — 登录密码</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param request 创建医生的请求对象，包含医生信息和用户账号信息
     * @return 统一响应结果
     */
    @PostMapping
    public Result<Void> create(@RequestBody DoctorCreateRequest request) {
        requireRole("ADMIN");
        // 委托 DoctorService 在事务中同时创建医生记录和用户账号
        doctorService.createDoctor(request);
        return Result.ok();
    }

    /**
     * 更新医生信息。
     *
     * <p>
     * 根据传入医生对象的 ID 定位目标记录，更新基本信息。
     * 注意：该接口不会自动更新关联的用户账号信息。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code PUT /api/doctors}</pre>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>id</b> (Long, 必填) — 医生ID</li>
     *   <li>其它需要更新的 {@link Doctor} 字段</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param doctor 医生实体对象，必须包含有效的 ID
     * @return 统一响应结果
     */
    @PutMapping
    public Result<Void> update(@RequestBody Doctor doctor) {
        requireRole("ADMIN");
        // 委托 DoctorService 执行更新，可能包含用户信息同步等额外逻辑
        doctorService.updateDoctor(doctor);
        return Result.ok();
    }

    /**
     * 删除医生信息。
     *
     * <p>
     * 根据医生 ID 删除医生记录及其关联的系统用户账号。
     * 注意：如果该医生已有排班、挂号等业务数据，删除操作可能会失败。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code DELETE /api/doctors/{id}}</pre>
     *
     * <h4>路径参数</h4>
     * <ul>
     *   <li><b>id</b> (Long) — 要删除的医生 ID</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param id 医生ID，来自 URL 路径变量
     * @return 统一响应结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        requireRole("ADMIN");
        // 委托 DoctorService 同时删除医生记录和关联用户账号
        doctorService.deleteDoctor(id);
        return Result.ok();
    }
}
