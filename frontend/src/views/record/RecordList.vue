<template>
  <!--
    病历管理页面 RecordList.vue
    - 页面用途：查看和管理患者的电子病历记录。
    - 角色要求：医生和管理员可查看病历详情。病历内容经AES-256加密存储。
    - 核心功能：
      1. 搜索筛选：按患者姓名筛选
      2. 病历列表：展示ID、患者、诊断、医生、加密状态、创建时间
      3. 查看详情弹窗：展示完整的病历信息（患者、医生、诊断、治疗方案、病历内容）
      4. 加密标识：每行显示 AES-256 加密标签
    - 数据安全：病历内容存储时经AES-256加密，读取时自动解密
  -->
  <div class="page-card">
    <!-- 页面工具栏（仅有标题，无新增按钮，病历由系统自动生成） -->
    <div class="page-toolbar">
      <h3>病历管理</h3>
    </div>

    <!--
      搜索栏：按患者筛选，支持过滤搜索(filterable)
    -->
    <div class="search-bar">
      <el-select v-model="searchPatient" placeholder="筛选患者" filterable clearable style="width:200px">
        <el-option v-for="p in patientList" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <el-button type="primary" @click="fetchData">搜索</el-button>
    </div>

    <!--
      病历数据表格
      - 患者列：通过 getPatientName 辅助函数从 patientList 查找
      - 医生列：通过 getDoctorName 辅助函数从 doctorList 查找
      - 加密状态列：绿色标签显示 AES-256 + 锁图标
      - 操作列：仅"查看"按钮，打开详情弹窗
    -->
    <el-table :data="tableData" border v-loading="loading" empty-text="暂无病历">
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <!-- 患者：通过ID关联查找姓名 -->
      <el-table-column label="患者" width="100">
        <template #default="{ row }">{{ getPatientName(row.patientId) }}</template>
      </el-table-column>
      <el-table-column prop="diagnosis" label="诊断" min-width="160" show-overflow-tooltip />
      <!-- 医生：通过ID关联查找姓名 -->
      <el-table-column label="医生" width="100">
        <template #default="{ row }">{{ getDoctorName(row.doctorId) }}</template>
      </el-table-column>
      <!-- 加密状态：固定显示 AES-256 标识 -->
      <el-table-column label="加密状态" width="100" align="center">
        <template #default>
          <el-tag type="success" size="small" effect="light">
            <el-icon :size="12"><Lock /></el-icon> AES-256
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" :formatter="formatTime" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="viewDetail(row.id)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="page" v-model:page-size="pageSize" :total="total"
      :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @change="fetchData"
    />

    <!--
      病历详情弹窗
      - 使用 el-descriptions 组件展示结构化信息
      - 2列布局(border)，小号字体
      - 病历内容区域：使用白底灰背板显示完整的病历文本（pre-wrap 保留换行）
    -->
    <el-dialog title="病历详情" v-model="showDetail" width="560px">
      <el-descriptions v-if="detail" :column="2" border size="small">
        <!-- 患者和医生并排显示 -->
        <el-descriptions-item label="患者">{{ getPatientName(detail.patientId) }}</el-descriptions-item>
        <el-descriptions-item label="医生">{{ getDoctorName(detail.doctorId) }}</el-descriptions-item>
        <!-- 诊断独占2列 -->
        <el-descriptions-item label="诊断" :span="2">{{ detail.diagnosis }}</el-descriptions-item>
        <!-- 治疗方案独占2列 -->
        <el-descriptions-item label="治疗方案" :span="2">{{ detail.treatmentPlan || '-' }}</el-descriptions-item>
        <!-- 病历内容独占2列，灰色背景区域展示 -->
        <el-descriptions-item label="病历内容" :span="2">
          <div style="white-space:pre-wrap;line-height:1.8;background:#f8f9fa;padding:12px;border-radius:6px">
            {{ detail.content }}
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
// -- 核心导入 --
import { ref } from 'vue'                        // Vue 3 API
import { Lock } from '@element-plus/icons-vue'   // 锁图标（加密标识用）
import { recordApi, patientApi, doctorApi } from '@/api' // 病历API + 关联API

// -- 表格状态 --
const loading = ref(false)
const tableData = ref([])          // 病历表格数据
const patientList = ref([])        // 患者列表（用于下拉筛选和显示关联名称）
const doctorList = ref([])         // 医生列表（用于显示关联名称）
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// -- 搜索与弹窗状态 --
const searchPatient = ref('')      // 按患者ID筛选
const showDetail = ref(false)      // 病历详情弹窗
const detail = ref(null)           // 当前查看的病历详情数据

/**
 * getPatientName / getDoctorName - 辅助查找函数
 * 根据ID从已加载的列表中查找对应的姓名显示
 */
function getPatientName(id) { return patientList.value.find(p => p.id === id)?.name || '-' }
function getDoctorName(id) { return doctorList.value.find(d => d.id === id)?.name || '-' }

/**
 * loadRefs - 加载患者和医生引用数据
 * 
 * 使用 Promise.all 并发请求：
 *   - 患者列表：pageSize=500，足够覆盖大部分筛选需求
 *   - 医生列表：pageSize=200，覆盖所有医生
 * 
 * 数据流：
 *   Promise.all([patientApi.list, doctorApi.list])
 *   -> 赋值到 patientList 和 doctorList
 */
async function loadRefs() {
  const [pRes, dRes] = await Promise.all([
    patientApi.list({ page: 1, pageSize: 500 }),
    doctorApi.list({ page: 1, pageSize: 200 }),
  ])
  patientList.value = pRes.data.records
  doctorList.value = dRes.data.records
}

/**
 * fetchData - 获取病历列表
 * 
 * 数据流：
 *   recordApi.list({ page, pageSize, patientId })
 *   -> GET /api/records?page=1&pageSize=10&patientId=xxx
 *   -> 更新 tableData 和 total
 */
async function fetchData() {
  loading.value = true
  try {
    const res = await recordApi.list({ page: page.value, pageSize: pageSize.value, patientId: searchPatient.value || undefined })
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
 * viewDetail - 查看病历详情
 * 
 * 数据流：
 *   recordApi.getById(id) -> GET /api/records/{id}
 *   -> 后端自动解密 AES-256 加密的病历内容
 *   -> 返回完整的病历信息（诊断、治疗方案、病历内容）
 *   -> 赋值给 detail 并在弹窗中展示
 */
async function viewDetail(id) {
  const res = await recordApi.getById(id)
  detail.value = res.data
  showDetail.value = true
}

// 组件初始化：加载引用数据 + 病历列表
loadRefs()
fetchData()
</script>
