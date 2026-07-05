/**
 * utils/request.js — HTTP 请求封装（Axios 实例）
 * --------------------------------------------------------
 * 用途：
 *   - 创建统一的 Axios 实例，配置基础 URL、超时时间等全局参数
 *   - 请求拦截器：自动为每个请求附加 JWT 认证 token
 *   - 响应拦截器：统一处理业务状态码和 HTTP 错误
 *
 * 导出：
 *   - request: 配置好的 Axios 实例，供 api/index.js 中各接口使用
 *
 * 设计要点：
 *   - baseURL 统一为 '/api'，通过 Vite 代理转发到后端（避免跨域）
 *   - 超时时间 15 秒，超时视为网络异常
 *   - 401 未授权错误自动清除登录状态并跳转登录页
 *   - 业务错误（code !== 200）统一通过 Element Plus 消息提示
 *
 * 使用方式：
 *   import request from '@/utils/request'
 *   request({ url: '/patients', method: 'get', params: { page: 1 } })
 */

import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 创建 Axios 实例
 *
 * baseURL: '/api'
 *   - 所有请求路径会以此为前缀，如 request({ url: '/patients' }) 实际请求 /api/patients
 *   - 开发环境下通过 Vite proxy 配置转发到后端服务
 *   - 生产环境下由 Nginx 反向代理处理
 *
 * timeout: 15000（毫秒）
 *   - 请求超过 15 秒未响应则自动中断，抛出超时错误
 *   - 超时错误在响应拦截器的 error 回调中统一处理
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// ================================================================
// 请求拦截器（Request Interceptor）
// ================================================================
/**
 * 在请求发送前执行：
 *   1. 从 localStorage 读取 JWT token
 *   2. 如果 token 存在，将其附加到请求头的 Authorization 字段
 *   3. 格式：Bearer <token>（JWT 标准认证格式）
 *
 * 这样做的好处：
 *   - 每个请求自动携带认证信息，无需在各接口调用处手动传递
 *   - 后端通过解析 Authorization 头验证用户身份
 */
request.interceptors.request.use(
  config => {
    // 从 localStorage 获取认证 token
    const token = localStorage.getItem('token')

    // 如果 token 存在，附加到请求头
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 返回处理后的 config，继续发送请求
    return config
  },
  // 请求配置错误（如无效的 URL），直接 reject
  error => Promise.reject(error)
)

// ================================================================
// 响应拦截器（Response Interceptor）
// ================================================================
/**
 * 在响应到达后、组件收到数据前执行：
 *
 * 成功回调：
 *   1. 提取 response.data（后端返回的业务数据）
 *   2. 检查业务状态码 code 是否为 200
 *   3. code === 200 → 返回 res（组件可直接使用 data 等字段）
 *   4. code !== 200 → 弹出错误提示，返回 Promise.reject
 *
 * 失败回调：
 *   1. 检查 HTTP 状态码
 *   2. 401 未授权 → 清除 token 和用户信息，跳转到登录页
 *   3. 其他错误 → 弹出错误提示
 */
request.interceptors.response.use(
  // ---- 成功回调：HTTP 状态码 2xx ----
  response => {
    const res = response.data   // 后端统一返回格式：{ code, data, message }

    // 业务状态码非 200，视为业务错误
    if (res.code !== 200) {
      // 使用 Element Plus 消息组件显示错误信息
      // res.message 由后端定义，如 "用户名或密码错误"
      ElMessage.error(res.message || '请求失败')

      // 返回 rejected Promise，调用方可在 .catch() 中捕获
      return Promise.reject(new Error(res.message))
    }

    // 业务正常，返回响应数据
    return res
  },

  // ---- 失败回调：HTTP 状态码非 2xx（如 4xx, 5xx） ----
  error => {
    // HTTP 401 未授权：token 过期或无效
    if (error.response?.status === 401) {
      // 清除本地认证数据
      localStorage.removeItem('token')
      localStorage.removeItem('user')

      // 强制跳转到登录页
      // 使用 window.location.href 而非 router.push
      // 因为 request.js 不直接依赖 Vue Router，保持工具函数的独立性
      window.location.href = '/login'
    }

    // 显示错误提示
    // error.message: Axios 生成的错误描述，如 "Network Error", "timeout of 15000ms exceeded"
    ElMessage.error(error.message || '网络异常')

    // 返回 rejected Promise，调用方可进一步处理
    return Promise.reject(error)
  }
)

// 导出封装好的 Axios 实例
export default request
