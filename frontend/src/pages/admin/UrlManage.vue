<template>
  <div class="url-manage">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">短网址管理</h1>
      <p class="page-subtitle">管理和监控所有短网址</p>
    </div>

    <!-- 搜索和过滤 -->
    <a-card class="filter-card">
      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :sm="8" :md="6">
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索短网址或原始URL"
            enter-button="搜索"
            @search="handleSearch"
            allow-clear
          />
        </a-col>
        <a-col :xs="12" :sm="6" :md="4">
          <a-select
            v-model:value="statusFilter"
            placeholder="状态筛选"
            style="width: 100%"
            @change="handleFilter"
            allow-clear
          >
            <a-select-option value="1">正常</a-select-option>
            <a-select-option value="0">已禁用</a-select-option>
          </a-select>
        </a-col>
        <a-col :xs="12" :sm="6" :md="4">
          <a-range-picker
            v-model:value="dateRange"
            style="width: 100%"
            @change="handleDateFilter"
            :placeholder="['开始日期', '结束日期']"
          />
        </a-col>
        <a-col :xs="24" :sm="4" :md="10" class="action-buttons">
          <a-button type="primary" @click="showCreateModal = true">
            <PlusOutlined />
            创建短网址
          </a-button>
          <a-button @click="handleExport" style="margin-left: 8px;">
            <ExportOutlined />
            导出数据
          </a-button>
          <a-button @click="handleRefresh" style="margin-left: 8px;">
            <ReloadOutlined />
            刷新
          </a-button>
        </a-col>
      </a-row>
    </a-card>

    <!-- 数据表格 -->
    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="urlList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        :scroll="{ x: 1200 }"
      >
        <!-- 短网址列 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'shortKey'">
            <div class="short-url-cell">
              <div class="short-key">
                <a :href="record.shortUrl" target="_blank" @click.prevent="copyUrl(record.shortUrl)">
                  {{ record.shortKey }}
                </a>
              </div>
              <div class="copy-btn">
                <a-button
                  type="text"
                  size="small"
                  @click="copyUrl(record.shortUrl)"
                >
                  <CopyOutlined />
                </a-button>
              </div>
            </div>
          </template>

          <!-- 原始URL列 -->
          <template v-else-if="column.key === 'originalUrl'">
            <div class="original-url-cell">
              <div class="url-text" :title="record.originalUrl">
                {{ record.originalUrl }}
              </div>
              <div class="url-title" v-if="record.title">
                {{ record.title }}
              </div>
            </div>
          </template>

          <!-- 点击数列 -->
          <template v-else-if="column.key === 'clickCount'">
            <div class="click-count-cell">
              <div class="count">{{ record.clickCount }}</div>
              <a @click="viewStats(record)" class="view-stats">查看统计</a>
            </div>
          </template>

          <!-- 状态列 -->
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'success' : 'error'">
              {{ record.status === 1 ? '正常' : '禁用' }}
            </a-tag>
          </template>

          <!-- 创建日期列 -->
          <template v-else-if="column.key === 'createdTime'">
            <div class="time-cell">
              <div>{{ formatDateTime(record.createdTime) }}</div>
              <div class="time-ago">{{ formatTimeAgo(record.createdTime) }}</div>
            </div>
          </template>

          <!-- 操作列 -->
          <template v-else-if="column.key === 'action'">
            <div class="action-cell">
              <a-space>
                <a @click="viewDetails(record)">详情</a>
                <a-divider type="vertical" />
                <a @click="toggleStatus(record)">
                  {{ record.status === 1 ? '禁用' : '启用' }}
                </a>
                <a-divider type="vertical" />
                <a @click="deleteUrl(record)" style="color: #ff4d4f;">删除</a>
              </a-space>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 创建短网址模态框 -->
    <a-modal
      v-model:visible="showCreateModal"
      title="创建短网址"
      @ok="handleCreateUrl"
      @cancel="handleCancelCreate"
      :confirm-loading="creating"
    >
      <a-form
        :model="createForm"
        :rules="createRules"
        ref="createFormRef"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 20 }"
      >
        <a-form-item label="原始URL" name="originalUrl" required>
          <a-input
            v-model:value="createForm.originalUrl"
            placeholder="请输入完整的URL，如：https://example.com"
          />
        </a-form-item>
        <a-form-item label="标题" name="title">
          <a-input
            v-model:value="createForm.title"
            placeholder="为短网址添加描述性标题（可选）"
          />
        </a-form-item>
        <a-form-item label="过期时间" name="expiredAt">
          <a-date-picker
            v-model:value="createForm.expiredAt"
            show-time
            style="width: 100%"
            placeholder="设置过期时间（可选）"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- URL详情模态框 -->
    <a-modal
      v-model:visible="showDetailModal"
      title="短网址详情"
      :footer="null"
      width="600px"
    >
      <div v-if="selectedUrl" class="detail-content">
        <a-descriptions bordered :column="1">
          <a-descriptions-item label="短网址Key">
            {{ selectedUrl.shortKey }}
          </a-descriptions-item>
          <a-descriptions-item label="完整短网址">
            <a :href="selectedUrl.shortUrl" target="_blank">
              {{ selectedUrl.shortUrl }}
            </a>
          </a-descriptions-item>
          <a-descriptions-item label="原始URL">
            <div class="original-url-detail">
              {{ selectedUrl.originalUrl }}
            </div>
          </a-descriptions-item>
          <a-descriptions-item label="标题">
            {{ selectedUrl.title || '无标题' }}
          </a-descriptions-item>
          <a-descriptions-item label="点击次数">
            {{ selectedUrl.clickCount }}
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="selectedUrl.status === 1 ? 'success' : 'error'">
              {{ selectedUrl.status === 1 ? '正常' : '禁用' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ formatDateTime(selectedUrl.createdTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="过期时间">
            {{ selectedUrl.expiredTime ? formatDateTime(selectedUrl.expiredTime) : '永不过期' }}
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ExportOutlined,
  ReloadOutlined,
  CopyOutlined
} from '@ant-design/icons-vue'
import { getUrlList, updateUrlStatus, deleteUrl as deleteUrlApi } from '@/api/admin'
import { shortenUrl } from '@/api/url'
import { urlRules, titleRules, handleFormError } from '@/utils/validators'

const router = useRouter()

// 搜索和过滤
const searchKeyword = ref('')
const statusFilter = ref(undefined)
const dateRange = ref([])

// 表格数据
const urlList = ref([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条/共 ${total} 条`
})

// 表格列定义
const columns = [
  {
    title: '短网址',
    dataIndex: 'shortKey',
    key: 'shortKey',
    width: 150
  },
  {
    title: '原始URL',
    dataIndex: 'originalUrl',
    key: 'originalUrl',
    ellipsis: true
  },
  {
    title: '点击次数',
    dataIndex: 'clickCount',
    key: 'clickCount',
    width: 120,
    sorter: true
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100
  },
  {
    title: '创建时间',
    dataIndex: 'createdTime',
    key: 'createdTime',
    width: 180,
    sorter: true
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    fixed: 'right'
  }
]

// 创建短网址表单
const showCreateModal = ref(false)
const creating = ref(false)
const createFormRef = ref()
const createForm = reactive({
  originalUrl: '',
  title: '',
  expiredAt: null
})

const createRules = {
  originalUrl: urlRules,
  title: titleRules
}

// 详情模态框
const showDetailModal = ref(false)
const selectedUrl = ref(null)

/**
 * 加载短网址列表
 */
const loadUrlList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.current,
      size: pagination.pageSize,
      keyword: searchKeyword.value,
      status: statusFilter.value
    }

    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    const response = await getUrlList(params)
    if (response.code === 200) {
      urlList.value = response.data.list
      pagination.total = response.data.total
    }
  } catch (error) {
    console.error('加载短网址列表失败:', error)
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

/**
 * 处理搜索
 */
const handleSearch = () => {
  pagination.current = 1
  loadUrlList()
}

/**
 * 处理状态筛选
 */
const handleFilter = () => {
  pagination.current = 1
  loadUrlList()
}

/**
 * 处理日期筛选
 */
const handleDateFilter = () => {
  pagination.current = 1
  loadUrlList()
}

/**
 * 处理表格变化
 */
const handleTableChange = (pag, filters, sorter) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadUrlList()
}

/**
 * 处理刷新
 */
const handleRefresh = () => {
  searchKeyword.value = ''
  statusFilter.value = undefined
  dateRange.value = []
  pagination.current = 1
  loadUrlList()
}

/**
 * 复制URL
 */
const copyUrl = async (url) => {
  try {
    await navigator.clipboard.writeText(url)
    message.success('链接已复制到剪贴板')
  } catch (error) {
    message.error('复制失败，请手动复制')
  }
}

/**
 * 创建短网址
 */
const handleCreateUrl = async () => {
  try {
    await createFormRef.value.validate()
    creating.value = true

    const params = {
      originalUrl: createForm.originalUrl,
      title: createForm.title,
      expiredAt: createForm.expiredAt
    }

    const response = await shortenUrl(params)
    if (response.code === 200) {
      message.success('短网址创建成功')
      showCreateModal.value = false
      createFormRef.value.resetFields()
      loadUrlList()
    }
  } catch (error) {
    if (error.errorFields) {
      const errorMessage = handleFormError(error)
      message.warning(errorMessage)
      return
    }
    console.error('创建短网址失败:', error)
    message.error('创建失败: ' + (error.message || '未知错误'))
  } finally {
    creating.value = false
  }
}

/**
 * 取消创建
 */
const handleCancelCreate = () => {
  showCreateModal.value = false
  createFormRef.value?.resetFields()
}

/**
 * 查看统计
 */
const viewStats = (record) => {
  router.push(`/stats?key=${record.shortKey}`)
}

/**
 * 查看详情
 */
const viewDetails = (record) => {
  selectedUrl.value = record
  showDetailModal.value = true
}

/**
 * 切换状态
 */
const toggleStatus = (record) => {
  const newStatus = record.status === 1 ? 0 : 1
  const actionText = newStatus === 1 ? '启用' : '禁用'

  Modal.confirm({
    title: `确认${actionText}`,
    content: `确定要${actionText}短网址 ${record.shortKey} 吗？`,
    onOk: async () => {
      try {
        const response = await updateUrlStatus(record.id, newStatus)
        if (response.code === 200) {
          message.success(`${actionText}成功`)
          loadUrlList()
        }
      } catch (error) {
        console.error(`${actionText}失败:`, error)
        message.error(`${actionText}失败`)
      }
    }
  })
}

/**
 * 删除短网址
 */
const deleteUrl = (record) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除短网址 ${record.shortKey} 吗？此操作不可恢复。`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      try {
        const response = await deleteUrlApi(record.id)
        if (response.code === 200) {
          message.success('删除成功')
          loadUrlList()
        }
      } catch (error) {
        console.error('删除失败:', error)
        message.error('删除失败')
      }
    }
  })
}

/**
 * 导出数据
 */
const handleExport = async () => {
  try {
    message.loading('正在导出数据...', 0)

    // 构建导出参数
    const params = {
      keyword: searchKeyword.value,
      status: statusFilter.value,
      exportAll: true // 标记为导出全部数据
    }

    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    // 调用导出API
    const response = await getUrlList(params)

    if (response.code === 200 && response.data.list) {
      exportToCSV(response.data.list, '短网址数据')
      message.success('数据导出成功')
    } else {
      message.error('导出失败：未获取到数据')
    }
  } catch (error) {
    console.error('导出数据失败:', error)
    message.error('导出失败，请稍后重试')
  }
}

/**
 * 导出CSV文件
 */
const exportToCSV = (data, filename) => {
  if (!data || data.length === 0) {
    message.warning('没有数据可导出')
    return
  }

  // CSV表头
  const headers = [
    '短网址Key',
    '完整短网址',
    '原始URL',
    '标题',
    '点击次数',
    '状态',
    '创建时间',
    '过期时间'
  ]

  // 数据转换
  const csvData = data.map(item => [
    item.shortKey || '',
    item.shortUrl || '',
    item.originalUrl || '',
    item.title || '',
    item.clickCount || 0,
    item.status === 1 ? '正常' : '禁用',
    formatDateTime(item.createdTime),
    item.expiredTime ? formatDateTime(item.expiredTime) : '永不过期'
  ])

  // 构建CSV内容
  const csvContent = [
    headers.join(','),
    ...csvData.map(row => row.map(field => `"${String(field).replace(/"/g, '""')}"`).join(','))
  ].join('\n')

  // 添加BOM以支持中文
  const BOM = '\uFEFF'
  const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })

  // 创建下载链接
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  link.setAttribute('download', `${filename}_${new Date().toISOString().slice(0, 10)}.csv`)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  // 清理URL对象
  URL.revokeObjectURL(url)
}

/**
 * 格式化日期时间
 */
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return new Date(dateTime).toLocaleString()
}

/**
 * 格式化相对时间
 */
const formatTimeAgo = (dateTime) => {
  if (!dateTime) return ''
  const now = new Date()
  const diff = now - new Date(dateTime)
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (days > 0) return `${days}天前`
  if (hours > 0) return `${hours}小时前`
  if (minutes > 0) return `${minutes}分钟前`
  return '刚刚'
}

/**
 * 生命周期钩子
 */
</script>

<style scoped>
.url-manage {
  min-height: 100%;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 600;
  color: #262626;
}

.page-subtitle {
  margin: 0;
  color: #8c8c8c;
  font-size: 16px;
}

.filter-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.table-card {
  border-radius: 8px;
}

.short-url-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.short-key {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-weight: 500;
}

.copy-btn {
  opacity: 0;
  transition: opacity 0.3s;
}

.short-url-cell:hover .copy-btn {
  opacity: 1;
}

.original-url-cell {
  max-width: 300px;
}

.url-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #595959;
}

.url-title {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}

.click-count-cell {
  text-align: center;
}

.count {
  font-size: 16px;
  font-weight: 600;
  color: #1890ff;
}

.view-stats {
  font-size: 12px;
  margin-top: 4px;
}

.time-cell {
  line-height: 1.4;
}

.time-ago {
  font-size: 12px;
  color: #8c8c8c;
}

.action-cell {
  white-space: nowrap;
}

.detail-content {
  padding: 16px 0;
}

.original-url-detail {
  word-break: break-all;
  max-height: 100px;
  overflow-y: auto;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .action-buttons {
    margin-top: 16px;
    justify-content: flex-start;
  }

  .original-url-cell {
    max-width: 200px;
  }
}
</style>
