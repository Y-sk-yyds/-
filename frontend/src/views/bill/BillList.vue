<template>
  <!--
    收费管理页面 BillList.vue
    - 页面用途：管理账单创建和费用收取，属于诊所收费收银流程。
    - 角色要求：收银员(CASHIER)为主要操作角色，负责账单生成和收费结算。
    - 核心功能：
      1. 账单列表：展示账单号、挂号ID、挂号费、药费、总金额、支付方式、状态
      2. 创建账单弹窗：输入挂号ID和处方ID生成账单
      3. 收费结算弹窗：选择支付方式（微信/支付宝/现金/银行卡）完成收款
      4. 支付方式映射：WECHAT/ALIPAY/CASH/CARD
    - 费用计算：总金额 = 挂号费 + 药费（后端自动计算）
  -->
  <div class="page-card">
    <!-- 页面工具栏 -->
    <div class="page-toolbar">
      <h3>收费管理</h3>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 创建账单
      </el-button>
    </div>

    <!--
      搜索栏：按支付状态筛选（未支付0 / 已支付1）
    -->
    <div class="search-bar">
      <el-select v-model="searchStatus" placeholder="状态筛选" clearable style="width:140px">
        <el-option label="未支付" :value="0" />
        <el-option label="已支付" :value="1" />
      </el-select>
      <el-button type="primary" @click="fetchData">搜索</el-button>
    </div>

    <!--
      收费账单表格
      - 挂号费/药费：右对齐，¥符号
      - 总金额：红色加粗大字突出显示
      - 支付方式：通过 payLabel/payType 映射为中文和标签颜色
      - 状态列：已支付(绿色) / 未支付(红色)
      - 操作列：仅未支付账单显示"收费"按钮
    -->
    <el-table :data="tableData" border v-loading="loading" empty-text="暂无账单">
      <el-table-column prop="id" label="账单号" width="80" align="center" />
      <el-table-column prop="registrationId" label="挂号ID" width="80" align="center" />
      <!-- 挂号费 -->
      <el-table-column label="挂号费" width="100" align="right">
        <template #default="{ row }">¥{{ row.registrationFee }}</template>
      </el-table-column>
      <!-- 药费 -->
      <el-table-column label="药费" width="100" align="right">
        <template #default="{ row }">¥{{ row.drugFee }}</template>
      </el-table-column>
      <!-- 总金额：红色加粗 -->
      <el-table-column label="总金额" width="110" align="right">
        <template #default="{ row }">
          <span style="font-size:16px;font-weight:700;color:#E74C3C">¥{{ row.totalAmount }}</span>
        </template>
      </el-table-column>
      <!-- 支付方式标签 -->
      <el-table-column label="支付方式" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="payType(row.paymentMethod)" size="small" effect="light">
            {{ payLabel(row.paymentMethod) }}
          </el-tag>
        </template>
      </el-table-column>
      <!-- 支付状态 -->
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="light">
            {{ row.status === 1 ? '已支付' : '未支付' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" :formatter="formatTime" />
      <!-- 操作列：仅未支付账单可收费 -->
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" type="success" link @click="handlePay(row)">收费</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="page" v-model:page-size="pageSize" :total="total"
      :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @change="fetchData"
    />

    <!--
      创建账单弹窗
      - 挂号ID：必填，数字输入
      - 处方ID：可选（部分账单不关联处方，如仅挂号费）
      - 后端根据挂号ID和处方ID自动计算费用
    -->
    <el-dialog title="创建账单" v-model="showDialog" width="400px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="挂号ID" prop="registrationId">
          <el-input-number v-model="form.registrationId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="处方ID">
          <el-input-number v-model="form.prescriptionId" :min="1" style="width:100%" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">生成账单</el-button>
      </template>
    </el-dialog>

    <!--
      收费结算弹窗
      - 显示待收总金额（红色大字）及费用明细（挂号费 + 药费）
      - 支付方式选择：微信/支付宝/现金/银行卡（el-radio 纵向排列）
      - 确认收款按钮带loading状态
    -->
    <el-dialog title="收费结算" v-model="showPayDialog" width="360px">
      <!-- 金额展示区 -->
      <div style="text-align:center;margin-bottom:20px">
        <div style="font-size:28px;font-weight:700;color:#E74C3C">¥{{ currentBill?.totalAmount }}</div>
        <div style="font-size:13px;color:#999;margin-top:4px">挂号费 ¥{{ currentBill?.registrationFee }} + 药费 ¥{{ currentBill?.drugFee }}</div>
      </div>
      <!-- 支付方式选择：纵向排列的radio按钮 -->
      <el-radio-group v-model="payMethod" class="pay-methods">
        <el-radio v-for="m in payMethods" :key="m.value" :value="m.value" border>
          {{ m.label }}
        </el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="showPayDialog = false">取消</el-button>
        <el-button type="success" :loading="saving" @click="confirmPay">确认收款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// -- 核心导入 --
import { ref, reactive } from 'vue'              // Vue 3 API
import { ElMessage } from 'element-plus'          // Element Plus消息
import { Plus } from '@element-plus/icons-vue'    // 图标
import { billApi } from '@/api'                   // 账单API

// 支付方式常量数组
const payMethods = [
  { label: '微信支付', value: 'WECHAT' },
  { label: '支付宝', value: 'ALIPAY' },
  { label: '现金', value: 'CASH' },
  { label: '银行卡', value: 'CARD' },
]

// -- 表格状态 --
const loading = ref(false)
const saving = ref(false)        // 收款按钮loading
const tableData = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// -- 搜索状态 --
const searchStatus = ref('')     // 支付状态筛选（0/1）

// -- 弹窗状态 --
const showDialog = ref(false)       // 创建账单弹窗
const showPayDialog = ref(false)    // 收费结算弹窗
const currentBill = ref(null)       // 当前正在收费的账单对象
const payMethod = ref('WECHAT')     // 默认支付方式：微信
const formRef = ref()

// 创建账单表单
const form = reactive({ registrationId: null, prescriptionId: null })
// 验证规则：挂号ID为必填
const rules = {
  registrationId: [{ required: true, message: '请输入挂号ID', trigger: 'blur' }],
}

/**
 * payLabel / payType - 支付方式映射函数
 * 
 * 将后端英文支付方式代码映射为中文显示和标签颜色：
 *   WECHAT -> 微信(默认蓝)  ALIPAY -> 支付宝(primary蓝)
 *   CASH   -> 现金(warning橙)  CARD -> 银行卡(info灰)
 */
function payLabel(v) { const m = { WECHAT: '微信', ALIPAY: '支付宝', CASH: '现金', CARD: '银行卡' }; return m[v] || '-' }
function payType(v) { const m = { WECHAT: '', ALIPAY: 'primary', CASH: 'warning', CARD: 'info' }; return m[v] || '' }

/**
 * fetchData - 获取账单列表
 * 
 * 数据流：
 *   billApi.list({ page, pageSize, status })
 *   -> GET /api/bills?page=1&pageSize=10&status=0
 *   -> 更新 tableData 和 total
 */
async function fetchData() {
  loading.value = true
  try {
    const res = await billApi.list({ page: page.value, pageSize: pageSize.value, status: searchStatus.value !== '' ? searchStatus.value : undefined })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) { /* handled */ }
  finally { loading.value = false }
}

function formatTime(row, column, value) {
  if (!value) return '-'
  if (Array.isArray(value)) {
    const [y, m, d, h = 0, min = 0] = value
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}`
  }
  return value
}

/**
 * openCreate - 打开创建账单弹窗
 * 重置表单为空值
 */
function openCreate() {
  Object.assign(form, { registrationId: null, prescriptionId: null })
  showDialog.value = true
}

/**
 * handleCreate - 生成账单
 * 
 * 数据流：
 *   表单验证 -> billApi.create({ registrationId, prescriptionId })
 *   -> POST /api/bills (后端自动计算挂号费和药费)
 *   -> 成功后关闭弹窗，刷新账单列表
 */
async function handleCreate() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  await billApi.create(form)
  ElMessage.success('账单已生成')
  showDialog.value = false
  fetchData()
}

/**
 * handlePay - 打开收费结算弹窗
 * 
 * 参数：当前行的账单对象
 * 操作：存储当前账单到 currentBill，重置支付方式为微信
 */
function handlePay(row) {
  currentBill.value = row
  payMethod.value = 'WECHAT'
  showPayDialog.value = true
}

/**
 * confirmPay - 确认收款
 * 
 * 数据流：
 *   billApi.pay(billId, { paymentMethod })
 *   -> POST /api/bills/{id}/pay
 *   -> 后端更新账单状态为已支付，记录支付方式
 *   -> 成功后关闭弹窗，刷新列表
 */
async function confirmPay() {
  saving.value = true
  try {
    await billApi.pay(currentBill.value.id, { paymentMethod: payMethod.value })
    ElMessage.success('收费成功')
    showPayDialog.value = false
    fetchData()
  } catch (e) { /* handled */ }
  finally { saving.value = false }
}

// 组件初始化：加载账单列表
fetchData()
</script>

<style scoped>
/* 支付方式选择：纵向排列，间距10px */
.pay-methods { display: flex; flex-direction: column; gap: 10px; }
.pay-methods .el-radio { width: 100%; margin-right: 0; justify-content: center; }
</style>
