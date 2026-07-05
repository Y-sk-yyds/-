<template>
  <!--
    就诊管理页面 ConsultationList.vue
    - 页面用途：医生处理患者就诊流程——接诊、记录主诉诊断、完成就诊。
    - 角色要求：仅医生角色可操作。管理员和收银员只能查看。
    - 业务场景：
      1. 待接诊区域：展示已挂号但尚未接诊的患者卡片（来自挂号记录）
      2. 就诊记录表格：展示已接诊的就诊记录
      3. 完成就诊弹窗：填写主诉、诊断、医嘱
  -->
  <div class="page-card">
    <div class="page-toolbar">
      <h3>就诊管理</h3>
    </div>

    <!--
      待接诊区域 - 仅在有等待患者时显示
      - 展示已挂号(status=1)的就诊卡片
      - 每张卡片包含患者姓名、时段标签、排队号、日期
      - "开始就诊"按钮触发接诊流程
    -->
    <div class="pending-section" v-if="pendingRegs.length > 0">
      <div class="section-title">待接诊患者</div>
      <div class="pending-cards">
        <!-- 循环渲染待就诊患者卡片 -->
        <div v-for="reg in pendingRegs" :key="reg.id" class="pending-card">
          <div class="card-info">
            <span class="card-name">{{ reg.patientName }}</span>
            <div class="card-meta">
              <el-tag size="small" type="primary" effect="light">{{ reg.periodLabel }}</el-tag>
              <span class="card-queue">{{ reg.queueNum }}号</span>
              <span class="card-date">{{ reg.workDateStr }}</span>
            </div>
          </div>
          <el-button type="primary" @click="handleStart(reg)">开始就诊</el-button>
        </div>
      </div>
      <el-divider />
    </div>
    <!-- 无待接诊患者时的空状态 -->
    <div v-else-if="!loading" class="empty-pending">
      <el-icon :size="40" color="#c0c4cc"><CircleCheck /></el-icon>
      <p>暂无待接诊患者</p>
    </div>

    <!--
      搜索栏 - 按就诊状态筛选
    -->
    <div class="search-bar">
      <el-select v-model="searchStatus" placeholder="状态筛选" clearable style="width:140px" @change="fetchData">
        <el-option label="就诊中" :value="1" />
        <el-option label="已完成" :value="2" />
      </el-select>
    </div>

    <!--
      就诊记录表格
      - 挂号ID：关联到挂号记录
      - 主诉/诊断：医疗核心信息
      - 状态列：就诊中(橙色)/已完成(绿色)
      - 操作列：就诊中的记录可"完成就诊"，已完成的显示灰色文字
    -->
    <el-table :data="tableData" border v-loading="loading" empty-text="暂无就诊记录">
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column prop="registrationId" label="挂号ID" width="80" align="center" />
      <el-table-column prop="complaint" label="主诉" min-width="120" show-overflow-tooltip />
      <el-table-column prop="diagnosis" label="诊断" min-width="140" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small" effect="light">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <!-- 就诊中(status=1)的记录可完成 -->
          <el-button v-if="row.status === 1" type="success" link @click="openComplete(row)">完成就诊</el-button>
          <span v-else style="color:#c0c4cc;font-size:13px">已完成</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="page" v-model:page-size="pageSize" :total="total"
      :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @change="fetchData"
    />

    <!--
      完成就诊弹窗
      - 填写主诉（患者主诉症状描述）
      - 填写诊断（医生诊断结果）
      - 填写医嘱（后续治疗建议）
      - 提交后更新就诊状态为已完成(status=2)
    -->
    <el-dialog title="完成就诊" v-model="showCompleteDialog" width="480px" destroy-on-close>
      <el-form ref="completeFormRef" :model="completeForm" label-width="80px">
        <el-form-item label="主诉" prop="complaint">
          <el-input v-model="completeForm.complaint" type="textarea" :rows="2" placeholder="患者主诉" />
        </el-form-item>
        <el-form-item label="诊断" prop="diagnosis">
          <el-input v-model="completeForm.diagnosis" placeholder="诊断结果" />
        </el-form-item>
        <el-form-item label="医嘱" prop="advice">
          <el-input v-model="completeForm.advice" type="textarea" :rows="2" placeholder="医嘱建议" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCompleteDialog = false">取消</el-button>
        <el-button type="primary" @click="handleComplete">完成就诊</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// -- 核心导入 --
import { ref, reactive, onMounted } from 'vue'        // Vue 3 API
import { ElMessage } from 'element-plus'              // Element Plus消息
import { CircleCheck } from '@element-plus/icons-vue'  // 图标
import { consultationApi, registrationApi } from '@/api' // 就诊API + 挂号API
import { useUserStore } from '@/stores/user'           // 用户Store

// -- 全局状态 --
const userStore = useUserStore()     // 获取当前登录医生信息（用于筛选自己的待接诊）

const loading = ref(false)           // 表格loading
const tableData = ref([])           // 就诊记录表格数据
const pendingRegs = ref([])         // 待接诊的挂号记录
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchStatus = ref('')        // 搜索状态筛选

// -- 完成就诊弹窗状态 --
const showCompleteDialog = ref(false)
const currentConsultation = ref(null)     // 当前正在处理的就诊记录
const completeFormRef = ref()
const completeForm = reactive({ complaint: '', diagnosis: '', advice: '' })

// -- 状态映射函数 --
function statusLabel(s) { const map = { 1: '就诊中', 2: '已完成' }; return map[s] || s }
function statusType(s) { const map = { 1: 'warning', 2: 'success' }; return map[s] || 'info' }

/**
 * fetchData - 获取就诊记录列表
 * 
 * 数据流：
 *   consultationApi.list(params) -> GET /api/consultations?page=1&pageSize=10&status=1
 *   -> 更新 tableData 和 total
 */
async function fetchData() {
  loading.value = true
  try {
    const res = await consultationApi.list({ page: page.value, pageSize: pageSize.value, status: searchStatus.value || undefined })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) { /* handled */ }
  finally { loading.value = false }
}

/**
 * loadPending - 加载待接诊列表
 * 
 * 查询已挂号(status=1)且分配给当前登录医生的挂号记录
 * 
 * 数据流：
 *   registrationApi.list({ status: 1, doctorId: userStore.user?.doctorId })
 *   -> GET /api/registrations?status=1&doctorId=xxx
 *   -> 更新 pendingRegs
 */
async function loadPending() {
  const res = await registrationApi.list({ page: 1, pageSize: 100, status: 1, doctorId: userStore.user?.doctorId })
  pendingRegs.value = res.data.records
}

/**
 * handleStart - 开始接诊
 * 
 * 业务流程：
 *   医生点击"开始就诊"按钮 -> 调用后端接口将挂号状态改为"就诊中"
 *   -> 创建就诊记录(status=1)
 *   -> 刷新待接诊列表和就诊记录表格
 * 
 * 数据流：
 *   consultationApi.start(registrationId) -> POST /api/consultations/start
 *   -> 后端自动: 更新挂号状态 + 创建就诊记录
 */
async function handleStart(reg) {
  try {
    await consultationApi.start(reg.id)
    ElMessage.success('接诊成功')
    loadPending()    // 刷新待接诊卡片
    fetchData()      // 刷新就诊记录表格
  } catch (e) { /* handled */ }
}

/**
 * openComplete - 打开完成就诊弹窗
 * 将当前就诊记录的数据预填到表单中
 */
function openComplete(row) {
  currentConsultation.value = row
  completeForm.complaint = row.complaint || ''
  completeForm.diagnosis = row.diagnosis || ''
  completeForm.advice = row.advice || ''
  showCompleteDialog.value = true
}

/**
 * handleComplete - 完成就诊
 * 
 * 数据流：
 *   consultationApi.complete(id, { complaint, diagnosis, advice })
 *   -> PUT /api/consultations/{id}/complete
 *   -> 后端自动: 更新挂号状态为"已就诊" + 更新就诊记录状态
 */
async function handleComplete() {
  await consultationApi.complete(currentConsultation.value.id, completeForm)
  ElMessage.success('就诊完成')
  showCompleteDialog.value = false
  fetchData()
}

// 组件挂载：加载待接诊 + 就诊记录
onMounted(() => { loadPending(); fetchData() })
</script>

<style scoped>
/* 待接诊区域样式 */
.pending-section { margin-bottom: 8px; }
.section-title { font-size: 14px; font-weight: 600; color: #1a1a2e; margin-bottom: 12px; }

/* 待接诊卡片：横向排列，自动换行 */
.pending-cards { display: flex; gap: 12px; flex-wrap: wrap; }
.pending-card {
  display: flex; align-items: center; justify-content: space-between;
  background: linear-gradient(135deg, #f0f7ff 0%, #e8f4fe 100%);
  border: 1px solid #d0e4f7; border-radius: 10px; padding: 14px 18px;
  min-width: 300px; transition: all 0.2s;
}
.pending-card:hover { border-color: #2775B6; box-shadow: 0 2px 8px rgba(39,117,182,0.12); }
.card-name { font-size: 15px; font-weight: 600; color: #1a1a2e; margin-right: 14px; }
.card-meta { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #909399; }
.card-queue { color: #2775B6; font-weight: 600; }
.card-date { font-size: 12px; color: #909399; }

/* 空状态 */
.empty-pending { text-align: center; padding: 30px 0; color: #c0c4cc; }
.empty-pending p { margin-top: 8px; font-size: 13px; }
</style>
