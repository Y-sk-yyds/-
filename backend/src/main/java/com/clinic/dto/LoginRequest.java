package com.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * <h2>登录请求 DTO</h2>
 *
 * <p>用于接收前端提交的登录表单数据，包含用户名和密码两个必填字段。
 * 通过 Jakarta Bean Validation 实现参数校验，确保传入数据不为空。</p>
 *
 * <h3>校验规则</h3>
 * <ul>
 *   <li><b>username</b> — 不能为空，否则提示"用户名不能为空"</li>
 *   <li><b>password</b> — 不能为空，否则提示"密码不能为空"</li>
 * </ul>
 *
 * <h3>请求示例</h3>
 * <pre>{@code
 * POST /api/login
 * Content-Type: application/json
 *
 * {
 *   "username": "admin",
 *   "password": "123456"
 * }
 * }</pre>
 *
 * @author clinic
 * @version 1.0
 */
@Data
public class LoginRequest {

    /**
     * 登录用户名
     * <p>不能为空，例如：工号、手机号或系统登录名</p>
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 登录密码（明文）
     * <p>不能为空，服务端对密码进行加密后与数据库密文比对</p>
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
