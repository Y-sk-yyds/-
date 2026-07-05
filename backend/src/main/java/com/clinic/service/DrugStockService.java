package com.clinic.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clinic.entity.DrugStock;
import com.clinic.mapper.DrugStockMapper;
import org.springframework.stereotype.Service;

/**
 * 药品库存服务类
 * <p>
 * 负责药品库存信息的管理，核心功能为根据药品ID查询库存记录。
 * 继承了 MyBatis-Plus 的 {@code ServiceImpl}，自动拥有基础的增删改查能力。
 * </p>
 * <p>
 * <b>数据关系：</b>药品表（Drug）与库存表（DrugStock）通过 drug_id 建立一对多关联。
 * 库存表记录每种药品的当前库存量和库存预警阈值。
 * 当库存低于预警阈值时，前端会显示库存预警提示。
 * </p>
 *
 * @author clinic
 */
@Service
public class DrugStockService extends ServiceImpl<DrugStockMapper, DrugStock> {

    /**
     * 根据药品ID查询库存记录
     * <p>
     * 通过 MyBatis-Plus 的 LambdaQueryWrapper 构建等值查询条件，
     * 查找 drug_id 匹配的库存记录。
     * 当前设计下每种药品只有一条库存记录（一对一关系）。
     * </p>
     *
     * @param drugId 药品ID
     * @return 药品库存记录，如果不存在则返回 null
     */
    public DrugStock getByDrugId(Long drugId) {
        return baseMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DrugStock>()
                        .eq(DrugStock::getDrugId, drugId));
    }
}
