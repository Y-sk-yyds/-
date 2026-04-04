package com.example.demo.controller;

import com.example.demo.commen.Result;
import com.example.demo.service.LoginTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private LoginTicketService loginTicketService;

    // 生成二维码
    @GetMapping("/qrcode")
    public Result<?> getQRCode() {
        Map<String, String> data = loginTicketService.generateQRCode();
        return Result.success(data);
    }

    // 移动端确认登录（需要携带登录用户的JWT）
    @PostMapping("/qrcode/confirm")
    public Result<?> confirmLogin(@RequestParam String token, @RequestAttribute Long userId) {
        boolean success = loginTicketService.confirmLogin(token, userId);
        if (success) {
            return Result.success("登录确认成功");
        } else {
            return Result.error("二维码无效或已过期");
        }
    }

    @PostMapping("/qrcode/scan")
    public Result<?> scanQRCode(@RequestParam String token, @RequestAttribute Long userId) {
        boolean success = loginTicketService.confirmLogin(token, userId);
        if (success) {
            return Result.success("登录确认成功");
        } else {
            return Result.error("二维码无效或已过期");
        }
    }

    // PC端轮询登录状态
    @GetMapping("/qrcode/status")
    public Result<?> pollLoginStatus(@RequestParam String token) {
        String jwt = loginTicketService.checkLoginStatus(token);
        if (jwt != null) {
            return Result.success(jwt);
        }
        return Result.success("waiting");
    }
}