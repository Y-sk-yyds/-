package com.example.demo.mapper;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {

    // 插入订单
    @Insert("INSERT INTO orders(order_no, user_id, total_amount, status, address, remark) " +
            "VALUES(#{orderNo}, #{userId}, #{totalAmount}, #{status}, #{address}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrder(Order order);

    // 插入订单详情
    @Insert("INSERT INTO order_item(order_id, product_id, product_name, product_price, quantity) " +
            "VALUES(#{orderId}, #{productId}, #{productName}, #{productPrice}, #{quantity})")
    int insertOrderItem(OrderItem orderItem);

    // 查询用户的订单列表
    @Select("SELECT * FROM orders WHERE user_id = #{userId} ORDER BY created_at DESC")
    @Results({
            @Result(column = "order_no", property = "orderNo"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "total_amount", property = "totalAmount"),
            @Result(column = "created_at", property = "createdAt")
    })
    List<Order> findByUserId(@Param("userId") Long userId);

    // 查询订单详情
    @Select("SELECT * FROM orders WHERE id = #{orderId}")
    @Results({
            @Result(column = "order_no", property = "orderNo"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "total_amount", property = "totalAmount"),
            @Result(column = "created_at", property = "createdAt")
    })
    Order findById(@Param("orderId") Long orderId);

    // 查询订单下的商品列表
    @Select("SELECT * FROM order_item WHERE order_id = #{orderId}")
    @Results({
            @Result(column = "order_id", property = "orderId"),
            @Result(column = "product_id", property = "productId"),
            @Result(column = "product_name", property = "productName"),
            @Result(column = "product_price", property = "productPrice"),
            @Result(column = "created_at", property = "createdAt")
    })
    List<OrderItem> findItemsByOrderId(@Param("orderId") Long orderId);

    @Update("UPDATE orders SET status = #{status} WHERE id = #{id}")
    int updateStatus(Order order);
}