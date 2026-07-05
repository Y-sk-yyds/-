package com.clinic.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 令牌提供者 —— 负责 JWT 令牌的生成、解析与校验。
 *
 * <p>核心职责：</p>
 * <ul>
 *   <li>生成带用户信息（用户ID、用户名、角色）的 JWT 令牌</li>
 *   <li>解析令牌中的声明信息（Claims）</li>
 *   <li>验证令牌是否合法且在有效期内</li>
 *   <li>从令牌中提取用户ID、用户名、角色等业务信息</li>
 * </ul>
 *
 * <p>令牌使用 HMAC-SHA256 签名算法，密钥通过配置文件中的 {@code jwt.secret} 注入，
 * 有效时长通过 {@code jwt.expiration} 配置（单位：毫秒）。</p>
 *
 * @author clinic
 */
@Component
public class JwtTokenProvider {

    /**
     * JWT 签名密钥，从配置文件 {@code jwt.secret} 注入。
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * JWT 令牌过期时间（毫秒），从配置文件 {@code jwt.expiration} 注入。
     */
    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * 获取 HMAC-SHA256 签名密钥。
     *
     * <p>将配置的密钥字符串转换为 256 位（32 字节）的 HMAC 密钥。
     * 如果配置的密钥长度不足 32 字节，则用零填充；超出则截断。</p>
     *
     * @return 用于 JWT 签名/验签的 {@link SecretKey}
     */
    private SecretKey getKey() {
        // 将密钥字符串转换为 UTF-8 字节数组
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        // 创建固定 256 位（32 字节）的数组
        byte[] key256 = new byte[32];
        // 拷贝配置密钥到 256 位数组中（不足补零，超出截断）
        System.arraycopy(keyBytes, 0, key256, 0, Math.min(keyBytes.length, 32));
        // 生成 HMAC-SHA256 密钥
        return Keys.hmacShaKeyFor(key256);
    }

    /**
     * 生成 JWT 令牌。
     *
     * <p>令牌中包含以下声明：</p>
     * <ul>
     *   <li>{@code sub} —— 用户名（标准声明）</li>
     *   <li>{@code userId} —— 用户ID（自定义声明）</li>
     *   <li>{@code role} —— 用户角色（自定义声明）</li>
     *   <li>{@code iat} —— 签发时间（标准声明）</li>
     *   <li>{@code exp} —— 过期时间（标准声明）</li>
     * </ul>
     *
     * @param userId   用户唯一标识
     * @param username 用户名
     * @param role     用户角色（如 ADMIN、DOCTOR、PATIENT）
     * @return 签发的 JWT 令牌字符串
     */
    public String generateToken(Long userId, String username, String role) {
        // 记录当前时间作为签发时间
        Date now = new Date();
        // 构建并签名 JWT 令牌
        return Jwts.builder()
                .subject(username)                                        // 主题：用户名
                .claim("userId", userId)                                 // 自定义声明：用户ID
                .claim("role", role)                                     // 自定义声明：用户角色
                .issuedAt(now)                                           // 签发时间
                .expiration(new Date(now.getTime() + expiration))        // 过期时间 = 当前 + 配置时长
                .signWith(getKey())                                      // 使用 HMAC-SHA256 签名
                .compact();                                              // 压缩为字符串
    }

    /**
     * 解析 JWT 令牌，提取其中的声明信息。
     *
     * <p>如果令牌签名无效、已过期或被篡改，将抛出异常。</p>
     *
     * @param token JWT 令牌字符串（不含 "Bearer " 前缀）
     * @return 解析出的 {@link Claims} 声明对象
     * @throws JwtException 当令牌无效、过期或签名不匹配时抛出
     */
    public Claims parseToken(String token) {
        // 构建解析器，设置验证密钥，解析并返回载荷
        return Jwts.parser()
                .verifyWith(getKey())    // 使用 HMAC 密钥验证签名
                .build()                 // 构建解析器
                .parseSignedClaims(token) // 解析已签名的令牌
                .getPayload();           // 获取载荷（Claims）
    }

    /**
     * 验证 JWT 令牌是否合法。
     *
     * <p>验证逻辑：尝试解析令牌，若解析成功则令牌有效，若捕获到异常则无效。</p>
     *
     * @param token JWT 令牌字符串（不含 "Bearer " 前缀）
     * @return 令牌合法返回 {@code true}，否则返回 {@code false}
     */
    public boolean validateToken(String token) {
        try {
            // 尝试解析令牌，不抛异常即表示有效
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 捕获签名无效、过期、格式错误等异常
            return false;
        }
    }

    /**
     * 从 JWT 令牌中提取用户 ID。
     *
     * @param token JWT 令牌字符串（不含 "Bearer " 前缀）
     * @return 用户 ID，令牌中包含的自定义声明
     */
    public Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    /**
     * 从 JWT 令牌中提取用户名。
     *
     * <p>用户名存储在 JWT 标准声明 {@code sub}（主题）字段中。</p>
     *
     * @param token JWT 令牌字符串（不含 "Bearer " 前缀）
     * @return 用户名
     */
    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从 JWT 令牌中提取用户角色。
     *
     * @param token JWT 令牌字符串（不含 "Bearer " 前缀）
     * @return 用户角色字符串（如 ADMIN、DOCTOR、PATIENT）
     */
    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }
}
