/**
 * 管理后台相关API接口
 */

import request from '@/utils/request'

/**
 * 获取用户列表
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页数量
 * @param {string} params.keyword - 搜索关键词
 * @returns {Promise} 用户列表
 */
export const getUserList = (params) => {
  return request({
    url: '/api/admin/users',
    method: 'get',
    params
  })
}

/**
 * 创建用户
 * @param {Object} data - 用户数据
 * @returns {Promise} 操作结果
 */
export const createUser = (data) => {
  return request({
    url: '/api/admin/users',
    method: 'post',
    data
  })
}

/**
 * 获取用户详情
 * @param {number} userId - 用户ID
 * @returns {Promise} 用户详情
 */
export const getUserDetail = (userId) => {
  return request({
    url: `/api/admin/users/${userId}`,
    method: 'get'
  })
}

/**
 * 更新用户状态
 * @param {number} userId - 用户ID
 * @param {number} status - 状态 (1:正常, 0:禁用)
 * @returns {Promise} 操作结果
 */
export const updateUserStatus = (userId, status) => {
  return request({
    url: `/api/admin/users/${userId}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 更新用户信息
 * @param {number} userId - 用户ID
 * @param {Object} data - 更新的数据
 * @param {string} data.email - 邮箱
 * @param {string} data.role - 角色
 * @returns {Promise} 操作结果
 */
export const updateUserInfo = (userId, data) => {
  return request({
    url: `/api/admin/users/${userId}`,
    method: 'put',
    data
  })
}

/**
 * 删除用户
 * @param {number} userId - 用户ID
 * @returns {Promise} 操作结果
 */
export const deleteUser = (userId) => {
  return request({
    url: `/api/admin/users/${userId}`,
    method: 'delete'
  })
}

/**
 * 重置用户密码
 * @param {number} userId - 用户ID
 * @returns {Promise} 操作结果
 */
export const resetUserPassword = (userId) => {
  return request({
    url: `/api/admin/users/${userId}/reset-password`,
    method: 'post'
  })
}

/**
 * 获取短网址列表
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页数量
 * @param {string} params.keyword - 搜索关键词
 * @param {number} params.status - 状态筛选
 * @returns {Promise} 短网址列表
 */
export const getUrlList = (params) => {
  return request({
    url: '/api/admin/urls',
    method: 'get',
    params
  })
}

/**
 * 获取短网址详情
 * @param {number} id - 短网址ID
 * @returns {Promise} 短网址详情
 */
export const getUrlDetail = (id) => {
  return request({
    url: `/api/admin/urls/${id}`,
    method: 'get'
  })
}

/**
 * 更新短网址状态
 * @param {number} id - 短网址ID
 * @param {number} status - 状态 (1:正常, 0:禁用)
 * @returns {Promise} 操作结果
 */
export const updateUrlStatus = (id, status) => {
  return request({
    url: `/api/admin/urls/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 删除短网址
 * @param {number} id - 短网址ID
 * @returns {Promise} 操作结果
 */
export const deleteUrl = (id) => {
  return request({
    url: `/api/admin/urls/${id}`,
    method: 'delete'
  })
}

/**
 * 批量更新短网址状态
 * @param {Array} ids - 短网址ID列表
 * @param {number} status - 状态
 * @returns {Promise} 操作结果
 */
export const batchUpdateUrlStatus = (ids, status) => {
  return request({
    url: '/api/admin/urls/batch-status',
    method: 'put',
    params: { ids, status }
  })
}

/**
 * 批量删除短网址
 * @param {Array} ids - 短网址ID列表
 * @returns {Promise} 操作结果
 */
export const batchDeleteUrls = (ids) => {
  return request({
    url: '/api/admin/urls/batch',
    method: 'delete',
    data: ids
  })
}

/**
 * 获取短网址统计信息
 * @param {string} shortKey - 短网址Key
 * @returns {Promise} 统计信息
 */
export const getUrlStats = (shortKey) => {
  return request({
    url: `/api/admin/urls/${shortKey}/stats`,
    method: 'get'
  })
}

/**
 * 获取系统统计信息
 * @returns {Promise} 系统统计信息
 */
export const getSystemStats = () => {
  return request({
    url: '/api/admin/system-stats',
    method: 'get'
  })
}

/**
 * 获取操作日志列表
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页数量
 * @param {number} params.userId - 用户ID
 * @param {string} params.operationType - 操作类型
 * @param {string} params.module - 操作模块
 * @param {string} params.startTime - 开始时间
 * @param {string} params.endTime - 结束时间
 * @returns {Promise} 操作日志列表
 */
export const getOperationLogs = (params) => {
  return request({
    url: '/api/admin/operation-logs',
    method: 'get',
    params
  })
}

/**
 * 导出操作日志
 * @param {Object} params - 查询参数
 * @returns {Promise} 导出结果
 */
export const exportOperationLogs = (params) => {
  return request({
    url: '/api/admin/operation-logs/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

/**
 * 获取操作日志统计
 * @returns {Promise} 统计结果
 */
export const getOperationLogStats = () => {
  return request({
    url: '/api/admin/operation-logs/stats',
    method: 'get'
  })
}