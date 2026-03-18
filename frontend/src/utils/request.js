import axios from 'axios'
import { message, notification } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    const token = userStore.token

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: Date.now()
      }
    }

    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response.data
    }

    const payload = response.data
    if (payload && typeof payload === 'object' && 'code' in payload && 'message' in payload) {
      if (payload.code === 200) {
        return payload
      }

      const businessError = new Error(payload.message || '操作失败')
      businessError.response = {
        status: response.status,
        data: payload,
        config: response.config
      }

      if (response.config.showError !== false) {
        message.error(payload.message || '操作失败')
      }

      return Promise.reject(businessError)
    }

    return payload
  },
  (error) => {
    const userStore = useUserStore()

    if (!error.response) {
      notification.error({
        message: '网络错误',
        description: '请检查网络连接后重试',
        duration: 3
      })
      return Promise.reject(error)
    }

    const { status, data } = error.response
    const errorMessage = data?.message || error.message || '请求失败'

    if (status === 401) {
      userStore.logout()
      message.error('登录已失效，请重新登录')
      setTimeout(() => {
        window.location.href = '/login'
      }, 1200)
    } else if (status === 403) {
      notification.error({
        message: '权限不足',
        description: errorMessage,
        duration: 3
      })
    } else if (status === 404) {
      message.error(errorMessage || '请求资源不存在')
    } else if (status === 429) {
      message.warning(errorMessage || '请求过于频繁，请稍后重试')
    } else if (status >= 500) {
      notification.error({
        message: '服务异常',
        description: errorMessage,
        duration: 3
      })
    } else {
      message.error(errorMessage)
    }

    return Promise.reject(error)
  }
)

export const get = (url, params = {}, config = {}) => request({ method: 'get', url, params, ...config })
export const post = (url, data = {}, config = {}) => request({ method: 'post', url, data, ...config })
export const put = (url, data = {}, config = {}) => request({ method: 'put', url, data, ...config })
export const del = (url, config = {}) => request({ method: 'delete', url, ...config })

export default request
