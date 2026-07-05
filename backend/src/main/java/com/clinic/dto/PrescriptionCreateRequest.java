package com.clinic.dto;

import lombok.Data;
import java.util.List;

/**
 * <h2>处方创建请求 DTO</h2>
 *
 * <p>医生开立处方时，前端提交的请求数据结构。
 * 包含就诊ID、医生ID、诊断结果、处方内容以及药品明细列表。</p>
 *
 * <h3>请求示例</h3>
 * <pre>{@code
 * POST /api/prescriptions
 * Content-Type: application/json
 *
 * {
 *   "consultationId": 5001,
 *   "doctorId": 1001,
 *   "diagnosis": "上呼吸道感染",
 *   "prescriptionContent": "多喝热水，注意休息",
 *   "drugs": [
 *     {
 *       "drugId": 2001,
 *       "quantity": 2,
 *       "usageMethod": "口服",
 *       "dosage": "1片/次",
 *       "frequency": "每日3次"
 *     }
 *   ]
 * }
 * }</pre>
 *
 * @author clinic
 * @version 1.0
 */
@Data
public class PrescriptionCreateRequest {

    /**
     * 就诊记录 ID
     * <p>关联的就诊问诊记录，用于追溯处方来源</p>
     */
    private Long consultationId;

    /**
     * 开方医生 ID
     * <p>标识开具此处方的医生</p>
     */
    private Long doctorId;

    /**
     * 诊断结果
     * <p>医生对患者病情的诊断描述，如"上呼吸道感染"</p>
     */
    private String diagnosis;

    /**
     * 处方医嘱内容
     * <p>非结构化的医嘱文字说明，如"多休息、多喝水"</p>
     */
    private String prescriptionContent;

    /**
     * 处方药品明细列表
     * <p>包含一种或多种药品的用法用量信息，详见 {@link DrugItem}</p>
     */
    private List<DrugItem> drugs;

    /**
     * <h2>处方药品明细项</h2>
     *
     * <p>表示处方中一种药品的具体用法用量信息，作为
     * {@link PrescriptionCreateRequest} 的内部类使用。</p>
     *
     * <h3>字段说明</h3>
     * <ul>
     *   <li><b>drugId</b> — 药品 ID，关联药品字典表</li>
     *   <li><b>quantity</b> — 开药数量</li>
     *   <li><b>usageMethod</b> — 用法（如口服、外用、静滴）</li>
     *   <li><b>dosage</b> — 单次用量（如 1片/次、5ml/次）</li>
     *   <li><b>frequency</b> — 用药频次（如每日3次、每日1次）</li>
     * </ul>
     */
    @Data
    public static class DrugItem {

        /**
         * 药品 ID，对应药品字典表（t_drug）的主键
         */
        private Long drugId;

        /**
         * 开药数量
         * <p>单位为最小包装单位（如盒、瓶、片）</p>
         */
        private Integer quantity;

        /**
         * 用法说明（如"口服"、"外用"、"静滴"等）
         */
        private String usageMethod;

        /**
         * 单次用量说明（如"1片/次"、"10ml/次"）
         */
        private String dosage;

        /**
         * 用药频次说明（如"每日3次"、"每日1次"、"必要时"）
         */
        private String frequency;
    }
}
