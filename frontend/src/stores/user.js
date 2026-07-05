/**
 * stores/user.js — 用户状态管理（Pinia Store）
 * --------------------------------------------------------
 * 用途：
 *   - 管理全局用户认证状态：token（JWT 令牌）和 user（用户信息）
 *   - 提供登录和登出的业务逻辑
 *   - 配合 localStorage 实现状态持久化（页面刷新后仍保持登录状态）
 *
 * 采用 Composition API 风格的 Store 定义（推荐方式）：
 *   - defineStore('store名', setup函数) 创建 store
 *   - setup 函数内使用 ref() 定义响应式状态
 *   - 返回需要暴露的状态和方法
 *
 * 导出：
 *   - useUserStore: store 实例获取函数，组件中调用 useUserStore() 获取
 *   - 暴露 { token, user, login, logout }
 *
 * 状态持久化策略：
 *   - 初始化：从 localStorage 读取已有的 token 和 user
 *   - 登录：调用后端 API → 更新响应式状态 → 同步写入 localStorage
 *   - 登出：清空响应式状态 → 清除 localStorage
 *   - 目的：页面刷新后恢复登录状态，避免用户频繁重新登录
 *
 * 使用示例：
 *   import { useUserStore } from '@/stores/user'
 *   const userStore = useUserStore()
 *   userStore.login({ username: 'admin', password: '123' })
 *   userStore.logout()
 */

import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi } from '@/api'

/**
 * 用户 Store
 * store id: 'user' — 全局唯一标识，devtools 中可见
 */
export const useUserStore = defineStore('user', () => {
  // ================================================================
  // 响应式状态（State）
  // ================================================================

  /**
   * JWT 认证令牌
   * 初始值：尝试从 localStorage 恢复上次登录的 token，没有则为空字符串
   */
  const token = ref(localStorage.getItem('token') || '')

  /**
   * 当前登录用户信息对象
   * 结构：{ username, realName, role, doctorId }
   * 初始值：尝试从 localStorage 恢复，没有则为 null
   */
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  // ================================================================
  // 操作方法（Actions）
  // ================================================================

  /**
   * 用户登录
   * @param {Object} credentials - 登录凭证 { username: string, password: string }
   *
   * 执行流程：
   *   1. 调用 loginApi 向后端发送登录请求
   *   2. 从响应中提取 token 和用户信息
   *   3. 更新响应式状态（token, user）
   *   4. 将认证信息持久化到 localStorage（应对页面刷新）
   */
  async function login(credentials) {
    // 调用后端登录接口
    const res = await loginApi(credentials)

    // 更新响应式状态
    token.value = res.data.token
    user.value = {
      username: res.data.username,   // 登录用户名
      realName: res.data.realName,   // 真实姓名（用于页面显示）
      role: res.data.role,           // 角色（ADMIN/DOCTOR 等）
      doctorId: res.data.doctorId    // 关联的医生 ID（医生角色时有效）
    }

    // 持久化到 localStorage，确保页面刷新后保持登录状态
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  /**
   * 用户登出
   *
   * 执行流程：
   *   1. 清空响应式状态（token 置空，user 置 null）
   *   2. 清除 localStorage 中的认证数据
   *   3. 路由守卫检测到无 token 后会自动重定向到登录页
   */
  function logout() {
    // 清空响应式状态
    token.value = ''
    user.value = null

    // 清除持久化数据
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  // ================================================================
  // 对外暴露
  // ================================================================
  // 返回需要外部访问的状态和方法
  return { token, user, login, logout }
})
