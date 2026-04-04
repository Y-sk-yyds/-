package com.example.demo.controller;

import com.example.demo.commen.Result;
import com.example.demo.dto.LoginDto;
import com.example.demo.dto.RegisterDto;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterDto dto) {
        try {
            Object data = userService.register(dto);
            return Result.success("注册成功", data);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginDto dto) {
        try {
            Object data = userService.login(dto);
            return Result.success("登录成功", data);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/info")
    public Result<?> getUserInfo(@RequestAttribute Long userId) {
        User user = userService.getById(userId);
        if (user == null) return Result.error("用户不存在");
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("phone", user.getPhone());
        data.put("role", user.getRole());
        return Result.success(data);
    }
//    @GetMapping("/info")
//    public Result<?> getUserInfo(@RequestAttribute Long userId) {
//        User user = userService.getById(userId);
//        if (user == null) return Result.error("用户不存在");
//        Map<String, Object> data = new HashMap<>();
//        data.put("id", user.getId());
//        data.put("username", user.getUsername());
//        data.put("role", user.getRole());
//        return Result.success(data);
//    }
}