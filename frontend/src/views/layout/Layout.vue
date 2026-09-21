<template>
  <!--
    主布局页面 Layout.vue
    - 页面用途：整个管理系统的外壳布局，采用经典的"侧边栏 + 顶部栏 + 主体"结构。
    - 角色要求：所有登录用户均可访问。用户管理菜单仅 ADMIN 角色可见。
    - 关键功能：
      1. 左侧可折叠侧边栏：展示所有功能模块的导航菜单
      2. 顶部信息栏：显示页面标题、当前用户信息、退出按钮
      3. 主体区域：通过 <router-view /> 动态渲染子路由页面
  -->
  <el-container class="app-layout">
    <!--
      左侧侧边栏区域
      - :width 根据 isCollapse 状态切换宽度：展开220px / 折叠72px
      - 深色背景主题，所有菜单使用 router 模式（点击自动导航）
    -->
    <el-aside :width="isCollapse ? '62px' : '270px'" class="app-sidebar">
      <!-- Logo 区域：点击可切换侧边栏展开/折叠 -->
      <div class="sidebar-logo" @click="isCollapse = !isCollapse">
        <div class="logo-box">
          <svg viewBox="0 0 36 36" width="28" height="28">
            <rect x="8" y="3" width="20" height="30" rx="3" fill="white" opacity="0.3"/>
            <rect x="11" y="7" width="14" height="3" rx="1.5" fill="white" opacity="0.6"/>
            <rect x="12" y="12" width="10" height="10" rx="2" fill="white"/>
            <circle cx="17" cy="17" r="2.5" fill="var(--clinic-primary)"/>
          </svg>
        </div>
        <!-- 折叠时隐藏文字，展开时渐入显示 -->
        <transition name="fade">
          <span v-show="!isCollapse" class="logo-text">诊所管理系统</span>
        </transition>
      </div>

      <!--
        导航菜单区域
        - router 属性：启用路由模式，el-menu-item 的 index 即为路由路径
        - collapse 属性：控制文字的显示/隐藏
        - 菜单项对应10个功能模块 + 1个用户管理（仅管理员可见）
      -->
      <el-menu
        :default-active="activeMenu"
        router
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="transparent"
        text-color="rgba(255,255,255,0.65)"
        active-text-color="#fff"
        class="sidebar-menu"
      >
        <!-- 工作台 - 首页仪表盘 -->
        <el-menu-item index="/dashboard">
          <template #title>
            <div class="menu-item">
              <el-icon :size="18"><DataBoard /></el-icon>
              <span>工作台</span>
            </div>
          </template>
        </el-menu-item>
        <!-- 患者管理 -->
        <el-menu-item index="/patient">
          <template #title>
            <div class="menu-item">
              <el-icon :size="18"><User /></el-icon>
              <span>患者管理</span>
            </div>
          </template>
        </el-menu-item>
        <!-- 医生管理：仅管理员可见 -->
        <el-menu-item index="/doctor" v-if="userStore.user?.role === 'ADMIN'">
          <template #title>
            <div class="menu-item">
              <el-icon :size="18"><Avatar /></el-icon>
              <span>医生管理</span>
            </div>
          </template>
        </el-menu-item>
        <!-- 排班管理：仅管理员可见 -->
        <el-menu-item index="/schedule" v-if="userStore.user?.role === 'ADMIN'">
          <template #title>
            <div class="menu-item">
              <el-icon :size="18"><Calendar /></el-icon>
              <span>排班管理</span>
            </div>
          </template>
        </el-menu-item>
        <!-- 挂号管理 -->
        <el-menu-item index="/registration">
          <template #title>
            <div class="menu-item">
              <el-icon :size="18"><Tickets /></el-icon>
              <span>挂号管理</span>
            </div>
          </template>
        </el-menu-item>
        <!-- 就诊管理：仅管理员和医生可见 -->
        <el-menu-item index="/consultation" v-if="userStore.user?.role === 'ADMIN' || userStore.user?.role === 'DOCTOR'">
          <template #title>
            <div class="menu-item">
              <el-icon :size="18"><Document /></el-icon>
              <span>就诊管理</span>
            </div>
          </template>
        </el-menu-item>
        <!-- 处方管理 -->
        <el-menu-item index="/prescription">
          <template #title>
            <div class="menu-item">
              <el-icon :size="18"><Notebook /></el-icon>
              <span>处方管理</span>
            </div>
          </template>
        </el-menu-item>
        <!-- 药品管理：仅管理员和医生可见 -->
        <el-menu-item index="/drug" v-if="userStore.user?.role === 'ADMIN' || userStore.user?.role === 'DOCTOR'">
          <template #title>
            <div class="menu-item">
              <el-icon :size="18"><Box /></el-icon>
              <span>药品管理</span>
            </div>
          </template>
        </el-menu-item>
        <!-- 收费管理：仅管理员和收银员可见 -->
        <el-menu-item index="/bill" v-if="userStore.user?.role === 'ADMIN' || userStore.user?.role === 'CASHIER'">
          <template #title>
            <div class="menu-item">
              <el-icon :size="18"><Coin /></el-icon>
              <span>收费管理</span>
            </div>
          </template>
        </el-menu-item>
        <!-- 病历管理：仅 ADMIN 和 DOCTOR 可见 -->
        <el-menu-item index="/record" v-if="userStore.user?.role === 'ADMIN' || userStore.user?.role === 'DOCTOR'">
          <template #title>
            <div class="menu-item">
              <el-icon :size="18"><Files /></el-icon>
              <span>病历管理</span>
            </div>
          </template>
        </el-menu-item>
        <!-- 用户管理：仅 ADMIN 角色可见，通过 v-if 控制显示 -->
        <el-menu-item index="/user" v-if="userStore.user?.role === 'ADMIN'">
          <template #title>
            <div class="menu-item">
              <el-icon :size="18"><Setting /></el-icon>
              <span>用户管理</span>
            </div>
          </template>
        </el-menu-item>
      </el-menu>

      <!-- 侧边栏底部：折叠/展开箭头按钮 -->
      <div class="sidebar-footer">
        <div class="collapse-trigger" @click="isCollapse = !isCollapse">
          <el-icon :size="16"><ArrowLeftBold v-if="!isCollapse" /><ArrowRightBold v-else /></el-icon>
        </div>
      </div>
    </el-aside>

    <!-- 右侧容器：顶部栏 + 主体内容 -->
    <el-container>
      <!--
        顶部信息栏
        - 左侧显示当前页面的标题（由路由 meta.title 提供）
        - 右侧显示用户头像（首字母）、姓名、角色、退出按钮
      -->
      <el-header class="app-header">
        <!-- 面包屑/页面标题：从路由元信息获取 -->
        <div class="header-breadcrumb">
          <span class="page-title">{{ route.meta.title }}</span>
        </div>
        <!-- 用户信息区 -->
        <div class="header-right">
          <!-- 用户头像：展示用户真实姓名的首字母 -->
          <span class="user-avatar">{{ userStore.user?.realName?.[0] }}</span>
          <div class="user-info">
            <span class="user-name">{{ userStore.user?.realName }}</span>
            <!-- 角色标签：ADMIN->管理员, DOCTOR->医生, CASHIER->收银员 -->
            <span class="user-role">{{ roleLabel }}</span>
          </div>
          <!-- 退出登录按钮 -->
          <el-button text @click="handleLogout" class="logout-btn">
            <el-icon><SwitchButton /></el-icon>
          </el-button>
        </div>
      </el-header>

      <!--
        主体内容区域
        - <router-view /> 根据当前路由动态渲染对应的页面组件
        - 这是Vue Router的核心：不同路径切换不同的子组件
      -->
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
// -- 核心导入 --
import { ref, computed } from 'vue'           // Vue 3 API
import { useRouter, useRoute } from 'vue-router' // 路由实例
import { useUserStore } from '@/stores/user'   // 用户状态管理 Store
import {
  DataBoard, User, Avatar, Calendar, Tickets, Document,
  Notebook, Box, Coin, Files, Setting, SwitchButton,
  ArrowLeftBold, ArrowRightBold
} from '@element-plus/icons-vue'              // Element Plus 图标组件

// -- 全局实例与状态 --
const router = useRouter()         // 路由实例，用于退出后跳转到登录页
const route = useRoute()           // 当前路由信息，获取 meta.title
const userStore = useUserStore()   // 用户 Store，获取当前登录用户信息
const isCollapse = ref(false)      // 侧边栏折叠状态，false=展开, true=折叠

/**
 * activeMenu - 计算属性：当前激活的菜单项
 * 
 * 数据流：绑定到 el-menu 的 default-active 属性，
 * 使当前路由路径对应的菜单项高亮显示。
 */
const activeMenu = computed(() => route.path)

/**
 * roleLabel - 计算属性：将角色枚举值转为中文显示
 * 
 * 映射关系：
 *   ADMIN   -> 管理员
 *   DOCTOR  -> 医生
 *   CASHIER -> 收银员
 */
const roleLabel = computed(() => {
  const map = { ADMIN: '管理员', DOCTOR: '医生', CASHIER: '收银员' }
  return map[userStore.user?.role] || ''
})

/**
 * handleLogout - 退出登录
 * 
 * 数据流：
 * 1. 调用 userStore.logout() 清除 localStorage 中的 token 和用户信息
 * 2. 跳转到 /login 登录页面
 */
const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
/* 整体布局占满视口高度 */
.app-layout { height: 100vh; }

/* ---- 侧边栏样式 ---- */
.app-sidebar {
  background: var(--clinic-sidebar);
  display: flex; flex-direction: column;
  transition: width 0.25s ease;       /* 折叠/展开过渡动画 */
  overflow: hidden;
}
/* Logo区域 */
.sidebar-logo {
  height: 64px; display: flex; align-items: center; gap: 10px;
  padding: 0 20px; border-bottom: 1px solid rgba(255,255,255,0.06);
  cursor: pointer; white-space: nowrap;
}
.logo-box { flex-shrink: 0; }
.logo-text { font-size: 17px; font-weight: 600; color: #fff; letter-spacing: 1px; }

/* 菜单样式 */
.sidebar-menu { border-right: none; flex: 1; padding: 8px; overflow-y: auto; overflow-x: hidden; }
.sidebar-menu .el-menu-item {
  border-radius: 8px; margin-bottom: 2px; height: 44px; line-height: 44px;
}
.sidebar-menu .el-menu-item:hover { background: rgba(159, 25, 105, 0.08) !important; }
.sidebar-menu .el-menu-item.is-active { background: var(--clinic-primary) !important; color: #fff !important; }
.menu-item { display: flex; align-items: center; gap: 10px; }

/* 底部折叠按钮 */
.sidebar-footer { padding: 8px; border-top: 1px solid rgba(255,255,255,0.06); }
.collapse-trigger {
  display: flex; align-items: center; justify-content: center; height: 36px;
  border-radius: 8px; cursor: pointer; color: rgba(255,255,255,0.4);
  transition: all 0.2s;
}
.collapse-trigger:hover { background: rgba(255,255,255,0.08); color: rgba(255,255,255,0.8); }

/* ---- 顶部栏样式 ---- */
.app-header {
  display: flex; align-items: center; justify-content: space-between;
  height: 60px; background: rgba(29, 61, 131, 0.77); padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.24);
}
.page-title { font-size: 16px; font-weight: 600; color: #1a1a2e; }

/* 用户信息区 */
.header-right { display: flex; align-items: center; gap: 10px; }
/* 用户头像圆形背景 */
.user-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: var(--clinic-primary); color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600;
}
.user-info { display: flex; flex-direction: column; line-height: 1.2; }
.user-name { font-size: 13px; font-weight: 500; color: #303133; }
.user-role { font-size: 11px; color: #909399; }
.logout-btn { color: #909399; font-size: 18px; }
.logout-btn:hover { color: var(--clinic-danger); }

/* ---- 主体内容区域 ---- */
.app-main { background: var(--clinic-bg); padding: 20px; min-height: calc(100vh - 60px); }

/* ---- Logo文字的过渡动画 ---- */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ---- 侧边栏滚动条美化 ---- */
.sidebar-menu::-webkit-scrollbar { width: 4px; }
.sidebar-menu::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.15); border-radius: 2px; }
</style>
