package com.clinic.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.clinic.common.Result;
import com.clinic.entity.MedicalRecord;
import com.clinic.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 病历管理控制器 —— 处理病历的创建、查看和查询 HTTP 请求。
 *
 * <p>
 * 本控制器管理患者的电子病历，核心功能包括：
 * </p>
 * <ul>
 *   <li>创建病历记录（关联患者、就诊、医生）</li>
 *   <li>病历详情查看（解密后返回明文内容）</li>
 *   <li>病历列表查询（出于隐私保护，列表不返回加密的详细内容）</li>
 * </ul>
 *
 * <p>
 * <b>安全设计</b>：
 * </p>
 * <ul>
 *   <li>病历内容（content）在数据库中加密存储</li>
 *   <li><b>列表查询</b>：返回时将 content 字段置为 null，保护患者隐私</li>
 *   <li><b>详情查询</b>：通过 {@link MedicalRecordService#getDecrypted(Long)}
 *       解密后返回完整内容，仅限有权限的用户查看</li>
 * </ul>
 *
 * <h3>请求路径前缀</h3>
 * <p>所有接口均位于 {@code /api/records} 路径下。</p>
 *
 * @see MedicalRecordService 病历服务层，封装病历创建、加密存储与解密读取的业务逻辑
 * @see MedicalRecord 病历实体类，映射 medical_records 数据库表
 */
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class MedicalRecordController {

    /**
     * 病历服务 —— 由 Spring 通过 {@code @RequiredArgsConstructor} 自动注入。
     * 内部封装了内容加密/解密逻辑。
     */
    private final MedicalRecordService recordService;

    /**
     * 分页查询病历列表（隐去详细内容以保护隐私）。
     *
     * <p>
     * 支持按患者 ID 筛选该患者的所有病历记录。
     * <b>数据脱敏</b>：查询结果中每条病历的 content 字段会被置为 null，
     * 仅返回元数据（患者、医生、就诊时间、诊断等），
     * 详细的病历内容需要通过 {@link #detail(Long)} 接口单独获取。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code GET /api/records?page=1&pageSize=10&patientId=3}</pre>
     *
     * <h4>请求参数（Query String）</h4>
     * <ul>
     *   <li><b>page</b> (Integer, 可选, 默认 1) — 页码</li>
     *   <li><b>pageSize</b> (Integer, 可选, 默认 10) — 每页记录数</li>
     *   <li><b>patientId</b> (Long, 可选) — 患者ID，筛选指定患者的病历</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <ul>
     *   <li>成功时返回 {@link Result}{@code <IPage<MedicalRecord>>}，data 的 records 为病历列表
     *       （每条病历的 content 字段为 null，仅有基本信息）</li>
     * </ul>
     *
     * @param page      页码，默认第 1 页
     * @param pageSize  每页条数，默认 10 条
     * @param patientId 患者ID（可选）
     * @return 病历分页数据的统一响应结果（内容已脱敏）
     */
    @GetMapping
    public Result<IPage<MedicalRecord>> list(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) Long patientId) {
        // 委托 recordService 执行分页查询
        IPage<MedicalRecord> result = recordService.pageQuery(page, pageSize, patientId);
        // 隐私保护：列表不返回加密的病历详细内容
        // 将每条记录的 content 字段清空，仅保留元数据信息
        result.getRecords().forEach(r -> r.setContent(null));
        return Result.ok(result);
    }

    /**
     * 查看病历详情（解密后返回完整内容）。
     *
     * <p>
     * 根据病历 ID 获取单条病历的完整信息。
     * 系统会从数据库中读取加密存储的病历内容，
     * 调用解密服务将其还原为明文后返回给前端。
     * 只有具备相应权限的用户才能调用此接口查看完整病历。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code GET /api/records/{id}}</pre>
     *
     * <h4>路径参数</h4>
     * <ul>
     *   <li><b>id</b> (Long) — 病历 ID</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <ul>
     *   <li>成功时返回 {@link Result}{@code <MedicalRecord>}，data 为解密后的完整病历记录</li>
     * </ul>
     *
     * @param id 病历ID，来自 URL 路径变量
     * @return 包含解密后完整病历信息的统一响应结果
     */
    @GetMapping("/{id}")
    public Result<MedicalRecord> detail(@PathVariable Long id) {
        // 调用 recordService 的解密方法，将加密内容还原为明文
        return Result.ok(recordService.getDecrypted(id));
    }

    /**
     * 创建病历记录。
     *
     * <p>
     * 医生在完成就诊后，将诊断结果和治疗方案记录到病历中。
     * 病历内容（content）会由 {@link MedicalRecordService#create} 方法
     * 在保存到数据库之前自动进行加密处理。
     * </p>
     *
     * <h4>请求方式</h4>
     * <pre>{@code POST /api/records}</pre>
     *
     * <h4>请求参数（请求体 JSON）</h4>
     * <ul>
     *   <li><b>patientId</b> (Long, 必填) — 患者ID</li>
     *   <li><b>consultationId</b> (Long, 必填) — 就诊记录 ID</li>
     *   <li><b>doctorId</b> (Long, 必填) — 医生ID</li>
     *   <li><b>content</b> (String) — 病历详细内容（明文写入，服务端加密存储）</li>
     *   <li><b>diagnosis</b> (String) — 诊断结果</li>
     *   <li><b>treatmentPlan</b> (String, 可选, 默认 "") — 治疗方案</li>
     * </ul>
     *
     * <h4>返回值</h4>
     * <ul>
     *   <li>成功时返回 {@link Result}{@code <MedicalRecord>}，data 为新创建的加密病历记录</li>
     * </ul>
     *
     * @param body 请求体 Map，包含病历所需的各字段
     * @return 包含新创建病历记录的统一响应结果
     */
    @PostMapping
    public Result<MedicalRecord> create(@RequestBody Map<String, Object> body) {
        // 委托 recordService 创建病历，内容在服务层自动加密
        return Result.ok(recordService.create(
                Long.valueOf(body.get("patientId").toString()),
                Long.valueOf(body.get("consultationId").toString()),
                Long.valueOf(body.get("doctorId").toString()),
                (String) body.get("content"),                    // 详细病历内容
                (String) body.get("diagnosis"),                   // 诊断结果
                (String) body.getOrDefault("treatmentPlan", "").toString() // 治疗方案（可选）
        ));
    }
}
