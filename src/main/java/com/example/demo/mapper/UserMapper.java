package com.example.demo.mapper;

import com.example.demo.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Insert("INSERT INTO user(username, password, phone, role) VALUES(#{username}, #{password}, #{phone}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT * FROM user WHERE id = #{id}")
    @Results({
            @Result(column = "created_at", property = "createdAt")
    })
    User selectById(Long id);

    // 新增：根据用户名查询用户
    @Select("SELECT * FROM user WHERE username = #{username}")
    @Results({
            @Result(column = "created_at", property = "createdAt")
    })

    User selectByUsername(String username);
}