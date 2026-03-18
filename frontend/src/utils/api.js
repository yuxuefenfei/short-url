import request from './request'

class ApiClient {
  get(url, params = {}) {
    return request({
      url: this.normalizeUrl(url),
      method: 'get',
      params
    })
  }

  post(url, data = {}) {
    return request({
      url: this.normalizeUrl(url),
      method: 'post',
      data
    })
  }

  put(url, data = {}) {
    return request({
      url: this.normalizeUrl(url),
      method: 'put',
      data
    })
  }

  delete(url, data) {
    return request({
      url: this.normalizeUrl(url),
      method: 'delete',
      data
    })
  }

  normalizeUrl(url) {
    return url.startsWith('/api') ? url : `/api${url}`
  }
}

export const apiClient = new ApiClient()

export const api = {
  auth: {
    login: (data) => apiClient.post('/auth/login', data),
    register: (data) => apiClient.post('/auth/register', data),
    refreshToken: () => apiClient.post('/auth/refresh-token'),
    logout: () => apiClient.post('/auth/logout'),
    getCurrentUser: () => apiClient.get('/auth/me')
  },
  url: {
    create: (data) => apiClient.post('/shorten', data),
    stats: (shortKey) => apiClient.get(`/stats/${shortKey}`)
  },
  user: {
    list: (params) => apiClient.get('/admin/users', params),
    create: (data) => apiClient.post('/admin/users', data),
    update: (id, data) => apiClient.put(`/admin/users/${id}`, data),
    delete: (id) => apiClient.delete(`/admin/users/${id}`),
    resetPassword: (id) => apiClient.post(`/admin/users/${id}/reset-password`)
  },
  logs: {
    list: (params) => apiClient.get('/admin/operation-logs', params),
    stats: () => apiClient.get('/admin/operation-logs/stats')
  },
  system: {
    stats: () => apiClient.get('/admin/system-stats'),
    dashboard: (days = 7) => apiClient.get('/admin/dashboard/overview', { days }),
    health: () => apiClient.get('/health')
  }
}

export default api
