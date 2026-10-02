package com.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clinic.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {

    /**
     * 按 ID 批量查询排班，<b>包含逻辑删除（deleted=1）的记录</b>。
     *
     * <p>业务背景：过去日期的排班会被置为 {@code deleted=1} 归档
     * （避免排班列表首页全是过期数据）。但历史就诊记录仍需要回显
     * 「就诊日期 / 上午下午」，若用 MyBatis-Plus 的 {@code listByIds}，
     * 逻辑删除拦截器会自动追加 {@code deleted=0}，导致日期与医生信息丢失。</p>
     *
     * <p>因此这里用原生 SQL 绕过逻辑删除，与 PC 端挂号列表
     * （{@code RegistrationMapper.selectRegistrationPage} 的 LEFT JOIN）行为保持一致。</p>
     *
     * @param ids 排班 ID 集合，调用方需保证非空
     * @return 排班列表（含已归档）
     */
    @Select("<script>"
            + "SELECT * FROM schedule WHERE id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<Schedule> selectByIdsIncludeDeleted(@Param("ids") List<Long> ids);
}
