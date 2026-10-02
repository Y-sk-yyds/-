<template>
  <div class="mobile-page">
    <div class="mobile-header">
      <h2>排班挂号</h2>
    </div>

    <!-- 搜索框：按医生姓名 / 科室 / 职称模糊匹配 -->
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索医生 / 科室 / 职称"
        clearable
        :prefix-icon="Search"
      />
    </div>

    <!-- 科室分类：由当日排班动态推导，避免硬编码 -->
    <div class="dept-tabs">
      <span class="dept-chip" :class="{ active: activeDept === '' }" @click="activeDept = ''">全部</span>
      <span
        v-for="d in departments"
        :key="d"
        class="dept-chip"
        :class="{ active: activeDept === d }"
        @click="activeDept = d"
      >{{ d }}</span>
    </div>

    <!-- 日期选择 -->
    <div class="date-tabs">
      <el-button
        v-for="d in dates"
        :key="d.value"
        :type="selectedDate === d.value ? 'primary' : ''"
        size="small"
        @click="selectDate(d.value)"
      >{{ d.label }}</el-button>
    </div>

    <!-- 医生排班卡片 -->
    <div class="schedule-list" v-loading="loading">
      <div class="schedule-card" v-for="s in filteredSchedules" :key="s.id" @click="showRegister(s)">
        <div>
          <div class="doctor-name">
            {{ s.doctorName }}
            <span class="doctor-title" v-if="s.title">· {{ s.title }}</span>
          </div>
          <div class="doctor-dept">{{ s.department }}</div>
          <!-- 挂号费：向患者明示本次挂号将产生的费用 -->
          <div class="doctor-fee">挂号费 ¥{{ Number(s.registrationFee ?? 10).toFixed(2) }}</div>
        </div>
        <div class="schedule-detail">
          <span class="period-tag" :class="s.period === 'AM' ? 'am' : 'pm'">
            {{ s.period === 'AM' ? '上午' : '下午' }}
          </span>
          <span class="remain">剩余 {{ s.remainNum }} 号</span>
        </div>
        <el-icon v-if="s.remainNum > 0" color="#409EFF" :size="20"><ArrowRight /></el-icon>
        <span v-else class="full-tag">已满</span>
      </div>
      <!-- 空态：区分"当天确实没排班"与"筛选后无结果" -->
      <div v-if="!loading && filteredSchedules.length === 0" class="empty-hint">
        <div>{{ keyword || activeDept ? '没有符合条件的医生' : '当日暂无排班' }}</div>
        <el-button v-if="keyword || activeDept" size="small" class="empty-btn" @click="resetFilter">清空筛选条件</el-button>
      </div>
      <!-- 结果计数 -->
      <div v-if="!loading && filteredSchedules.length > 0" class="result-count">
        共 {{ filteredSchedules.length }} 位医生可预约
      </div>
    </div>

    <!-- 挂号弹窗 -->
    <el-dialog v-model="showDialog" title="确认挂号" width="85%" top="15vh" :close-on-click-modal="false">
      <div class="register-info" v-if="selectedSchedule">
        <p><strong>医生：</strong>{{ selectedSchedule.doctorName }}<span v-if="selectedSchedule.title"> · {{ selectedSchedule.title }}</span></p>
        <p><strong>科室：</strong>{{ selectedSchedule.department }}</p>
        <p><strong>时间：</strong>{{ selectedSchedule.workDate }} {{ selectedSchedule.period === 'AM' ? '上午' : '下午' }}</p>
        <p><strong>剩余号源：</strong>{{ selectedSchedule.remainNum }}</p>
        <p><strong>挂号费：</strong><span class="fee-text">¥{{ Number(selectedSchedule.registrationFee ?? 10).toFixed(2) }}</span></p>
      </div>
      <div class="form-row">
        <label>手机号：</label>
        <el-input v-model="phone" placeholder="请输入手机号" maxlength="11" />
      </div>
      <p class="dialog-tip">需完成缴费后才正式挂号</p>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="doRegister">下一步：缴费</el-button>
      </template>
    </el-dialog>

    <!-- 新患者注册弹窗 -->
    <el-dialog v-model="showNewPatient" title="首次就诊 · 建档" width="85%" top="12vh" :close-on-click-modal="false">
      <div class="form-row">
        <label>姓名：</label>
        <el-input v-model="newName" placeholder="请输入真实姓名" />
      </div>
      <div class="form-row">
        <label>手机号：</label>
        <el-input v-model="phone" disabled />
      </div>
      <div class="form-row">
        <label>性别：</label>
        <el-radio-group v-model="newGender">
          <el-radio label="M" size="small">男</el-radio>
          <el-radio label="F" size="small">女</el-radio>
        </el-radio-group>
      </div>
      <div class="form-row">
        <label>年龄：</label>
        <el-input-number v-model="newAge" :min="0" :max="150" size="small" style="width:100px" />
      </div>
      <template #footer>
        <el-button @click="showNewPatient = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="doCreatePatient">建档并去缴费</el-button>
      </template>
    </el-dialog>

    <!-- 缴费确认：本系统挂号含缴费动作，此处尚未占号，付款成功才生成挂号记录 -->
    <el-dialog v-model="showPayDialog" title="确认挂号并缴费" width="85%" top="14vh"
               :close-on-click-modal="false" :show-close="false">
      <div class="pay-info">
        <p class="pay-amount">¥{{ payAmount.toFixed(2) }}</p>
        <p class="pay-label">挂号费</p>
      </div>
      <div class="pay-doctor" v-if="selectedSchedule">
        <p>{{ selectedSchedule.doctorName }}<span v-if="selectedSchedule.title"> · {{ selectedSchedule.title }}</span></p>
        <p class="pay-sub">{{ selectedSchedule.department }} · {{ selectedSchedule.workDate }} {{ selectedSchedule.period === 'AM' ? '上午' : '下午' }}</p>
      </div>
      <div class="pay-methods">
        <div class="pm-item" :class="{ active: payMethod === 'WECHAT' }" @click="payMethod = 'WECHAT'">
          <span class="pm-icon wechat">微</span><span>微信支付</span>
        </div>
        <div class="pm-item" :class="{ active: payMethod === 'ALIPAY' }" @click="payMethod = 'ALIPAY'">
          <span class="pm-icon alipay">支</span><span>支付宝</span>
        </div>
      </div>
      <p class="pay-tip">付款成功后才会占用号源并生成挂号记录；取消付款则不挂号</p>
      <template #footer>
        <el-button @click="cancelPay">取消</el-button>
        <el-button type="primary" :loading="paying" @click="confirmRegisterPay">
          支付 ¥{{ payAmount.toFixed(2) }} 并挂号
        </el-button>
      </template>
    </el-dialog>

    <!-- 挂号成功 -->
    <el-dialog v-model="showSuccess" title="挂号成功" width="85%" top="15vh">
      <div class="success-info">
        <p class="queue-num">{{ queueNum }}</p>
        <p class="queue-label">您的排队号</p>
        <p class="paid-tag" v-if="paidAmount > 0">挂号费 ¥{{ paidAmount.toFixed(2) }} 已支付</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="$router.push('/mobile/records?phone=' + phone)">查看我的挂号</el-button>
      </template>
    </el-dialog>

    <!-- 底部导航 -->
    <div class="bottom-nav">
      <div class="nav-item active">
        <el-icon><List /></el-icon>
        <span>排班挂号</span>
      </div>
      <div class="nav-item" @click="$router.push('/mobile/records')">
        <el-icon><Document /></el-icon>
        <span>我的就诊</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowRight, List, Document, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { mobileApi } from '@/api'
import request from '@/utils/request'

const loading = ref(false)
const creating = ref(false)
const schedules = ref([])
const selectedDate = ref('')
const keyword = ref('')      // 搜索关键字：匹配医生姓名 / 科室 / 职称
const activeDept = ref('')   // 已选科室，空串表示「全部」
const selectedSchedule = ref(null)
const showDialog = ref(false)
const showNewPatient = ref(false)
const showSuccess = ref(false)
const showPayDialog = ref(false)   // 挂号缴费确认弹窗（此处尚未占号）
const paying = ref(false)          // 支付按钮 loading
const payMethod = ref('WECHAT')    // 支付方式：WECHAT / ALIPAY
const paidAmount = ref(0)          // 挂号成功页展示的实付金额
const phone = ref('')
const newName = ref('')
const newGender = ref('M')
const newAge = ref(30)
const queueNum = ref('')

const dates = computed(() => {
  const arr = []
  const today = new Date()
  for (let i = 0; i < 7; i++) {
    const d = new Date(today)
    d.setDate(today.getDate() + i)
    // 用本地日期拼接，不用 toISOString —— UTC 会让凌晨 0~8 点的"今天"错成昨天
    const val = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
    arr.push({ label: (i === 0 ? '今天' : (i === 1 ? '明天' : val.slice(5))) + ' ' + week, value: val })
  }
  return arr
})

// 科室列表：由当日排班动态推导，避免硬编码（后端未提供科室字典接口）
const departments = computed(() => {
  const set = new Set(schedules.value.map(s => s.department).filter(Boolean))
  return [...set].sort()
})

// 本次挂号应付的挂号费：取该医生定价，缺省 10 元（与后端兜底一致）
const payAmount = computed(() => Number(selectedSchedule.value?.registrationFee ?? 10))

// 前端过滤：当天最多 16 条排班，本地筛选足够，不新增后端接口
const filteredSchedules = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return schedules.value.filter(s => {
    if (activeDept.value && s.department !== activeDept.value) return false
    if (!kw) return true
    return [s.doctorName, s.department, s.title]
      .some(v => (v || '').toLowerCase().includes(kw))
  })
})

// 清空搜索与科室筛选（切换日期时保留筛选条件）
const resetFilter = () => { keyword.value = ''; activeDept.value = '' }

const selectDate = (val) => { selectedDate.value = val; fetchSchedules() }

const fetchSchedules = async () => {
  loading.value = true
  try {
    const res = await mobileApi.schedules(selectedDate.value)
    schedules.value = res.data || []
  } finally { loading.value = false }
}

const showRegister = (s) => {
  if (s.remainNum <= 0) return
  selectedSchedule.value = s
  showDialog.value = true
}

/**
 * 挂号第一步：校验手机号后进入缴费确认
 * 注意：此时还没有调用挂号接口 —— 号源要等付款成功才占用。
 */
const doRegister = () => {
  if (!phone.value || phone.value.length !== 11) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  showDialog.value = false
  showPayDialog.value = true
}

const doCreatePatient = async () => {
  if (!newName.value.trim()) {
    ElMessage.warning('请输入姓名')
    return
  }
  creating.value = true
  try {
    await request({ url: '/mobile/patients', method: 'post', data: { name: newName.value, phone: phone.value, gender: newGender.value, age: String(newAge.value) } })
    // 建档完成 → 进入缴费，付款成功才算挂号（与老患者同一入口）
    showNewPatient.value = false
    showPayDialog.value = true
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '建档失败')
  } finally { creating.value = false }
}

/**
 * 支付挂号费并挂号 —— 一次调用完成「扣号源 + 建挂号费单 + 结算」
 * 后端在同一流程内处理，任一环节失败都会回退，不会出现"已挂号但欠费"。
 */
const confirmRegisterPay = async () => {
  paying.value = true
  try {
    const res = await mobileApi.register({
      phone: phone.value,
      scheduleId: selectedSchedule.value.id,
      paymentMethod: payMethod.value
    })
    queueNum.value = res.data?.queueNum
    // 仅当后端确认已结算（paid=true）才展示"已支付"，避免旧接口下误报
    paidAmount.value = res.data?.paid === true
      ? Number(res.data?.billAmount ?? payAmount.value)
      : 0
    showPayDialog.value = false
    showSuccess.value = true
    fetchSchedules()   // 号源已扣，刷新剩余号源显示
  } catch (e) {
    const msg = e?.response?.data?.message || ''
    if (msg.includes('未找到该手机号')) {
      // 该手机号尚未建档 → 转建档流程，建档后再回来缴费
      showPayDialog.value = false
      showNewPatient.value = true
    } else {
      ElMessage.error(msg || '挂号失败')
    }
  } finally { paying.value = false }
}

/** 取消付款：号源与挂号记录都不会产生 */
const cancelPay = () => {
  showPayDialog.value = false
  ElMessage.info('已取消，未完成挂号')
}

onMounted(() => { selectedDate.value = dates.value[0].value; fetchSchedules() })
</script>

<style scoped>
.mobile-page { max-width: 480px; margin: 0 auto; padding-bottom: 70px; background: #f5f7fa; min-height: 100vh; font-size: 14px; }
.mobile-header { background: #409EFF; color: #fff; padding: 16px; text-align: center; }
.mobile-header h2 { margin: 0; font-size: 18px; font-weight: 500; }
/* 搜索框：与科室栏、日期栏同处白色头部区，形成"筛选区"视觉整体 */
.search-bar { background: #fff; padding: 12px 12px 0; }
/* 科室横向滚动标签 */
.dept-tabs { display: flex; overflow-x: auto; gap: 8px; padding: 10px 12px; background: #fff; white-space: nowrap; border-bottom: 1px solid #f0f0f0; }
.dept-chip { flex: 0 0 auto; padding: 5px 14px; border-radius: 16px; font-size: 13px; color: #606266; background: #f2f3f5; cursor: pointer; transition: all .2s; }
.dept-chip.active { background: #409EFF; color: #fff; font-weight: 500; }
/* 筛选结果计数 */
.result-count { text-align: center; font-size: 12px; color: #909399; padding: 4px 0 20px; }
.empty-btn { margin-top: 14px; }
.date-tabs { display: flex; overflow-x: auto; padding: 12px; gap: 8px; background: #fff; white-space: nowrap; }
.schedule-list { padding: 12px; }
.schedule-card { background: #fff; border-radius: 10px; padding: 16px; margin-bottom: 10px; display: flex; align-items: center; cursor: pointer; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.schedule-card > *:first-child { flex: 1; }
.doctor-name { font-size: 16px; font-weight: 500; }
.doctor-title { font-size: 12px; color: #909399; font-weight: normal; }
.doctor-dept { font-size: 12px; color: #909399; }
.doctor-fee { font-size: 12px; color: #e6a23c; margin-top: 2px; }
.fee-text { color: #e6a23c; font-weight: 600; font-size: 16px; }
.schedule-detail { text-align: center; margin-right: 12px; }
.period-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.period-tag.am { background: #e6f7ff; color: #1890ff; }
.period-tag.pm { background: #fff7e6; color: #fa8c16; }
.remain { display: block; font-size: 12px; color: #909399; margin-top: 4px; }
.full-tag { color: #f56c6c; font-size: 13px; }
.form-row { margin-top: 12px; }
.form-row label { display: block; margin-bottom: 6px; font-size: 13px; color: #606266; }
.register-info p { margin: 6px 0; font-size: 14px; }
.success-info { text-align: center; padding: 20px 0; }
/* 挂号费支付弹窗 */
.pay-info { text-align: center; padding: 12px 0 4px; }
.pay-amount { font-size: 40px; color: #e6a23c; font-weight: bold; margin: 0; }
.pay-label { font-size: 13px; color: #909399; margin-top: 6px; }
.pay-tip { text-align: center; font-size: 12px; color: #909399; margin: 12px 0 0; }
.dialog-tip { margin: 10px 0 0; font-size: 12px; color: #e6a23c; }
.pay-doctor { text-align: center; padding: 6px 0 2px; font-size: 14px; color: #303133; }
.pay-doctor p { margin: 2px 0; }
.pay-sub { font-size: 12px; color: #909399; }
/* 支付方式选择 */
.pay-methods { display: flex; gap: 10px; margin-top: 14px; }
.pm-item { flex: 1; display: flex; align-items: center; justify-content: center; gap: 6px; padding: 10px 0; border: 1px solid #dcdfe6; border-radius: 8px; font-size: 13px; color: #606266; cursor: pointer; }
.pm-item.active { border-color: #409EFF; color: #409EFF; background: #ecf5ff; }
.pm-icon { width: 20px; height: 20px; border-radius: 50%; color: #fff; font-size: 12px; display: inline-flex; align-items: center; justify-content: center; }
.pm-icon.wechat { background: #07C160; }
.pm-icon.alipay { background: #1677FF; }
.paid-tag { margin-top: 12px; font-size: 13px; color: #67C23A; }
.queue-num { font-size: 48px; color: #409EFF; font-weight: bold; margin: 0; }
.queue-label { font-size: 14px; color: #909399; margin-top: 8px; }
.empty-hint { text-align: center; color: #909399; padding: 40px 0; }
.bottom-nav { position: fixed; bottom: 0; left: 0; right: 0; max-width: 480px; margin: 0 auto; display: flex; background: #fff; border-top: 1px solid #e4e7ed; padding: 8px 0; }
.nav-item { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; font-size: 11px; color: #909399; cursor: pointer; }
.nav-item.active { color: #409EFF; }
</style>
