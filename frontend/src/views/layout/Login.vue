<template>
  <!--
    登录页面 Login.vue
    - 页面用途：社区诊所管理系统的用户登录入口页面。
    - 角色要求：所有角色（管理员/医生/收银员）共用此登录页，
      根据用户名判断角色并分配对应权限。
    - 功能流程：用户输入用户名和密码 -> 前端表单验证 ->
      调用 userStore.login() 提交后端验证 -> 成功跳转首页。
  -->
  <div class="login-page">
    <!-- 背景装饰：浮动圆形元素增加视觉层次 -->
    <div class="login-bg">
      <div class="bg-shape shape-1"></div>
      <div class="bg-shape shape-2"></div>
    </div>

    <!-- 登录卡片容器 -->
    <div class="login-card">
      <!-- 卡片头部：展示Logo图标和系统名称 -->
      <div class="card-header">
        <div class="logo-icon">
          <!-- 简化的药箱/医院十字SVG图标 -->
          <svg viewBox="0 0 40 40"><rect x="8" y="4" width="24" height="32" rx="4" fill="white" opacity="0.25"/><rect x="12" y="8" width="16" height="4" rx="2" fill="white" opacity="0.6"/><rect x="14" y="15" width="12" height="10" rx="2" fill="white"/><circle cx="20" cy="20" r="3" fill="var(--clinic-primary)"/></svg>
        </div>
        <h1>社区诊所管理系统</h1>
        <p>Community Clinic Management</p>
      </div>

      <!-- 登录表单区域
           @formRef - 表单组件引用，用于手动触发表单验证
           :model="form" - 双向绑定的表单数据对象
           :rules="rules" - 表单验证规则配置
      -->
      <el-form ref="formRef" :model="form" :rules="rules" size="large" class="login-form">
        <!-- 用户名输入项 -->
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
        </el-form-item>
        <!-- 密码输入项：支持显示/隐藏密码和回车提交 -->
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <!-- 登录按钮：loading状态防止重复提交 -->
        <el-form-item>
          <el-button type="primary" :loading="loading" block class="login-btn" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>

      <!-- 卡片底部：提示初始测试账号信息 -->
      <div class="card-footer">
        <span>初始账号: admin / doctor01 / cashier01</span>
        <span>统一密码: admin123</span>
      </div>
    </div>
  </div>
</template>

<script setup>
// -- 核心导入 --
import { ref, reactive } from 'vue'         // Vue 3 响应式API
import { useRouter } from 'vue-router'      // 路由实例，用于页面跳转
import { useUserStore } from '@/stores/user' // Pinia 用户状态管理 store
import { User, Lock } from '@element-plus/icons-vue' // Element Plus 图标组件
import { ElMessage } from 'element-plus'    // Element Plus 消息提示

// -- 全局实例与状态 --
const router = useRouter()           // 路由实例，用于登录成功后跳转
const userStore = useUserStore()     // 用户Store，管理登录状态和token
const formRef = ref()                // 表单组件引用，用于 validate() 验证
const loading = ref(false)           // 登录按钮loading状态，防止重复提交

// -- 表单数据（reactive响应式） --
// 默认预填admin账号方便开发测试
const form = reactive({ username: 'admin', password: 'admin123' })

// -- 表单验证规则 --
// 两个字段都为必填项，失焦时触发验证
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

/**
 * handleLogin - 处理登录提交
 * 
 * 数据流：
 * 1. 调用 formRef.value.validate() 进行Element Plus表单验证
 * 2. 验证通过后，调用 userStore.login(form) 发送POST请求到后端 /api/login
 * 3. userStore 内部处理：保存token到localStorage + 保存用户信息到state
 * 4. 成功后显示提示消息，并跳转到首页 '/'（Layout.vue）
 * 5. 错误信息由axios拦截器统一处理(utils/request.js)
 */
async function handleLogin() {
  // 表单验证，catch(() => false) 确保验证失败时返回false而非抛出异常
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    // 调用 Pinia Store 中的登录 action
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push('/')   // 跳转到首页布局（Layout.vue）
  } catch (e) {
    // 错误已由 axios 响应拦截器统一处理（显示错误消息）
    // 此处无需额外处理，仅捕获异常防止未处理promise rejection
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 登录页全屏容器：渐变蓝色背景，居中布局 */
.login-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1B2838 0%, #2775B6 50%, #1ABC9C 100%);
  position: relative; overflow: hidden;
}
/* 背景装饰层 */
.login-bg { position: absolute; inset: 0; overflow: hidden; }
/* 浮动圆形装饰元素 */
.bg-shape { position: absolute; border-radius: 50%; opacity: 0.08; background: white; }
.shape-1 { width: 600px; height: 600px; top: -200px; right: -100px; animation: float 20s ease-in-out infinite; }
.shape-2 { width: 400px; height: 400px; bottom: -150px; left: -80px; animation: float 15s ease-in-out infinite reverse; }
@keyframes float { 0%, 100% { transform: translate(0, 0); } 50% { transform: translate(30px, -30px); } }

/* 登录卡片：毛玻璃效果 + 圆角阴影 */
.login-card {
  position: relative; z-index: 1; width: 420px;
  background: rgba(255,255,255,0.95); backdrop-filter: blur(20px);
  border-radius: 16px; box-shadow: 0 20px 60px rgba(0,0,0,0.25); overflow: hidden;
}
/* 卡片头部渐变背景 */
.card-header {
  background: linear-gradient(135deg, var(--clinic-primary), var(--clinic-accent));
  padding: 40px 40px 30px; text-align: center; color: white;
}
.logo-icon { width: 56px; height: 56px; margin: 0 auto 14px; background: rgba(255,255,255,0.2); border-radius: 14px; display: flex; align-items: center; justify-content: center; padding: 8px; }
.card-header h1 { font-size: 22px; font-weight: 600; margin: 0; letter-spacing: 1px; }
.card-header p { font-size: 12px; opacity: 0.75; margin: 6px 0 0; letter-spacing: 0.5px; }

/* 登录表单区域样式 */
.login-form { padding: 32px 40px 20px; }
.login-form :deep(.el-input__wrapper) { border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.login-form :deep(.el-input__wrapper:hover) { box-shadow: 0 1px 6px rgba(39,117,182,0.15); }
.login-btn { height: 46px; font-size: 16px; letter-spacing: 4px; border-radius: 8px; margin-top: 8px; }

/* 底部提示文字 */
.card-footer { padding: 0 40px 28px; text-align: center; font-size: 11px; color: #999; display: flex; flex-direction: column; gap: 4px; }
</style>
