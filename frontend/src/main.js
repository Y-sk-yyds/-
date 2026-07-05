/**
 * main.js — 应用入口文件
 * --------------------------------------------------------
 * 用途：
 *   - 创建 Vue 应用实例并挂载到 DOM
 *   - 注册全局插件：Pinia（状态管理）、Vue Router（路由）、Element Plus（UI 组件库）
 *   - 注册 Element Plus 所有图标为全局组件
 *   - 引入全局样式：Element Plus 主题 + 项目自定义主题
 *
 * 启动流程：
 *   1. createApp(App) — 以 App.vue 为根组件创建应用实例
 *   2. 遍历注册 Element Plus 图标组件
 *   3. app.use(createPinia()) — 注册 Pinia 状态管理
 *   4. app.use(router) — 注册 Vue Router 路由系统
 *   5. app.use(ElementPlus) — 注册 Element Plus UI 组件库
 *   6. app.mount('#app') — 将应用挂载到 index.html 中的 #app 元素
 *
 * 依赖关系：
 *   - router 在挂载前注册 → 路由守卫可在页面加载时生效
 *   - Pinia 在挂载前注册 → 组件中可直接使用 useXxxStore()
 *   - Element Plus 图标全局注册 → 模板中直接使用 <el-icon-xxx /> 无需额外引入
 */

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'       // Element Plus 全局样式
import '@/assets/theme.css'                 // 项目自定义主题色变量
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

// 创建 Vue 应用实例，传入根组件 App.vue
const app = createApp(App)

// 批量注册所有 Element Plus 图标为全局组件
// 遍历 @element-plus/icons-vue 导出的所有图标组件
// key: 图标名称（如 'Edit', 'Delete', 'Search'）
// component: 对应的 Vue 组件定义
// 效果：模板中可以直接使用 <el-icon-edit /> 等标签
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册 Pinia 状态管理库
// Pinia 是 Vue 3 官方推荐的状态管理方案，替代 Vuex
app.use(createPinia())

// 注册 Vue Router 路由系统
// 路由配置定义在 @/router/index.js 中
app.use(router)

// 注册 Element Plus UI 组件库
// 使所有 Element Plus 组件（如 el-button, el-table, el-dialog 等）全局可用
app.use(ElementPlus)

// 将 Vue 应用挂载到 public/index.html 中的 <div id="app"></div> 元素上
// 挂载后应用开始渲染，路由守卫生效
app.mount('#app')
