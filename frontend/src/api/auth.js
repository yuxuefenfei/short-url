/**
 * 认证相关API接口
 */

import request from '@/utils/request'

/**
 * 用户登录
 * @param {Object} data - 登录信息
 * @param {string} data.username - 用户名
 * @param {string} data.password - 密码
 * @returns {Promise} 登录结果
 */
export const login = (data) => {
  return request({
    url: '/api/auth/login',
    method: 'post',
    data
  })
}

/**
 * 用户注册
 * @param {Object} data - 注册信息
 * @param {string} data.username - 用户名
 * @param {string} data.password - 密码
 * @param {string} data.email - 邮箱（可选）
 * @returns {Promise} 注册结果
 */
export const register = (data) => {
  return request({
    url: '/api/auth/register',
    method: 'post',
    data
  })
}

/**
 * 刷新Token
 * @param {string} refreshToken - 刷新Token
 * @returns {Promise} 新的Token
 */
export const refreshToken = (refreshToken) => {
  return request({
    url: '/api/auth/refresh-token',
    method: 'post',
    headers: {
      'Refresh-Token': refreshToken
    }
  })
}

/**
 * 用户登出
 * @param {string} token - 用户Token
 * @returns {Promise} 登出结果
 */
export const logout = (token) => {
  return request({
    url: '/api/auth/logout',
    method: 'post',
    headers: {
      'Authorization': token
    }
  })
}

/**
 * 验证Token有效性
 * @param {string} token - 用户Token
 * @returns {Promise} 验证结果
 */
export const validateToken = (token) => {
  return request({
    url: '/api/auth/validate-token',
    method: 'get',
    headers: {
      'Authorization': token
    }
  })
}

/**
 * 获取当前用户信息
 * @param {string} token - 用户Token
 * @returns {Promise} 用户信息
 */
export const getCurrentUser = (token) => {
  return request({
    url: '/api/auth/current-user',
    method: 'get',
    headers: {
      'Authorization': token
    }
  })
}