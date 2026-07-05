package com.clinic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clinic.entity.Patient;
import com.clinic.mapper.PatientMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 患者服务类
 * <p>
 * 负责患者信息的管理，核心功能为分页查询患者列表。
 * 继承了 MyBatis-Plus 的 {@code ServiceImpl}，自动拥有基础的增删改查能力
 * （如 getById、save、updateById、removeById 等）。
 * </p>
 * <p>
 * <b>查询特性：</b>支持按姓名和手机号进行模糊搜索，方便医护人员快速定位患者。
 * </p>
 *
 * @author clinic
 */
@Service
public class PatientService extends ServiceImpl<PatientMapper, Patient> {

    /**
     * 分页查询患者列表
     * <p>
     * 支持按姓名和手机号进行模糊匹配筛选：
     * <ul>
     *   <li>姓名（name）：使用 LIKE 模糊查询，支持部分匹配</li>
     *   <li>手机号（phone）：使用 LIKE 模糊查询，支持部分匹配</li>
     * </ul>
     * 两个条件可同时使用（AND 关系），结果按创建时间降序排列。
     * </p>
     *
     * @param page     页码（从 1 开始）
     * @param pageSize 每页记录数
     * @param name     患者姓名（可选，模糊匹配）
     * @param phone    手机号（可选，模糊匹配）
     * @return 分页的患者记录列表
     */
    public IPage<Patient> pageQuery(Integer page, Integer pageSize, String name, String phone) {
        // 构建查询条件的包装器
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();

        // 姓名模糊搜索（使用 hasText 避免空字符串和纯空格）
        if (StringUtils.hasText(name)) {
            wrapper.like(Patient::getName, name);
        }

        // 手机号模糊搜索
        if (StringUtils.hasText(phone)) {
            wrapper.like(Patient::getPhone, phone);
        }

        // 按创建时间降序排列，最新创建的患者排在最前面
        wrapper.orderByDesc(Patient::getCreateTime);

        return baseMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }
}
