package com.clinic.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clinic.dto.DoctorCreateRequest;
import com.clinic.entity.Doctor;
import com.clinic.entity.User;
import com.clinic.mapper.DoctorMapper;
import com.clinic.mapper.UserMapper;
import com.clinic.vo.DoctorVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 医生服务类
 * <p>
 * 负责医生信息的全生命周期管理，核心功能包括：
 * <ul>
 *   <li>分页查询医生列表（支持按姓名、科室筛选，关联查询用户信息）</li>
 *   <li>新增医生：同时创建系统用户账号和医生档案</li>
 *   <li>编辑医生：更新医生信息并同步关联的用户信息</li>
 *   <li>删除医生：删除医生记录并禁用关联的系统用户账号</li>
 * </ul>
 * </p>
 * <p>
 * <b>设计说明：</b>每位医生关联一个系统用户（User），user_id 是两者之间的关联键。
 * 创建医生时自动生成默认密码的医生账号；删除医生时禁用而非删除用户账号，保留操作记录。
 * </p>
 *
 * @author clinic
 */
@Service
@RequiredArgsConstructor
public class DoctorService extends ServiceImpl<DoctorMapper, Doctor> {

    /** 操作医生表的 Mapper（包含自定义 SQL 关联查询） */
    private final DoctorMapper doctorMapper;

    /** 操作用户表的 Mapper */
    private final UserMapper userMapper;

    /** Spring Security 密码编码器（暂未直接使用，预留于未来扩展） */
    private final PasswordEncoder passwordEncoder;

    /** 用户服务，用于创建和禁用医生关联的用户账号 */
    private final UserService userService;

    /**
     * 分页查询医生列表
     * <p>
     * 委托 Mapper 层的自定义 SQL（关联查询 User 表），
     * 返回包含用户信息的医生视图对象列表。
     * </p>
     *
     * @param page       页码
     * @param pageSize   每页大小
     * @param name       医生姓名（可选，模糊匹配）
     * @param department 科室名称（可选，模糊匹配）
     * @return 分页的医生视图对象列表
     */
    public IPage<DoctorVO> pageQuery(Integer page, Integer pageSize, String name, String department) {
        return doctorMapper.selectDoctorPage(new Page<>(page, pageSize), name, department);
    }

    /**
     * 新增医生
     * <p>
     * 业务流程（事务操作）：
     * <ol>
     *   <li>构建医生实体对象，填充姓名、科室、职称、电话、简介等基本信息</li>
     *   <li>调用 UserService 创建对应的系统用户账号（默认角色 DOCTOR，默认密码 admin123）</li>
     *   <li>将生成的用户ID关联到医生的 userId 字段</li>
     *   <li>保存医生记录到数据库</li>
     * </ol>
     * </p>
     *
     * @param request 医生创建请求，包含姓名、科室、职称、电话、简介
     */
    @Transactional
    public void createDoctor(DoctorCreateRequest request) {
        // ===== 第1步：构建医生实体 =====
        Doctor doctor = new Doctor();
        doctor.setName(request.getName());               // 医生姓名
        doctor.setDepartment(request.getDepartment());   // 所属科室
        doctor.setTitle(request.getTitle());             // 职称（如主任医师、副主任医师）
        doctor.setPhone(request.getPhone());             // 联系电话
        doctor.setIntroduction(request.getIntroduction()); // 医生简介

        // ===== 第2步：创建关联的系统用户账号 =====
        // 自动生成用户名（doc_随机数）和默认密码（admin123），角色为 DOCTOR
        User user = userService.createDoctorUser(request.getName(), request.getPhone());

        // ===== 第3步：关联用户ID并保存医生记录 =====
        doctor.setUserId(user.getId());
        save(doctor);
    }

    /**
     * 更新医生信息
     * <p>
     * 业务流程：
     * <ol>
     *   <li>更新医生表记录</li>
     *   <li>如果医生关联了系统用户，同步更新用户的真实姓名和手机号</li>
     * </ol>
     * 确保医生表和用户表中的姓名、联系方式保持同步一致。
     * </p>
     *
     * @param doctor 医生实体对象（包含更新后的字段值）
     */
    @Transactional
    public void updateDoctor(Doctor doctor) {
        // 更新医生表记录
        updateById(doctor);

        // ===== 同步更新关联的用户信息 =====
        // 如果医生关联了系统用户账号，同步更新用户的真实姓名和手机号
        if (doctor.getUserId() != null) {
            User user = userMapper.selectById(doctor.getUserId());
            if (user != null) {
                user.setRealName(doctor.getName());   // 同步真实姓名
                user.setPhone(doctor.getPhone());      // 同步手机号
                userMapper.updateById(user);
            }
        }
    }

    /**
     * 删除医生
     * <p>
     * 业务逻辑：
     * <ol>
     *   <li>从医生表中删除记录</li>
     *   <li>禁用（而非删除）关联的系统用户账号，保留操作记录</li>
     * </ol>
     * <b>注意：</b>使用逻辑禁用而非物理删除用户，因为用户账号可能关联了历史操作记录，
     * 直接删除会导致数据引用问题。
     * </p>
     *
     * @param id 医生ID
     */
    @Transactional
    public void deleteDoctor(Long id) {
        Doctor doctor = getById(id);
        if (doctor != null) {
            // 从医生表中物理删除记录
            removeById(id);

            // ===== 禁用关联的用户账号 =====
            // 将用户状态设为 0（禁用），而非物理删除，保留历史数据
            if (doctor.getUserId() != null) {
                userService.disableUser(doctor.getUserId());
            }
        }
    }
}
