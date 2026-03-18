import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { refreshToken as refreshTokenApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')

  const isLoggedIn = computed(() => Boolean(token.value && userInfo.value))
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const userId = computed(() => userInfo.value?.id)
  const username = computed(() => userInfo.value?.username)

  const setToken = (newToken, newRefreshToken = '') => {
    token.value = newToken || ''
    refreshToken.value = newRefreshToken || ''

    if (token.value) {
      localStorage.setItem('token', token.value)
    } else {
      localStorage.removeItem('token')
    }

    if (refreshToken.value) {
      localStorage.setItem('refreshToken', refreshToken.value)
    } else {
      localStorage.removeItem('refreshToken')
    }
  }

  const setUserInfo = (info) => {
    userInfo.value = info || null

    if (userInfo.value) {
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    } else {
      localStorage.removeItem('userInfo')
    }
  }

  const login = (loginData) => {
    setToken(loginData.token, loginData.refreshToken)
    setUserInfo(loginData.userInfo)
  }

  const logout = () => {
    setToken('', '')
    setUserInfo(null)
    sessionStorage.clear()
  }

  const updateUserInfo = (updates) => {
    if (!userInfo.value) {
      return
    }

    userInfo.value = {
      ...userInfo.value,
      ...updates
    }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  const hasRole = (role) => userInfo.value?.role === role

  const hasPermission = () => isAdmin.value

  const isTokenExpiring = () => {
    if (!token.value) {
      return true
    }

    try {
      const payload = JSON.parse(atob(token.value.split('.')[1]))
      return payload.exp * 1000 - Date.now() < 5 * 60 * 1000
    } catch {
      return true
    }
  }

  const refreshAuthToken = async () => {
    if (!refreshToken.value) {
      logout()
      return false
    }

    try {
      const response = await refreshTokenApi(refreshToken.value)
      setToken(response.data?.token, response.data?.refreshToken || refreshToken.value)
      return true
    } catch {
      logout()
      return false
    }
  }

  return {
    token,
    userInfo,
    refreshToken,
    isLoggedIn,
    isAdmin,
    userId,
    username,
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
