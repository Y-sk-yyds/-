package com.example.demo.service;

import com.example.demo.entity.Product;
import com.example.demo.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    @Autowired
    private ProductMapper productMapper;

    // 获取商品列表（分页）
    public Map<String, Object> list(int page, int pageSize, String keyword) {
        int offset = (page - 1) * pageSize;
        List<Product> list;
        int total;

        if (keyword != null && !keyword.trim().isEmpty()) {
            list = productMapper.searchPage(keyword, offset, pageSize);
            total = productMapper.countByKeyword(keyword);
        } else {
            list = productMapper.findPage(offset, pageSize);
            total = productMapper.count();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("pages", (total + pageSize - 1) / pageSize);

        return result;
    }

    // 获取商品详情
    public Product detail(Long id) {
        return productMapper.findById(id);
    }
}