package com.example.demo.config;

import com.example.demo.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")  // 拦截所有 /api 开头的请求
                .excludePathPatterns(
                        "/api/user/register",  // 放行注册
                        "/api/user/login",    // 放行登录
                        "/api/ai/chat",      // 放行 AI 客服（可以不登录）
                        "/api/ai/health",     // 放行健康检查
                        "/api/auth/qrcode",
                        "/api/auth/qrcode/status"
                );
    }


}