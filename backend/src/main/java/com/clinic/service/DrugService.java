package com.clinic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clinic.entity.Drug;
import com.clinic.entity.DrugStock;
import com.clinic.mapper.DrugMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 药品服务类
 * <p>
 * 负责药品信息的管理，核心功能包括：
 * <ul>
 *   <li>分页查询药品列表（含库存数据填充）</li>
 *   <li>新增药品（同时创建库存记录）</li>
 *   <li>更新药品信息（同时更新库存阈值）</li>
 * </ul>
 * </p>
 * <p>
 * <b>库存关联：</b>药品表和库存表通过 drug_id 一对多关联。
 * 查询药品列表时会自动批量填充当前库存量和预警阈值，避免 N+1 查询问题。
 * </p>
 *
 * @author clinic
 */
@Service
@RequiredArgsConstructor
public class DrugService extends ServiceImpl<DrugMapper, Drug> {

    /** 药品库存服务，用于关联查询和更新库存信息 */
    private final DrugStockService drugStockService;

    /**
     * 分页查询药品列表（含库存数据）
     * <p>
     * 查询流程：
     * <ol>
     *   <li>根据条件分页查询药品主表（仅查询启用状态的药品）</li>
     *   <li>如果有查询结果，收集所有药品ID，一次性批量查询对应的库存记录</li>
     *   <li>将库存数据（当前库存量、预警阈值）填充到每个药品实体的临时字段中</li>
     * </ol>
     * 采用批量查询而非逐条查询，避免出现 N+1 查询性能问题。
     * </p>
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param name     药品名称（可选，模糊匹配）
     * @return 分页的药品记录列表（含库存数据）
     */
    public IPage<Drug> pageQuery(Integer page, Integer pageSize, String name) {
        LambdaQueryWrapper<Drug> wrapper = new LambdaQueryWrapper<>();

        // 药品名称模糊搜索
        if (StringUtils.hasText(name)) {
            wrapper.like(Drug::getName, name);
        }

        // 仅查询启用状态（status=1）的药品，排除已停用的药品
        wrapper.eq(Drug::getStatus, 1).orderByDesc(Drug::getCreateTime);

        IPage<Drug> result = baseMapper.selectPage(new Page<>(page, pageSize), wrapper);

        // ===== 批量填充库存数据 =====
        if (!result.getRecords().isEmpty()) {
            // 收集当前页所有药品的ID
            List<Long> drugIds = result.getRecords().stream()
                    .map(Drug::getId)
                    .collect(Collectors.toList());

            // 一次性批量查询对应的库存记录，避免 N+1 查询
            List<DrugStock> stocks = drugStockService.list(
                    new LambdaQueryWrapper<DrugStock>().in(DrugStock::getDrugId, drugIds));

            // 构建 drugId -> DrugStock 的映射表，便于快速查找
            Map<Long, DrugStock> stockMap = stocks.stream()
                    .collect(Collectors.toMap(DrugStock::getDrugId, s -> s));

            // 为每个药品实体填充对应的库存信息
            result.getRecords().forEach(drug -> {
                DrugStock stock = stockMap.get(drug.getId());
                if (stock != null) {
                    drug.setCurrentStock(stock.getCurrentStock());         // 当前库存量
                    drug.setWarningThreshold(stock.getWarningThreshold()); // 库存预警阈值
                }
            });
        }

        return result;
    }

    /**
     * 新增药品（含库存初始化）
     * <p>
     * 业务执行流程（事务操作）：
     * <ol>
     *   <li>保存药品主表记录</li>
     *   <li>将生成的药品ID设置到库存对象中</li>
     *   <li>保存库存记录（初始库存量和预警阈值）</li>
     * </ol>
     * </p>
     *
     * @param drug  药品实体（含名称、单价、单位等基本信息）
     * @param stock 库存实体（含初始库存量和预警阈值）
     */
    @Transactional
    public void createDrug(Drug drug, DrugStock stock) {
        // 先保存药品主表，获取自增ID
        save(drug);

        // 将药品ID关联到库存记录
        stock.setDrugId(drug.getId());

        // 保存库存记录
        drugStockService.save(stock);
    }

    /**
     * 更新药品信息（含库存更新）
     * <p>
     * 业务流程：
     * <ol>
     *   <li>更新药品主表记录（名称、单价等基本信息）</li>
     *   <li>如果传入了 currentStock，同步更新库存量和预警阈值</li>
     * </ol>
     * </p>
     *
     * @param drug            药品实体（含更新后的字段值）
     * @param currentStock    当前库存量（可选，为 null 时不更新）
     * @param warningThreshold 库存预警阈值（可选，为 null 时默认使用 10）
     */
    @Transactional
    public void updateDrug(Drug drug, Integer currentStock, Integer warningThreshold) {
        // 更新药品主表
        updateById(drug);

        // 如果指定了库存量，同步更新库存记录
        if (currentStock != null) {
            drugStockService.update()
                    .set("current_stock", currentStock)
                    // 预警阈值：优先使用传入值，否则默认 10
                    .set("warning_threshold", warningThreshold != null ? warningThreshold : 10)
                    .eq("drug_id", drug.getId())
                    .update();
        }
    }
}
