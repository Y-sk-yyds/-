/**
 * router/index.js — 前端路由配置
 * --------------------------------------------------------
 * 用途：
 *   - 定义应用所有前端路由规则，控制页面导航和组件渲染
 *   - 实现路由级别的代码分割（懒加载），优化首屏加载性能
 *   - 通过全局前置守卫实现登录认证拦截
 *
 * 路由架构：
 *   - 顶层路由分为两个分支：
 *     1. /login → 登录页（独立路由，无布局框架）
 *     2. / → 主布局框架（Layout.vue），包含侧边栏和顶部导航
 *        └── children: 各业务页面路由（dashboard, patient, doctor 等）
 *
 * 关键设计：
 *   - 使用动态 import() 实现路由懒加载，每个页面组件独立打包为 chunk
 *     格式：() => import('@/views/模块名/组件名.vue')
 *     效果：访问某个页面时才请求对应的 JS chunk，减少首屏加载体积
 *   - meta.title 存储页面标题，可用于动态设置 document.title 或面包屑导航
 *   - meta.roles 存储权限要求，用户管理页仅 ADMIN 角色可访问
 *   - 路由守卫检查 localStorage 中的 token，未登录用户强制跳转到登录页
 */

import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由配置表
 * 每个路由对象包含：
 *   - path: URL 路径
 *   - name: 路由名称（可用于编程式导航 router.push({ name: 'xxx' })）
 *   - component: 路由对应的页面组件（支持懒加载）
 *   - redirect: 重定向路径
 *   - meta: 路由元信息（标题、权限等）
 *   - children: 嵌套子路由
 */
const routes = [
  // ================================================================
  // 登录路由（独立页面，不使用 Layout 布局）
  // ================================================================
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/layout/Login.vue'),  // 懒加载登录组件
    meta: { title: '登录' }
  },

  // ================================================================
  // 主布局路由（嵌套所有业务页面）
  // ================================================================
  {
    path: '/',
    component: () => import('@/views/layout/Layout.vue'),  // 懒加载主布局组件
    redirect: '/dashboard',    // 访问根路径时默认重定向到工作台
    children: [
      // ---- 工作台（首页仪表盘） ----
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '工作台' }
      },
      // ---- 患者管理 ----
      {
        path: 'patient',
        name: 'Patient',
        component: () => import('@/views/patient/PatientList.vue'),
        meta: { title: '患者管理' }
      },
      // ---- 医生管理 ----
      {
        path: 'doctor',
        name: 'Doctor',
        component: () => import('@/views/doctor/DoctorList.vue'),
        meta: { title: '医生管理' }
      },
      // ---- 排班管理 ----
      {
        path: 'schedule',
        name: 'Schedule',
        component: () => import('@/views/schedule/ScheduleList.vue'),
        meta: { title: '排班管理' }
      },
      // ---- 挂号管理 ----
      {
        path: 'registration',
        name: 'Registration',
        component: () => import('@/views/registration/RegistrationList.vue'),
        meta: { title: '挂号管理' }
      },
      // ---- 就诊管理 ----
      {
        path: 'consultation',
        name: 'Consultation',
        component: () => import('@/views/consultation/ConsultationList.vue'),
        meta: { title: '就诊管理' }
      },
      // ---- 处方管理 ----
      {
        path: 'prescription',
        name: 'Prescription',
        component: () => import('@/views/prescription/PrescriptionList.vue'),
        meta: { title: '处方管理' }
      },
      // ---- 药品管理 ----
      {
        path: 'drug',
        name: 'Drug',
        component: () => import('@/views/drug/DrugList.vue'),
        meta: { title: '药品管理' }
      },
      // ---- 收费管理 ----
      {
        path: 'bill',
        name: 'Bill',
        component: () => import('@/views/bill/BillList.vue'),
        meta: { title: '收费管理' }
      },
      // ---- 病历管理 ----
      {
        path: 'record',
        name: 'Record',
        component: () => import('@/views/record/RecordList.vue'),
        meta: { title: '病历管理' }
      },
      // ---- 用户管理（管理员专用） ----
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/UserList.vue'),
        meta: {
          title: '用户管理',
          roles: ['ADMIN']   // 仅管理员角色可访问，前端权限校验
        }
      }
    ]
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),  // 使用 HTML5 History 模式（去除 URL 中的 # 号）
  routes                        // 传入路由配置表
})

// ================================================================
// 全局前置路由守卫（Navigation Guard）
// ================================================================
/**
 * 每次路由跳转前触发，用于认证拦截
 *
 * to: 目标路由对象（即将进入的路由）
 * from: 来源路由对象（即将离开的路由）
 * next: 路由放行函数
 *   - next(): 放行，允许导航
 *   - next('/login'): 重定向到登录页
 *
 * 逻辑：
 *   1. 从 localStorage 读取 token（登录时由 userStore 写入）
 *   2. 如果目标不是登录页 && 没有 token → 强制跳转到登录页
 *   3. 否则放行（已登录或有 token 则正常访问，目标为登录页也放行）
 */
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  // 未认证用户试图访问非登录页 → 重定向到登录页
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    // 已登录或访问的是登录页 → 正常放行
    next()
  }
})

// 导出路由实例，供 main.js 中的 app.use(router) 使用
export default router
