import request from '@/utils/request'

export const shortenUrl = (data) =>
  request({
    url: '/api/shorten',
    method: 'post',
    data: {
      ...data,
      expiredTime: data.expiredTime ?? data.expiredAt ?? null
    }
  })

export const getUrlStats = (shortKey) =>
  request({
    url: `/api/stats/${shortKey}`,
    method: 'get'
  })

export const getUrlList = (params = {}) =>
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

export const updateUrl = (id, data) =>
  request({
    url: `/api/admin/urls/${id}`,
    method: 'put',
    data
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
