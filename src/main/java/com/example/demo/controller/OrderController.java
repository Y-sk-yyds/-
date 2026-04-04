package com.example.demo.controller;

import com.example.demo.commen.Result;
import com.example.demo.dto.CreateOrderDto;
import com.example.demo.entity.Order;
import com.example.demo.service.OrderService;
import com.example.demo.utils.QRCodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // 创建订单
    @PostMapping("/create")
    public Result<?> createOrder(@RequestAttribute Long userId, @RequestBody CreateOrderDto dto) {
        try {
            Map<String, Object> data = orderService.createOrder(userId, dto);
            return Result.success("下单成功", data);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 我的订单列表
    @GetMapping("/list")
    public Result<?> listOrders(@RequestAttribute Long userId) {
        List<?> orders = orderService.listOrders(userId);
        return Result.success(orders);
    }

    // 订单详情
    @GetMapping("/{orderId}")
    public Result<?> orderDetail(@RequestAttribute Long userId, @PathVariable Long orderId) {
        try {
            Map<String, Object> detail = orderService.orderDetail(userId, orderId);
            return Result.success(detail);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 取消订单
    @PutMapping("/{id}/status")
    public Result<?> cancelOrder(@PathVariable Long id, @RequestAttribute Long userId, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (!"CANCELLED".equals(status)) {
            return Result.error("无效的状态");
        }
        try {
            orderService.cancelOrder(id, userId);
            return Result.success("订单已取消");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 获取批量支付二维码（一起结算）
    @PostMapping("/payment/batch/qrcode")
    public Result<?> getBatchPaymentQRCode(@RequestBody List<Long> orderIds, @RequestAttribute Long userId) {
        // 验证所有订单属于当前用户且都是待支付状态
        List<Order> orders = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Long orderId : orderIds) {
            Order order = orderService.getOrderById(orderId);
            if (order == null || !order.getUserId().equals(userId)) {
                return Result.error("订单不存在或无权操作");
            }
            if (!"PENDING".equals(order.getStatus())) {
                return Result.error("订单 " + order.getOrderNo() + " 状态不正确");
            }
            orders.add(order);
            totalAmount = totalAmount.add(order.getTotalAmount());
        }

        // 生成支付二维码内容（包含订单ID列表和总金额）
        String orderIdsStr = orderIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String content = String.format("BATCH_PAY:%s:%.2f", orderIdsStr, totalAmount);
        String qrCodeBase64 = QRCodeUtil.generateQRCodeBase64(content, 250, 250);

        Map<String, Object> result = new HashMap<>();
        result.put("qrCode", "data:image/png;base64," + qrCodeBase64);
        result.put("orderIds", orderIds);
        result.put("totalAmount", totalAmount);
        result.put("count", orders.size());

        return Result.success(result);
    }

    // 确认批量支付
    @PostMapping("/payment/batch/confirm")
    public Result<?> confirmBatchPayment(@RequestBody List<Long> orderIds, @RequestAttribute Long userId) {
        try {
            orderService.batchConfirmPayment(orderIds, userId);
            return Result.success("支付成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}