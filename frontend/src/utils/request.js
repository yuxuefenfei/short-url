import axios from 'axios'
import { message, notification } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'

/**
 * HTTP请求封装
 *
 * 模块职责：
 * - 统一处理HTTP请求
 * - 自动添加认证Token
 * - 统一错误处理
 * - 请求和响应拦截
 *
 * 特性：
 * - 自动Token管理
 * - 统一的错误提示
 * - 请求取消支持
 * - 响应数据标准化
 */

// 创建axios实例
const request = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/',
    timeout: 10000, // 10秒超时
    headers: {
        'Content-Type': 'application/json'
    }
})

// 请求拦截器
request.interceptors.request.use(
    (config) => {
        // 添加认证Token
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
        if (import.meta.env.DEV) {
            console.log('请求发送:', {
                url: config.url,
                method: config.method,
                data: config.data,
                params: config.params
            })
        }

        return config
    },
    (error) => {
        console.error('请求配置错误:', error)
        return Promise.reject(error)
    }
)

// 响应拦截器
request.interceptors.response.use(
    (response) => {
        const { data, config } = response

        // 开发环境日志
        if (import.meta.env.DEV) {
            console.log('响应接收:', {
                url: config.url,
                status: response.status,
                data: data
            })
        }

        // 检查响应格式
        if (data && typeof data === 'object') {
            // 统一响应格式处理
            if (data.code === 200) {
                return data.data !== undefined ? data.data : data
            } else {
                // 业务错误处理
                const errorMessage = data.message || '操作失败'

                if (config.showError !== false) {
                    message.error(errorMessage)
                }

                return Promise.reject(new Error(errorMessage))
            }
        }

        return response.data
    },
    (error) => {
        const userStore = useUserStore()

        // 处理网络错误
        if (!error.response) {
            notification.error({
                message: '网络错误',
                description: '请检查网络连接后重试',
                duration: 3
            })
            return Promise.reject(new Error('网络连接失败'))
        }

        const { status, data } = error.response
        const errorMessage = data?.message || error.message

        // 根据状态码处理不同错误
        switch (status) {
            case 401:
                // 未授权，清除用户信息并跳转到登录页
                userStore.logout()
                message.error('登录已过期，请重新登录')

                // 延迟跳转到登录页
                setTimeout(() => {
                    window.location.href = '/login'
                }, 1500)
                break

            case 403:
                notification.error({
                    message: '权限不足',
                    description: '您没有权限执行此操作',
                    duration: 3
                })
                break

            case 404:
                message.error('请求的资源不存在')
                break

            case 429:
                message.warning('请求过于频繁，请稍后重试')
                break

            case 500:
                notification.error({
                    message: '服务器错误',
                    description: '服务器内部错误，请稍后重试',
                    duration: 3
                })
                break

            case 502:
            case 503:
            case 504:
                notification.warning({
                    message: '服务暂时不可用',
                    description: '服务器维护中，请稍后重试',
                    duration: 3
                })
                break

            default:
                if (status >= 400 && status < 500) {
                    message.error(errorMessage || '请求参数错误')
                } else if (status >= 500) {
                    message.error('服务器错误，请稍后重试')
                } else {
                    message.error(errorMessage || '请求失败')
                }
        }

        // 开发环境输出详细错误信息
        if (import.meta.env.DEV) {
            console.error('请求错误详情:', {
                status,
                data,
                message: error.message,
                stack: error.stack
            })
        }

        return Promise.reject(error)
    }
)

// 请求取消令牌
let cancelTokenSource = null

// 取消上一个请求
export const cancelRequest = () => {
    if (cancelTokenSource) {
        cancelTokenSource.cancel('操作被取消')
    }
}

// 创建新的取消令牌
export const createCancelToken = () => {
    cancelTokenSource = axios.CancelToken.source()
    return cancelTokenSource.token
}

// GET请求
export const get = (url, params = {}, config = {}) => {
    return request({
        method: 'get',
        url,
        params,
        ...config
    })
}

// POST请求
export const post = (url, data = {}, config = {}) => {
    return request({
        method: 'post',
        url,
        data,
        ...config
    })
}

// PUT请求
export const put = (url, data = {}, config = {}) => {
    return request({
        method: 'put',
        url,
        data,
        ...config
    })
}

// DELETE请求
export const del = (url, config = {}) => {
    return request({
        method: 'delete',
        url,
        ...config
    })
}

// 文件上传
export const upload = (url, file, config = {}) => {
    const formData = new FormData()
    formData.append('file', file)

    return request({
        method: 'post',
        url,
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data'
        },
        ...config
    })
}

// 下载文件
export const download = (url, params = {}, filename) => {
    return request({
        method: 'get',
        url,
        params,
        responseType: 'blob'
    }).then(response => {
        const blob = new Blob([response])
        const downloadUrl = window.URL.createObjectURL(blob)

        const link = document.createElement('a')
        link.href = downloadUrl
        link.download = filename || `download-${Date.now()}`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)

        window.URL.revokeObjectURL(downloadUrl)
    })
}

// 批量请求
export const batch = (requests) => {
    return Promise.all(requests.map(req => request(req)))
}

// 并发请求限制
export const concurrent = async (requests, limit = 5) => {
    const results = []
    const executing = []

    for (const requestConfig of requests) {
        const promise = Promise.resolve().then(() => request(requestConfig))
        results.push(promise)

        const executingPromise = promise.then(() => {
            // 从执行队列中移除已完成的请求
            const index = executing.indexOf(executingPromise)
            if (index !== -1) {
                executing.splice(index, 1)
            }
        })

        executing.push(executingPromise)

        // 如果并发数达到限制，等待一个请求完成
        if (executing.length >= limit) {
            await Promise.race(executing)
        }
    }

    return Promise.all(results)
}

export default request