<template>
  <!--
    医生管理页面 DoctorList.vue
    - 页面用途：管理诊所所有医生信息，新增医生时自动创建对应登录账号。
    - 角色要求：管理员和医生可查看。新增/编辑/删除需要管理员权限。
    - 核心功能：
      1. 搜索栏：按姓名模糊搜索 + 按科室下拉筛选
      2. 数据表格：展示医生姓名、科室、职称、手机号、登录账号、账号状态、简介
      3. 新增/编辑弹窗：表单包含科室下拉和职称下拉选择
      4. 删除操作：删除医生时关联的登录账号将被禁用（软删除）
  -->
  <div class="page-card">
    <!-- 页面工具栏 -->
    <div class="page-toolbar">
      <h3>医生管理</h3>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新增医生
      </el-button>
    </div>

    <!--
      搜索栏
      - 姓名搜索：文本输入框，支持回车触发
      - 科室筛选：下拉选择框，选项来自 departments 常量数组
    -->
    <div class="search-bar">
      <el-input v-model="searchName" placeholder="搜索姓名" clearable style="width:160px" @keyup.enter="fetchData" />
      <el-select v-model="searchDept" placeholder="科室筛选" clearable style="width:140px">
        <el-option v-for="d in departments" :key="d" :label="d" :value="d" />
      </el-select>
      <el-button type="primary" @click="fetchData">搜索</el-button>
      <el-button @click="searchName=''; searchDept=''; fetchData()">重置</el-button>
    </div>

    <!--
      医生数据表格
      - 科室列使用 el-tag 标签展示
      - 账号状态列：启用=绿色，禁用=红色
      - 简介列：使用 show-overflow-tooltip 长文本溢出提示
    -->
    <el-table :data="tableData" border v-loading="loading" empty-text="暂无医生数据">
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column prop="name" label="姓名" width="100" />
      <!-- 科室：用标签美化显示 -->
      <el-table-column prop="department" label="科室" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.department }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="职称" width="120" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="username" label="登录账号" width="120" />
      <!-- 账号状态：1=启用，0=禁用 -->
      <el-table-column label="账号状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.userStatus === 1 ? 'success' : 'danger'" size="small" effect="light">
            {{ row.userStatus === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="introduction" label="简介" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页组件 -->
    <el-pagination
      v-model:current-page="page" v-model:page-size="pageSize" :total="total"
      :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @change="fetchData"
    />

    <!--
      新增/编辑医生弹窗
      - 科室选择：从 departments 数组下拉选择
      - 职称选择：从预定义列表（主任医师/副主任医师/主治医师/住院医师）下拉选择
    -->
    <el-dialog :title="editing ? '编辑医生' : '新增医生'" v-model="showDialog" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="科室" prop="department">
              <el-select v-model="form.department" placeholder="选择科室" style="width:100%">
                <el-option v-for="d in departments" :key="d" :label="d" :value="d" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="职称" prop="title">
              <el-select v-model="form.title" placeholder="选择职称" style="width:100%">
                <el-option label="主任医师" value="主任医师" />
                <el-option label="副主任医师" value="副主任医师" />
                <el-option label="主治医师" value="主治医师" />
                <el-option label="住院医师" value="住院医师" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="简介">
          <el-input v-model="form.introduction" type="textarea" :rows="2" placeholder="医生简介" />
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
import { ref, reactive } from 'vue'               // Vue 3 API
import { ElMessage, ElMessageBox } from 'element-plus'  // Element Plus 组件
import { Plus } from '@element-plus/icons-vue'    // 图标
import { doctorApi } from '@/api'                 // 医生API

// 预定义科室列表（下拉选项）
const departments = ['内科', '外科', '儿科', '妇科', '骨科', '眼科', '口腔科', '皮肤科', '中医科', '耳鼻喉科']

// -- 表格状态 --
const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// -- 搜索状态 --
const searchName = ref('')       // 搜索医生姓名
const searchDept = ref('')      // 搜索科室

// -- 弹窗与表单状态 --
const showDialog = ref(false)
const editing = ref(null)        // 编辑中的医生ID，null表示新增
const formRef = ref()

// 表单数据
const form = reactive({
  name: '', department: '', title: '', phone: '', introduction: ''
})

// 验证规则：姓名、科室、职称为必填/必选
const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  department: [{ required: true, message: '请选择科室', trigger: 'change' }],
  title: [{ required: true, message: '请选择职称', trigger: 'change' }],
}

/**
 * fetchData - 分页获取医生列表
 * 
 * 数据流：
 *   GET /api/doctors?page=1&pageSize=10&name=xxx&department=内科
 *   后端返回 { data: { records, total } }
 */
async function fetchData() {
  loading.value = true
  try {
    const res = await doctorApi.list({
      page: page.value, pageSize: pageSize.value,
      name: searchName.value, department: searchDept.value
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) { /* 错误已由拦截器处理 */ }
  finally { loading.value = false }
}

/**
 * openCreate - 打开新增弹窗
 * 重置表单为默认值，清空 editing 标识
 */
function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', department: '', title: '', phone: '', introduction: '' })
  showDialog.value = true
}

/**
 * openEdit - 打开编辑弹窗
 * 1. 设置 editing = 被编辑医生的 ID
 * 2. 将该行数据填入表单
 */
function openEdit(row) {
  editing.value = row.id
  Object.assign(form, row)
  showDialog.value = true
}

/**
 * handleDelete - 删除医生
 * 注意：后端会软删除关联的登录账号（禁用而非物理删除）
 * 
 * 数据流：
 *   确认对话框 -> doctorApi.delete(id) -> DELETE /api/doctors/{id} -> 刷新列表
 */
async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该医生吗？关联登录账号将被禁用。', '提示', { type: 'warning' })
  try {
    await doctorApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { /* 取消或错误 */ }
}

/**
 * handleSave - 保存医生（新增/编辑）
 * 
 * 数据流：
 *   验证表单 -> 判断模式 -> 
 *   新增: doctorApi.create(form) -> POST /api/doctors (自动创建登录账号，默认密码 admin123)
 *   编辑: doctorApi.update(form) -> PUT /api/doctors/{id}
 */
async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (editing.value) {
      await doctorApi.update({ ...form, id: editing.value })
      ElMessage.success('更新成功')
    } else {
      await doctorApi.create(form)
      ElMessage.success('新增成功，默认密码 admin123')
    }
    showDialog.value = false
    fetchData()
  } catch (e) { /* 错误已拦截 */ }
}

// 组件初始化：加载医生列表
fetchData()
</script>
