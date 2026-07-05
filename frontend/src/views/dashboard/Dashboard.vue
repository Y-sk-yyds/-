<template>
  <!--
    工作台仪表盘 Dashboard.vue
    - 页面用途：系统首页，展示诊所运营概况的仪表盘页面。
    - 角色要求：所有登录用户均可访问。不同角色看到的数据由后端根据权限过滤。
    - 页面结构：
      1. 欢迎栏：显示当前时间、日期、温馨问候语
      2. 统计卡片：今日挂号/待就诊/已完成/待收费 四项核心指标
      3. 图表区域：近7日门诊量趋势图 + 科室就诊占比饼图
      4. 待办提醒：待处理事项列表
  -->
  <div class="dashboard">
    <!-- 欢迎栏：展示用户名、今日日期、时间 -->
    <div class="welcome-bar">
      <div>
        <h2>早上好，{{ userStore.user?.realName }}</h2>
        <p>{{ today }} · 今日诊所运营概况</p>
      </div>
      <div class="date-badge">{{ now }}</div>
    </div>

    <!--
      统计卡片网格：4列展示核心指标
      - v-for 循环渲染 stats 数组中的每个统计项
      - :style 动态设置左边框强调色
      - 每个卡片显示：图标 + 数值 + 标题 + 趋势百分比
    -->
    <div class="stat-grid">
      <div class="stat-card" v-for="item in stats" :key="item.key" :style="{ '--accent': item.color }">
        <div class="stat-icon">
          <el-icon :size="24"><component :is="item.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.title }}</div>
        </div>
        <div class="stat-trend" :class="item.trend > 0 ? 'up' : 'down'">
          {{ item.trend > 0 ? '+' : '' }}{{ item.trend }}%
        </div>
      </div>
    </div>

    <!-- 图表行：16+8 栅格布局 -->
    <el-row :gutter="20" class="chart-row">
      <!-- 左侧：近7日门诊量趋势折线图 (占16/24) -->
      <el-col :span="16">
        <div class="chart-card">
          <div class="chart-header">
            <span>近7日门诊量统计</span>
            <el-tag size="small" type="info">本周</el-tag>
          </div>
          <!-- ECharts 图表挂载容器，ref 用于获取DOM元素 -->
          <div ref="chartRef1" class="chart-body"></div>
        </div>
      </el-col>
      <!-- 右侧：科室就诊占比饼图 (占8/24) -->
      <el-col :span="8">
        <div class="chart-card">
          <div class="chart-header">
            <span>科室就诊占比</span>
            <el-tag size="small" type="info">本月</el-tag>
          </div>
          <div ref="chartRef2" class="chart-body"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 待处理提醒面板：列表展示待办事项 -->
    <div class="chart-card">
      <div class="chart-header">
        <span>待处理提醒</span>
        <el-tag size="small" type="warning">待办</el-tag>
      </div>
      <div class="todo-list">
        <!-- 循环渲染待办事项，每项包含图标、文本、时间 -->
        <div class="todo-item" v-for="todo in todos" :key="todo.id">
          <el-icon :size="16" :color="todo.color"><WarningFilled /></el-icon>
          <span>{{ todo.text }}</span>
          <span class="todo-time">{{ todo.time }}</span>
        </div>
        <!-- 无数据时的空状态提示 -->
        <div v-if="todos.length === 0" class="empty-hint">暂无待处理事项</div>
      </div>
    </div>
  </div>
</template>

<script setup>
// -- 核心导入 --
import { ref, computed, onMounted, nextTick } from 'vue'
import { useUserStore } from '@/stores/user'
import { dashboardApi } from '@/api'
import * as echarts from 'echarts'
import { WarningFilled } from '@element-plus/icons-vue'

// -- 全局实例与状态 --
const userStore = useUserStore()   // 获取当前登录用户信息
const chartRef1 = ref()            // 第一个图表（折线图）的DOM引用
const chartRef2 = ref()            // 第二个图表（饼图）的DOM引用

// -- 计算属性 --

/**
 * now - 实时时间字符串（格式：HH:MM）
 * 使用 toLocaleTimeString 根据中文环境格式化
 */
const now = computed(() => new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }))

/**
 * today - 今日日期字符串（格式：YYYY年MM月DD日 星期X）
 * 显示完整的年月日和星期信息
 */
const today = computed(() => new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }))

// -- 响应式数据 --

/** 图表实例引用，watch 中 setOption */
let chart1 = null
let chart2 = null

/** 远程仪表盘数据缓存 */
const dashboardData = ref(null)
const loading = ref(false)

/**
 * stats - 统计卡片（从 API 数据计算）
 */
const stats = computed(() => {
  const d = dashboardData.value
  if (!d) return [
    { key: 'todayReg', title: '今日挂号', value: 0, icon: 'Tickets', color: '#2775B6', trend: 0 },
    { key: 'waiting', title: '待就诊', value: 0, icon: 'Clock', color: '#F39C12', trend: 0 },
    { key: 'completed', title: '已完成', value: 0, icon: 'CircleCheck', color: '#27AE60', trend: 0 },
    { key: 'unpaid', title: '待收费', value: 0, icon: 'Money', color: '#E74C3C', trend: 0 }
  ]
  return [
    { key: 'todayReg', title: '今日挂号', value: d.todayRegistrations ?? 0, icon: 'Tickets', color: '#2775B6', trend: 0 },
    { key: 'waiting', title: '待就诊', value: d.waitingConsultations ?? 0, icon: 'Clock', color: '#F39C12', trend: 0 },
    { key: 'completed', title: '已完成', value: d.completedToday ?? 0, icon: 'CircleCheck', color: '#27AE60', trend: 0 },
    { key: 'unpaid', title: '待收费', value: d.unpaidBills ?? 0, icon: 'Money', color: '#E74C3C', trend: 0 }
  ]
})

/** 待办列表 */
const todos = computed(() => dashboardData.value?.todos ?? [])

/**
 * updateCharts - 用远程数据更新图表
 */
function updateCharts() {
  const d = dashboardData.value
  if (!d || !chart1 || !chart2) return

  // 折线图：近7日趋势
  const trend = d.weeklyTrend || []
  chart1.setOption({
    xAxis: { data: trend.map(t => t.date) },
    series: [{ data: trend.map(t => t.count ?? 0) }]
  })

  // 饼图：科室分布
  const dept = d.departmentDistribution || []
  const colors = ['#2775B6', '#1ABC9C', '#F39C12', '#bdc3c7']
  chart2.setOption({
    series: [{
      data: dept.map((item, i) => ({
        value: item.value, name: item.name,
        itemStyle: { color: colors[i % colors.length] }
      }))
    }]
  })
}

/**
 * initCharts - 初始化 ECharts 图表骨架（占位数据由 updateCharts 覆盖）
 */
function initCharts() {
  // 折线图
  chart1 = echarts.init(chartRef1.value)
  chart1.setOption({
    tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#eee', textStyle: { color: '#333' } },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category', data: [],
      axisLine: { lineStyle: { color: '#eee' } },
      axisLabel: { color: '#999' }
    },
    yAxis: {
      type: 'value', splitLine: { lineStyle: { color: '#f5f5f5' } },
      axisLabel: { color: '#999' }
    },
    series: [{
      data: [], type: 'line', smooth: true,
      lineStyle: { color: '#2775B6', width: 3 },
      itemStyle: { color: '#2775B6' },
      areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1,[
        { offset: 0, color: 'rgba(39,117,182,0.25)' },
        { offset: 1, color: 'rgba(39,117,182,0.02)' }
      ])}
    }]
  })

  // 饼图
  chart2 = echarts.init(chartRef2.value)
  chart2.setOption({
    tooltip: { trigger: 'item', backgroundColor: '#fff', borderColor: '#eee', textStyle: { color: '#333' } },
    legend: { bottom: 0, textStyle: { color: '#999', fontSize: 12 } },
    series: [{
      type: 'pie', radius: ['55%', '78%'], center: ['50%', '43%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 3 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: []
    }]
  })

  // 初始数据已加载则立即填入
  updateCharts()
}

// -- 生命周期 --

/**
 * fetchDashboard - 从后端拉取仪表盘聚合数据
 */
async function fetchDashboard() {
  loading.value = true
  try {
    const res = await dashboardApi.get()
    dashboardData.value = res.data
    updateCharts()
  } catch (e) { /* handled by interceptor */ }
  finally { loading.value = false }
}

onMounted(async () => {
  await nextTick()
  initCharts()
  fetchDashboard()
})
</script>

<style scoped>
/* 页面淡入动画 */
.dashboard { animation: fadeIn 0.4s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

/* ---- 欢迎栏 ---- */
.welcome-bar {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 24px;
}
.welcome-bar h2 { font-size: 22px; font-weight: 600; color: #1a1a2e; margin: 0; }
.welcome-bar p { font-size: 13px; color: #909399; margin: 4px 0 0; }
.date-badge {
  background: #fff; padding: 6px 16px; border-radius: 8px;
  font-size: 14px; color: #606266; font-weight: 500;
  box-shadow: var(--clinic-card-shadow);
}

/* ---- 统计卡片 ---- */
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card {
  background: #fff; border-radius: 12px; padding: 20px;
  display: flex; align-items: center; gap: 16px;
  box-shadow: var(--clinic-card-shadow); transition: all 0.25s; position: relative; overflow: hidden;
}
/* 左边彩色强调条：通过CSS变量--accent动态设置颜色 */
.stat-card::before {
  content: ''; position: absolute; top: 0; left: 0; width: 4px; height: 100%;
  background: var(--accent); border-radius: 0 4px 4px 0;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: var(--clinic-card-hover); }
.stat-icon {
  width: 48px; height: 48px; border-radius: 12px;
  background: color-mix(in srgb, var(--accent) 12%, transparent);
  color: var(--accent); display: flex; align-items: center; justify-content: center;
}
.stat-body { flex: 1; }
.stat-value { font-size: 28px; font-weight: 700; color: #1a1a2e; line-height: 1.2; }
.stat-label { font-size: 13px; color: #909399; margin-top: 2px; }
.stat-trend { font-size: 12px; font-weight: 600; padding: 2px 8px; border-radius: 6px; }
.stat-trend.up { color: #27AE60; background: rgba(39,174,96,0.1); }
.stat-trend.down { color: #E74C3C; background: rgba(231,76,60,0.1); }

/* ---- 图表区域 ---- */
.chart-row { margin-bottom: 20px; }
.chart-card {
  background: #fff; border-radius: 12px; padding: 20px;
  box-shadow: var(--clinic-card-shadow); margin-bottom: 20px;
}
.chart-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; font-size: 15px; font-weight: 600; color: #1a1a2e; }
.chart-body { height: 300px; }

/* ---- 待办事项列表 ---- */
.todo-list { display: flex; flex-direction: column; gap: 8px; }
.todo-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; background: #fafbfc; border-radius: 8px; font-size: 13px;
}
.todo-time { margin-left: auto; color: #999; font-size: 12px; }
.empty-hint { text-align: center; color: #c0c4cc; padding: 20px; font-size: 13px; }
</style>
