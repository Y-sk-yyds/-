package com.example.demo.service;

import com.example.demo.dto.CreateOrderDto;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    // 生成订单号
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // 创建订单
    @Transactional
    public Map<String, Object> createOrder(Long userId, CreateOrderDto dto) {
        // 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证商品并计算总金额
        List<CreateOrderDto.OrderItemDto> items = dto.getItems();
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("订单不能为空");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CreateOrderDto.OrderItemDto item : items) {
            Product product = productMapper.findById(item.getProductId());
            if (product == null) {
                throw new RuntimeException("商品不存在: " + item.getProductId());
            }
            if (item.getQuantity() <= 0) {
                throw new RuntimeException("商品数量必须大于0");
            }

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductPrice(product.getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItems.add(orderItem);
        }

        // 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus("PENDING");
        order.setAddress(dto.getAddress());
        order.setRemark(dto.getRemark());
        order.setCreatedAt(LocalDateTime.now());

        orderMapper.insertOrder(order);

        // 插入订单详情
        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());
            orderMapper.insertOrderItem(orderItem);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("totalAmount", order.getTotalAmount());
        result.put("status", order.getStatus());

        return result;
    }

    // 查询用户订单列表
    public List<Order> listOrders(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    // 查询订单详情（包含商品列表）
    public Map<String, Object> orderDetail(Long userId, Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        // 验证订单是否属于当前用户
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权查看此订单");
        }

        List<OrderItem> items = orderMapper.findItemsByOrderId(orderId);

        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        return result;
    }


    public Order getOrderById(Long id) {
        return orderMapper.findById(id);
    }

    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("只有待支付的订单可以取消");
        }
        order.setStatus("CANCELLED");
        orderMapper.updateStatus(order);
    }

    @Transactional
    public void batchConfirmPayment(List<Long> orderIds, Long userId) {
        for (Long orderId : orderIds) {
            Order order = orderMapper.findById(orderId);
            if (order == null) {
                throw new RuntimeException("订单不存在: " + orderId);
            }
            if (!order.getUserId().equals(userId)) {
                throw new RuntimeException("无权操作订单");
            }
            if (!"PENDING".equals(order.getStatus())) {
                throw new RuntimeException("订单 " + order.getOrderNo() + " 状态不正确");
            }
            order.setStatus("PAID");
            orderMapper.updateStatus(order);
        }
    }
}