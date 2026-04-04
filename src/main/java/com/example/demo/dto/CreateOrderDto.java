package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateOrderDto {
    private String address;
    private String remark;
    private List<OrderItemDto> items;

    @Data
    public static class OrderItemDto {
        private Long productId;
        private Integer quantity;
    }
}