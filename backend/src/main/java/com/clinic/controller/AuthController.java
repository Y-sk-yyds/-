package com.clinic.controller;

import com.clinic.common.Result;
import com.clinic.dto.LoginRequest;
import com.clinic.dto.LoginResponse;
import com.clinic.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 —— 处理用户登录认证相关的 HTTP 请求。
 *
 * <p>
 * 本控制器是系统的安全入口，负责接收前端提交的登录凭证，
 * 调用 {@link AuthService} 进行身份校验，并返回登录令牌（JWT Token）与用户信息。
 * </p>
 *
 * <h3>请求路径前缀</h3>
 * <p>所有接口均位于 {@code /api/auth} 路径下。</p>
 *
 * @see AuthService 认证服务层，执行实际的登录校验逻辑
 * @see LoginRequest 登录请求的 DTO 对象，包含用户名和密码
 * @see LoginResponse 登录响应的 DTO 对象，包含令牌和用户基本信息
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /**
     * 认证服务 —— 由 Spring 通过 {@code @RequiredArgsConstructor} 自动注入。
     */
    private final AuthService authService;

    /**
     * 用户登录接口。
     *
     * <p>
     * 接收用户名与密码，验证通过后返回 JWT 令牌、用户角色、姓名等信息。
     * 前端后续请求需在请求头中携带此令牌以通过安全拦截器。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code POST /api/auth/login}</pre>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>username</b> (String, 必填) — 用户登录名</li>
     *   <li><b>password</b> (String, 必填) — 用户密码</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <ul>
     *   <li>成功时返回 {@link Result}{@code <LoginResponse>}，data 中包含：
     *     <ul>
     *       <li><b>token</b> — JWT 令牌字符串</li>
     *       <li><b>userId</b> — 用户ID</li>
     *       <li><b>username</b> — 用户名</li>
     *       <li><b>realName</b> — 用户真实姓名</li>
     *       <li><b>role</b> — 用户角色（如 ADMIN、DOCTOR、PATIENT）</li>
     *     </ul>
     *   </li>
     *   <li>用户名或密码错误时返回错误码与提示信息</li>
     * </ul>
     *
     * @param request 登录请求体，包含用户名和密码，通过 {@code @Valid} 进行 JSR-303 校验
     * @return 统一响应结果，data 字段为 {@link LoginResponse} 对象
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // 委托 AuthService 执行密码验证与令牌生成
        return Result.ok(authService.login(request));
    }
}
