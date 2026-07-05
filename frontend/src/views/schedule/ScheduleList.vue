<template>
  <!--
    排班管理页面 ScheduleList.vue
    - 页面用途：管理医生的出诊排班，设定各时段号源数量。
    - 角色要求：管理员负责排班设置，医生只能查看自己的排班。
    - 核心功能：
      1. 搜索：按医生筛选 + 按日期范围筛选
      2. 表格：展示医生、科室、日期、时段、号源使用进度、状态
      3. 号源进度条：显示剩余/总数比例，低于5个号源变红色预警
      4. 新增/编辑弹窗：选择医生、日期、时段、号源数、启用状态
  -->
  <div class="page-card">
    <!-- 页面工具栏 -->
    <div class="page-toolbar">
      <h3>排班管理</h3>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新增排班
      </el-button>
    </div>

    <!--
      搜索栏
      - 医生选择：下拉列表来自 doctorList（组件加载时请求全部医生）
      - 日期范围：使用 el-date-picker 的 daterange 类型，
        value-format="YYYY-MM-DD" 确保值为字符串格式
    -->
    <div class="search-bar">
      <el-select v-model="searchDoctor" placeholder="选择医生" clearable style="width:180px">
        <el-option v-for="d in doctorList" :key="d.id" :label="`${d.name} - ${d.department}`" :value="d.id" />
      </el-select>
      <el-date-picker v-model="searchDate" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" />
      <el-button type="primary" @click="fetchData">搜索</el-button>
      <el-button @click="searchDoctor=''; searchDate=[]; fetchData()">重置</el-button>
    </div>

    <!--
      排班数据表格
      - 医生和科室列：通过 getDoctorName/getDoctorDept 辅助函数从 doctorList 中查找
      - 时段列：AM=上午(蓝色), PM=下午(橙色)
      - 号源列：使用 el-progress 进度条展示剩余/总数
      - 状态列：1=正常(绿色), 0=停诊(红色)
    -->
    <el-table :data="tableData" border v-loading="loading" empty-text="暂无排班数据">
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <!-- 医生：通过 doctorList 查找显示 -->
      <el-table-column label="医生" width="110">
        <template #default="{ row }">
          {{ getDoctorName(row.doctorId) }}
        </template>
      </el-table-column>
      <!-- 科室：通过 doctorList 查找显示 -->
      <el-table-column label="科室" width="100">
        <template #default="{ row }">
          {{ getDoctorDept(row.doctorId) }}
        </template>
      </el-table-column>
      <el-table-column prop="workDate" label="日期" width="110" align="center" />
      <!-- 时段标签 -->
      <el-table-column label="时段" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.period === 'AM' ? 'primary' : 'warning'" size="small" effect="light">
            {{ row.period === 'AM' ? '上午' : '下午' }}
          </el-tag>
        </template>
      </el-table-column>
      <!-- 号源进度条：显示剩余/总数，低于5号源变红 -->
      <el-table-column label="号源" width="160" align="center">
        <template #default="{ row }">
          <el-progress :percentage="row.totalNum ? Math.round((row.remainNum / row.totalNum) * 100) : 0"
            :color="row.remainNum > 5 ? '#2775B6' : '#E74C3C'" :stroke-width="8" style="width:130px">
            <span style="font-size:12px">{{ row.remainNum }}/{{ row.totalNum }}</span>
          </el-progress>
        </template>
      </el-table-column>
      <!-- 排班状态 -->
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="light">
            {{ row.status === 1 ? '正常' : '停诊' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="page" v-model:page-size="pageSize" :total="total"
      :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @change="fetchData"
    />

    <!--
      新增/编辑排班弹窗
      - 医生选择：编辑模式下 disabled（不可修改已排班的医生）
      - 日期：日期选择器
      - 时段：单选框 AM/PM
      - 总号源：数字输入（1-200）
      - 状态：开关切换 正常/停诊
    -->
    <el-dialog :title="editing ? '编辑排班' : '新增排班'" v-model="showDialog" width="450px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="医生" prop="doctorId">
          <!-- 编辑模式下医生不可修改，防止误操作 -->
          <el-select v-model="form.doctorId" placeholder="选择医生" style="width:100%" :disabled="!!editing">
            <el-option v-for="d in doctorList" :key="d.id" :label="`${d.name} - ${d.department}`" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="workDate">
          <el-date-picker v-model="form.workDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="时段" prop="period">
          <el-radio-group v-model="form.period">
            <el-radio value="AM">上午</el-radio>
            <el-radio value="PM">下午</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="总号源" prop="totalNum">
          <el-input-number v-model="form.totalNum" :min="1" :max="200" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="停诊" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// -- 核心导入 --
import { ref, reactive, onMounted } from 'vue'        // Vue 3 API
import { ElMessage, ElMessageBox } from 'element-plus' // Element Plus
import { Plus } from '@element-plus/icons-vue'         // 图标
import { scheduleApi, doctorApi } from '@/api'         // 排班API + 医生API

// -- 数据状态 --
const loading = ref(false)
const tableData = ref([])          // 排班表格数据
const doctorList = ref([])         // 医生列表（用于下拉选择和表格显示）
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// -- 搜索状态 --
const searchDoctor = ref('')       // 搜索：医生ID
const searchDate = ref([])         // 搜索：日期范围 [startDate, endDate]

// -- 弹窗与表单 --
const showDialog = ref(false)
const editing = ref(null)
const formRef = ref()

// 排班表单默认值：上午、30个号源、正常状态
const form = reactive({
  doctorId: '', workDate: '', period: 'AM', totalNum: 30, status: 1
})

// 验证规则：医生、日期、时段、号源数全部必填
const rules = {
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  workDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  period: [{ required: true, message: '请选择时段', trigger: 'change' }],
  totalNum: [{ required: true, message: '请设置号源数', trigger: 'blur' }],
}

/**
 * getDoctorName / getDoctorDept - 辅助函数
 * 根据医生ID从 doctorList 中查找对应医生的姓名/科室
 * 用于表格中显示医生关联信息
 */
function getDoctorName(id) {
  const d = doctorList.value.find(d => d.id === id)
  return d?.name || '-'
}
function getDoctorDept(id) {
  const d = doctorList.value.find(d => d.id === id)
  return d?.department || '-'
}

/**
 * loadDoctors - 加载全部医生列表（用于下拉选择和表格关联显示）
 * 
 * 数据流：
 *   GET /api/doctors?page=1&pageSize=200  加载最多200条医生数据
 */
async function loadDoctors() {
  const res = await doctorApi.list({ page: 1, pageSize: 200 })
  doctorList.value = res.data.records
}

/**
 * fetchData - 获取排班列表（带分页和筛选条件）
 * 
 * 数据流：
 *   构建params参数，包含分页、医生ID、日期范围
 *   -> scheduleApi.list(params) -> GET /api/schedules?...
 *   -> 更新 tableData 和 total
 */
async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value, doctorId: searchDoctor.value || undefined }
    // 日期范围处理：当选择了开始和结束日期时，传递参数
    if (searchDate.value?.length === 2) {
      params.startDate = searchDate.value[0]
      params.endDate = searchDate.value[1]
    }
    const res = await scheduleApi.list(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) { /* handled */ }
  finally { loading.value = false }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { doctorId: '', workDate: '', period: 'AM', totalNum: 30, status: 1 })
  showDialog.value = true
}

function openEdit(row) {
  editing.value = row.id
  Object.assign(form, {
    doctorId: row.doctorId, workDate: row.workDate, period: row.period,
    totalNum: row.totalNum, status: row.status
  })
  showDialog.value = true
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该排班吗？', '提示', { type: 'warning' })
  try {
    await scheduleApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { /* cancelled */ }
}

/**
 * handleSave - 保存排班（新增/编辑）
 * 
 * 数据流：
 *   验证 -> 判断模式 ->
 *   新增: scheduleApi.create(form) -> POST /api/schedules
 *   编辑: scheduleApi.update(form) -> PUT /api/schedules/{id}
 */
async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (editing.value) {
      await scheduleApi.update({ ...form, id: editing.value })
      ElMessage.success('更新成功')
    } else {
      await scheduleApi.create(form)
      ElMessage.success('新增成功')
    }
    showDialog.value = false
    fetchData()
  } catch (e) { /* handled */ }
}

// 组件挂载时：加载医生列表 + 加载排班数据
onMounted(() => { loadDoctors(); fetchData() })
</script>
