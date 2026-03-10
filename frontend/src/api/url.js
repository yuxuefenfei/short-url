import request from '@/utils/request'

/**
 * 短网址相关API接口
 *
 * 模块职责：
 * - 封装短网址生成相关接口
 * - 处理短网址统计查询
 * - 提供统一的数据格式
 *
 * API列表：
 * - shortenUrl: 创建短网址
 * - getUrlStats: 获取统计信息
 * - getUrlList: 获取短网址列表（管理后台）
 * - deleteUrl: 删除短网址
 */

/**
 * 创建短网址
 *
 * @param {Object} data 请求数据
 * @param {string} data.originalUrl 原始URL
 * @param {string} data.title 网址标题
 * @param {string} data.expiredTime 过期时间
 * @returns {Promise} 短网址信息
 */
export const shortenUrl = (data) => {
    return request({
        url: '/shorten',
        method: 'post',
        data
    })
}

/**
 * 获取短网址统计信息
 *
 * @param {string} shortKey 短网址key
 * @returns {Promise} 统计信息
 */
export const getUrlStats = (shortKey) => {
    return request({
        url: `/stats/${shortKey}`,
        method: 'get'
    })
}

/**
 * 获取短网址列表（管理后台）
 *
 * @param {Object} params 查询参数
 * @param {number} params.page 页码
 * @param {number} params.size 每页大小
 * @param {string} params.status 状态筛选
 * @param {string} params.keyword 关键词搜索
 * @returns {Promise} 分页数据
 */
export const getUrlList = (params = {}) => {
    const {
        page = 1,
        size = 20,
        status,
        keyword
    } = params

    return request({
        url: '/admin/urls',
        method: 'get',
        params: {
            page,
            size,
            status,
            keyword
        }
    })
}

/**
 * 删除短网址
 *
 * @param {number} id 短网址ID
 * @returns {Promise} 删除结果
 */
export const deleteUrl = (id) => {
    return request({
        url: `/admin/urls/${id}`,
        method: 'delete'
    })
}

/**
 * 更新短网址状态
 *
 * @param {number} id 短网址ID
 * @param {number} status 新状态
 * @returns {Promise} 更新结果
 */
export const updateUrlStatus = (id, status) => {
    return request({
        url: `/admin/urls/${id}/status`,
        method: 'put',
        data: { status }
    })
}

/**
 * 批量操作短网址
 *
 * @param {Object} data 批量操作数据
 * @param {Array} data.ids 短网址ID数组
 * @param {string} data.action 操作类型（delete/batchDisable/batchEnable）
 * @returns {Promise} 操作结果
 */
export const batchOperation = (data) => {
    return request({
        url: '/admin/urls/batch',
        method: 'post',
        data
    })
}

/**
 * 导出短网址数据
 *
 * @param {Object} params 导出参数
 * @param {string} params.format 导出格式（csv/excel/json）
 * @param {string} params.dateRange 日期范围
 * @returns {Promise} 导出文件
 */
export const exportUrlData = (params = {}) => {
    return request({
        url: '/admin/urls/export',
        method: 'get',
        params,
        responseType: 'blob'
    })
}

/**
 * 获取短网址详细信息
 *
 * @param {number} id 短网址ID
 * @returns {Promise} 详细信息
 */
export const getUrlDetail = (id) => {
    return request({
        url: `/admin/urls/${id}`,
        method: 'get'
    })
}

/**
 * 更新短网址信息
 *
 * @param {number} id 短网址ID
 * @param {Object} data 更新数据
 * @returns {Promise} 更新结果
 */
export const updateUrl = (id, data) => {
    return request({
        url: `/admin/urls/${id}`,
        method: 'put',
        data
    })
}