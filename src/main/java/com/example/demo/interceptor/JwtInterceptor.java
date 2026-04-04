package com.example.demo.interceptor;

import com.example.demo.utils.JwtUtil;
import com.example.demo.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 请求（预检请求）
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 获取 token
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或token无效\"}");
            return false;
        }

        token = token.substring(7);

        // 验证 token
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"message\":\"token无效或已过期\"}");
            return false;
        }

        // 将用户名存入请求属性，供后续使用
        String username = jwtUtil.getUsernameFromToken(token);
        request.setAttribute("username", username);

        // 从数据库查询用户 ID
        com.example.demo.entity.User user = userMapper.selectByUsername(username);
        if (user != null) {
            request.setAttribute("userId", user.getId());
        }

        return true;
    }
}