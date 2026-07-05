package com.clinic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clinic.dto.LoginRequest;
import com.clinic.dto.LoginResponse;
import com.clinic.entity.Doctor;
import com.clinic.entity.User;
import com.clinic.mapper.DoctorMapper;
import com.clinic.mapper.UserMapper;
import com.clinic.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务类
 * <p>
 * 负责处理用户登录认证流程，包括：
 * <ul>
 *   <li>根据用户名查询用户信息</li>
 *   <li>校验账号状态（是否被禁用）</li>
 *   <li>校验密码是否正确</li>
 *   <li>生成 JWT 令牌</li>
 *   <li>对医生角色额外查询其关联的医生ID</li>
 * </ul>
 * </p>
 *
 * @author clinic
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /** 操作用户表的 Mapper */
    private final UserMapper userMapper;

    /** 操作医生表的 Mapper */
    private final DoctorMapper doctorMapper;

    /** Spring Security 密码编码器，用于密码比对 */
    private final PasswordEncoder passwordEncoder;

    /** JWT 令牌生成与解析工具 */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 用户登录
     * <p>
     * 登录流程：
     * <ol>
     *   <li>根据请求中的用户名查询数据库中的用户记录</li>
     *   <li>如果用户不存在，抛出异常</li>
     *   <li>检查用户状态（status=0 表示已禁用），已禁用则抛出异常</li>
     *   <li>使用 PasswordEncoder 比对明文密码与加密后的密码</li>
     *   <li>密码匹配成功后，调用 JwtTokenProvider 生成 JWT 令牌</li>
     *   <li>如果用户角色是 DOCTOR，额外查询该医生关联的 doctorId 并返回</li>
     * </ol>
     * </p>
     *
     * @param request 登录请求，包含用户名和密码
     * @return 登录响应，包含 JWT 令牌、用户名、真实姓名、角色和可选的医生ID
     * @throws IllegalArgumentException 用户名或密码错误、账号被禁用时抛出
     */
    public LoginResponse login(LoginRequest request) {
        // 第一步：根据用户名从数据库查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );

        // 第二步：校验用户是否存在
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 第三步：校验账号状态，0 表示已禁用
        if (user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用");
        }

        // 第四步：校验密码（明文 vs 加密密文）
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 第五步：生成 JWT 令牌，令牌中包含用户ID、用户名和角色信息
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 第六步：如果登录用户是医生角色，额外查询其医生ID，方便前端路由和权限控制
        Long doctorId = null;
        if ("DOCTOR".equals(user.getRole())) {
            Doctor doctor = doctorMapper.selectOne(
                    new LambdaQueryWrapper<Doctor>().eq(Doctor::getUserId, user.getId()));
            if (doctor != null) {
                doctorId = doctor.getId();
            }
        }

        // 构建并返回登录响应
        return new LoginResponse(token, user.getUsername(), user.getRealName(), user.getRole(), doctorId);
    }
}
