<template>
  <!--
    药品管理页面 DrugList.vue
    - 页面用途：管理诊所药房的药品库存信息。
    - 角色要求：管理员负责药品信息维护。医生和收银员可查看库存。
    - 核心功能：
      1. 搜索栏：按药品名称模糊搜索
      2. 数据表格：药品名称、规格、单位、单价、分类、库存（含预警颜色）
      3. 新增/编辑弹窗：填写药品完整信息（名称、分类、规格、单位、单价、库存、预警阈值）
      4. 库存预警：当前库存 <= 预警阈值时，库存列显示红色标签
    - 分类/单位：使用常量数组提供下拉选项
  -->
  <div class="page-card">
    <!-- 页面工具栏 -->
    <div class="page-toolbar">
      <h3>药品管理</h3>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 新增药品
      </el-button>
    </div>

    <!--
      搜索栏：按药品名称搜索，支持回车触发
    -->
    <div class="search-bar">
      <el-input v-model="searchName" placeholder="搜索药品" clearable style="width:200px" @keyup.enter="fetchData" />
      <el-button type="primary" @click="fetchData">搜索</el-button>
    </div>

    <!--
      药品数据表格
      - 单价列：右对齐，¥符号前缀
      - 库存列：自动判断预警，低于阈值显示红色danger标签
      - 分类列：展示药品分类
    -->
    <el-table :data="tableData" border v-loading="loading" empty-text="暂无药品">
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column prop="name" label="药品名称" width="140" />
      <el-table-column prop="specification" label="规格" width="120" />
      <el-table-column prop="unit" label="单位" width="70" align="center" />
      <!-- 单价：货币格式右对齐 -->
      <el-table-column label="单价" width="100" align="right">
        <template #default="{ row }">¥{{ row.unitPrice }}</template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="100" />
      <!-- 库存：动态颜色，低于预警阈值变红 -->
      <el-table-column label="库存" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.currentStock <= (row.warningThreshold || 10) ? 'danger' : 'success'" size="small">
            {{ row.currentStock ?? 0 }}
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
      新增/编辑药品弹窗
      - 两列布局：名称+分类 / 规格+单位 / 单价+库存
      - 分类选择：从 categories 常量数组下拉选择
      - 单位选择：盒/瓶/支/片/袋/粒
      - 单价：支持2位小数精度
      - 预警阈值：独立一行，默认值10
    -->
    <el-dialog :title="editing ? '编辑药品' : '新增药品'" v-model="showDialog" width="460px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <!-- 第一行：名称 + 分类 -->
        <el-row :gutter="16">
          <el-col :span="14">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" placeholder="药品名称" />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="分类" prop="category">
              <el-select v-model="form.category" style="width:100%">
                <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 第二行：规格 + 单位 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规格" prop="specification">
              <el-input v-model="form.specification" placeholder="0.25g×24片" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-select v-model="form.unit" style="width:100%">
                <el-option v-for="u in units" :key="u" :label="u" :value="u" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 第三行：单价 + 库存 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="单价" prop="unitPrice">
              <el-input-number v-model="form.unitPrice" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库存" prop="currentStock">
              <el-input-number v-model="form.currentStock" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 预警阈值：独立一行 -->
        <el-form-item label="预警阈值">
          <el-input-number v-model="form.warningThreshold" :min="1" :max="999" style="width:160px" />
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
import { drugApi } from '@/api'                        // 药品API

// 药品分类常量（下拉选项）
const categories = ['抗生素', '解热镇痛', '消化系统', '心血管', '呼吸系统', '中成药', '外用药', '其他']
// 药品单位常量（下拉选项）
const units = ['盒', '瓶', '支', '片', '袋', '粒']

// -- 表格状态 --
const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

// -- 搜索状态 --
const searchName = ref('')       // 搜索药品名称

// -- 弹窗与表单 --
const showDialog = ref(false)
const editing = ref(null)        // 编辑中的药品ID，null=新增
const formRef = ref()

// 药品表单默认值：单位默认"盒"，预警阈值默认10
const form = reactive({
  name: '', specification: '', unit: '盒', category: '', unitPrice: 0, currentStock: 0, warningThreshold: 10
})

// 验证规则：名称、单位、单价为必填
const rules = {
  name: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
  unit: [{ required: true, message: '请选择单位', trigger: 'change' }],
  unitPrice: [{ required: true, message: '请输入单价', trigger: 'blur' }],
}

/**
 * fetchData - 获取药品列表
 * 
 * 数据流：
 *   drugApi.list({ page, pageSize, name }) -> GET /api/drugs?name=xxx
 *   -> 更新 tableData 和 total
 */
async function fetchData() {
  loading.value = true
  try {
    const res = await drugApi.list({ page: page.value, pageSize: pageSize.value, name: searchName.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e) { /* handled */ }
  finally { loading.value = false }
}

/**
 * openCreate - 打开新增药品弹窗
 * 重置表单为默认值
 */
function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', specification: '', unit: '盒', category: '', unitPrice: 0, currentStock: 0, warningThreshold: 10 })
  showDialog.value = true
}

/**
 * openEdit - 打开编辑药品弹窗
 * 将选中行的数据填充到表单
 */
function openEdit(row) {
  editing.value = row.id
  Object.assign(form, {
    name: row.name, specification: row.specification, unit: row.unit,
    category: row.category, unitPrice: row.unitPrice, currentStock: row.currentStock, warningThreshold: row.warningThreshold
  })
  showDialog.value = true
}

/**
 * handleDelete - 删除药品
 * 
 * 数据流：
 *   确认对话框 -> drugApi.delete(id) -> DELETE /api/drugs/{id}
 *   -> 刷新列表
 */
async function handleDelete(row) {
  await ElMessageBox.confirm('确定删除该药品吗？', '提示', { type: 'warning' })
  try { await drugApi.delete(row.id); ElMessage.success('已删除'); fetchData() } catch (e) { /* cancelled */ }
}

/**
 * handleSave - 保存药品（新增/编辑）
 * 
 * 数据流：
 *   表单验证 -> 判断模式 ->
 *   新增: drugApi.create(form) -> POST /api/drugs
 *   编辑: drugApi.update({ ...form, id }) -> PUT /api/drugs/{id}
 */
async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    if (editing.value) {
      await drugApi.update({ ...form, id: editing.value })
      ElMessage.success('更新成功')
    } else {
      await drugApi.create(form)
      ElMessage.success('新增成功')
    }
    showDialog.value = false
    fetchData()
  } catch (e) { /* handled */ }
}

// 组件初始化：加载药品列表
fetchData()
</script>
