import request from '@/utils/request'

export const login = (data) =>
  request({
    url: '/api/auth/login',
    method: 'post',
    data
  })

export const register = (data) =>
  request({
    url: '/api/auth/register',
    method: 'post',
    data
  })

export const refreshToken = (token) =>
  request({
    url: '/api/auth/refresh-token',
    method: 'post',
    headers: {
      'Refresh-Token': token
    }
  })

export const logout = () =>
  request({
    url: '/api/auth/logout',
    method: 'post'
  })

export const validateToken = () =>
  request({
    url: '/api/auth/validate-token',
    method: 'get'
  })

export const getCurrentUser = () =>
  request({
    url: '/api/auth/current-user',
    method: 'get'
  })

export const checkUsernameExists = (username) =>
  request({
    url: '/api/auth/check-username',
    method: 'get',
    params: { username }
  })
