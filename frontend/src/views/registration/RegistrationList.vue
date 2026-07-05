<template>
  <!--
    挂号管理页面 RegistrationList.vue
    - 页面用途：处理患者挂号业务，将患者与医生的排班关联起来。
    - 角色要求：所有登录用户均可创建挂号记录。取消挂号仅限已挂号状态。
    - 核心功能：
      1. 搜索筛选：按患者 + 按状态（已挂号/已就诊/已取消）
      2. 挂号列表：展示患者、医生、科室、日期、时段、排队号、状态
      3. 新增挂号弹窗：选择患者 + 选择有号源的排班
      4. 取消挂号：释放号源
    - 数据联动：挂号创建需要同时关联患者(patient)和排班(schedule)
  -->
  <div class="page-card">
    <!-- 页面工具栏：新增挂号按钮在患者/排班数据未加载时禁用 -->
    <div class="page-toolbar">
      <h3>挂号管理</h3>
      <el-button type="primary" @click="openCreate" :disabled="patientList.length === 0 || scheduleList.length === 0">
        <el-icon><Plus /></el-icon> 新增挂号
      </el-button>
    </div>

    <!--
      搜索栏
      - 患者筛选：下拉选择，支持过滤搜索(filterable)
      - 状态筛选：1=已挂号, 2=已就诊, 0=已取消
    -->
    <div class="search-bar">
      <el-select v-model="searchPatient" placeholder="筛选患者" clearable filterable style="width:180px">
        <el-option v-for="p in patientList" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <el-select v-model="searchStatus" placeholder="状态筛选" clearable style="width:130px">
        <el-option label="已挂号" :value="1" />
        <el-option label="已就诊" :value="2" />
        <el-option label="已取消" :value="0" />
      </el-select>
      <el-button type="primary" @click="fetchData">搜索</el-button>
    </div>

    <!--
      挂号数据表格
      - 排队号列：使用圆形标签展示
      - 状态列：通过 statusLabel/statusType 辅助函数映射显示
      - 操作列：仅已挂号(status=1)的记录可取消
    -->
    <el-table :data="tableData" border v-loading="loading" empty-text="暂无挂号记录">
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column prop="patientName" label="患者" width="100" />
      <el-table-column prop="doctorName" label="医生" width="100" />
      <el-table-column prop="department" label="科室" width="90" />
      <el-table-column prop="workDateStr" label="日期" width="110" align="center" />
      <el-table-column prop="periodLabel" label="时段" width="70" align="center" />
      <!-- 排队号：圆形编号标签 -->
      <el-table-column prop="queueNum" label="排队号" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" round>{{ row.queueNum }}号</el-tag>
        </template>
      </el-table-column>
      <!-- 挂号状态：映射为中文和对应颜色 -->
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small" effect="light">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="挂号时间" width="160" />
      <!-- 操作：仅已挂号状态可取消 -->
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 1" type="danger" link @click="handleCancel(row)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="page" v-model:page-size="pageSize" :total="total"
      :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @change="fetchData"
    />

    <!--
      新增挂号弹窗
      - 患者选择：filterable 支持输入过滤
      - 排班选择：从 availableSchedules 计算属性获取，
        自动过滤掉 state!=1 或 remainNum<=0 的排班
      - 排班选项展示：日期 + 时段 + 医生 + 剩余号源数
    -->
    <el-dialog title="新增挂号" v-model="showDialog" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="患者" prop="patientId">
          <el-select v-model="form.patientId" placeholder="选择患者" filterable style="width:100%">
            <el-option v-for="p in patientList" :key="p.id" :label="`${p.name} (${p.phone || '-'})`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排班" prop="scheduleId">
          <el-select v-model="form.scheduleId" placeholder="选择排班" style="width:100%">
            <!-- 循环渲染可用排班，号源为0的排班设置为 disabled -->
            <el-option v-for="s in availableSchedules" :key="s.id"
              :label="renderSchedule(s)" :value="s.id"
              :disabled="s.remainNum <= 0"
            >
              <span>{{ renderSchedule(s) }}</span>
              <!-- 剩余号源标签：多于5个绿色，少于等于5个红色 -->
              <el-tag size="small" :type="s.remainNum > 5 ? 'success' : 'danger'" style="float:right">
                剩 {{ s.remainNum }} 号
              </el-tag>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确认挂号</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// -- 核心导入 --
import { ref, reactive, computed, onMounted } from 'vue'   // Vue 3 API
import { ElMessage, ElMessageBox } from 'element-plus'     // Element Plus
import { Plus } from '@element-plus/icons-vue'             // 图标
import { registrationApi, patientApi, scheduleApi, doctorApi } from '@/api'  // 多API

// -- 数据与引用列表状态 --
const loading = ref(false)
const saving = ref(false)        // 保存/挂号提交loading
const tableData = ref([])        // 挂号记录表格数据
const patientList = ref([])      // 患者列表（下拉选择用）
const scheduleList = ref([])     // 排班列表（下拉选择用）
const doctorList = ref([])       // 医生列表（辅助显示用）
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// -- 搜索状态 --
const searchPatient = ref('')    // 按患者筛选
const searchStatus = ref('')     // 按状态筛选

// -- 弹窗与表单 --
const showDialog = ref(false)
const formRef = ref()
const form = reactive({ patientId: '', scheduleId: '' })

// 验证规则：患者和排班均为必选
const rules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  scheduleId: [{ required: true, message: '请选择排班', trigger: 'change' }],
}

/**
 * availableSchedules - 计算属性：可用排班列表
 * 
 * 过滤条件：
 *   1. status === 1（正常排班，非停诊）
 *   2. remainNum > 0（有余号）
 * 
 * 排序：按日期升序，同期按上午优先（AM在PM前面）
 * 
 * 数据流：
 *   scheduleList 变化时自动重新计算
 */
const availableSchedules = computed(() =>
  scheduleList.value.filter(s => s.status === 1 && s.remainNum > 0)
    .sort((a, b) => a.workDate.localeCompare(b.workDate) || a.period.localeCompare(b.period))
)

// -- 辅助函数 --

/**
 * getDoctorName / getDoctorDept - 根据医生ID查找姓名/科室
 */
function getDoctorName(id) {
  return doctorList.value.find(d => d.id === id)?.name || '-'
}
function getDoctorDept(id) {
  return doctorList.value.find(d => d.id === id)?.department || '-'
}

/**
 * renderSchedule - 格式化排班选项显示文本
 * 格式：YYYY-MM-DD 上午/下午 | 医生名 (科室)
 */
function renderSchedule(s) {
  return `${s.workDate} ${s.period === 'AM' ? '上午' : '下午'} | ${getDoctorName(s.doctorId)} (${getDoctorDept(s.doctorId)})`
}

/**
 * statusLabel / statusType - 状态码映射
 * 0=已取消(info), 1=已挂号(primary), 2=已就诊(success), 3=爽约(danger)
 */
function statusLabel(s) {
  const map = { 0: '已取消', 1: '已挂号', 2: '已就诊', 3: '爽约' }
  return map[s] || s
}
function statusType(s) {
  const map = { 0: 'info', 1: 'primary', 2: 'success', 3: 'danger' }
  return map[s] || 'info'
}

/**
 * loadRefs - 加载所有引用数据（患者、排班、医生）
 * 
 * 使用 Promise.all 并发请求三个API，提升加载效率
 * - 患者：加载最多500条
 * - 排班：从今天开始的排班数据
 * - 医生：加载全部医生
 * 
 * 数据流：
 *   Promise.all([patientApi.list, scheduleApi.list, doctorApi.list])
 *   -> 将结果分别赋值到 patientList, scheduleList, doctorList
 */
async function loadRefs() {
  const [patients, schedules, doctors] = await Promise.all([
    patientApi.list({ page: 1, pageSize: 500 }),
    scheduleApi.list({ page: 1, pageSize: 500, startDate: new Date().toISOString().slice(0, 10) }),
    doctorApi.list({ page: 1, pageSize: 200 }),
  ])
  patientList.value = patients.data.records
  scheduleList.value = schedules.data.records
  doctorList.value = doctors.data.records
}

/**
 * fetchData - 获取挂号记录列表
 * 
 * 数据流：
 *   registrationApi.list(params) -> GET /api/registrations?...
 *   -> 更新 tableData 和 total
 */
async function fetchData() {
  loading.value = true
  try {
    const res = await registrationApi.list({
      page: page.value, pageSize: pageSize.value,
      patientId: searchPatient.value || undefined,
      status: searchStatus.value !== '' ? searchStatus.value : undefined
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) { /* handled */ }
  finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { patientId: '', scheduleId: '' })
  showDialog.value = true
}

/**
 * handleCancel - 取消挂号
 * 
 * 数据流：
 *   确认对话框 -> registrationApi.cancel(id) -> POST /api/registrations/{id}/cancel
 *   -> 后端自动释放对应号源 -> 刷新列表
 */
async function handleCancel(row) {
  await ElMessageBox.confirm('确定取消该挂号吗？号源将自动释放。', '提示', { type: 'warning' })
  try {
    await registrationApi.cancel(row.id)
    ElMessage.success('取消成功')
    fetchData()
  } catch (e) { /* cancelled */ }
}

/**
 * handleSave - 创建挂号
 * 
 * 数据流：
 *   表单验证 -> registrationApi.create({ patientId, scheduleId })
 *   -> POST /api/registrations (后端自动生成排队号、扣减号源)
 *   -> 成功后刷新列表
 */
async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await registrationApi.create(form)
    ElMessage.success('挂号成功')
    showDialog.value = false
    fetchData()
  } catch (e) { /* handled */ }
  finally { saving.value = false }
}

// 组件挂载：加载引用数据 + 挂号记录
onMounted(() => { loadRefs(); fetchData() })
</script>
