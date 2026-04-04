package com.example.demo.mapper;

import com.example.demo.entity.Product;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductMapper {

    // 分页查询商品（只显示上架的）
    @Select("SELECT * FROM product WHERE status = 1 ORDER BY id DESC LIMIT #{offset}, #{pageSize}")
    @Results({
            @Result(column = "image_url", property = "imageUrl"),
            @Result(column = "created_at", property = "createdAt")
    })
    List<Product> findPage(@Param("offset") int offset, @Param("pageSize") int pageSize);

    // 搜索商品
    @Select("SELECT * FROM product WHERE status = 1 AND name LIKE CONCAT('%', #{keyword}, '%') ORDER BY id DESC LIMIT #{offset}, #{pageSize}")
    @Results({
            @Result(column = "image_url", property = "imageUrl"),
            @Result(column = "created_at", property = "createdAt")
    })
    List<Product> searchPage(@Param("keyword") String keyword, @Param("offset") int offset, @Param("pageSize") int pageSize);

    // 查询总数
    @Select("SELECT COUNT(*) FROM product WHERE status = 1")
    int count();

    // 搜索总数
    @Select("SELECT COUNT(*) FROM product WHERE status = 1 AND name LIKE CONCAT('%', #{keyword}, '%')")
    int countByKeyword(@Param("keyword") String keyword);

    // 根据 ID 查询商品详情
    @Select("SELECT * FROM product WHERE id = #{id} AND status = 1")
    @Results({
            @Result(column = "image_url", property = "imageUrl"),
            @Result(column = "created_at", property = "createdAt")
    })
    Product findById(@Param("id") Long id);
}