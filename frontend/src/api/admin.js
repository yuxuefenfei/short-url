import request from '@/utils/request'

export const getUserList = (params) =>
  request({
    url: '/api/admin/users',
    method: 'get',
    params
  })

export const createUser = (data) =>
  request({
    url: '/api/admin/users',
    method: 'post',
    data
  })

export const getUserDetail = (userId) =>
  request({
    url: `/api/admin/users/${userId}`,
    method: 'get'
  })

export const updateUserStatus = (userId, status) =>
  request({
    url: `/api/admin/users/${userId}/status`,
    method: 'put',
    data: { status }
  })

export const updateUserInfo = (userId, data) =>
  request({
    url: `/api/admin/users/${userId}`,
    method: 'put',
    data
  })

export const deleteUser = (userId) =>
  request({
    url: `/api/admin/users/${userId}`,
    method: 'delete'
  })

export const resetUserPassword = (userId) =>
  request({
    url: `/api/admin/users/${userId}/reset-password`,
    method: 'post'
  })

export const getUrlList = (params) =>
  request({
    url: '/api/admin/urls',
    method: 'get',
    params
  })

export const getUrlDetail = (id) =>
  request({
    url: `/api/admin/urls/${id}`,
    method: 'get'
  })

export const updateUrlStatus = (id, status) =>
  request({
    url: `/api/admin/urls/${id}/status`,
    method: 'put',
    data: { status }
  })

export const deleteUrl = (id) =>
  request({
    url: `/api/admin/urls/${id}`,
    method: 'delete'
  })

export const batchUpdateUrlStatus = (ids, status) =>
  request({
    url: '/api/admin/urls/batch-status',
    method: 'put',
    params: { ids },
    data: { status }
  })

export const batchDeleteUrls = (ids) =>
  request({
    url: '/api/admin/urls/batch',
    method: 'delete',
    data: ids
  })

export const getUrlStats = (shortKey) =>
  request({
    url: `/api/admin/urls/${shortKey}/stats`,
    method: 'get'
  })

export const getSystemStats = () =>
  request({
    url: '/api/admin/system-stats',
    method: 'get'
  })

export const getDashboardOverview = (days = 7) =>
  request({
    url: '/api/admin/dashboard/overview',
    method: 'get',
    params: { days }
  })

export const getOperationLogs = (params) =>
  request({
    url: '/api/admin/operation-logs',
    method: 'get',
    params
  })

export const getOperationLogStats = () =>
  request({
    url: '/api/admin/operation-logs/stats',
    method: 'get'
  })
