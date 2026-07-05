package com.clinic.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.Result;
import com.clinic.entity.User;
import com.clinic.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理控制器 —— 处理系统用户的增删改查 HTTP 请求。
 *
 * <p>
 * 本控制器管理系统用户账号，核心功能包括：
 * </p>
 * <ul>
 *   <li>用户创建（自动对密码进行 BCrypt 加密）</li>
 *   <li>用户信息更新（不更新密码字段）</li>
 *   <li>用户状态切换（启用/停用）</li>
 *   <li>用户列表分页查询</li>
 * </ul>
 *
 * <p>
 * <b>安全设计</b>：
 * </p>
 * <ul>
 *   <li>创建用户时，密码通过 {@link PasswordEncoder}（BCrypt）加密后存储，
 *       确保数据库中不存储明文密码</li>
 *   <li>更新用户时，密码字段被显式置为 null，防止误修改密码</li>
 *   <li>默认密码为 "admin123"（当前端未传 password 时使用）</li>
 * </ul>
 *
 * <h3>请求路径前缀</h3>
 * <p>所有接口均位于 {@code /api/users} 路径下。</p>
 *
 * @see UserService 用户服务层
 * @see User 用户实体类，映射 users 数据库表
 * @see PasswordEncoder Spring Security 密码编码器
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    /**
     * 用户服务 —— 由 Spring 通过 {@code @RequiredArgsConstructor} 自动注入。
     */
    private final UserService userService;

    /**
     * 密码编码器 —— Spring Security 提供的 BCrypt 实现。
     * 用于在保存密码前进行单向哈希加密。
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询用户列表。
     *
     * <h4>请求方式</h4>
     * <pre>{@code GET /api/users?page=1&pageSize=10}</pre>
     *
     * <h4>请求参数（Query String）</h4>
     * <ul>
     *   <li><b>page</b> (Integer, 可选, 默认 1) — 页码</li>
     *   <li><b>pageSize</b> (Integer, 可选, 默认 10) — 每页记录数</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <ul>
     *   <li>成功时返回 {@link Result}{@code <IPage<User>>}，data 的 records 为用户列表
     *       （注意：出于安全考虑，返回的用户对象不应包含密码字段，由序列化配置控制）</li>
     * </ul>
     *
     * @param page     页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 用户分页数据的统一响应结果
     */
    @GetMapping
    public Result<IPage<User>> list(@RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "10") Integer pageSize) {
        // 使用 MyBatis-Plus 的分页对象执行无条件分页查询
        return Result.ok(userService.page(new Page<>(page, pageSize)));
    }

    /**
     * 创建系统用户。
     *
     * <p>
     * 创建用户时，密码会通过 {@link PasswordEncoder#encode(CharSequence)}
     * 进行 BCrypt 加密后存储。如果前端未传入密码，则使用默认密码 "admin123"。
     * 新用户的状态默认为 1（启用）。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code POST /api/users}</pre>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>username</b> (String, 必填) — 登录用户名，需保证唯一性</li>
     *   <li><b>password</b> (String, 可选, 默认 "admin123") — 登录密码（明文传入，加密存储）</li>
     *   <li><b>realName</b> (String) — 用户真实姓名</li>
     *   <li><b>phone</b> (String) — 联系电话</li>
     *   <li><b>role</b> (String) — 用户角色（如 ADMIN、DOCTOR、CASHIER、PHARMACIST）</li>
     *   <li><b>status</b> (Integer, 可选, 默认 1) — 状态（1-启用, 0-停用）</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param body 请求体 Map，包含用户基本信息
     * @return 统一响应结果
     */
    @PostMapping
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        // 从请求体构建用户实体对象
        User user = new User();
        user.setUsername((String) body.get("username"));
        // 密码加密存储：使用 BCrypt 进行单向哈希
        // 默认密码为 "admin123"，管理员应在首次登录后修改
        user.setPassword(passwordEncoder.encode((String) body.getOrDefault("password", "admin123")));
        user.setRealName((String) body.get("realName"));
        user.setPhone((String) body.get("phone"));
        user.setRole((String) body.get("role"));
        // 状态默认为 1（启用），可选传入
        user.setStatus(body.get("status") != null ? (Integer) body.get("status") : 1);
        // 调用 MyBatis-Plus 的 save 方法持久化用户数据
        userService.save(user);
        return Result.ok();
    }

    /**
     * 更新用户信息（不更新密码）。
     *
     * <p>
     * 更新用户的基本信息，但<b>不会修改密码</b>。
     * 方法内部将 password 字段显式置为 null，
     * 由 MyBatis-Plus 的字段策略决定是否跳过该字段的更新。
     * 如需修改密码，应通过单独的密码修改接口处理。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code PUT /api/users}</pre>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>id</b> (Long, 必填) — 用户ID</li>
     *   <li><b>username</b> (String) — 用户名</li>
     *   <li><b>realName</b> (String) — 真实姓名</li>
     *   <li><b>phone</b> (String) — 联系电话</li>
     *   <li><b>role</b> (String) — 角色</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param user 用户实体对象，必须包含有效的 ID，password 字段会被忽略
     * @return 统一响应结果
     */
    @PutMapping
    public Result<Void> update(@RequestBody User user) {
        // 安全措施：更新时不修改密码，将 password 置为 null
        // MyBatis-Plus 的 updateById 默认会跳过 null 字段（取决于更新策略配置）
        user.setPassword(null);
        // 调用 MyBatis-Plus 的 updateById 方法按主键更新
        userService.updateById(user);
        return Result.ok();
    }

    /**
     * 切换用户状态（启用/停用）。
     *
     * <p>
     * 将指定用户的启用状态取反：启用变为停用，停用变为启用。
     * 停用的用户无法登录系统。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code PUT /api/users/{id}/toggle-status}</pre>
     *
     * <h4>路径参数</h4>
     * <ul>
     *   <li><b>id</b> (Long) — 用户 ID</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param id 用户ID，来自 URL 路径变量
     * @return 统一响应结果
     */
    @PutMapping("/{id}/toggle-status")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        // 查询用户记录
        User user = userService.getById(id);
        if (user != null) {
            // 状态取反：1(启用) → 0(停用)，0(停用) → 1(启用)
            user.setStatus(user.getStatus() == 1 ? 0 : 1);
            // 更新用户状态
            userService.updateById(user);
        }
        return Result.ok();
    }
}
