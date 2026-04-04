package com.example.demo.controller;

import com.example.demo.commen.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "阶段一完成 ✅ Spring Boot 3.5.13 运行正常";
    }

    @GetMapping("/api/test")
    public Result<?> test(@RequestAttribute String username) {
        return Result.success("当前登录用户：" + username);
    }
}
