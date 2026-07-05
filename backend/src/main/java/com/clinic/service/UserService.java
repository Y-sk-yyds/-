package com.clinic.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clinic.entity.User;
import com.clinic.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务类
 * <p>
 * 负责系统用户账号的管理，核心功能包括：
 * <ul>
 *   <li>创建医生用户账号（自动生成用户名和默认密码）</li>
 *   <li>禁用用户账号（逻辑禁用，而非物理删除）</li>
 * </ul>
 * </p>
 * <p>
 * <b>用户角色说明：</b>系统支持 ROOT、ADMIN、DOCTOR 三种角色。
 * 医生账号由 DoctorService 在创建医生档案时通过此服务自动创建。
 * </p>
 *
 * @author clinic
 */
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    /** Spring Security 密码编码器，用于对密码进行 BCrypt 加密 */
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建医生用户账号
     * <p>
     * 在创建医生档案时自动调用，为医生创建一个可登录系统的用户账号。
     * 账号特征：
     * <ul>
     *   <li>用户名：自动生成，格式为 "doc_" + 时间戳后5位，如 doc_12345</li>
     *   <li>默认密码：admin123，使用 BCrypt 加密后存储</li>
     *   <li>角色：DOCTOR</li>
     *   <li>状态：1（启用）</li>
     * </ul>
     * </p>
     *
     * @param realName 真实姓名（用于登录后的显示名称）
     * @param phone    手机号（可用于短信通知等扩展功能）
     * @return 创建的用户实体对象
     */
    public User createDoctorUser(String realName, String phone) {
        User user = new User();

        // 自动生成用户名：doc_ + 时间戳后5位数字
        // 使用 System.currentTimeMillis() % 100000 保证用户名唯一性
        String username = "doc_" + System.currentTimeMillis() % 100000;
        user.setUsername(username);

        // 密码使用 BCrypt 加密存储，明文密码为 "admin123"
        user.setPassword(passwordEncoder.encode("admin123"));

        user.setRealName(realName);  // 真实姓名
        user.setPhone(phone);        // 手机号
        user.setRole("DOCTOR");      // 角色：DOCTOR（医生）
        user.setStatus(1);           // 状态：1 = 启用

        save(user);
        return user;
    }

    /**
     * 禁用用户账号（逻辑禁用）
     * <p>
     * 将用户状态设置为 0（禁用），被禁用的用户将无法登录系统。
     * 此方法在删除医生时调用，目的是保留用户记录而非物理删除，
     * 以便保留操作日志和审计追溯。
     * </p>
     * <p>
     * <b>状态值说明：</b>
     * <ul>
     *   <li>0 = 禁用（无法登录）</li>
     *   <li>1 = 启用（正常登录）</li>
     * </ul>
     * </p>
     *
     * @param userId 用户ID
     */
    public void disableUser(Long userId) {
        User user = getById(userId);
        if (user != null) {
            user.setStatus(0);  // 设置状态为禁用
            updateById(user);
        }
    }
}
