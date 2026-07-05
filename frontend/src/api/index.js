/**
 * api/index.js — API 接口层
 * --------------------------------------------------------
 * 用途：
 *   - 集中定义所有后端 API 接口，作为前端数据访问的统一入口
 *   - 按业务模块组织接口（认证、患者、医生、排班、挂号、就诊、处方、药品、账单、病历、用户管理）
 *   - 所有接口底层复用 @/utils/request.js 封装的 Axios 实例
 *
 * 设计思路：
 *   - 每个业务模块导出为一个对象（如 patientApi），包含该模块的 CRUD 操作
 *   - 接口函数统一接收 (params/data) 参数，透传给 request 实例
 *   - 请求方法遵循 RESTful 规范：GET 查询、POST 创建、PUT 更新、DELETE 删除
 *   - 特殊业务操作（如取消挂号、开始就诊、支付账单等）使用带动作后缀的 PUT 请求
 *
 * 返回值：
 *   - 所有接口返回 Promise，由 request.js 的响应拦截器统一处理
 *   - 成功时返回 { code: 200, data: ..., message: ... } 格式
 *   - 失败时 Promise.reject，业务 code 非 200 会全局提示错误信息
 */

import request from '@/utils/request'

// ================================================================
// 认证模块
// ================================================================

/**
 * 用户登录
 * @param {Object} data - { username: string, password: string }
 * @returns {Promise} 登录成功后返回 { token, username, realName, role, doctorId }
 */
export const loginApi = (data) => request({ url: '/auth/login', method: 'post', data })

// ================================================================
// 患者管理模块
// ================================================================

/**
 * 患者管理 API
 * - list: 分页查询患者列表
 * - create: 新增患者信息
 * - update: 更新患者信息
 * - delete: 根据 ID 删除患者
 */
export const patientApi = {
  list: (params) => request({ url: '/patients', method: 'get', params }),
  create: (data) => request({ url: '/patients', method: 'post', data }),
  update: (data) => request({ url: '/patients', method: 'put', data }),
  delete: (id) => request({ url: `/patients/${id}`, method: 'delete' })
}

// ================================================================
// 医生管理模块
// ================================================================

/**
 * 医生管理 API
 * 提供医生的增删改查操作
 */
export const doctorApi = {
  list: (params) => request({ url: '/doctors', method: 'get', params }),
  create: (data) => request({ url: '/doctors', method: 'post', data }),
  update: (data) => request({ url: '/doctors', method: 'put', data }),
  delete: (id) => request({ url: `/doctors/${id}`, method: 'delete' })
}

// ================================================================
// 排班管理模块
// ================================================================

/**
 * 排班管理 API
 * 管理医生的出诊时间安排
 */
export const scheduleApi = {
  list: (params) => request({ url: '/schedules', method: 'get', params }),
  create: (data) => request({ url: '/schedules', method: 'post', data }),
  update: (data) => request({ url: '/schedules', method: 'put', data }),
  delete: (id) => request({ url: `/schedules/${id}`, method: 'delete' })
}

// ================================================================
// 挂号管理模块
// ================================================================

/**
 * 挂号管理 API
 * - list: 查询挂号记录列表
 * - create: 创建新挂号（患者挂号预约）
 * - cancel: 取消已挂号记录（带 /cancel 动作后缀的 PUT 请求）
 */
export const registrationApi = {
  list: (params) => request({ url: '/registrations', method: 'get', params }),
  create: (data) => request({ url: '/registrations', method: 'post', data }),
  cancel: (id) => request({ url: `/registrations/${id}/cancel`, method: 'put' })
}

// ================================================================
// 就诊管理模块
// ================================================================

/**
 * 就诊管理 API
 * - list: 查询就诊记录列表
 * - start: 开始就诊（将状态从"待就诊"改为"就诊中"）
 * - complete: 完成就诊（记录诊断结果等数据）
 */
export const consultationApi = {
  list: (params) => request({ url: '/consultations', method: 'get', params }),
  start: (id) => request({ url: `/consultations/${id}/start`, method: 'put' }),
  complete: (id, data) => request({ url: `/consultations/${id}/complete`, method: 'put', data })
}

// ================================================================
// 处方管理模块
// ================================================================

/**
 * 处方管理 API
 * - list: 查询处方列表
 * - create: 开具新处方
 * - getById: 根据 ID 查看处方详情
 */
export const prescriptionApi = {
  list: (params) => request({ url: '/prescriptions', method: 'get', params }),
  create: (data) => request({ url: '/prescriptions', method: 'post', data }),
  getById: (id) => request({ url: `/prescriptions/${id}`, method: 'get' })
}

// ================================================================
// 药品管理模块
// ================================================================

/**
 * 药品管理 API
 * - list: 查询药品列表
 * - create: 新增药品
 * - update: 更新药品信息
 * - delete: 删除药品
 * - stock: 查询药品库存信息（特殊查询端点 /drugs/stock）
 */
export const drugApi = {
  list: (params) => request({ url: '/drugs', method: 'get', params }),
  create: (data) => request({ url: '/drugs', method: 'post', data }),
  update: (data) => request({ url: '/drugs', method: 'put', data }),
  delete: (id) => request({ url: `/drugs/${id}`, method: 'delete' }),
  stock: (params) => request({ url: '/drugs/stock', method: 'get', params })
}

// ================================================================
// 账单/收费管理模块
// ================================================================

/**
 * 账单管理 API
 * - list: 查询账单列表
 * - create: 创建新账单
 * - pay: 支付账单（带 /pay 动作后缀的 PUT 请求，可传入支付数据）
 */
export const billApi = {
  list: (params) => request({ url: '/bills', method: 'get', params }),
  create: (data) => request({ url: '/bills', method: 'post', data }),
  pay: (id, data) => request({ url: `/bills/${id}/pay`, method: 'put', data })
}

// ================================================================
// 病历管理模块
// ================================================================

/**
 * 病历管理 API
 * - list: 查询病历列表
 * - getById: 根据 ID 查看病历详情
 */
export const recordApi = {
  list: (params) => request({ url: '/records', method: 'get', params }),
  getById: (id) => request({ url: `/records/${id}`, method: 'get' })
}

// ================================================================
// 用户管理模块
// ================================================================

/**
 * 用户管理 API（系统管理员专用）
 * - list: 查询用户列表
 * - create: 创建新用户
 * - update: 更新用户信息
 * - delete: 删除用户
 * - toggleStatus: 切换用户启用/禁用状态（带 /toggle-status 动作后缀的 PUT 请求）
 */
export const userApi = {
  list: (params) => request({ url: '/users', method: 'get', params }),
  create: (data) => request({ url: '/users', method: 'post', data }),
  update: (data) => request({ url: '/users', method: 'put', data }),
  delete: (id) => request({ url: `/users/${id}`, method: 'delete' }),
  toggleStatus: (id) => request({ url: `/users/${id}/toggle-status`, method: 'put' })
}

// ================================================================
// 仪表盘模块
// ================================================================

/**
 * 仪表盘 API——获取首页聚合统计数据
 */
export const dashboardApi = {
  get: () => request({ url: '/dashboard', method: 'get' })
}
