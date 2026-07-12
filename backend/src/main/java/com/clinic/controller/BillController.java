package com.clinic.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.clinic.common.ForbiddenException;
import com.clinic.common.Result;
import com.clinic.entity.Bill;
import com.clinic.security.SecurityUtils;
import com.clinic.service.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 账单管理控制器 —— 处理账单的创建、查询和支付 HTTP 请求。
 *
 * <p>
 * 本控制器管理诊所的收费流程，核心功能包括：
 * </p>
 * <ul>
 *   <li>手动创建账单（关联挂号记录、处方等）</li>
 *   <li>分页查询账单列表，支持按支付状态筛选</li>
 *   <li>账单支付确认（记录支付方式和支付时间）</li>
 * </ul>
 *
 * <p>
 * <b>自动化集成</b>：处方开具时（{@link PrescriptionController#create}），
 * 系统会自动调用 {@link BillService#createBill(Long, Long)} 生成账单，
 * 将挂号费和药费汇总。因此大部分账单是通过处方开具自动生成的，
 * 本控制器提供的创建接口主要用于异常情况或纯挂号费的账单补录。
 * </p>
 *
 * <h3>请求路径前缀</h3>
 * <p>所有接口均位于 {@code /api/bills} 路径下。</p>
 *
 * @see BillService 账单服务层，封装账单创建与支付的业务逻辑
 * @see Bill 账单实体类，映射 bills 数据库表
 */
@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillController {

    /**
     * 账单服务 —— 由 Spring 通过 {@code @RequiredArgsConstructor} 自动注入。
     */
    private final BillService billService;

    private void requireRole(String... roles) {
        String currentRole = SecurityUtils.getCurrentRole();
        for (String role : roles) if (role.equals(currentRole)) return;
        throw new ForbiddenException("权限不足，当前角色：" + currentRole);
    }

    /**
     * 分页查询账单列表。
     *
     * <h4>请求方式</h4>
     * <pre>{@code GET /api/bills?page=1&pageSize=10&status=0}</pre>
     *
     * <h4>请求参数（Query String）</h4>
     * <ul>
     *   <li><b>page</b> (Integer, 可选, 默认 1) — 页码</li>
     *   <li><b>pageSize</b> (Integer, 可选, 默认 10) — 每页记录数</li>
     *   <li><b>status</b> (Integer, 可选) — 支付状态（0-未支付, 1-已支付）</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <ul>
     *   <li>成功时返回 {@link Result}{@code <IPage<Bill>>}，data 的 records 为账单列表，
     *       每条账单包含金额、状态、关联的挂号信息等</li>
     * </ul>
     *
     * @param page     页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @param status   支付状态（可选，0-未支付, 1-已支付）
     * @return 账单分页数据的统一响应结果
     */
    @GetMapping
    public Result<IPage<Bill>> list(@RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                     @RequestParam(required = false) Integer status) {
        requireRole("ADMIN", "CASHIER");
        // 委托 BillService 执行带条件的分页查询
        return Result.ok(billService.pageQuery(page, pageSize, status));
    }

    /**
     * 手动创建账单。
     *
     * <p>
     * 根据挂号记录 ID 和处方 ID 创建一条账单记录。
     * 通常情况下账单由处方开具时自动生成，此接口用于异常情况的手动补录。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code POST /api/bills}</pre>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>registrationId</b> (Long, 必填) — 挂号记录 ID</li>
     *   <li><b>prescriptionId</b> (Long, 可选) — 处方 ID，无处方时可传 null</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <ul>
     *   <li>成功时返回 {@link Result}{@code <Bill>}，data 为新创建的账单记录</li>
     * </ul>
     *
     * @param body 请求体 Map，包含 registrationId 和 prescriptionId
     * @return 包含新创建账单记录的统一响应结果
     */
    @PostMapping
    public Result<Bill> create(@RequestBody Map<String, Long> body) {
        requireRole("ADMIN", "CASHIER");
        // 委托 BillService 创建账单并计算费用
        return Result.ok(billService.createBill(body.get("registrationId"), body.get("prescriptionId")));
    }

    /**
     * 账单支付确认。
     *
     * <p>
     * 将指定账单的状态更新为"已支付"，记录支付方式和支付时间。
     * 支付成功后，患者即可取药。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code PUT /api/bills/{id}/pay}</pre>
     *
     * <h4>路径参数</h4>
     * <ul>
     *   <li><b>id</b> (Long) — 账单 ID</li>
     * </ul>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>paymentMethod</b> (String) — 支付方式（如 "现金"、"微信"、"支付宝"、"医保卡"）</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <p>成功时返回 {@link Result}{@code <Void>}。</p>
     *
     * @param id   账单ID，来自 URL 路径变量
     * @param body 请求体 Map，包含支付方式
     * @return 统一响应结果
     */
    @PutMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id, @RequestBody Map<String, String> body) {
        requireRole("ADMIN", "CASHIER");
        // 委托 BillService 处理支付确认（更新状态、记录支付方式和时间）
        billService.pay(id, body.get("paymentMethod"));
        return Result.ok();
    }
}
