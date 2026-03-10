import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 用户状态管理
 *
 * 模块职责：
 * - 管理用户登录状态
 * - 存储用户信息
 * - 处理认证相关操作
 * - 提供权限检查
 */

export const useUserStore = defineStore('user', () => {
    // 状态
    const token = ref(localStorage.getItem('token') || '')
    const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
    const refreshToken = ref(localStorage.getItem('refreshToken') || '')

    // 计算属性
    const isLoggedIn = computed(() => {debugger
        return !!token.value && !!userInfo.value
    })

    const isAdmin = computed(() => {
        return userInfo.value?.role === 'ADMIN'
    })

    const userId = computed(() => {
        return userInfo.value?.id
    })

    const username = computed(() => {
        return userInfo.value?.username
    })

    // 动作
    const setToken = (newToken, newRefreshToken = '') => {
        token.value = newToken
        refreshToken.value = newRefreshToken

        if (newToken) {
            localStorage.setItem('token', newToken)
            if (newRefreshToken) {
                localStorage.setItem('refreshToken', newRefreshToken)
            }
        } else {
            localStorage.removeItem('token')
            localStorage.removeItem('refreshToken')
        }
    }

    const setUserInfo = (info) => {
        userInfo.value = info

        if (info) {
            localStorage.setItem('userInfo', JSON.stringify(info))
        } else {
            localStorage.removeItem('userInfo')
        }
    }

    const login = (loginData) => {
        const { token: newToken, refreshToken: newRefreshToken, userInfo: info } = loginData

        setToken(newToken, newRefreshToken)
        setUserInfo(info)
    }

    const logout = () => {
        token.value = ''
        userInfo.value = null
        refreshToken.value = ''

        // 清除本地存储
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        localStorage.removeItem('refreshToken')

        // 清除其他相关存储
        sessionStorage.clear()
    }

    const updateUserInfo = (updates) => {
        if (userInfo.value) {
            userInfo.value = { ...userInfo.value, ...updates }
            localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
        }
    }

    const hasPermission = (permission) => {
        if (!isLoggedIn.value) return false
        if (isAdmin.value) return true // 管理员拥有所有权限

        // TODO: 可以实现更细粒度的权限控制
        // 基于用户角色和权限列表进行判断
        return false
    }

    const hasRole = (role) => {
        return userInfo.value?.role === role
    }

    // 检查Token是否即将过期（5分钟内）
    const isTokenExpiring = () => {
        if (!token.value) return true

        try {
            const payload = JSON.parse(atob(token.value.split('.')[1]))
            const exp = payload.exp * 1000 // 转换为毫秒
            const now = Date.now()
            const fiveMinutes = 5 * 60 * 1000

            return exp - now < fiveMinutes
        } catch (error) {
            console.error('Token解析失败:', error)
            return true
        }
    }

    // 刷新Token
    const refreshAuthToken = async () => {
        if (!refreshToken.value) {
            logout()
            return false
        }

        try {
            const response = await fetch('/api/auth/refresh-token', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${refreshToken.value}`
                }
            })

            if (response.ok) {
                const data = await response.json()
                setToken(data.token, data.refreshToken)
                return true
            } else {
                logout()
                return false
            }
        } catch (error) {
            console.error('Token刷新失败:', error)
            logout()
            return false
        }
    }

    return {
        // 状态
        token,
        userInfo,
        refreshToken,

        // 计算属性
        isLoggedIn,
        isAdmin,
        userId,
        username,

        // 动作
        setToken,
        setUserInfo,
        login,
        logout,
        updateUserInfo,
        hasPermission,
        hasRole,
        isTokenExpiring,
        refreshAuthToken
    }
})