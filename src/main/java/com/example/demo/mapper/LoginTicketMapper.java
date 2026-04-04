package com.example.demo.mapper;

import com.example.demo.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Mapper
public interface LoginTicketMapper {

//    @Insert("INSERT INTO login_ticket(token, user_id, status, expire_time, create_time) " +
//            "VALUES(#{token}, #{userId}, #{status}, #{expireTime}, #{createTime})")
//    @Options(useGeneratedKeys = true, keyProperty = "id")
//    int insert(LoginTicket ticket);
    @Insert("INSERT INTO login_ticket(token, status, expire_time, create_time) VALUES(#{token}, #{status}, #{expireTime}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LoginTicket ticket);

    @Select("SELECT * FROM login_ticket WHERE token = #{token}")
    @Results({
            @Result(column = "expire_time", property = "expireTime"),
            @Result(column = "create_time", property = "createTime")
    })
    LoginTicket selectByToken(String token);

    @Update("UPDATE login_ticket SET status = #{status}, user_id = #{userId} WHERE token = #{token}")
    int updateStatus(@Param("token") String token, @Param("status") Integer status, @Param("userId") Long userId);

    @Delete("DELETE FROM login_ticket WHERE expire_time < NOW()")
    int deleteExpired();
}