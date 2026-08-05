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

    <!-- 我的挂号 + 账单 -->
    <div class="list-area" v-if="tab === 'registrations' && patient" v-loading="loading">
      <div class="reg-card" v-for="r in registrations" :key="r.id">
        <div class="reg-top">
          <span class="reg-date">{{ r.registrationDate }}</span>
          <el-tag size="small" :type="statusTag(r.status)">{{ statusText(r.status) }}</el-tag>
        </div>
        <div class="reg-body">
          <span>排队号：{{ r.queueNum || '—' }}</span>
        </div>
        <!-- 关联账单 -->
        <div class="bill-row" v-if="bills[r.id]">
          <span>费用 ¥{{ bills[r.id].totalAmount }}</span>
          <el-button
            v-if="bills[r.id].status === 0"
            type="danger" size="small" round
            @click="pay(bills[r.id])"
          >去支付</el-button>
          <el-tag v-else size="small" type="success">已支付</el-tag>
        </div>
      </div>
      <div v-if="!loading && registrations.length === 0" class="empty-hint">暂无挂号记录</div>
    </div>

    <!-- 排队看板 -->
    <div class="queue-area" v-if="tab === 'queue' && patient">
      <div v-if="queue.length" class="queue-board">
        <div class="queue-doctor" v-if="queue[0].doctorName">{{ queue[0].doctorName }} · {{ queue[0].department }}</div>
        <div class="queue-item" v-for="q in queue" :key="q.queueNum"
          :class="{ 'is-me': q.isMe, 'is-called': q.status === 2 }">
          <span class="q-num">{{ q.queueNum || '—' }}</span>
          <span class="q-name">{{ q.patientName }}</span>
          <span class="q-status">{{ q.status === 2 ? '已就诊' : '等待中' }}</span>
        </div>
      </div>
      <div v-else class="empty-hint">暂无排队信息</div>
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
          {{ r.content?.length > 80 ? r.content.slice(0, 80) + '...' : r.content }}
        </div>
      </div>
      <div v-if="!loading && records.length === 0" class="empty-hint">暂无病历记录</div>
    </div>

    <!-- 支付弹窗 -->
    <el-dialog v-model="showPay" title="确认支付" width="80%" top="20vh">
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
import { ref, onMounted } from 'vue'
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
const queue = ref([])
const showPay = ref(false)
const payBill = ref(null)
const paying = ref(false)

const statusText = (s) => ({ 0: '已取消', 1: '已挂号', 2: '已就诊', 3: '爽约' }[s] || '')
const statusTag = (s) => ({ 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }[s] || 'info')

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
    queue.value = d.queue || []
  } catch (e) { ElMessage.error('查询失败') }
  finally { loading.value = false }
}

const pay = (bill) => {
  payBill.value = bill
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
.bill-row { margin-top: 10px; padding-top: 10px; border-top: 1px dashed #e4e7ed; display: flex; justify-content: space-between; align-items: center; }
.record-header { margin-bottom: 6px; font-size: 13px; color: #606266; display: flex; align-items: center; justify-content: space-between; }
.record-diag { font-size: 14px; font-weight: 500; margin-bottom: 4px; }
.record-content { font-size: 12px; color: #909399; line-height: 1.5; }
.queue-board { background: #fff; border-radius: 10px; padding: 8px; }
.queue-doctor { font-size: 13px; color: #409EFF; padding: 6px 12px 10px; border-bottom: 1px solid #f0f0f0; margin-bottom: 4px; }
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
