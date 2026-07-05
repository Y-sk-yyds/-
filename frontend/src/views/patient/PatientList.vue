<template>
  <!--
    患者管理页面 PatientList.vue
    - 页面用途：管理诊所所有患者的基本信息（档案管理）。
    - 角色要求：所有登录用户均可查看。新增/编辑/删除操作通常需要管理员或医生权限。
    - 核心功能：
      1. 搜索栏：按姓名或手机号模糊搜索
      2. 数据表格：分页展示患者列表（姓名、性别、年龄、手机号、身份证、过敏史、住址）
      3. 新增/编辑弹窗：表单包含所有患者字段
      4. 删除操作：带确认对话框
  -->
  <div class="page-card">
    <!-- 页面工具栏：标题 + 新增按钮 -->
    <div class="page-toolbar">
      <h3>患者管理</h3>
      <!-- 新增患者按钮：点击打开弹窗，并重置编辑状态 -->
      <el-button type="primary" @click="showDialog = true; editing = null">
        <el-icon><Plus /></el-icon> 新增患者
      </el-button>
    </div>

    <!--
      搜索栏区域
      - 姓名搜索：支持回车触发表单提交（@keyup.enter）
      - 手机号搜索：模糊匹配
      - 搜索/重置按钮：调用 fetchData() 重新加载数据
    -->
    <div class="search-bar">
      <el-input v-model="searchName" placeholder="搜索姓名" clearable style="width:180px" @keyup.enter="fetchData" />
      <el-input v-model="searchPhone" placeholder="搜索手机号" clearable style="width:180px" />
      <el-button type="primary" @click="fetchData">搜索</el-button>
      <el-button @click="searchName=''; searchPhone=''; fetchData()">重置</el-button>
    </div>

    <!--
      患者数据表格
      - :data="tableData" - 绑定后端返回的患者列表数据
      - v-loading="loading" - 数据加载时的loading遮罩
      - 性别列使用 el-tag 显示，M=蓝色男性标签, F=红色女性标签
      - 过敏史列：有过敏史红色高亮显示，无则灰色"无"
      - 操作列：编辑/删除链接按钮
    -->
    <el-table :data="tableData" border stripe v-loading="loading" empty-text="暂无患者数据">
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column prop="name" label="姓名" width="100" />
      <!-- 性别列：根据 M/F 值显示不同颜色的标签 -->
      <el-table-column label="性别" width="70" align="center">
        <template #default="{ row }">
          <el-tag :type="row.gender === 'M' ? 'primary' : 'danger'" size="small" effect="light">
            {{ row.gender === 'M' ? '男' : '女' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="age" label="年龄" width="70" align="center" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="idCard" label="身份证号" width="180" show-overflow-tooltip />
      <!-- 过敏史列：关键信息，用红色突出显示 -->
      <el-table-column prop="allergyHistory" label="过敏史" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.allergyHistory" style="color:#E74C3C">{{ row.allergyHistory }}</span>
          <span v-else style="color:#c0c4cc">无</span>
        </template>
      </el-table-column>
      <el-table-column prop="address" label="住址" min-width="160" show-overflow-tooltip />
      <!-- 操作列：固定在右侧，不随滚动条移动 -->
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!--
      分页组件
      - v-model双向绑定当前页和每页条数
      - @change 事件在页码或条数变化时重新触发 fetchData
    -->
    <el-pagination
      v-model:current-page="page" v-model:page-size="pageSize" :total="total"
      :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @change="fetchData"
    />

    <!--
      新增/编辑患者弹窗
      - title 根据 editing 状态动态切换"编辑患者"/"新增患者"
      - destroy-on-close：关闭时销毁表单内容，重置验证状态
      - 使用 el-row/el-col 实现两列布局
    -->
    <el-dialog :title="editing ? '编辑患者' : '新增患者'" v-model="showDialog" width="520px" destroy-on-close>
      <!--
        表单：ref="formRef" 用于手动验证
        :rules="rules" 绑定验证规则（姓名为必填、性别为必选）
      -->
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <!-- 第一行：姓名 + 性别 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" placeholder="请选择" style="width:100%">
                <el-option label="男" value="M" />
                <el-option label="女" value="F" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 第二行：年龄 + 手机号 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="年龄" prop="age">
              <el-input-number v-model="form.age" :min="0" :max="150" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 身份证号（独占一行） -->
        <el-form-item label="身份证号">
          <el-input v-model="form.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <!-- 过敏史（文本域） -->
        <el-form-item label="过敏史">
          <el-input v-model="form.allergyHistory" type="textarea" :rows="2" placeholder="请输入过敏史（无则留空）" />
        </el-form-item>
        <!-- 住址 -->
        <el-form-item label="住址">
          <el-input v-model="form.address" placeholder="请输入住址" />
        </el-form-item>
      </el-form>
      <!-- 弹窗底部按钮 -->
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// -- 核心导入 --
import { ref, reactive } from 'vue'              // Vue 3 响应式API
import { ElMessage, ElMessageBox } from 'element-plus' // Element Plus 消息提示和确认框
import { Plus } from '@element-plus/icons-vue'   // Element Plus 图标
import { patientApi } from '@/api'               // 患者相关API接口

// -- 表格状态 --
const loading = ref(false)        // 表格数据加载状态
const tableData = ref([])        // 表格数据源
const page = ref(1)              // 当前页码
const pageSize = ref(10)         // 每页条数（默认10）
const total = ref(0)             // 总数据条数

// -- 搜索状态 --
const searchName = ref('')       // 搜索：患者姓名
const searchPhone = ref('')      // 搜索：手机号

// -- 弹窗与表单状态 --
const showDialog = ref(false)    // 控制新增/编辑弹窗的显示
const editing = ref(null)        // 编辑模式：存储正在编辑的患者ID，null表示新增模式
const formRef = ref()            // 表单组件引用，用于 validate()

// -- 表单数据（reactive响应式），默认值 --
const form = reactive({
  name: '', gender: 'M', age: null, phone: '', idCard: '', allergyHistory: '', address: ''
})

// -- 表单验证规则 --
const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
}

/**
 * fetchData - 获取患者列表数据（带分页和搜索条件）
 * 
 * 数据流：
 * 1. 设置 loading = true
 * 2. 调用 patientApi.list(params)  ->  GET /api/patients?page=1&pageSize=10&name=xxx
 * 3. 后端返回 { code, data: { records, total } } 格式的分页数据
 * 4. 将 records 赋值给 tableData，total 赋值给分页组件
 * 5. 设置 loading = false
 * 6. 错误由 axios 拦截器统一处理
 */
async function fetchData() {
  loading.value = true
  try {
    const res = await patientApi.list({ page: page.value, pageSize: pageSize.value, name: searchName.value, phone: searchPhone.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    // 错误已由 axios 响应拦截器统一处理
  } finally {
    loading.value = false
  }
}

/**
 * handleEdit - 编辑患者
 * 1. 将 editing 设为该患者的 ID（标识编辑模式）
 * 2. 将行数据复制到 form 对象（Object.assign 浅拷贝）
 * 3. 打开弹窗
 */
function handleEdit(row) {
  editing.value = row.id
  Object.assign(form, row)
  showDialog.value = true
}

/**
 * handleDelete - 删除患者
 * 
 * 数据流：
 * 1. ElMessageBox.confirm 弹出确认对话框
 * 2. 用户确认后调用 patientApi.delete(id) -> DELETE /api/patients/{id}
 * 3. 成功后刷新表格数据 fetchData()
 * 4. 取消或错误时不做任何处理（catch块空白）
 */
async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该患者吗？', '提示', { type: 'warning' })
  try {
    await patientApi.delete(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { /* 取消或接口错误 */ }
}

/**
 * handleSave - 保存患者（新增或编辑）
 * 
 * 数据流：
 * 1. 表单验证（formRef.value.validate()）
 * 2. 判断 editing 状态：
 *    - 有值 -> patientApi.update(form) -> PUT /api/patients/{id}
 *    - 无值 -> patientApi.create(form) -> POST /api/patients
 * 3. 成功后关闭弹窗，刷新列表
 */
async function handleSave() {
  // 表单验证，失败时返回false
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (editing.value) {
      // 编辑模式：更新操作
      await patientApi.update({ ...form, id: editing.value })
      ElMessage.success('更新成功')
    } else {
      // 新增模式：创建操作
      await patientApi.create(form)
      ElMessage.success('新增成功')
    }
    showDialog.value = false
    fetchData()
  } catch (e) { /* 错误已拦截 */ }
}

// -- 组件挂载时立即加载数据 --
fetchData()
</script>
