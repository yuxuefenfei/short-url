/**
 * API请求封装工具
 *
 * 模块职责：
 * - 统一的HTTP请求处理
 * - 自动错误处理和重试
 * - 请求/响应拦截
 * - 加载状态管理
 */

import axios from 'axios'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'
import errorHandler, { withErrorHandling } from './errorHandler'

// 创建Axios实例
const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    // 添加认证token
    const userStore = useUserStore()
    const token = userStore.token

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 添加时间戳防止缓存
    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: Date.now()
      }
    }

    // 开发环境日志
    if (process.env.NODE_ENV === 'development') {
      console.log('API Request:', config.method?.toUpperCase(), config.url, config.data || config.params)
    }

    return config
  },
  (error) => {
    console.error('请求配置错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => {
    const { data, config } = response

    // 开发环境日志
    if (process.env.NODE_ENV === 'development') {
      console.log('API Response:', config.method?.toUpperCase(), config.url, data)
    }

    // 处理统一响应格式
    if (data && typeof data === 'object') {
      // 成功响应
      if (data.code === 200 || data.success === true) {
        return data.data !== undefined ? data.data : data
      }
      // 业务错误
      else {
        const errorMessage = data.message || '操作失败'

        // 特殊处理认证错误
        if (data.code === 401) {
          const userStore = useUserStore()
          userStore.logout()

          // 延迟跳转到登录页
          setTimeout(() => {
            window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname)
          }, 1500)
        }

        return Promise.reject({
          response: {
            status: data.code || 400,
            data: data,
            config: config
          }
        })
      }
    }

    return response.data || response
  },
  (error) => {
    // 处理网络错误和超时
    if (!error.response) {
      error.response = {
        status: 0,
        data: { message: '网络连接失败' },
        config: error.config
      }
    }

    return Promise.reject(error)
  }
)

/**
 * API请求封装类
 */
class ApiClient {
  constructor() {
    this.loadingStates = new Map()
  }

  /**
   * GET请求
   */
  async get(url, params = {}, options = {}) {
    return this.request('get', url, { params }, options)
  }

  /**
   * POST请求
   */
  async post(url, data = {}, options = {}) {
    return this.request('post', url, { data }, options)
  }

  /**
   * PUT请求
   */
  async put(url, data = {}, options = {}) {
    return this.request('put', url, { data }, options)
  }

  /**
   * DELETE请求
   */
  async delete(url, options = {}) {
    return this.request('delete', url, {}, options)
  }

  /**
   * 通用请求方法
   */
  async request(method, url, config = {}, options = {}) {
    const {
      showLoading = true,
      showMessage = true,
      retry = false,
      loadingKey = `${method}:${url}`
    } = options

    // 创建加载状态管理器
    const loadingManager = showLoading ? this.getLoadingManager(loadingKey) : null

    // 包装请求函数
    const requestFn = async () => {
      return await apiClient({
        method,
        url,
        ...config
      })
    }

    // 应用错误处理装饰器
    const wrappedRequest = withErrorHandling(requestFn, {
      showMessage,
      retry,
      loading: loadingManager
    })

    try {
      return await wrappedRequest()
    } catch (error) {
      // 错误已经被处理，直接抛出
      throw error
    }
  }

  /**
   * 获取加载状态管理器
   */
  getLoadingManager(key) {
    if (!this.loadingStates.has(key)) {
      this.loadingStates.set(key, {
        loading: false,
        start: function() {
          this.loading = true
        },
        stop: function() {
          this.loading = false
        }
      })
    }
    return this.loadingStates.get(key)
  }

  /**
   * 获取加载状态
   */
  getLoadingState(key) {
    return this.loadingStates.get(key)?.loading || false
  }

  /**
   * 批量请求
   */
  async batch(requests, options = {}) {
    const { concurrency = 3, showLoading = true } = options

    if (!Array.isArray(requests) || requests.length === 0) {
      return []
    }

    const results = []
    const loadingManager = showLoading ? this.getLoadingManager('batch') : null

    try {
      if (loadingManager) {
        loadingManager.start()
      }

      // 分批处理请求
      for (let i = 0; i < requests.length; i += concurrency) {
        const batch = requests.slice(i, i + concurrency)
        const batchPromises = batch.map(req => {
          const { method, url, config, options: reqOptions } = req
          return this.request(method, url, config, { ...options, ...reqOptions, showLoading: false })
        })

        const batchResults = await Promise.allSettled(batchPromises)
        results.push(...batchResults)
      }

      return results
    } catch (error) {
      errorHandler.handleHttpError(error)
      throw error
    } finally {
      if (loadingManager) {
        loadingManager.stop()
      }
    }
  }

  /**
   * 文件上传
   */
  async upload(url, file, data = {}, options = {}) {
    const {
      showLoading = true,
      onProgress,
      ...otherOptions
    } = options

    const formData = new FormData()
    formData.append('file', file)

    // 添加其他数据
    Object.keys(data).forEach(key => {
      formData.append(key, data[key])
    })

    const loadingManager = showLoading ? this.getLoadingManager(`upload:${url}`) : null

    try {
      if (loadingManager) {
        loadingManager.start()
      }

      const response = await apiClient({
        method: 'post',
        url,
        data: formData,
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        onUploadProgress: (progressEvent) => {
          if (onProgress) {
            const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
            onProgress(percentCompleted)
          }
        }
      })

      return response
    } catch (error) {
      errorHandler.handleHttpError(error)
      throw error
    } finally {
      if (loadingManager) {
        loadingManager.stop()
      }
    }
  }

  /**
   * 文件下载
   */
  async download(url, params = {}, filename, options = {}) {
    const { showLoading = true } = options
    const loadingManager = showLoading ? this.getLoadingManager(`download:${url}`) : null

    try {
      if (loadingManager) {
        loadingManager.start()
      }

      const response = await apiClient({
        method: 'get',
        url,
        params,
        responseType: 'blob'
      })

      // 创建下载链接
      const blob = new Blob([response.data])
      const downloadUrl = window.URL.createObjectURL(blob)

      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = filename || `download_${Date.now()}`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)

      window.URL.revokeObjectURL(downloadUrl)

      return true
    } catch (error) {
      errorHandler.handleHttpError(error)
      throw error
    } finally {
      if (loadingManager) {
        loadingManager.stop()
      }
    }
  }

  /**
   * 设置全局配置
   */
  setGlobalConfig(config) {
    Object.assign(apiClient.defaults, config)
  }

  /**
   * 设置认证token
   */
  setAuthToken(token) {
    if (token) {
      apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`
    } else {
      delete apiClient.defaults.headers.common['Authorization']
    }
  }
}

// 创建API客户端实例
export const apiClient = new ApiClient()

// 常用API方法封装
export const api = {
  // 用户认证相关
  auth: {
    login: (data) => apiClient.post('/auth/login', data, { retry: true }),
    register: (data) => apiClient.post('/auth/register', data),
    refreshToken: (refreshToken) => apiClient.post('/auth/refresh', { refreshToken }),
    logout: () => apiClient.post('/auth/logout'),
    getCurrentUser: () => apiClient.get('/auth/me')
  },

  // 短网址相关
  url: {
    create: (data) => apiClient.post('/url/create', data),
    list: (params) => apiClient.get('/url/list', params, { retry: true }),
    update: (id, data) => apiClient.put(`/url/${id}`, data),
    delete: (id) => apiClient.delete(`/url/${id}`),
    stats: (shortKey) => apiClient.get(`/url/stats/${shortKey}`)
  },

  // 用户管理相关
  user: {
    list: (params) => apiClient.get('/admin/users', params, { retry: true }),
    create: (data) => apiClient.post('/admin/users', data),
    update: (id, data) => apiClient.put(`/admin/users/${id}`, data),
    delete: (id) => apiClient.delete(`/admin/users/${id}`),
    resetPassword: (id) => apiClient.post(`/admin/users/${id}/reset-password`)
  },

  // 操作日志相关
  logs: {
    list: (params) => apiClient.get('/admin/logs', params, { retry: true }),
    export: (params) => apiClient.post('/admin/logs/export', params)
  },

  // 系统相关
  system: {
    stats: () => apiClient.get('/admin/stats'),
    health: () => apiClient.get('/health')
  }
}

// 默认导出
export default api