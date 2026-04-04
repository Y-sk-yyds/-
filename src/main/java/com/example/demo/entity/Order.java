package com.example.demo.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Order {
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private String status;  // PENDING, PAID, DELIVERING, COMPLETED, CANCELLED
    private String address;
    private String remark;
    private LocalDateTime createdAt;
}