<template>
  <div class="mobile-page">
    <div class="mobile-header">
      <h2>排班挂号</h2>
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
      <div class="schedule-card" v-for="s in schedules" :key="s.id" @click="showRegister(s)">
        <div>
          <div class="doctor-name">{{ s.doctorName }}</div>
          <div class="doctor-dept">{{ s.department }}</div>
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
      <div v-if="!loading && schedules.length === 0" class="empty-hint">当日暂无排班</div>
    </div>

    <!-- 挂号弹窗 -->
    <el-dialog v-model="showDialog" title="确认挂号" width="85%" top="15vh" :close-on-click-modal="false">
      <div class="register-info" v-if="selectedSchedule">
        <p><strong>医生：</strong>{{ selectedSchedule.doctorName }}</p>
        <p><strong>科室：</strong>{{ selectedSchedule.department }}</p>
        <p><strong>时间：</strong>{{ selectedSchedule.workDate }} {{ selectedSchedule.period === 'AM' ? '上午' : '下午' }}</p>
        <p><strong>剩余号源：</strong>{{ selectedSchedule.remainNum }}</p>
      </div>
      <div class="form-row">
        <label>手机号：</label>
        <el-input v-model="phone" placeholder="请输入手机号" maxlength="11" />
      </div>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="registering" @click="doRegister">确认挂号</el-button>
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
        <el-button type="primary" :loading="creating" @click="doCreatePatient">建档并挂号</el-button>
      </template>
    </el-dialog>

    <!-- 挂号成功 -->
    <el-dialog v-model="showSuccess" title="挂号成功" width="85%" top="15vh">
      <div class="success-info">
        <p class="queue-num">{{ queueNum }}</p>
        <p class="queue-label">您的排队号</p>
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
import { ArrowRight, List, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { mobileApi } from '@/api'
import request from '@/utils/request'

const loading = ref(false)
const registering = ref(false)
const creating = ref(false)
const schedules = ref([])
const selectedDate = ref('')
const selectedSchedule = ref(null)
const showDialog = ref(false)
const showNewPatient = ref(false)
const showSuccess = ref(false)
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
    const val = d.toISOString().slice(0, 10)
    const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
    arr.push({ label: (i === 0 ? '今天' : (i === 1 ? '明天' : val.slice(5))) + ' ' + week, value: val })
  }
  return arr
})

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

const doRegister = async () => {
  if (!phone.value || phone.value.length !== 11) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  registering.value = true
  try {
    const res = await mobileApi.register({ phone: phone.value, scheduleId: selectedSchedule.value.id })
    queueNum.value = res.data?.queueNum
    showDialog.value = false
    showSuccess.value = true
  } catch (e) {
    const msg = e?.response?.data?.message || ''
    if (msg.includes('未找到该手机号')) {
      showDialog.value = false
      showNewPatient.value = true
    } else {
      ElMessage.error(msg || '挂号失败')
    }
  } finally { registering.value = false }
}

const doCreatePatient = async () => {
  if (!newName.value.trim()) {
    ElMessage.warning('请输入姓名')
    return
  }
  creating.value = true
  try {
    await request({ url: '/mobile/patients', method: 'post', data: { name: newName.value, phone: phone.value, gender: newGender.value, age: String(newAge.value) } })
    showNewPatient.value = false
    // 建档成功后直接挂号
    const res = await mobileApi.register({ phone: phone.value, scheduleId: selectedSchedule.value.id })
    queueNum.value = res.data?.queueNum
    showSuccess.value = true
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '建档失败')
  } finally { creating.value = false }
}

onMounted(() => { selectedDate.value = dates.value[0].value; fetchSchedules() })
</script>

<style scoped>
.mobile-page { max-width: 480px; margin: 0 auto; padding-bottom: 70px; background: #f5f7fa; min-height: 100vh; font-size: 14px; }
.mobile-header { background: #409EFF; color: #fff; padding: 16px; text-align: center; }
.mobile-header h2 { margin: 0; font-size: 18px; font-weight: 500; }
.date-tabs { display: flex; overflow-x: auto; padding: 12px; gap: 8px; background: #fff; white-space: nowrap; }
.schedule-list { padding: 12px; }
.schedule-card { background: #fff; border-radius: 10px; padding: 16px; margin-bottom: 10px; display: flex; align-items: center; cursor: pointer; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.schedule-card > *:first-child { flex: 1; }
.doctor-name { font-size: 16px; font-weight: 500; }
.doctor-dept { font-size: 12px; color: #909399; }
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
.queue-num { font-size: 48px; color: #409EFF; font-weight: bold; margin: 0; }
.queue-label { font-size: 14px; color: #909399; margin-top: 8px; }
.empty-hint { text-align: center; color: #909399; padding: 40px 0; }
.bottom-nav { position: fixed; bottom: 0; left: 0; right: 0; max-width: 480px; margin: 0 auto; display: flex; background: #fff; border-top: 1px solid #e4e7ed; padding: 8px 0; }
.nav-item { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; font-size: 11px; color: #909399; cursor: pointer; }
.nav-item.active { color: #409EFF; }
</style>
