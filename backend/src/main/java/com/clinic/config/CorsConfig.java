package com.clinic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域资源共享（CORS）配置类 —— 允许前端应用跨域访问后端 API。
 *
 * <p>核心职责：</p>
 * <ul>
 *   <li>注册全局 CORS 过滤器 Bean</li>
 *   <li>配置允许跨域请求的来源、方法、请求头和凭证</li>
 * </ul>
 *
 * <p>配置说明：</p>
 * <ul>
 *   <li>允许所有来源（通过通配符模式匹配）</li>
 *   <li>允许所有 HTTP 方法（GET、POST、PUT、DELETE 等）</li>
 *   <li>允许所有请求头</li>
 *   <li>允许携带认证凭证（Cookie、Authorization 头等）</li>
 * </ul>
 *
 * @author clinic
 */
@Configuration
public class CorsConfig {

    /**
     * 创建并注册 CORS 过滤器。
     *
     * <p>该过滤器在请求进入 Spring Security 过滤链之前执行，
     * 为跨域预检请求（OPTIONS）和实际请求添加必要的 CORS 响应头。</p>
     *
     * @return 配置好的 CorsFilter 实例
     */
    @Bean
    public CorsFilter corsFilter() {
        // 创建 CORS 配置对象
        CorsConfiguration config = new CorsConfiguration();

        // 仅允许本机前端开发服务器跨域访问
        config.addAllowedOriginPattern("http://localhost:5173");
        // 允许所有请求头
        config.addAllowedHeader("*");
        // 允许所有 HTTP 方法（GET、POST、PUT、DELETE、OPTIONS 等）
        config.addAllowedMethod("*");
        // 允许请求携带认证凭证（如 JWT 在 Authorization 头中）
        // 注意：设置 allowCredentials(true) 后，allowedOrigin 不能使用 "*"，需使用 addAllowedOriginPattern
        config.setAllowCredentials(true);

        // 创建 URL 模式匹配的 CORS 配置源
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 将所有路径（/**）注册到该 CORS 配置
        source.registerCorsConfiguration("/**", config);

        // 返回配置好的 CORS 过滤器
        return new CorsFilter(source);
    }
}
