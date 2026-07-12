package com.clinic.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.clinic.common.ForbiddenException;
import com.clinic.common.Result;
import com.clinic.entity.Patient;
import com.clinic.security.SecurityUtils;
import com.clinic.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 患者管理控制器 —— 处理患者信息的增删改查（CRUD）HTTP 请求。
 *
 * <p>
 * 本控制器提供患者基本信息的创建、修改、删除和分页查询功能。
 * 患者是诊所业务的核心实体之一，挂号、就诊、处方等流程均关联患者信息。
 * </p>
 *
 * <h3>请求路径前缀</h3>
 * <p>所有接口均位于 {@code /api/patients} 路径下。</p>
 *
 * @see PatientService 患者服务层，封装患者数据的业务逻辑
 * @see Patient 患者实体类，映射 patients 数据库表
 */
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    /**
     * 患者服务 —— 由 Spring 通过 {@code @RequiredArgsConstructor} 自动注入。
     */
    private final PatientService patientService;

    private void requireRole(String... roles) {
        String currentRole = SecurityUtils.getCurrentRole();
        for (String role : roles) if (role.equals(currentRole)) return;
        throw new ForbiddenException("权限不足，当前角色：" + currentRole);
    }

    /**
     * 分页查询患者列表。
     *
     * <p>
     * 支持按姓名（模糊匹配）和手机号进行筛选。
     * 返回分页对象，包含总记录数、当前页数据等信息。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code GET /api/patients?page=1&pageSize=10&name=张三&phone=13800138000}</pre>
     *
     * <h4>请求参数（Query String）</h4>
     * <ul>
     *   <li><b>page</b> (Integer, 可选, 默认 1) — 页码，从 1 开始</li>
     *   <li><b>pageSize</b> (Integer, 可选, 默认 10) — 每页记录数</li>
     *   <li><b>name</b> (String, 可选) — 患者姓名，支持模糊搜索</li>
     *   <li><b>phone</b> (String, 可选) — 患者手机号，支持模糊搜索</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <ul>
     *   <li>成功时返回 {@link Result}{@code <IPage<Patient>>}，data 中包含：
     *     <ul>
     *       <li><b>records</b> — 当前页患者列表</li>
     *       <li><b>total</b> — 总记录数</li>
     *       <li><b>size</b> — 每页大小</li>
     *       <li><b>current</b> — 当前页码</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param page     页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @param name     按姓名筛选（可选，模糊匹配）
     * @param phone    按手机号筛选（可选，模糊匹配）
     * @return 患者分页数据的统一响应结果
     */
    @GetMapping
    public Result<IPage<Patient>> list(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                       @RequestParam(required = false) String name,
                                       @RequestParam(required = false) String phone) {
        // 委托 PatientService 执行带条件的分页查询
        return Result.ok(patientService.pageQuery(page, pageSize, name, phone));
    }

    /**
     * 新增患者信息。
     *
     * <p>
     * 接收完整的患者信息 JSON 对象，将其写入数据库。
     * 患者 ID 由数据库自增主键自动生成。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code POST /api/patients}</pre>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>name</b> — 姓名</li>
     *   <li><b>gender</b> — 性别（0-未知, 1-男, 2-女）</li>
     *   <li><b>age</b> — 年龄</li>
     *   <li><b>phone</b> — 联系电话</li>
     *   <li><b>address</b> — 住址</li>
     *   <li><b>idCard</b> — 身份证号</li>
     *   <li>其它 {@link Patient} 实体字段</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}，无 data 数据。</p>
     *
     * @param patient 患者实体对象，由请求体 JSON 自动反序列化
     * @return 统一响应结果，表示操作成功或失败
     */
    @PostMapping
    public Result<Void> create(@RequestBody Patient patient) {
        // 收银员可创建初诊患者，医生和管理员可创建所有患者
        requireRole("ADMIN", "DOCTOR", "CASHIER");
        // 直接调用 MyBatis-Plus 的 save 方法持久化患者数据
        patientService.save(patient);
        return Result.ok();
    }

    /**
     * 更新患者信息。
     *
     * <p>
     * 根据传入患者对象的 ID 字段定位目标记录，更新其所有字段。
     * 采用全量更新策略，未传入的字段可能被覆盖为空值。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code PUT /api/patients}</pre>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>id</b> (Long, 必填) — 患者ID，用于定位要更新的记录</li>
     *   <li>其它需要更新的 {@link Patient} 字段</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param patient 患者实体对象，必须包含有效的 ID
     * @return 统一响应结果
     */
    @PutMapping
    public Result<Void> update(@RequestBody Patient patient) {
        requireRole("ADMIN", "DOCTOR");
        // 调用 MyBatis-Plus 的 updateById 方法按主键更新
        patientService.updateById(patient);
        return Result.ok();
    }

    /**
     * 删除患者信息。
     *
     * <p>
     * 根据患者 ID 逻辑删除或物理删除对应记录。
     * 注意：如果该患者已有关联的挂号、就诊记录，
     * 删除操作可能因外键约束而失败或导致数据不一致。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code DELETE /api/patients/{id}}</pre>
     *
     * <h4>路径参数</h4>
     * <ul>
     *   <li><b>id</b> (Long) — 要删除的患者 ID</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param id 患者ID，来自 URL 路径变量
     * @return 统一响应结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        requireRole("ADMIN", "DOCTOR");
        // 调用 MyBatis-Plus 的 removeById 按主键删除
        patientService.removeById(id);
        return Result.ok();
    }
}
