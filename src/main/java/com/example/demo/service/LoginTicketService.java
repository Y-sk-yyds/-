package com.example.demo.service;

import com.example.demo.entity.LoginTicket;
import com.example.demo.mapper.LoginTicketMapper;
import com.example.demo.utils.JwtUtil;
import com.example.demo.utils.QRCodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class LoginTicketService {

    @Autowired
    private LoginTicketMapper loginTicketMapper;
    @Autowired
    private JwtUtil jwtUtil;

    // 生成登录二维码
    public Map<String, String> generateQRCode() {
        String loginToken = UUID.randomUUID().toString();
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(5);
        LoginTicket ticket = new LoginTicket();
        ticket.setToken(loginToken);
        ticket.setStatus(0);
        ticket.setExpireTime(expireTime);
        ticket.setCreateTime(LocalDateTime.now());
        loginTicketMapper.insert(ticket);

        // 二维码内容：前端轮询需要用到token，这里URL可以是前端的确认链接（移动端扫描后访问）
        // 实际移动端扫码后打开一个确认页面，携带token。我们这里简单将token作为参数
        String qrContent = "http://localhost:5173/scan-confirm?token=" + loginToken;
        String qrCodeBase64 = QRCodeUtil.generateQRCodeBase64(qrContent, 250, 250);
        Map<String, String> result = new HashMap<>();
        result.put("qrCode", "data:image/png;base64," + qrCodeBase64);
        result.put("loginToken", loginToken);
        return result;
    }

    // 移动端确认登录（需要用户已登录状态，这里假设移动端已经登录，通过请求头携带JWT）
    public boolean confirmLogin(String token, Long userId) {
        LoginTicket ticket = loginTicketMapper.selectByToken(token);
        if (ticket == null || ticket.getExpireTime().isBefore(LocalDateTime.now())) {
            return false;
        }
        if (ticket.getStatus() == 1) {
            return false; // 已经确认过了
        }
        loginTicketMapper.updateStatus(token, 1, userId);
        return true;
    }

    // PC端轮询检查登录状态
    public String checkLoginStatus(String token) {
        LoginTicket ticket = loginTicketMapper.selectByToken(token);
        if (ticket == null || ticket.getExpireTime().isBefore(LocalDateTime.now())) {
            return null;
        }
        if (ticket.getStatus() == 1 && ticket.getUserId() != null) {
            // 生成JWT返回
            return jwtUtil.generateToken(String.valueOf(ticket.getUserId())); // 注意：JwtUtil原先生成基于username，可能需要调整
        }
        return null;
    }
}