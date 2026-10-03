package com.clinic.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 令牌生成与校验单元测试
 * <p>验证令牌的生成、解析、过期拒绝等核心安全功能</p>
 */
@SpringBootTest
class JwtTokenTest {

    @Autowired private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("生成令牌后应能解析出原始信息")
    void testGenerateAndParse() {
        String token = jwtTokenProvider.generateToken(1L, "doctor01", "DOCTOR");

        assertNotNull(token);
        assertTrue(token.length() > 20, "JWT令牌应有足够长度");

        Long userId = jwtTokenProvider.getUserId(token);
        assertEquals(1L, userId);

        String username = jwtTokenProvider.getUsername(token);
        assertEquals("doctor01", username);

        String role = jwtTokenProvider.getRole(token);
        assertEquals("DOCTOR", role);
    }

    @Test
    @DisplayName("有效令牌应通过验证")
    void testValidTokenPassesValidation() {
        String token = jwtTokenProvider.generateToken(2L, "admin", "ADMIN");
        assertTrue(jwtTokenProvider.validateToken(token), "有效令牌应通过验证");
    }

    @Test
    @DisplayName("非法令牌应被拒绝")
    void testInvalidTokenFails() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken("eyJhbGciOiJIUzI1NiJ9.xxx.yyy"));
    }

    @Test
    @DisplayName("过期令牌应被拒绝")
    void testExpiredTokenFails() throws Exception {
        // 反射设置极短过期时间
        java.lang.reflect.Field expField = jwtTokenProvider.getClass().getDeclaredField("expiration");
        expField.setAccessible(true);
        long originalExp = expField.getLong(jwtTokenProvider);
        try {
            // 设过期时间为 1 毫秒，现在生成的 token 立即过期
            expField.setLong(jwtTokenProvider, 1);
            String token = jwtTokenProvider.generateToken(3L, "test", "DOCTOR");
            Thread.sleep(100); // 等 token 过期
            assertFalse(jwtTokenProvider.validateToken(token), "过期令牌应被拒绝");
        } finally {
            expField.setLong(jwtTokenProvider, originalExp);
        }
    }

    @Test
    @DisplayName("不同用户生成的令牌应携带不同信息")
    void testDifferentUsersProduceDifferentTokens() {
        String token1 = jwtTokenProvider.generateToken(1L, "doctor01", "DOCTOR");
        String token2 = jwtTokenProvider.generateToken(2L, "admin", "ADMIN");

        assertNotEquals(token1, token2, "不同用户的令牌应不同");

        assertEquals("doctor01", jwtTokenProvider.getUsername(token1));
        assertEquals("admin", jwtTokenProvider.getUsername(token2));
        assertEquals("DOCTOR", jwtTokenProvider.getRole(token1));
        assertEquals("ADMIN", jwtTokenProvider.getRole(token2));
    }
}
