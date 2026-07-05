<template>
  <!--
    处方管理页面 PrescriptionList.vue
    - 页面用途：医生开具处方，包含诊断信息和药品明细列表。
    - 角色要求：仅医生角色可开具处方。其他角色只能查看。
    - 核心功能：
      1. 处方列表：展示处方ID、就诊ID、诊断、用药说明、收费状态、时间
      2. 开具处方弹窗：选择就诊ID、填写诊断、动态添加药品明细行
      3. 药品明细行：每行包含药品选择、数量、用法、用量、删除按钮
      4. 状态流：待收费(0) -> 收费完成后变为已收费(1)
    - 数据联动：开具处方时需要从 drugList 中选择药品
  -->
  <div class="page-card">
    <!-- 页面工具栏 -->
    <div class="page-toolbar">
      <h3>处方管理</h3>
      <!-- 开具处方按钮：药品列表未加载时禁用 -->
      <el-button type="primary" @click="openCreate" :disabled="drugList.length === 0">
        <el-icon><Plus /></el-icon> 开具处方
      </el-button>
    </div>

    <!--
      处方数据表格
      - 诊断列：长文本溢出提示
      - 用药说明列：展示处方内容概览
      - 状态列：待收费=橙色(warning) / 已收费=绿色(success)
    -->
    <el-table :data="tableData" border v-loading="loading" empty-text="暂无处方">
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column prop="consultationId" label="就诊ID" width="80" align="center" />
      <el-table-column prop="diagnosis" label="诊断" min-width="140" show-overflow-tooltip />
      <el-table-column prop="prescriptionContent" label="用药说明" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'warning' : 'success'" size="small" effect="light">
            {{ row.status === 0 ? '待收费' : '已收费' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="160" />
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="page" v-model:page-size="pageSize" :total="total"
      :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @change="fetchData"
    />

    <!--
      开具处方弹窗
      - 就诊ID：数字输入框，关联已有就诊记录
      - 诊断：文本输入框
      - 用药说明：文本域
      - 药品明细区域（核心功能）：
        · 使用 v-for 循环渲染动态药品行
        · 每行包含：药品选择(下拉)、数量(数字输入)、单位、用法、用量、删除按钮
        · "+ 添加药品"按钮追加新行
        · 最少保留一行，最后一行禁用删除按钮
    -->
    <el-dialog title="开具处方" v-model="showDialog" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <!-- 就诊关联 -->
        <el-form-item label="就诊ID" prop="consultationId">
          <el-input-number v-model="form.consultationId" :min="1" style="width:200px" />
        </el-form-item>
        <!-- 诊断 -->
        <el-form-item label="诊断" prop="diagnosis">
          <el-input v-model="form.diagnosis" placeholder="请输入诊断" />
        </el-form-item>
        <!-- 用药说明 -->
        <el-form-item label="用药说明">
          <el-input v-model="form.prescriptionContent" type="textarea" :rows="2" placeholder="用法用量说明" />
        </el-form-item>

        <!-- 药品明细分隔线 -->
        <el-divider>药品明细</el-divider>

        <!--
          动态药品明细行
          - v-for="(item, idx) in form.drugs" 循环渲染每个药品项
          - 药品选择：下拉框带搜索(filterable)，显示药品名称和单价
          - 数量：数字输入，范围1-99
          - 用法/用量：文本输入
          - 删除按钮：最后一行不可删除（:disabled="form.drugs.length <= 1"）
        -->
        <div v-for="(item, idx) in form.drugs" :key="idx" class="drug-row">
          <el-select v-model="item.drugId" placeholder="药品" filterable style="width:160px">
            <el-option v-for="d in drugList" :key="d.id" :label="`${d.name} (¥${d.unitPrice})`" :value="d.id" />
          </el-select>
          <el-input-number v-model="item.quantity" :min="1" :max="99" size="small" style="width:100px" />
          <span class="drug-unit">{{ item.unit || '盒' }}</span>
          <el-input v-model="item.usageMethod" placeholder="用法" style="width:80px" />
          <el-input v-model="item.dosage" placeholder="用量" style="width:80px" />
          <el-button text type="danger" @click="form.drugs.splice(idx, 1)" :disabled="form.drugs.length <= 1">✕</el-button>
        </div>

        <!-- 添加药品按钮：向 drugs 数组追加一个新药品空白行 -->
        <el-button type="primary" link @click="form.drugs.push({ drugId: '', quantity: 1, usageMethod: '口服', dosage: '', frequency: '' })">
          + 添加药品
        </el-button>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">开具处方</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// -- 核心导入 --
import { ref, reactive, onMounted } from 'vue'        // Vue 3 API
import { ElMessage } from 'element-plus'               // Element Plus消息
import { Plus } from '@element-plus/icons-vue'          // 图标
import { prescriptionApi, drugApi } from '@/api'       // 处方API + 药品API

// -- 表格状态 --
const loading = ref(false)       // 表格loading
const saving = ref(false)        // 保存处方loading
const tableData = ref([])        // 处方表格数据
const drugList = ref([])         // 药品列表（供处方中药品种选择使用）
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// -- 弹窗与表单 --
const showDialog = ref(false)
const formRef = ref()

// 处方表单数据
// drugs 数组为动态明细行，每行包含药品ID、数量、用法、用量、频次
const form = reactive({
  consultationId: null, diagnosis: '', prescriptionContent: '',
  drugs: [{ drugId: '', quantity: 1, usageMethod: '口服', dosage: '', frequency: '' }]
})

// 验证规则：就诊ID和诊断为必填
const rules = {
  consultationId: [{ required: true, message: '请输入就诊ID', trigger: 'blur' }],
  diagnosis: [{ required: true, message: '请输入诊断', trigger: 'blur' }],
}

/**
 * fetchData - 获取处方列表
 * 
 * 数据流：
 *   prescriptionApi.list(params) -> GET /api/prescriptions?page=1&pageSize=10
 *   -> 更新 tableData 和 total
 */
async function fetchData() {
  loading.value = true
  try {
    const res = await prescriptionApi.list({ page: page.value, pageSize: pageSize.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) { /* handled */ }
  finally { loading.value = false }
}

/**
 * loadDrugs - 加载药品列表（供处方中药品种选择下拉）
 * 
 * 数据流：
 *   drugApi.list({ pageSize: 200 }) -> GET /api/drugs
 *   -> 更新 drugList
 */
async function loadDrugs() {
  const res = await drugApi.list({ page: 1, pageSize: 200 })
  drugList.value = res.data.records
}

/**
 * openCreate - 打开开具处方弹窗
 * 
 * 重置表单：
 *   - 清空表单字段
 *   - 重置药品明细为一行空白
 *   - 从 localStorage 读取当前用户的 doctorId 用于后端关联
 */
function openCreate() {
  const user = JSON.parse(localStorage.getItem('user'))
  Object.assign(form, {
    consultationId: null, diagnosis: '', prescriptionContent: '',
    drugs: [{ drugId: '', quantity: 1, usageMethod: '口服', dosage: '', frequency: '' }],
    doctorId: user?.doctorId
  })
  showDialog.value = true
}

/**
 * handleSave - 开具处方
 * 
 * 数据流：
 *   表单验证 -> 从 localStorage 获取 doctorId
 *   -> prescriptionApi.create({ ...form, doctorId })
 *   -> POST /api/prescriptions (包含 drugs 药品明细数组)
 *   -> 后端创建处方和药品关联记录
 *   -> 成功后刷新列表
 */
async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const user = JSON.parse(localStorage.getItem('user'))
    await prescriptionApi.create({ ...form, doctorId: user?.doctorId })
    ElMessage.success('处方已开具')
    showDialog.value = false
    fetchData()
  } catch (e) { /* handled */ }
  finally { saving.value = false }
}

// 组件初始化：加载药品列表 + 加载处方列表
onMounted(() => { loadDrugs(); fetchData() })
</script>

<style scoped>
/* 药品明细行：横向排列，间距8px */
.drug-row { display: flex; gap: 8px; align-items: center; margin-bottom: 10px; }
/* 单位文字样式 */
.drug-unit { font-size: 12px; color: #909399; min-width: 20px; }
</style>
