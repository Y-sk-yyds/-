package com.clinic.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类 —— 提供静态方法，便捷地从 Spring Security 上下文获取当前登录用户信息。
 *
 * <p>核心职责：</p>
 * <ul>
 *   <li>获取当前登录用户的 ID</li>
 *   <li>获取当前登录用户的用户名</li>
 *   <li>获取当前登录用户的角色</li>
 * </ul>
 *
 * <p>所有方法均为静态方法，可在任何地方调用，无需依赖注入。</p>
 *
 * <p>使用前提：请求已通过 {@link JwtAuthenticationFilter} 认证，安全上下文中已存入认证信息。</p>
 *
 * @author clinic
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户的 ID。
     *
     * <p>用户 ID 存储在认证对象 {@link Authentication#getCredentials()} 字段中，
     * 该值由 {@link JwtAuthenticationFilter} 在认证时写入。</p>
     *
     * @return 当前用户的 ID，若未登录或认证信息格式不正确则返回 {@code null}
     */
    public static Long getCurrentUserId() {
        // 从安全上下文中获取当前认证信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // 校验认证信息存在且 credentials 字段为 Long 类型（即 userId）
        if (auth != null && auth.getCredentials() instanceof Long) {
            return (Long) auth.getCredentials();
        }
        return null;
    }

    /**
     * 获取当前登录用户的用户名。
     *
     * <p>用户名存储在认证对象的 {@link Authentication#getName()} 字段中。</p>
     *
     * @return 当前用户名，若未登录则返回 {@code null}
     */
    public static String getCurrentUsername() {
        // 从安全上下文中获取当前认证信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getName();
        }
        return null;
    }

    /**
     * 获取当前登录用户的角色。
     *
     * <p>角色存储在认证对象的权限列表中，格式为 {@code "ROLE_XXX"}。
     * 本方法会去掉 {@code "ROLE_"} 前缀，返回纯角色名。</p>
     *
     * @return 当前用户角色（如 ADMIN、DOCTOR、PATIENT），若未登录或无权限则返回 {@code null}
     */
    public static String getCurrentRole() {
        // 从安全上下文中获取当前认证信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // 校验认证信息和权限列表存在
        if (auth != null && !auth.getAuthorities().isEmpty()) {
            // 取第一个权限，去掉 "ROLE_" 前缀返回纯角色名
            return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        }
        return null;
    }
}
