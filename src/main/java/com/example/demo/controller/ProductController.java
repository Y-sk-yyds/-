package com.example.demo.controller;

import com.example.demo.commen.Result;
import com.example.demo.entity.Product;
import com.example.demo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    // 商品列表（分页 + 搜索）
    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {

        Map<String, Object> data = productService.list(page, pageSize, keyword);
        return Result.success(data);
    }

    // 商品详情
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        Product product = productService.detail(id);
        if (product == null) {
            return Result.error("商品不存在");
        }
        return Result.success(product);
    }
}