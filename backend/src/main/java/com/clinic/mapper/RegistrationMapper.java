package com.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.entity.Registration;
import com.clinic.vo.RegistrationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {

    IPage<RegistrationVO> selectRegistrationPage(Page<Registration> page,
                                                  @Param("patientId") Long patientId,
                                                  @Param("status") Integer status,
                                                  @Param("scheduleDoctorId") Long scheduleDoctorId);
}
