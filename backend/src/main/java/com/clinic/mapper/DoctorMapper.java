package com.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.entity.Doctor;
import com.clinic.vo.DoctorVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DoctorMapper extends BaseMapper<Doctor> {

    IPage<DoctorVO> selectDoctorPage(Page<Doctor> page, @Param("name") String name, @Param("department") String department);
}
