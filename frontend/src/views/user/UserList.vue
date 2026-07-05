<template>
  <!--
    用户管理页面 UserList.vue
    - 页面用途：系统管理员管理所有用户的登录账号和权限。
    - 角色要求：仅 ADMIN 角色可访问（菜单已通过 v-if 控制）。其他角色不显示此页面。
    - 核心功能：
      1. 用户列表：用户名、姓名、手机号、角色、启用/禁用状态
      2. 新增用户：创建登录账号（用户名、密码、角色、启用状态）
      3. 编辑用户：修改用户信息和角色（用户名不可修改）
      4. 启用/禁用切换：快速切换用户账号状态
      5. 权限保护：admin 超级管理员账号不允许禁用
    - 角色枚举：ADMIN(管理员) / DOCTOR(医生) / CASHIER(收银员)
  -->
  <div class="page-card">
    <!-- 页面工具栏 -->
    <div class="page-toolbar">
      <h3>用户管理</h3>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新增用户
      </el-button>
    </div>

    <!--
      用户数据表格
      - 角色列：通过 roleType 函数映射不同的标签颜色：
        ADMIN=红色(danger), DOCTOR=蓝色(primary), CASHIER=橙色(warning)
      - 状态列：启用=绿色(success), 禁用=红色(danger)
      - 操作列：编辑按钮 + 启用/禁用切换按钮
        · admin 用户不显示启用/禁用按钮（v-if="row.username !== 'admin'"）
    -->
    <el-table :data="tableData" border v-loading="loading" empty-text="暂无用户">
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <!-- 角色：不同颜色标签区分 -->
      <el-table-column label="角色" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="roleType(row.role)" size="small" effect="light">{{ row.role }}</el-tag>
        </template>
      </el-table-column>
      <!-- 账号状态：1=启用, 0=禁用 -->
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="light">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <!-- admin 超级管理员不允许启用/禁用操作 -->
          <el-button v-if="row.username !== 'admin'" type="warning" link @click="handleToggle(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="page" v-model:page-size="pageSize" :total="total"
      :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @change="fetchData"
    />

    <!--
      新增/编辑用户弹窗
      - 编辑模式下用户名不可更改（:disabled="!!editing"）
      - 新增模式下显示密码输入框（v-if="!editing"），默认密码 admin123
      - 角色选择：管理员 ADMIN / 医生 DOCTOR / 收银员 CASHIER
      - 状态切换：启用/禁用的开关
    -->
    <el-dialog :title="editing ? '编辑用户' : '新增用户'" v-model="showDialog" width="420px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <!-- 用户名：编辑模式下不可修改 -->
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="!!editing" placeholder="登录用户名" />
        </el-form-item>
        <!-- 密码：仅新增时显示 -->
        <el-form-item label="密码" prop="password" v-if="!editing">
          <el-input v-model="form.password" type="password" placeholder="默认 admin123" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <!-- 角色选择 -->
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width:100%">
            <el-option label="管理员 ADMIN" value="ADMIN" />
            <el-option label="医生 DOCTOR" value="DOCTOR" />
            <el-option label="收银员 CASHIER" value="CASHIER" />
          </el-select>
        </el-form-item>
        <!-- 状态开关：active-value=1启用, inactive-value=0禁用 -->
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
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
import { ref, reactive } from 'vue'                   // Vue 3 API
import { ElMessage, ElMessageBox } from 'element-plus' // Element Plus
import { Plus } from '@element-plus/icons-vue'        // 图标
import { userApi } from '@/api'                        // 用户管理API

// -- 表格状态 --
const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// -- 弹窗与表单 --
const showDialog = ref(false)
const editing = ref(null)        // 编辑中的用户ID，null=新增
const formRef = ref()

// 用户表单默认值：默认角色为DOCTOR，状态为启用(1)
const form = reactive({
  username: '', password: 'admin123', realName: '', phone: '', role: 'DOCTOR', status: 1
})

// 验证规则：用户名和角色为必填
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

/**
 * roleType - 角色标签颜色映射
 * 
 *   ADMIN   -> danger(红色) - 最高权限角色
 *   DOCTOR  -> primary(蓝色) - 医生角色
 *   CASHIER -> warning(橙色) - 收银员角色
 */
function roleType(r) { const m = { ADMIN: 'danger', DOCTOR: 'primary', CASHIER: 'warning' }; return m[r] || '' }

/**
 * fetchData - 获取用户列表
 * 
 * 数据流：
 *   userApi.list({ page, pageSize }) -> GET /api/users?page=1&pageSize=10
 *   -> 更新 tableData 和 total
 * 
 * 注意：此接口通常需要 ADMIN 权限，后端会验证 token 中的角色
 */
async function fetchData() {
  loading.value = true
  try {
    const res = await userApi.list({ page: page.value, pageSize: pageSize.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) { /* handled */ }
  finally { loading.value = false }
}

/**
 * openCreate - 打开新增用户弹窗
 * 重置表单为默认值（角色DOCTOR、状态启用、密码admin123）
 */
function openCreate() {
  editing.value = null
  Object.assign(form, { username: '', password: 'admin123', realName: '', phone: '', role: 'DOCTOR', status: 1 })
  showDialog.value = true
}

/**
 * openEdit - 打开编辑用户弹窗
 * 
 * 注意：编辑时 password 字段不存在（v-if="!editing"），
 * 所以 Object.assign 不需要包含 password
 */
function openEdit(row) {
  editing.value = row.id
  Object.assign(form, { username: row.username, realName: row.realName, phone: row.phone, role: row.role, status: row.status })
  showDialog.value = true
}

/**
 * handleToggle - 启用/禁用用户切换
 * 
 * 数据流：
 *   确认对话框 -> userApi.toggleStatus(id)
 *   -> PUT /api/users/{id}/toggle-status (后端切换status字段)
 *   -> 刷新列表
 * 
 * 注意：admin 用户不显示此操作按钮
 */
async function handleToggle(row) {
  const action = row.status === 1 ? '禁用' : '启用'
  await ElMessageBox.confirm(`确定${action}该用户吗？`, '提示', { type: 'warning' })
  await userApi.toggleStatus(row.id)
  ElMessage.success(`${action}成功`)
  fetchData()
}

/**
 * handleSave - 保存用户（新增/编辑）
 * 
 * 数据流：
 *   表单验证 -> 判断模式 ->
 *   新增: userApi.create(form) -> POST /api/users (后端创建用户和关联角色)
 *   编辑: userApi.update({ ...form, id }) -> PUT /api/users/{id}
 *   -> 成功后关闭弹窗，刷新列表
 */
async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (editing.value) {
      await userApi.update({ ...form, id: editing.value })
      ElMessage.success('更新成功')
    } else {
      await userApi.create(form)
      ElMessage.success('新增成功')
    }
    showDialog.value = false
    fetchData()
  } catch (e) { /* handled */ }
}

// 组件初始化：加载用户列表
fetchData()
</script>
