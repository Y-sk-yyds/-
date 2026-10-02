<template>
  <div class="mobile-page">
    <div class="mobile-header">
      <h2>我的就诊</h2>
    </div>

    <!-- 手机号输入 -->
    <div class="phone-bar" v-if="!patient">
      <el-input v-model="queryPhone" placeholder="请输入手机号" maxlength="11">
        <template #append>
          <el-button type="primary" :loading="loading" @click="loadData">查询</el-button>
        </template>
      </el-input>
    </div>

    <!-- Tab 切换 -->
    <div class="tab-bar" v-if="patient">
      <span :class="{ active: tab === 'registrations' }" @click="tab = 'registrations'">我的挂号</span>
      <span :class="{ active: tab === 'queue' }" @click="tab = 'queue'">排队看板</span>
      <span :class="{ active: tab === 'records' }" @click="tab = 'records'">我的病历</span>
    </div>

    <!-- 我的挂号：按「待缴费 / 今日就诊 / 即将就诊 / 历史记录」分区 -->
    <div class="list-area" v-if="tab === 'registrations' && patient" v-loading="loading">
      <template v-for="sec in sections" :key="sec.key">
        <div class="group-title" :class="sec.key + '-title'">
          {{ sec.title }}<em> · {{ sec.list.length }} 笔</em>
        </div>
        <div class="reg-card" :class="{ 'pay-card': sec.key === 'needPay' }"
             v-for="r in visibleList(sec)" :key="r.id">
          <div class="reg-top">
            <span class="reg-date">{{ dateLabel(r.workDate) }} · {{ periodText(r.period) }}</span>
            <el-tag size="small" :type="statusTag(r.status)">{{ statusText(r.status) }}</el-tag>
          </div>
          <div class="reg-body">
            <span>{{ r.doctorName }}<em v-if="r.title" class="doc-title">（{{ r.title }}）</em> · {{ r.department }}</span>
            <span class="q-num-text">排队号 {{ r.queueNum || '—' }}</span>
          </div>
          <!-- 逐笔账单：挂号费单 / 药费单互相独立，各自支付 -->
          <div class="bill-list" v-if="r.bills && r.bills.length">
            <div class="bill-row" v-for="b in r.bills" :key="b.id">
              <span class="fee-kind" :class="'fee-' + b.feeType">{{ feeLabel(b.feeType) }}</span>
              <span class="amount">¥{{ Number(b.totalAmount || 0).toFixed(2) }}</span>
              <el-button v-if="b.status === 0" type="danger" size="small" round @click="payByReg(r, b)">去支付</el-button>
              <el-tag v-else size="small" type="success">已支付</el-tag>
            </div>
            <!-- 多笔时补一行合计，方便一眼看到该次就诊总花费 -->
            <div class="bill-total" v-if="r.bills.length > 1">
              <span>合计</span>
              <span>¥{{ Number(r.billAmount || 0).toFixed(2) }}</span>
            </div>
          </div>
        </div>
        <!-- 历史记录默认只展开最近 3 笔，避免列表随使用天数无限变长 -->
        <div class="more-row" v-if="sec.key === 'history' && sec.list.length > HISTORY_SHOW"
             @click="historyExpanded = !historyExpanded">
          {{ historyExpanded ? '收起 ⌃' : `展开全部 ${sec.list.length} 笔 ⌄` }}
        </div>
      </template>
      <div v-if="!loading && registrations.length === 0" class="empty-hint">暂无挂号记录</div>
    </div>

    <!-- 排队看板：只显示「今天」的排班，上午(AM)/下午(PM)各一组 —— 后端只下发今天的组 -->
    <div class="queue-area" v-if="tab === 'queue' && patient">
      <div class="queue-day-bar" v-if="todayStr">
        <span class="qd-label">今日排队</span>
        <span class="qd-date">{{ todayStr.slice(5).replace('-', '/') }}</span>
      </div>
      <template v-if="queueGroups.length">
        <div class="queue-board" v-for="g in queueGroups" :key="g.scheduleId">
          <div class="queue-head">
            <span class="period-tag" :class="g.period === 'AM' ? 'am' : 'pm'">{{ periodText(g.period) }}</span>
            <span class="q-doc">{{ g.doctorName }} · {{ g.department }}</span>
            <span class="q-count">{{ g.items.length }} 人</span>
          </div>
          <div class="queue-item" v-for="q in g.items" :key="q.queueNum"
            :class="{ 'is-me': q.isMe, 'is-called': q.status === 2 }">
            <span class="q-num">{{ q.queueNum || '—' }}</span>
            <span class="q-name">{{ q.patientName }}<em v-if="q.isMe" class="me-flag">我</em></span>
            <span class="q-status">{{ q.status === 2 ? '已就诊' : '等待中' }}</span>
          </div>
        </div>
      </template>
      <div v-else class="empty-hint">今天没有需要排队的号</div>

      <!-- 其他日期的预约不在排队看板展开（队列会随挂号变化），只给一个入口 -->
      <div class="other-appt" v-if="otherAppointments.length" @click="tab = 'registrations'">
        <span>还有其他日期的预约 {{ otherAppointments.length }} 个</span>
        <span class="arrow">去我的挂号 ›</span>
      </div>
    </div>

    <!-- 我的病历 -->
    <div class="list-area" v-if="tab === 'records' && patient" v-loading="loading">
      <div class="record-card" v-for="r in records" :key="r.id">
        <div class="record-header">
          <span>{{ r.createTime?.slice(0, 10) }}</span>
          <el-tag size="small">AES-256 加密</el-tag>
        </div>
        <div class="record-diag" v-if="r.diagnosis">诊断：{{ r.diagnosis }}</div>
        <div class="record-content" v-if="r.content">
          {{ r.content?.length > 40 ? r.content.slice(0, 40) + '...' : r.content }}
        </div>
      </div>
      <div v-if="!loading && records.length === 0" class="empty-hint">暂无病历记录</div>
    </div>

    <!-- 支付弹窗 -->
    <el-dialog v-model="showPay" title="确认支付" width="80%" top="20vh">
      <p>费用类型：{{ feeLabel(payBill?.feeType) }}</p>
      <p>支付方式：微信支付</p>
      <p><strong>金额：¥{{ payBill?.totalAmount }}</strong></p>
      <template #footer>
        <el-button @click="showPay = false">取消</el-button>
        <el-button type="primary" :loading="paying" @click="confirmPay">确认支付</el-button>
      </template>
    </el-dialog>

    <!-- 底部导航 -->
    <div class="bottom-nav">
      <div class="nav-item" @click="$router.push('/mobile')">
        <el-icon><List /></el-icon>
        <span>排班挂号</span>
      </div>
      <div class="nav-item active">
        <el-icon><Document /></el-icon>
        <span>我的就诊</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { List, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { mobileApi } from '@/api'
import request from '@/utils/request'

const route = useRoute()
const tab = ref('registrations')
const queryPhone = ref(route.query.phone || '')
const phone = ref('')
const patient = ref(null)
const loading = ref(false)
const registrations = ref([])
const bills = ref({})
const records = ref([])
const queueGroups = ref([])
const todayStr = ref('')   // 由后端下发，避免移动端本地时区把"今天"算错
const historyExpanded = ref(false)   // 「历史记录」是否已展开全部
const HISTORY_SHOW = 3               // 历史记录默认只展示最近 N 笔
const showPay = ref(false)
const payBill = ref(null)
const paying = ref(false)

const statusText = (s) => ({ 0: '已取消', 1: '已挂号', 2: '已就诊', 3: '爽约' }[s] || '')
const statusTag = (s) => ({ 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }[s] || 'info')
const periodText = (p) => (p === 'AM' ? '上午' : p === 'PM' ? '下午' : '')

/** 本地日期 YYYY-MM-DD（不用 toISOString，避免 UTC 把凌晨的"今天"算成昨天） */
const localToday = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** 就诊日的友好文案：今天 / 明天 / MM/DD */
const dateLabel = (d) => {
  if (!d) return '日期待定'
  const today = todayStr.value
  if (d === today) return '今天'
  const t = new Date(today + 'T00:00:00')
  t.setDate(t.getDate() + 1)
  const tomorrow = `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, '0')}-${String(t.getDate()).padStart(2, '0')}`
  if (d === tomorrow) return '明天'
  return d.slice(5).replace('-', '/')
}

/**
 * 挂号分区（各分区互斥，不重复展示）：
 *   needPay  待缴费  —— 未支付账单，跨日期，置顶（最紧急）
 *   today    今日就诊
 *   upcoming 即将就诊
 *   history  历史记录
 * 分区内顺序由后端保证：今天优先 → 未来升序 → 过去倒序，同日 AM 在 PM 前
 */
const sections = computed(() => {
  const today = todayStr.value
  const all = registrations.value
  const needPay = all.filter(r => r.billStatus === 0)
  const rest = all.filter(r => r.billStatus !== 0)
  return [
    { key: 'needPay',  title: '💳 待缴费',   list: needPay },
    { key: 'today',    title: '📍 今日就诊', list: rest.filter(r => r.workDate === today) },
    { key: 'upcoming', title: '🗓 即将就诊', list: rest.filter(r => r.workDate && r.workDate > today) },
    { key: 'history',  title: '📁 历史记录', list: rest.filter(r => !r.workDate || r.workDate < today) }
  ].filter(s => s.list.length > 0)
})

/** 分区内实际渲染的卡片：历史记录默认只取最近 HISTORY_SHOW 笔，点「展开全部」才全放 */
const visibleList = (sec) =>
  (sec.key === 'history' && !historyExpanded.value) ? sec.list.slice(0, HISTORY_SHOW) : sec.list

/**
 * 其他日期（非今天）的待就诊预约 —— 排队看板只显示今天的队列，
 * 这些不在看板展开，只做一个跳回「我的挂号」的提示入口。
 */
const otherAppointments = computed(() =>
  registrations.value.filter(r => r.status === 1 && r.workDate && r.workDate !== todayStr.value))

const loadData = async () => {
  if (!queryPhone.value || queryPhone.value.length !== 11) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  loading.value = true
  try {
    const res = await mobileApi.myData(queryPhone.value)
    if (res.code !== 200) { ElMessage.warning(res.message || '未找到'); return }
    const d = res.data
    patient.value = d.patient
    phone.value = queryPhone.value
    registrations.value = d.registrations || []
    bills.value = d.bills || {}
    records.value = d.records || []
    queueGroups.value = d.queueGroups || []
    todayStr.value = d.today || localToday()
    historyExpanded.value = false   // 换患者/刷新后历史记录回到折叠态
  } catch (e) { ElMessage.error('查询失败') }
  finally { loading.value = false }
}

/** 费用类型中文（由后端下发的 feeType 推导，不依赖额外数据库列） */
const feeLabel = (t) =>
  ({ REGISTRATION: '挂号费', DRUG: '药费', MIXED: '挂号费+药费' }[t] || '费用')

/** 账单行「去支付」：优先用该行自身的账单，缺省回退到挂号上的兼容字段 */
const payByReg = (r, b) => {
  const target = b || r
  if (!target?.id) { ElMessage.warning('未找到对应账单'); return }
  payBill.value = {
    id: target.id,
    totalAmount: target.totalAmount ?? r.billAmount,
    feeType: target.feeType
  }
  showPay.value = true
}

const confirmPay = async () => {
  paying.value = true
  try {
    await request({ url: `/mobile/bills/${payBill.value.id}/pay`, method: 'put', data: { paymentMethod: 'WECHAT' } })
    ElMessage.success('支付成功')
    showPay.value = false
    await loadData()
  } catch (e) { ElMessage.error('支付失败') }
  finally { paying.value = false }
}

onMounted(() => { if (queryPhone.value) loadData() })
</script>

<style scoped>
.mobile-page { max-width: 480px; margin: 0 auto; padding-bottom: 70px; background: #f5f7fa; min-height: 100vh; font-size: 14px; }
.mobile-header { background: #409EFF; color: #fff; padding: 16px; text-align: center; }
.mobile-header h2 { margin: 0; font-size: 18px; font-weight: 500; }
.phone-bar { padding: 16px; background: #fff; }
.tab-bar { display: flex; background: #fff; border-bottom: 1px solid #e4e7ed; }
.tab-bar span { flex: 1; text-align: center; padding: 12px 0; font-size: 14px; color: #909399; cursor: pointer; }
.tab-bar span.active { color: #409EFF; border-bottom: 2px solid #409EFF; font-weight: 500; }
.list-area, .queue-area { padding: 12px; }
.reg-card, .record-card { background: #fff; border-radius: 10px; padding: 14px; margin-bottom: 10px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.reg-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.reg-body { display: flex; justify-content: space-between; font-size: 13px; color: #606266; }
/* 逐笔账单：挂号费单 / 药费单分行展示，各自独立支付 */
.bill-list { margin-top: 10px; padding-top: 10px; border-top: 1px dashed #e4e7ed; }
.bill-row { display: flex; justify-content: space-between; align-items: center; padding: 4px 0; gap: 8px; }
.fee-kind { font-size: 12px; color: #606266; flex-shrink: 0; }
.fee-kind.fee-DRUG { color: #e6a23c; }
.bill-total { display: flex; justify-content: space-between; align-items: center; padding-top: 6px; margin-top: 4px; border-top: 1px solid #f0f0f0; font-size: 12px; color: #909399; }
/* 分区标题：待缴费/今日就诊/即将就诊/历史记录 */
.group-title { font-size: 13px; font-weight: 600; color: #303133; padding: 8px 2px; }
.group-title em { font-style: normal; font-weight: normal; color: #909399; font-size: 12px; }
.needPay-title { color: #f56c6c; }
.today-title { color: #67c23a; }
.upcoming-title { color: #409EFF; }
.history-title { color: #909399; }
/* 待缴费卡片左侧红条，视觉上突出紧迫性 */
.pay-card { border-left: 3px solid #f56c6c; }
.doc-title { font-style: normal; color: #909399; font-size: 12px; }
.q-num-text { color: #409EFF; flex-shrink: 0; }
.amount { color: #f56c6c; font-weight: 600; }
.record-header { margin-bottom: 6px; font-size: 13px; color: #606266; display: flex; align-items: center; justify-content: space-between; }
.record-diag { font-size: 14px; font-weight: 500; margin-bottom: 4px; }
.record-content { font-size: 12px; color: #909399; line-height: 1.5; }
.queue-board { background: #fff; border-radius: 10px; padding: 8px; margin-bottom: 10px; }
/* 排队看板顶部：只针对「今天」，故日期只出现一次 */
.queue-day-bar { display: flex; align-items: baseline; justify-content: space-between; padding: 2px 4px 10px; }
.qd-label { font-size: 14px; font-weight: 600; color: #303133; }
.qd-date { font-size: 12px; color: #909399; }
/* 排队看板表头：上午/下午 + 对应医生 + 排队人数 */
.queue-head { display: flex; align-items: center; gap: 8px; padding: 10px 12px; border-bottom: 1px solid #f0f0f0; margin-bottom: 4px; }
.q-count { font-size: 11px; color: #c0c4cc; margin-left: auto; }
/* 其他日期预约入口（排队看板按天只看今天，不跨天展开） */
.other-appt { display: flex; justify-content: space-between; align-items: center; background: #fff; border-radius: 10px; padding: 14px; margin-top: 10px; font-size: 13px; color: #606266; cursor: pointer; }
.other-appt .arrow { color: #409EFF; font-size: 12px; }
/* 历史记录「展开全部 / 收起」 */
.more-row { text-align: center; color: #409EFF; font-size: 13px; padding: 10px 0; cursor: pointer; }
.period-tag { padding: 1px 7px; border-radius: 4px; font-size: 12px; flex-shrink: 0; }
.period-tag.am { background: #e6f7ff; color: #1890ff; }
.period-tag.pm { background: #fff7e6; color: #fa8c16; }
.q-doc { font-size: 12px; color: #909399; }
.me-flag { font-style: normal; background: #409EFF; color: #fff; font-size: 10px; padding: 0 4px; border-radius: 3px; margin-left: 4px; }
.queue-item { display: flex; align-items: center; padding: 10px 12px; border-bottom: 1px solid #f0f0f0; }
.queue-item.is-me { background: #e6f7ff; border-radius: 8px; }
.queue-item.is-called .q-status { color: #909399; text-decoration: line-through; }
.q-num { width: 36px; height: 36px; line-height: 36px; text-align: center; border-radius: 50%; background: #409EFF; color: #fff; font-weight: bold; font-size: 16px; margin-right: 12px; flex-shrink: 0; }
.q-name { flex: 1; font-size: 14px; }
.q-status { font-size: 12px; color: #67c23a; }
.empty-hint { text-align: center; color: #909399; padding: 40px 0; }
.bottom-nav { position: fixed; bottom: 0; left: 0; right: 0; max-width: 480px; margin: 0 auto; display: flex; background: #fff; border-top: 1px solid #e4e7ed; padding: 8px 0; }
.nav-item { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; font-size: 11px; color: #909399; cursor: pointer; }
.nav-item.active { color: #409EFF; }
</style>
