package com.clinic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clinic.entity.Schedule;
import com.clinic.mapper.ScheduleMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

/**
 * 排班服务类
 * <p>
 * 负责医生排班的查询管理，核心功能为分页查询排班记录。
 * 继承了 MyBatis-Plus 的 {@code ServiceImpl}，自动拥有基础的增删改查能力。
 * </p>
 * <p>
 * <b>排班说明：</b>每条排班记录包含医生ID、工作日期、时段（上午/下午）和剩余号源数。
 * 患者挂号时通过排班ID选择具体的医生时段。
 * </p>
 *
 * @author clinic
 */
@Service
public class ScheduleService extends ServiceImpl<ScheduleMapper, Schedule> {

    /**
     * 分页查询排班记录
     * <p>
     * 支持多条件筛选：
     * <ul>
     *   <li>医生ID（doctorId）：筛选特定医生的排班</li>
     *   <li>开始日期（startDate）：筛选该日期及之后的排班</li>
     *   <li>结束日期（endDate）：筛选该日期及之前的排班</li>
     * </ul>
     * 结果按工作日期升序、时段升序排列（最近的排班和上午时段排前面）。
     * </p>
     *
     * @param page      页码（从 1 开始）
     * @param pageSize  每页记录数
     * @param doctorId  医生ID（可选）
     * @param startDate 查询起始日期（可选，包含该日期）
     * @param endDate   查询截止日期（可选，包含该日期）
     * @return 分页的排班记录列表
     */
    public IPage<Schedule> pageQuery(Integer page, Integer pageSize, Long doctorId, LocalDate startDate, LocalDate endDate) {
        // 构建查询条件的包装器
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();

        // 按医生ID筛选
        if (doctorId != null) {
            wrapper.eq(Schedule::getDoctorId, doctorId);
        }

        // 按起始日期筛选：排班日期 >= 起始日期
        if (startDate != null) {
            wrapper.ge(Schedule::getWorkDate, startDate);
        }

        // 按截止日期筛选：排班日期 <= 截止日期
        if (endDate != null) {
            wrapper.le(Schedule::getWorkDate, endDate);
        }

        // 按工作日期升序、时段升序排列
        // 这样最近日期的排班在前，同一天上午排班在下午之前
        wrapper.orderByAsc(Schedule::getWorkDate).orderByAsc(Schedule::getPeriod);

        return baseMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }
}
