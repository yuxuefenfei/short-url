import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * 短网址状态管理
 *
 * 模块职责：
 * - 管理短网址相关状态
 * - 处理短网址生成和查询
 * - 缓存短网址数据
 * - 管理访问统计
 */

export const useUrlStore = defineStore('url', () => {
    // 状态
    const recentUrls = ref(JSON.parse(localStorage.getItem('recentUrls') || '[]'))
    const urlStats = ref(new Map()) // 使用Map存储URL统计数据
    const isLoading = ref(false)
    const error = ref(null)

    // 计算属性
    const recentUrlsCount = computed(() => recentUrls.value.length)

    const hasRecentUrls = computed(() => recentUrls.value.length > 0)

    // 动作
    const addRecentUrl = (urlData) => {
        // 移除已存在的相同URL
        const filteredUrls = recentUrls.value.filter(url => url.shortKey !== urlData.shortKey)

        // 添加到列表开头
        const newUrls = [urlData, ...filteredUrls]

        // 限制数量（最多保存20个）
        const limitedUrls = newUrls.slice(0, 20)

        recentUrls.value = limitedUrls
        localStorage.setItem('recentUrls', JSON.stringify(limitedUrls))
    }

    const removeRecentUrl = (shortKey) => {
        const filteredUrls = recentUrls.value.filter(url => url.shortKey !== shortKey)
        recentUrls.value = filteredUrls
        localStorage.setItem('recentUrls', JSON.stringify(filteredUrls))
    }

    const clearRecentUrls = () => {
        recentUrls.value = []
        localStorage.removeItem('recentUrls')
    }

    const setUrlStats = (shortKey, stats) => {
        urlStats.value.set(shortKey, {
            ...stats,
            lastUpdated: new Date().toISOString()
        })
    }

    const getUrlStats = (shortKey) => {
        return urlStats.value.get(shortKey)
    }

    const removeUrlStats = (shortKey) => {
        urlStats.value.delete(shortKey)
    }

    const clearUrlStats = () => {
        urlStats.value.clear()
    }

    const setLoading = (loading) => {
        isLoading.value = loading
    }

    const setError = (errorMessage) => {
        error.value = errorMessage
    }

    const clearError = () => {
        error.value = null
    }

    // 检查统计数据是否需要刷新（超过1小时）
    const isStatsStale = (shortKey) => {
        const stats = urlStats.value.get(shortKey)
        if (!stats || !stats.lastUpdated) return true

        const lastUpdated = new Date(stats.lastUpdated)
        const oneHourAgo = new Date(Date.now() - 60 * 60 * 1000)

        return lastUpdated < oneHourAgo
    }

    // 批量添加短网址到最近使用列表
    const addMultipleUrls = (urls) => {
        let updatedUrls = [...recentUrls.value]

        urls.forEach(urlData => {
            // 移除重复项
            updatedUrls = updatedUrls.filter(url => url.shortKey !== urlData.shortKey)
            // 添加到开头
            updatedUrls.unshift(urlData)
        })

        // 限制数量
        updatedUrls = updatedUrls.slice(0, 20)

        recentUrls.value = updatedUrls
        localStorage.setItem('recentUrls', JSON.stringify(updatedUrls))
    }

    // 搜索最近使用的短网址
    const searchRecentUrls = (query) => {
        if (!query) return recentUrls.value

        const lowercaseQuery = query.toLowerCase()
        return recentUrls.value.filter(url =>
            url.shortKey.toLowerCase().includes(lowercaseQuery) ||
            url.originalUrl.toLowerCase().includes(lowercaseQuery) ||
            (url.title && url.title.toLowerCase().includes(lowercaseQuery))
        )
    }

    // 导出短网址数据
    const exportUrlData = () => {
        const data = {
            recentUrls: recentUrls.value,
            urlStats: Array.from(urlStats.value.entries()),
            exportTime: new Date().toISOString()
        }

        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
        const url = URL.createObjectURL(blob)

        const link = document.createElement('a')
        link.href = url
        link.download = `short-urls-${new Date().toISOString().split('T')[0]}.json`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)

        URL.revokeObjectURL(url)
    }

    // 导入短网址数据
    const importUrlData = (data) => {
        try {
            if (data.recentUrls) {
                recentUrls.value = data.recentUrls.slice(0, 20)
                localStorage.setItem('recentUrls', JSON.stringify(recentUrls.value))
            }

            if (data.urlStats) {
                urlStats.value.clear()
                data.urlStats.forEach(([shortKey, stats]) => {
                    urlStats.value.set(shortKey, stats)
                })
            }

            return true
        } catch (error) {
            console.error('导入数据失败:', error)
            return false
        }
    }

    return {
        // 状态
        recentUrls,
        urlStats,
        isLoading,
        error,

        // 计算属性
        recentUrlsCount,
        hasRecentUrls,

        // 动作
        addRecentUrl,
        removeRecentUrl,
        clearRecentUrls,
        setUrlStats,
        getUrlStats,
        removeUrlStats,
        clearUrlStats,
        setLoading,
        setError,
        clearError,
        isStatsStale,
        addMultipleUrls,
        searchRecentUrls,
        exportUrlData,
        importUrlData
    }
})
