package com.clinic.config;

import com.clinic.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 核心配置类 —— 定义认证策略、授权规则、密码编码器及其他安全相关 Bean。
 *
 * <p>核心职责：</p>
 * <ul>
 *   <li>配置安全过滤链：禁用 CSRF、设为无状态会话、定义 URL 授权规则</li>
 *   <li>注册 JWT 认证过滤器，在用户名密码认证过滤器之前执行</li>
 *   <li>提供 BCrypt 密码编码器 Bean，用于密码加密和校验</li>
 *   <li>暴露 AuthenticationManager Bean，供登录等场景使用</li>
 * </ul>
 *
 * @author clinic
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** JWT 认证过滤器，负责从请求中解析和验证 JWT 令牌 */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 配置 Spring Security 过滤链 —— 定义核心安全策略。
     *
     * <p>策略说明：</p>
     * <ul>
     *   <li><b>禁用 CSRF</b>：前后端分离 + JWT 无状态架构下不需要 CSRF 防护</li>
     *   <li><b>无状态会话</b>：不创建 HttpSession，每次请求都通过 JWT 独立认证</li>
     *   <li><b>登录接口放行</b>：{@code /api/auth/login} 无需认证即可访问</li>
     *   <li><b>其他接口需认证</b>：除登录外所有请求都需要携带有效 JWT</li>
     *   <li><b>JWT 过滤器前置</b>：在用户名密码过滤器之前执行，实现 Token 认证</li>
     * </ul>
     *
     * @param http HttpSecurity 配置构建器
     * @return 构建完成的安全过滤链
     * @throws Exception 配置过程中的异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. 禁用 CSRF 保护（前后端分离 + JWT 无状态，不需要）
            .csrf(AbstractHttpConfigurer::disable)
            // 2. 设置无状态会话策略（不创建 session，每次请求独立认证）
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 3. 配置 URL 级别的授权规则
            .authorizeHttpRequests(auth -> auth
                // 登录和退出接口允许匿名访问
                .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
                // 移动端患者服务接口（无需登录，通过手机号验证身份）
                .requestMatchers("/api/mobile/**").permitAll()
                // 其他所有接口需要认证后访问
                .anyRequest().authenticated()
            )
            // 4. 在用户名密码认证过滤器之前插入 JWT 认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // 返回构建好的过滤链
        return http.build();
    }

    /**
     * 提供密码编码器 Bean —— 使用 BCrypt 算法对密码进行哈希处理。
     *
     * <p>BCrypt 是一种自适应的密码哈希算法，内建盐值，能有效抵御彩虹表攻击。</p>
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 暴露 AuthenticationManager Bean —— 供登录逻辑中手动认证使用。
     *
     * @param config Spring Security 的认证配置对象
     * @return AuthenticationManager 实例
     * @throws Exception 获取过程中的异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
