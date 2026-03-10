/**
 * 前端错误处理工具
 *
 * 模块职责：
 * - 统一处理HTTP请求错误
 * - 提供用户友好的错误提示
 * - 记录错误日志
 * - 支持重试机制
 */

import { message } from 'ant-design-vue'

/**
 * HTTP状态码对应的错误信息
 */
const HTTP_ERROR_MESSAGES = {
  400: '请求参数错误',
  401: '未授权，请重新登录',
  403: '拒绝访问，权限不足',
  404: '请求地址不存在',
  405: '请求方法不被允许',
  408: '请求超时',
  413: '请求数据过大',
  429: '请求过于频繁，请稍后再试',
  500: '服务器内部错误',
  502: '网关错误',
  503: '服务暂时不可用',
  504: '网关超时'
}

/**
 * 错误类型枚举
 */
export const ERROR_TYPES = {
  NETWORK: 'network',
  TIMEOUT: 'timeout',
  AUTH: 'auth',
  VALIDATION: 'validation',
  SERVER: 'server',
  UNKNOWN: 'unknown'
}

/**
 * 错误处理器类
 */
class ErrorHandler {
  constructor() {
    this.retryConfig = {
      maxRetries: 3,
      baseDelay: 1000,
      maxDelay: 5000
    }
    this.errorLog = []
  }

  /**
   * 处理HTTP错误
   */
  handleHttpError(error, showMessage = true) {
    const errorInfo = this.parseError(error)

    // 记录错误日志
    this.logError(errorInfo)

    // 显示用户友好的错误提示
    if (showMessage) {
      this.showErrorMessage(errorInfo)
    }

    return errorInfo
  }

  /**
   * 解析错误对象
   */
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

    if (!error) {
      return errorInfo
    }

    // Axios错误格式
    if (error.response) {
      // 服务器响应错误
      const { status, data, config } = error.response
      errorInfo.status = status
      errorInfo.code = data?.code || status
      errorInfo.message = data?.message || HTTP_ERROR_MESSAGES[status] || '服务器错误'
      errorInfo.details = data
      errorInfo.url = config?.url
      errorInfo.method = config?.method

      if (status === 401 || status === 403) {
        errorInfo.type = ERROR_TYPES.AUTH
      } else if (status >= 400 && status < 500) {
        errorInfo.type = ERROR_TYPES.VALIDATION
      } else if (status >= 500) {
        errorInfo.type = ERROR_TYPES.SERVER
      }
    } else if (error.request) {
      // 请求已发送但没有响应
      errorInfo.type = ERROR_TYPES.NETWORK
      errorInfo.message = '网络连接失败，请检查网络设置'
      errorInfo.url = error.request?._url
      errorInfo.method = error.request?.method
    } else if (error.code === 'ECONNABORTED') {
      // 请求超时
      errorInfo.type = ERROR_TYPES.TIMEOUT
      errorInfo.message = '请求超时，请稍后重试'
    } else if (error.message) {
      // 其他错误
      errorInfo.message = error.message
    }

    return errorInfo
  }

  /**
   * 显示错误消息
   */
  showErrorMessage(errorInfo) {
    const { type, message: msg, status } = errorInfo

    // 根据错误类型显示不同的消息级别
    switch (type) {
      case ERROR_TYPES.AUTH:
        message.error({
          content: msg,
          duration: 3,
          onClick: () => this.handleAuthError()
        })
        break
      case ERROR_TYPES.VALIDATION:
        message.warning(msg)
        break
      case ERROR_TYPES.NETWORK:
        message.error({
          content: msg,
          duration: 5,
          onClick: () => this.handleNetworkError()
        })
        break
      case ERROR_TYPES.TIMEOUT:
        message.warning(msg)
        break
      case ERROR_TYPES.SERVER:
        message.error({
          content: msg,
          duration: 4,
          onClick: () => this.handleServerError()
        })
        break
      default:
        message.error(msg)
    }
  }

  /**
   * 处理认证错误
   */
  handleAuthError() {
    // 清除本地存储的用户信息
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('userInfo')

    // 跳转到登录页
    if (window.location.pathname !== '/login') {
      window.location.href = '/login?redirect=' + encodeURIComponent(window.location.pathname)
    }
  }

  /**
   * 处理网络错误
   */
  handleNetworkError() {
    // 可以显示网络诊断信息或重试按钮
    console.log('网络连接异常，建议检查网络设置')
  }

  /**
   * 处理服务器错误
   */
  handleServerError() {
    // 可以显示联系管理员的信息
    console.log('服务器出现错误，请联系系统管理员')
  }

  /**
   * 记录错误日志
   */
  logError(errorInfo) {
    this.errorLog.push(errorInfo)

    // 限制日志数量
    if (this.errorLog.length > 100) {
      this.errorLog = this.errorLog.slice(-100)
    }

    // 在开发环境打印详细错误
    if (process.env.NODE_ENV === 'development') {
      console.error('Error logged:', errorInfo)
    }

    // TODO: 在生产环境可以将错误日志发送到服务器
    // this.sendErrorToServer(errorInfo)
  }

  /**
   * 获取错误日志
   */
  getErrorLog() {
    return [...this.errorLog]
  }

  /**
   * 清空错误日志
   */
  clearErrorLog() {
    this.errorLog = []
  }

  /**
   * 重试机制
   */
  async retry(fn, options = {}) {
    const config = { ...this.retryConfig, ...options }
    let lastError = null

    for (let attempt = 1; attempt <= config.maxRetries; attempt++) {
      try {
        return await fn()
      } catch (error) {
        lastError = error

        // 不重试某些类型的错误
        const errorInfo = this.parseError(error)
        if ([ERROR_TYPES.AUTH, ERROR_TYPES.VALIDATION].includes(errorInfo.type)) {
          throw error
        }

        // 计算延迟时间（指数退避）
        const delay = Math.min(
          config.baseDelay * Math.pow(2, attempt - 1),
          config.maxDelay
        )

        console.log(`请求失败，第${attempt}次重试，${delay}ms后重试...`)

        if (attempt < config.maxRetries) {
          await this.sleep(delay)
        }
      }
    }

    throw lastError
  }

  /**
   * 睡眠函数
   */
  sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
  }

  /**
   * 设置重试配置
   */
  setRetryConfig(config) {
    this.retryConfig = { ...this.retryConfig, ...config }
  }

  /**
   * 创建加载状态管理器
   */
  createLoadingManager() {
    return {
      loading: false,
      start: function() { this.loading = true },
      stop: function() { this.loading = false },
      wrap: async function(fn) {
        this.start()
        try {
          const result = await fn()
          return result
        } finally {
          this.stop()
        }
      }
    }
  }

  /**
   * 格式化错误信息用于显示
   */
  formatErrorMessage(errorInfo) {
    const { message, status, type } = errorInfo

    const typeLabels = {
      [ERROR_TYPES.NETWORK]: '网络错误',
      [ERROR_TYPES.TIMEOUT]: '超时错误',
      [ERROR_TYPES.AUTH]: '认证错误',
      [ERROR_TYPES.VALIDATION]: '验证错误',
      [ERROR_TYPES.SERVER]: '服务器错误',
      [ERROR_TYPES.UNKNOWN]: '未知错误'
    }

    return {
      title: typeLabels[type] || '错误',
      message: message,
      code: status ? `状态码: ${status}` : null
    }
  }
}

// 创建全局错误处理器实例
export const errorHandler = new ErrorHandler()

// 默认导出
export default errorHandler

/**
 * 错误处理装饰器
 */
export const withErrorHandling = (fn, options = {}) => {
  return async (...args) => {
    const { showMessage = true, retry = false, loading = } = options

    try {
      // 启动加载状态
      if (loading) {
        loading.start()
      }

      const result = await fn(...args)
      return result
    } catch (error) {
      // 处理错误
      const errorInfo = errorHandler.handleHttpError(error, showMessage)

      // 如果需要重试且符合条件
      if (retry) {
        try {
          console.log('正在重试请求...')
          return await errorHandler.retry(() => fn(...args))
        } catch (retryError) {
          errorHandler.handleHttpError(retryError, showMessage)
          throw retryError
        }
      }

      throw error
    } finally {
      // 停止加载状态
      if (loading) {
        loading.stop()
      }
    }
  }
}

/**
 * 网络状态检测
 */
export const checkNetworkStatus = () => {
  return {
    online: navigator.onLine,
    type: navigator.connection?.effectiveType,
    downlink: navigator.connection?.downlink,
    rtt: navigator.connection?.rtt
  }
}

/**
 * 错误边界组件（用于Vue）
 */
export const createErrorBoundary = {
  name: 'ErrorBoundary',
  data() {
    return {
      error: null,
      errorInfo: null
    }
  },
  errorCaptured(err, vm, info) {
    this.error = err
    this.errorInfo = info

    // 记录错误
    errorHandler.logError({
      type: ERROR_TYPES.UNKNOWN,
      message: err.message,
      details: { component: vm.$options.name, info },
      timestamp: new Date().toISOString()
    })

    // 阻止错误向上传播
    return false
  },
  render() {
    if (this.error) {
      return (
        <div class="error-boundary">
          <h3>组件出现错误</h3>
          <p>{this.error.message}</p>
          <button onClick={() => { this.error = null; this.errorInfo = null }}>
            重试
          </button>
        </div>
      )
    }
    return this.$slots.default
  }
}