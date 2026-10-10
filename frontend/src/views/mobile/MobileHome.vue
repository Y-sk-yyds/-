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

    <!-- 下拉刷新：号源/排班会变，患者需要能主动刷新而不用退出重进。
         手写 touch 三件套，不引第三方组件。 -->
    <div class="pull-wrap"
         @touchstart="onPullStart" @touchmove="onPullMove" @touchend="onPullEnd">
      <div class="pull-indicator" :style="{ height: pullDistance + 'px' }">
        <span class="pull-arrow" :class="{ ready: canRelease }">↓</span>
        <span>{{ canRelease ? '松开刷新' : '下拉刷新' }}</span>
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
          <!-- 挂号费：向患者明示本次挂号将产生的费用；定价缺失时不猜数字 -->
          <div class="doctor-fee" v-if="s.registrationFee != null">挂号费 ¥{{ Number(s.registrationFee).toFixed(2) }}</div>
          <div class="doctor-fee unknown-fee" v-else>挂号费 以医院实际收费为准</div>
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
      <!-- 空态：区分「加载失败」「筛选后无结果」「当天确实没排班」三种 -->
      <div v-if="!loading && filteredSchedules.length === 0" class="empty-hint">
        <template v-if="loadError">
          <div>排班加载失败</div>
          <el-button size="small" class="empty-btn" @click="fetchSchedules">重新加载</el-button>
        </template>
        <template v-else>
          <div>{{ keyword || activeDept ? '没有符合条件的医生' : '当日暂无排班' }}</div>
          <el-button v-if="keyword || activeDept" size="small" class="empty-btn" @click="resetFilter">清空筛选条件</el-button>
        </template>
      </div>
      <!-- 结果计数 -->
      <div v-if="!loading && filteredSchedules.length > 0" class="result-count">
        共 {{ filteredSchedules.length }} 位医生可预约
      </div>
      </div><!-- /.schedule-list -->
    </div><!-- /.pull-wrap -->

    <!-- 挂号弹窗 -->
    <el-dialog v-model="showDialog" title="确认挂号" width="85%" top="15vh" :close-on-click-modal="false">
      <div class="register-info" v-if="selectedSchedule">
        <p><strong>医生：</strong>{{ selectedSchedule.doctorName }}<span v-if="selectedSchedule.title"> · {{ selectedSchedule.title }}</span></p>
        <p><strong>科室：</strong>{{ selectedSchedule.department }}</p>
        <p><strong>时间：</strong>{{ selectedSchedule.workDate }} {{ selectedSchedule.period === 'AM' ? '上午' : '下午' }}</p>
        <p><strong>剩余号源：</strong>{{ selectedSchedule.remainNum }}</p>
        <p><strong>挂号费：</strong><span class="fee-text" v-if="hasFee">¥{{ payAmount.toFixed(2) }}</span><span class="fee-text unknown-fee" v-else>以医院实际收费为准</span></p>
      </div>
      <div class="form-row">
        <label>手机号：</label>
        <!-- type="tel" + inputmode="numeric"：移动端弹数字键盘，否则默认弹字母键盘 -->
        <el-input v-model="phone" type="tel" inputmode="numeric" placeholder="请输入手机号" maxlength="11" />
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
        <p class="pay-amount" v-if="hasFee">¥{{ payAmount.toFixed(2) }}</p>
        <p class="pay-amount pay-amount-unknown" v-else>以医院实际收费为准</p>
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
          {{ hasFee ? `支付 ¥${payAmount.toFixed(2)} 并挂号` : '确认支付并挂号' }}
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
        <el-button type="primary" @click="goToRecords">查看我的挂号</el-button>
      </template>
    </el-dialog>

    <!-- 底部导航（公共组件） -->
    <MobileNav current="home" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { mobileApi } from '@/api'
import request from '@/utils/request'
import MobileNav from './MobileNav.vue'

const loading = ref(false)
const loadError = ref(false)   // 排班请求失败（区别于"当天没有排班"）
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

/** 排班可见天数：种子里排班铺到两周内，7 天会让患者看不到后面的号 */
const DAYS_AHEAD = 14

const dates = computed(() => {
  const arr = []
  const today = new Date()
  for (let i = 0; i < DAYS_AHEAD; i++) {
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

/**
 * 本次挂号应付的挂号费 —— **只取后端下发的医生定价，不在前端重复定义兜底值**。
 * 后端 BillService 已有 DEFAULT_REGISTRATION_FEE 兜底，两边各写一份会漂移：
 * 后端改了默认价而前端没跟上，就会出现"显示 10 元、实际扣的是别的钱"。
 * 定价缺失时显示"以医院实际收费为准"，真实金额以后端返回的 billAmount 为准
 * （成功页 paidAmount 取的就是后端返回值）。
 */
const hasFee = computed(() => selectedSchedule.value?.registrationFee != null)
const payAmount = computed(() => Number(selectedSchedule.value?.registrationFee ?? 0))

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

/**
 * 拉取当日排班。请求失败必须与"当天没排班"区分开：
 * 否则断网时患者看到的是空列表 + "当日暂无排班"，会误以为今天没号而不再重试。
 */
const fetchSchedules = async () => {
  loading.value = true
  loadError.value = false
  try {
    const res = await mobileApi.schedules(selectedDate.value)
    schedules.value = res.data || []
  } catch (e) {
    schedules.value = []
    loadError.value = true
    ElMessage.error(e?.response?.data?.message || '排班加载失败，请检查网络')
  } finally { loading.value = false }
}

/* ===== 下拉刷新（手写 touch 三件套，不引第三方组件） =====
   阻尼 0.4：手指下拉 100px 时视觉只移 40px，避免一次拉到底。
   只有「页面顶部 + 手指向下」才接管手势并 preventDefault，
   否则会卡住页面正常上滑 —— 这是手写下拉刷新最容易写错的地方。 */
const PULL_THRESHOLD = 60   // px，达到即触发刷新
const PULL_MAX = 100         // 指示器视觉上限
const pullDistance = ref(0)
const refreshing = ref(false)
let pulling = false
let startY = 0

const canRelease = computed(() => pullDistance.value >= PULL_THRESHOLD)

const onPullStart = (e) => {
  if (refreshing.value) return
  if (window.scrollY > 0) { pulling = false; return }   // 不在顶部就不接管手势
  pulling = true
  startY = e.touches[0].clientY
}

const onPullMove = (e) => {
  if (!pulling) return
  const delta = e.touches[0].clientY - startY
  if (delta <= 0) {                        // 方向锁定：一旦转向上滑立刻释放
    pulling = false
    pullDistance.value = 0
    return
  }
  pullDistance.value = Math.min(delta * 0.4, PULL_MAX)
  e.preventDefault()                       // 只在下拉状态下阻止默认滚动
}

const onPullEnd = async () => {
  if (!pulling) return
  pulling = false
  if (pullDistance.value >= PULL_THRESHOLD) {
    refreshing.value = true
    pullDistance.value = PULL_MAX         // 保持在顶部，等待刷新完成
    await fetchSchedules()
    refreshing.value = false
    ElMessage.success('号源已刷新')
  }
  pullDistance.value = 0
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

/**
 * 挂号成功 → 「我的就诊」。
 * 手机号**不放进 URL**：URL 会进浏览器历史记录、可能被 Referer 带出去、截图也会带上，
 * 属于敏感信息。改用 sessionStorage 中转 —— 同标签页内有效、关闭标签页即销毁。
 */
const router = useRouter()
const goToRecords = () => {
  sessionStorage.setItem('clinic:patientPhone', phone.value)
  router.push('/mobile/records')
}

onMounted(() => { selectedDate.value = dates.value[0].value; fetchSchedules() })
</script>

<style scoped>
/* 底部导航 + 安全区：iPhone 底部有 Home Indicator，padding 必须叠加 env()，
   不支持 env() 的环境会回退为 0，不影响桌面浏览器预览 */
.mobile-page { max-width: 480px; margin: 0 auto; padding-bottom: calc(70px + env(safe-area-inset-bottom)); background: #f5f7fa; min-height: 100vh; font-size: 14px; }
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
/* 定价缺失：不猜数字，灰色弱化，避免患者把兜底价当成实际收费 */
.unknown-fee { color: #909399; font-weight: normal; }
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
.pay-amount-unknown { font-size: 20px; color: #909399; font-weight: 500; line-height: 1.5; }
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

/* ===== 下拉刷新 ===== */
.pull-wrap { position: relative; }
.pull-indicator {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  overflow: hidden; background: #f5f7fa; color: #909399; font-size: 12px;
}
.pull-arrow { display: inline-block; transition: transform .2s; }
.pull-arrow.ready { transform: rotate(180deg); }
</style>
