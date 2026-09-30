package com.clinic.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.clinic.common.ForbiddenException;
import com.clinic.common.Result;
import com.clinic.entity.Drug;
import com.clinic.entity.DrugStock;
import com.clinic.security.SecurityUtils;
import com.clinic.service.DrugService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 药品管理控制器 —— 处理药品及库存信息的增删改查 HTTP 请求。
 *
 * <p>
 * 本控制器管理诊所的药品目录和库存信息，核心功能包括：
 * </p>
 * <ul>
 *   <li>药品基本信息维护（名称、规格、单位、分类、单价）</li>
 *   <li>药品库存管理（当前库存量、预警阈值）</li>
 *   <li>创建药品时同时初始化库存记录</li>
 *   <li>分页查询药品列表，支持按名称模糊搜索</li>
 * </ul>
 *
 * <p>
 * <b>设计说明</b>：创建药品时，系统会同时创建一条 {@link DrugStock} 库存记录，
 * 包含初始库存量和预警阈值。当库存低于预警阈值时，前端可给出库存不足提示。
 * </p>
 *
 * <h3>请求路径前缀</h3>
 * <p>所有接口均位于 {@code /api/drugs} 路径下。</p>
 *
 * @see DrugService 药品服务层，封装药品与库存的业务逻辑
 * @see Drug 药品实体类，映射 drugs 数据库表
 * @see DrugStock 药品库存实体类，映射 drug_stock 数据库表
 */
@RestController
@RequestMapping("/api/drugs")
@RequiredArgsConstructor
public class DrugController {

    /**
     * 药品服务 —— 由 Spring 通过 {@code @RequiredArgsConstructor} 自动注入。
     */
    private final DrugService drugService;

    private void requireRole(String... roles) {
        String currentRole = SecurityUtils.getCurrentRole();
        for (String role : roles) if (role.equals(currentRole)) return;
        throw new ForbiddenException("权限不足，当前角色：" + currentRole);
    }

    /*
     * 分页查询药品列表。
     *
     * <h4>请求方式</h4>
     * <pre>{@code GET /api/drugs?page=1&pageSize=10&name=阿莫西林}</pre>
     *
     * <h4>请求参数（Query String）</h4>
     * <ul>
     *   <li><b>page</b> (Integer, 可选, 默认 1) — 页码</li>
     *   <li><b>pageSize</b> (Integer, 可选, 默认 10) — 每页记录数</li>
     *   <li><b>name</b> (String, 可选) — 药品名称，支持模糊搜索</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <ul>
     *   <li>成功时返回 {@link Result}{@code <IPage<Drug>>}，data 的 records 为药品列表</li>
     * </ul>
     *
     * @param page     页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @param name     药品名称（可选，模糊匹配）
     * @return 药品分页数据的统一响应结果
     */
    @GetMapping
    public Result<IPage<Drug>> list(@RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                     @RequestParam(required = false) String name,
                                     @RequestParam(required = false) Integer status) {
        // 委托 DrugService 执行带条件的分页查询
        requireRole("ADMIN", "DOCTOR");
        return Result.ok(drugService.pageQuery(page, pageSize, name, status));
    }

    /**
     * 新增药品（同时初始化库存记录）。
     *
     * <p>
     * 创建药品时，系统会同时创建一条库存记录（{@link DrugStock}）。
     * 药品状态（status）默认设置为 1（启用），
     * 库存预警阈值默认为 10（当前端未传 warningThreshold 时）。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code POST /api/drugs}</pre>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>name</b> (String, 必填) — 药品名称</li>
     *   <li><b>specification</b> (String) — 规格（如 "0.25g×24片"）</li>
     *   <li><b>unit</b> (String) — 单位（如 "盒"、"瓶"、"片"）</li>
     *   <li><b>category</b> (String) — 药品分类（如 "抗生素"、"中成药"）</li>
     *   <li><b>unitPrice</b> (BigDecimal/Number, 必填) — 单价</li>
     *   <li><b>currentStock</b> (Integer, 必填) — 初始库存数量</li>
     *   <li><b>warningThreshold</b> (Integer, 可选, 默认 10) — 库存预警阈值</li>
     *   <li><b>produceDate</b> (String, 必填) — 生产日期，格式 yyyy-MM-dd</li>
     *   <li><b>expireDate</b> (String, 必填) — 有效期至，格式 yyyy-MM-dd，须晚于生产日期</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param body 请求体 Map，包含药品基本信息和库存信息
     * @return 统一响应结果
     */
    @PostMapping
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        requireRole("ADMIN");
        // 从请求体构建药品实体对象
        Drug drug = new Drug();
        drug.setName((String) body.get("name"));
        drug.setSpecification((String) body.get("specification"));
        drug.setUnit((String) body.get("unit"));
        drug.setCategory((String) body.get("category"));
        // 单价转换为 BigDecimal 以保证精度
        drug.setUnitPrice(new java.math.BigDecimal(body.get("unitPrice").toString()));
        // 效期：录入药品时必须填写生产日期与有效期至
        // （有效期是过期自动下架与临期预警的唯一依据，缺失会导致药品永久不下架）
        LocalDate produceDate = parseDate(body.get("produceDate"));
        LocalDate expireDate = parseDate(body.get("expireDate"));
        checkExpiryDates(produceDate, expireDate);
        drug.setProduceDate(produceDate);
        drug.setExpireDate(expireDate);
        // 新药品默认状态为启用（1-启用, 0-停用）
        drug.setStatus(1);

        // 构建库存记录对象
        DrugStock stock = new DrugStock();
        stock.setCurrentStock((Integer) body.get("currentStock"));
        // 预警阈值：前端可传入，未传则默认为 10
        stock.setWarningThreshold((Integer) body.getOrDefault("warningThreshold", 10));

        // 委托 DrugService 在事务中同时创建药品和库存记录
        drugService.createDrug(drug, stock);
        return Result.ok();
    }

    /**
     * 更新药品信息（可选择是否更新库存）。
     *
     * <p>
     * 更新药品基本信息，同时支持更新库存数量和预警阈值。
     * 如果请求体中未传入 currentStock 或 warningThreshold，
     * 则不对库存记录做修改（通过 null 值判断）。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code PUT /api/drugs}</pre>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>id</b> (Long, 必填) — 药品ID</li>
     *   <li><b>name</b> (String) — 药品名称</li>
     *   <li><b>specification</b> (String) — 规格</li>
     *   <li><b>unit</b> (String) — 单位</li>
     *   <li><b>category</b> (String) — 分类</li>
     *   <li><b>unitPrice</b> (BigDecimal/Number) — 单价</li>
     *   <li><b>produceDate</b> (String, 必填) — 生产日期，格式 yyyy-MM-dd</li>
     *   <li><b>expireDate</b> (String, 必填) — 有效期至，格式 yyyy-MM-dd，须晚于生产日期</li>
     *   <li><b>currentStock</b> (Integer, 可选) — 当前库存量，不传则不更新</li>
     *   <li><b>warningThreshold</b> (Integer, 可选) — 预警阈值，不传则不更新</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param body 请求体 Map
     * @return 统一响应结果
     */
    @PutMapping
    public Result<Void> update(@RequestBody Map<String, Object> body) {
        requireRole("ADMIN");
        // 从请求体构建药品实体对象
        Drug drug = new Drug();
        drug.setId(Long.valueOf(body.get("id").toString()));
        drug.setName((String) body.get("name"));
        drug.setSpecification((String) body.get("specification"));
        drug.setUnit((String) body.get("unit"));
        drug.setCategory((String) body.get("category"));
        drug.setUnitPrice(new java.math.BigDecimal(body.get("unitPrice").toString()));
        // 效期：与新增一致，编辑药品时同样必须保留生产日期与有效期至
        // （不允许清空，否则该药品将脱离自动下架与临期预警的管控）
        LocalDate produceDate = parseDate(body.get("produceDate"));
        LocalDate expireDate = parseDate(body.get("expireDate"));
        checkExpiryDates(produceDate, expireDate);
        drug.setProduceDate(produceDate);
        drug.setExpireDate(expireDate);

        // 库存信息可选：仅当前端传了对应字段时才提取
        Integer currentStock = body.get("currentStock") != null ?
                Integer.valueOf(body.get("currentStock").toString()) : null;
        Integer warningThreshold = body.get("warningThreshold") != null ?
                Integer.valueOf(body.get("warningThreshold").toString()) : null;

        // 委托 DrugService 更新药品和库存（库存参数为 null 时不更新）
        drugService.updateDrug(drug, currentStock, warningThreshold);
        return Result.ok();
    }

    /**
     * 校验药品效期字段的完整性与先后顺序（内部工具方法）。
     *
     * <p>校验规则：</p>
     * <ul>
     *   <li>生产日期与有效期至均为必填，缺一不可 ——
     *       有效期是过期自动下架（{@code DrugExpiryScheduler}）与
     *       工作台临期预警的唯一判断依据，缺失会使该药品永久脱离管控</li>
     *   <li>有效期至必须晚于生产日期</li>
     * </ul>
     *
     * @param produceDate 生产日期
     * @param expireDate  有效期至
     * @throws IllegalArgumentException 校验不通过时抛出，由全局异常处理器转为 400 响应
     */
    private void checkExpiryDates(LocalDate produceDate, LocalDate expireDate) {
        if (produceDate == null) {
            throw new IllegalArgumentException("请填写药品生产日期");
        }
        if (expireDate == null) {
            throw new IllegalArgumentException("请填写药品有效期至");
        }
        if (!expireDate.isAfter(produceDate)) {
            throw new IllegalArgumentException("有效期至必须晚于生产日期");
        }
    }

    /**
     * 解析前端传入的日期字段（内部工具方法）。
     *
     * <p>兼容三种输入：</p>
     * <ul>
     *   <li>{@code null} —— 未传该字段，返回 {@code null}</li>
     *   <li>空字符串 / 空白串 —— 视为未填写，返回 {@code null}
     *       （随后由 {@link #checkExpiryDates} 判定为必填缺失）</li>
     *   <li>{@code "yyyy-MM-dd"} 格式字符串 —— 解析为 {@link LocalDate} 返回</li>
     * </ul>
     *
     * @param value 请求体中的原始字段值
     * @return 解析后的日期，或 {@code null}
     * @throws IllegalArgumentException 日期格式非法时抛出
     */
    private LocalDate parseDate(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式不正确，应为 yyyy-MM-dd：" + text);
        }
    }

    /**
     * 删除药品信息。
     *
     * <p>
     * 根据药品 ID 删除药品记录。
     * 注意：如果该药品已被处方引用，删除操作可能因外键约束而失败。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code DELETE /api/drugs/{id}}</pre>
     *
     * <h4>路径参数</h4>
     * <ul>
     *   <li><b>id</b> (Long) — 要删除的药品 ID</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param id 药品ID，来自 URL 路径变量
     * @return 统一响应结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        requireRole("ADMIN");
        // 调用 MyBatis-Plus 的 removeById 按主键删除
        drugService.removeById(id);
        return Result.ok();
    }
}
