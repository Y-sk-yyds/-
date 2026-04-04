package com.example.demo;

import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserMapperTest {
    @Autowired
    private UserMapper userMapper;

    @Test
    void insert() {
        User user = new User();
        user.setUsername("testuser55");
        user.setPassword("12345655");
        user.setRole("USER55");

        int rows = userMapper.insert(user);
        System.out.println("插入行数：" + rows);

        User saved = userMapper.selectById(user.getId());
        System.out.println("保存的用户：" + saved);
    }

    @Test
    void selectById() {
    }
}