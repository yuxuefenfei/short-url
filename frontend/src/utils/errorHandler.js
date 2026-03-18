import { message } from 'ant-design-vue'

const HTTP_ERROR_MESSAGES = {
  400: '请求参数错误',
  401: '未授权，请重新登录',
  403: '拒绝访问，权限不足',
  404: '请求地址不存在',
  405: '请求方法不被允许',
  408: '请求超时',
  413: '请求数据过大',
  429: '请求过于频繁，请稍后重试',
  500: '服务器内部错误',
  502: '网关错误',
  503: '服务暂时不可用',
  504: '网关超时'
}

export const ERROR_TYPES = {
  NETWORK: 'network',
  TIMEOUT: 'timeout',
  AUTH: 'auth',
  VALIDATION: 'validation',
  SERVER: 'server',
  UNKNOWN: 'unknown'
}

class ErrorHandler {
  constructor() {
    this.retryConfig = { maxRetries: 3, baseDelay: 1000, maxDelay: 5000 }
    this.errorLog = []
    this.lastReportAt = new Map()
    this.reportIntervalMs = 10000
  }

  handleHttpError(error, showMessage = true) {
    const errorInfo = this.parseError(error)
    this.logError(errorInfo)
    if (showMessage) this.showErrorMessage(errorInfo)
    return errorInfo
  }

  parseError(error) {
    const errorInfo = {
      type: ERROR_TYPES.UNKNOWN,
      message: '未知错误',
      code: null,
      status: null,
      details: null,
      timestamp: new Date().toISOString(),
      url: null,
      method: null
    }

    if (!error) return errorInfo

    if (error.code === 'ECONNABORTED') {
      errorInfo.type = ERROR_TYPES.TIMEOUT
      errorInfo.message = '请求超时，请稍后重试'
      return errorInfo
    }

    if (error.response) {
      const { status, data, config } = error.response
      errorInfo.status = status
      errorInfo.code = data?.code ?? status
      errorInfo.message = data?.message || HTTP_ERROR_MESSAGES[status] || '服务器错误'
      errorInfo.details = data ?? null
      errorInfo.url = config?.url || null
      errorInfo.method = config?.method || null

      if (status === 401 || status === 403) {
        errorInfo.type = ERROR_TYPES.AUTH
      } else if (status >= 400 && status < 500) {
        errorInfo.type = ERROR_TYPES.VALIDATION
      } else if (status >= 500) {
        errorInfo.type = ERROR_TYPES.SERVER
      }
      return errorInfo
    }

    if (error.request) {
      errorInfo.type = ERROR_TYPES.NETWORK
      errorInfo.message = '网络连接失败，请检查网络设置'
      errorInfo.url = error.request?._url || null
      errorInfo.method = error.request?.method || null
      return errorInfo
    }

    if (error.message) {
      errorInfo.message = error.message
    }

    return errorInfo
  }

  showErrorMessage(errorInfo) {
    const msg = errorInfo.message || '请求失败'
    switch (errorInfo.type) {
      case ERROR_TYPES.AUTH:
        message.error({ content: msg, duration: 3, onClick: () => this.handleAuthError() })
        break
      case ERROR_TYPES.VALIDATION:
        message.warning(msg)
        break
      case ERROR_TYPES.NETWORK:
        message.error(msg)
        break
      case ERROR_TYPES.TIMEOUT:
        message.warning(msg)
        break
      case ERROR_TYPES.SERVER:
        message.error(msg)
        break
      default:
        message.error(msg)
    }
  }

  handleAuthError() {
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')
    if (window.location.pathname !== '/login') {
      window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname)}`
    }
  }

  logError(errorInfo) {
    this.errorLog.push(errorInfo)
    if (this.errorLog.length > 100) {
      this.errorLog = this.errorLog.slice(-100)
    }

    if (process.env.NODE_ENV === 'development') {
      console.error('Error logged:', errorInfo)
    } else {
      this.sendErrorToServer(errorInfo)
    }
  }

  shouldReport(errorInfo) {
    const signature = `${errorInfo.type}|${errorInfo.status || ''}|${errorInfo.code || ''}|${errorInfo.url || ''}|${errorInfo.message || ''}`
    const now = Date.now()
    const last = this.lastReportAt.get(signature) || 0
    if (now - last < this.reportIntervalMs) return false
    this.lastReportAt.set(signature, now)
    return true
  }

  sendErrorToServer(errorInfo) {
    if (!this.shouldReport(errorInfo)) return

    const payload = {
      type: errorInfo.type || ERROR_TYPES.UNKNOWN,
      message: errorInfo.message || 'unknown error',
      code: errorInfo.code != null ? String(errorInfo.code) : null,
      status: errorInfo.status ?? null,
      url: errorInfo.url || null,
      method: errorInfo.method || null,
      timestamp: errorInfo.timestamp || new Date().toISOString(),
      details: errorInfo.details || null
    }

    fetch('/api/client-errors', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }).catch(() => {})
  }

  getErrorLog() {
    return [...this.errorLog]
  }

  clearErrorLog() {
    this.errorLog = []
  }

  async retry(fn, options = {}) {
    const config = { ...this.retryConfig, ...options }
    let lastError = null

    for (let attempt = 1; attempt <= config.maxRetries; attempt++) {
      try {
        return await fn()
      } catch (error) {
        lastError = error
        const errorInfo = this.parseError(error)
        if ([ERROR_TYPES.AUTH, ERROR_TYPES.VALIDATION].includes(errorInfo.type)) {
          throw error
        }

        const delay = Math.min(config.baseDelay * Math.pow(2, attempt - 1), config.maxDelay)
        if (attempt < config.maxRetries) {
          await this.sleep(delay)
        }
      }
    }

    throw lastError
  }

  sleep(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms))
  }

  setRetryConfig(config) {
    this.retryConfig = { ...this.retryConfig, ...config }
  }

  createLoadingManager() {
    return {
      loading: false,
      start() { this.loading = true },
      stop() { this.loading = false },
      async wrap(fn) {
        this.start()
        try {
          return await fn()
        } finally {
          this.stop()
        }
      }
    }
  }

  formatErrorMessage(errorInfo) {
    const labels = {
      [ERROR_TYPES.NETWORK]: '网络错误',
      [ERROR_TYPES.TIMEOUT]: '超时错误',
      [ERROR_TYPES.AUTH]: '认证错误',
      [ERROR_TYPES.VALIDATION]: '校验错误',
      [ERROR_TYPES.SERVER]: '服务错误',
      [ERROR_TYPES.UNKNOWN]: '未知错误'
    }
    return {
      title: labels[errorInfo.type] || '错误',
      message: errorInfo.message,
      code: errorInfo.status ? `状态码: ${errorInfo.status}` : null
    }
  }
}

export const errorHandler = new ErrorHandler()
export default errorHandler

export const withErrorHandling = (fn, options = {}) => {
  return async (...args) => {
    const { showMessage = true, retry = false, loading = null } = options
    try {
      if (loading) loading.start()
      return await fn(...args)
    } catch (error) {
      errorHandler.handleHttpError(error, showMessage)
      if (retry) {
        return await errorHandler.retry(() => fn(...args))
      }
      throw error
    } finally {
      if (loading) loading.stop()
    }
  }
}

export const checkNetworkStatus = () => ({
  online: navigator.onLine,
  type: navigator.connection?.effectiveType,
  downlink: navigator.connection?.downlink,
  rtt: navigator.connection?.rtt
})

export const createErrorBoundary = {
  name: 'ErrorBoundary',
  data() {
    return { error: null, errorInfo: null }
  },
  errorCaptured(err, vm, info) {
    this.error = err
    this.errorInfo = info
    errorHandler.logError({
      type: ERROR_TYPES.UNKNOWN,
      message: err?.message || 'component error',
      details: { component: vm?.$options?.name, info },
      timestamp: new Date().toISOString()
    })
    return false
  },
  render() {
    if (this.error) {
      return null
    }
    return this.$slots.default ? this.$slots.default() : null
  }
}
