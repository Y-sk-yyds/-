package com.clinic.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器 —— 在每个 HTTP 请求到达控制器之前，检查并验证 JWT 令牌。
 *
 * <p>核心职责：</p>
 * <ul>
 *   <li>从请求头 {@code Authorization} 中提取 Bearer 令牌</li>
 *   <li>验证令牌有效性</li>
 *   <li>令牌有效时将用户认证信息写入 Spring Security 的安全上下文</li>
 *   <li>令牌无效或不存在时放行（由后续过滤器或安全配置决定处理方式）</li>
 * </ul>
 *
 * <p>继承 {@link OncePerRequestFilter}，保证每个请求只执行一次过滤逻辑。</p>
 *
 * @author clinic
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT 令牌提供者，用于令牌解析和验证 */
    private final JwtTokenProvider jwtTokenProvider;

    /** Redis 字符串操作模板，用于检查 token 黑名单 */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 过滤器核心逻辑 —— 对每个 HTTP 请求执行 JWT 认证。
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>从请求头解析 Bearer 令牌</li>
     *   <li>校验令牌有效性</li>
     *   <li>令牌有效时：提取用户信息，构建认证对象并存入安全上下文</li>
     *   <li>放行请求，交给后续过滤器链处理</li>
     * </ol>
     *
     * @param request     HTTP 请求对象
     * @param response    HTTP 响应对象
     * @param filterChain 过滤器链，用于将请求传递给下一个过滤器
     * @throws ServletException 过滤器处理异常
     * @throws IOException      I/O 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 步骤1：从请求头中解析 JWT 令牌
        String token = resolveToken(request);

        // 步骤2：检查令牌是否在黑名单中（已退出登录）
        if (StringUtils.hasText(token)
                && Boolean.TRUE.equals(stringRedisTemplate.hasKey("blacklist:token:" + token))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 步骤3：检查令牌是否存在且有效
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            // 步骤3：从令牌中提取用户信息
            String username = jwtTokenProvider.getUsername(token);
            String role = jwtTokenProvider.getRole(token);
            Long userId = jwtTokenProvider.getUserId(token);

            // 步骤4：构建 Spring Security 认证对象
            // credentials 字段存储 userId，authorities 添加 ROLE_ 前缀
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, userId,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));

            // 步骤5：将认证信息存入安全上下文，后续可通过 SecurityContextHolder 获取
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 步骤6：无论是否认证成功，都继续过滤器链的处理
        filterChain.doFilter(request, response);
    }

    /**
     * 从 HTTP 请求头中解析 JWT 令牌。
     *
     * <p>解析规则：</p>
     * <ul>
     *   <li>从 {@code Authorization} 请求头中获取值</li>
     *   <li>检查是否以 {@code "Bearer "} 开头</li>
     *   <li>截取 "Bearer " 之后的内容作为令牌返回</li>
     * </ul>
     *
     * @param request HTTP 请求对象
     * @return 解析出的 JWT 令牌字符串（不含 "Bearer " 前缀），若未找到或格式不正确则返回 {@code null}
     */
    private String resolveToken(HttpServletRequest request) {
        // 从请求头中获取 Authorization 字段
        String bearerToken = request.getHeader("Authorization");
        // 检查是否为标准的 Bearer 令牌格式
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // 去掉 "Bearer " 前缀（长度为7），返回纯令牌
            return bearerToken.substring(7);
        }
        return null;
    }
}
