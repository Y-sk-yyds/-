package com.example.demo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LoginTicket {
    private Long id;
    private String token;        // 唯一标识
    private Long userId;         // 登录确认后关联的用户ID
    private Integer status;      // 0=未使用,1=已确认,2=已过期
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}