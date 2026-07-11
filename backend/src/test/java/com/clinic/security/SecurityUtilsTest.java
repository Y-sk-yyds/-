package com.clinic.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全工具类单元测试 — 角色权限隔离验证
 * <p>纯单元测试，不依赖 Spring 上下文，手动模拟安全上下文</p>
 */
class SecurityUtilsTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("未登录时应返回 null")
    void testNoAuthenticationReturnsNull() {
        assertNull(SecurityUtils.getCurrentUserId());
        assertNull(SecurityUtils.getCurrentUsername());
        assertNull(SecurityUtils.getCurrentRole());
    }

    @Test
    @DisplayName("医生登录后应正确返回医生信息")
    void testDoctorAuthentication() {
        setAuth(1L, "doctor01", "ROLE_DOCTOR");

        assertEquals(1L, SecurityUtils.getCurrentUserId());
        assertEquals("doctor01", SecurityUtils.getCurrentUsername());
        assertEquals("DOCTOR", SecurityUtils.getCurrentRole());
    }

    @Test
    @DisplayName("管理员登录后应正确返回管理员信息")
    void testAdminAuthentication() {
        setAuth(2L, "admin", "ROLE_ADMIN");

        assertEquals(2L, SecurityUtils.getCurrentUserId());
        assertEquals("admin", SecurityUtils.getCurrentUsername());
        assertEquals("ADMIN", SecurityUtils.getCurrentRole());
    }

    @Test
    @DisplayName("收银员登录后应正确返回收银员信息")
    void testCashierAuthentication() {
        setAuth(3L, "cashier01", "ROLE_CASHIER");

        assertEquals(3L, SecurityUtils.getCurrentUserId());
        assertEquals("cashier01", SecurityUtils.getCurrentUsername());
        assertEquals("CASHIER", SecurityUtils.getCurrentRole());
    }

    @Test
    @DisplayName("credentials 非 Long 类型时应安全返回 null")
    void testInvalidCredentialsReturnsNull() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test", "not-a-long",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))));
        assertNull(SecurityUtils.getCurrentUserId(), "credentials 非 Long 时应返回 null");
    }

    private void setAuth(Long userId, String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, userId,
                        Collections.singletonList(new SimpleGrantedAuthority(role))));
    }
}
