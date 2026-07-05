package com.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * <h2>登录响应 DTO</h2>
 *
 * <p>登录成功后返回给前端的认证信息，包含 JWT 令牌及当前用户的基本资料。
 * 前端收到此响应后，应将 token 存储到本地（如 localStorage），
 * 后续请求在请求头中携带此 token 以完成身份认证。</p>
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li><b>token</b> — JWT 令牌，用于后续接口的身份认证</li>
 *   <li><b>username</b> — 登录用户名</li>
 *   <li><b>realName</b> — 用户真实姓名</li>
 *   <li><b>role</b> — 用户角色（如 ADMIN、DOCTOR、PATIENT）</li>
 *   <li><b>doctorId</b> — 医生 ID，仅当角色为 DOCTOR 时有值</li>
 * </ul>
 *
 * <h3>响应示例</h3>
 * <pre>{@code
 * {
 *   "token": "eyJhbGciOiJIUzI1NiJ9...",
 *   "username": "zhangsan",
 *   "realName": "张三",
 *   "role": "DOCTOR",
 *   "doctorId": 1001
 * }
 * }</pre>
 *
 * @author clinic
 * @version 1.0
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    /**
     * JWT 认证令牌
     * <p>后续请求需在 Header 中携带（如 Authorization: Bearer &lt;token&gt;）</p>
     */
    private String token;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 用户真实姓名
     * <p>用于前端页面展示，如欢迎语"你好，张三"</p>
     */
    private String realName;

    /**
     * 用户角色标识
     * <p>取值示例：ADMIN（管理员）、DOCTOR（医生）、PATIENT（患者）</p>
     */
    private String role;

    /**
     * 关联的医生 ID
     * <p>仅当用户角色为 DOCTOR 时该字段有值，用于查询医生的排班、患者等信息。
     * 非医生角色时该字段为 {@code null}。</p>
     */
    // 仅DOCTOR角色有值
    private Long doctorId;
}
