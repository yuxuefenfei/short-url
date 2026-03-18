<template>
  <div class="operation-logs">
    <div class="page-header">
      <h1 class="page-title">操作日志</h1>
      <p class="page-subtitle">查看系统操作记录与审计信息</p>
    </div>

    <a-card class="filter-card">
      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :sm="8" :md="5">
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索用户、描述或 IP"
            enter-button="搜索"
            allow-clear
            @search="handleSearch"
          />
        </a-col>
        <a-col :xs="12" :sm="8" :md="4">
          <a-select
            v-model:value="moduleFilter"
            placeholder="模块筛选"
            style="width: 100%"
            allow-clear
            @change="handleFilter"
          >
            <a-select-option value="USER_MANAGEMENT">用户管理</a-select-option>
            <a-select-option value="URL_MANAGEMENT">短链管理</a-select-option>
            <a-select-option value="SYSTEM_MONITOR">系统监控</a-select-option>
            <a-select-option value="AUTH">认证授权</a-select-option>
          </a-select>
        </a-col>
        <a-col :xs="12" :sm="8" :md="4">
          <a-select
            v-model:value="operationFilter"
            placeholder="操作类型"
            style="width: 100%"
            allow-clear
            @change="handleFilter"
          >
            <a-select-option value="CREATE">创建</a-select-option>
            <a-select-option value="UPDATE">更新</a-select-option>
            <a-select-option value="DELETE">删除</a-select-option>
            <a-select-option value="QUERY">查询</a-select-option>
            <a-select-option value="LOGIN">登录</a-select-option>
            <a-select-option value="LOGOUT">登出</a-select-option>
          </a-select>
        </a-col>
        <a-col :xs="12" :sm="8" :md="3">
          <a-select
            v-model:value="statusFilter"
            placeholder="状态"
            style="width: 100%"
            allow-clear
            @change="handleFilter"
          >
            <a-select-option :value="1">成功</a-select-option>
            <a-select-option :value="0">失败</a-select-option>
          </a-select>
        </a-col>
        <a-col :xs="24" :sm="16" :md="5">
          <a-range-picker
            v-model:value="dateRange"
            style="width: 100%"
            :show-time="true"
            @change="handleDateFilter"
          />
        </a-col>
        <a-col :xs="24" :sm="24" :md="3" class="action-buttons">
          <a-space>
            <a-button @click="handleExport">
              <ExportOutlined />
              导出
            </a-button>
            <a-button @click="handleRefresh">
              <ReloadOutlined />
              刷新
            </a-button>
          </a-space>
        </a-col>
      </a-row>
    </a-card>

    <a-row :gutter="[16, 16]" class="stats-row">
      <a-col :xs="24" :sm="12" :md="4">
        <a-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon total-ops">
              <FileTextOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalOperations }}</div>
              <div class="stat-label">总操作数</div>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="4">
        <a-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon success-ops">
              <CheckCircleOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.successOperations }}</div>
              <div class="stat-label">成功操作</div>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="4">
        <a-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon failed-ops">
              <CloseCircleOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.failedOperations }}</div>
              <div class="stat-label">失败操作</div>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="4">
        <a-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon active-users">
              <UserOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.activeUsers }}</div>
              <div class="stat-label">活跃用户</div>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="4">
        <a-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon today-ops">
              <ClockCircleOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayOperations }}</div>
              <div class="stat-label">今日操作</div>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="logList"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 1400 }"
        :row-key="(record) => record.id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'username'">
            <div class="user-cell">
              <a-avatar :style="{ backgroundColor: getAvatarColor(record.username || '?') }" size="small">
                {{ (record.username || '?').charAt(0).toUpperCase() }}
              </a-avatar>
              <span class="username">{{ record.username || `用户#${record.userId || '-'}` }}</span>
            </div>
          </template>

          <template v-else-if="column.key === 'operationType'">
            <a-tag :color="getOperationTypeColor(record.operationType)">
              {{ getOperationTypeLabel(record.operationType) }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'module'">
            <a-tag :color="getModuleColor(record.module)">
              {{ getModuleLabel(record.module) }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'success' : 'error'">
              {{ record.status === 1 ? '成功' : '失败' }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'ipAddress'">
            <div class="ip-cell">
              <span class="ip">{{ record.ipAddress || '-' }}</span>
              <a-tooltip v-if="record.ipAddress" :title="`地区标签：${getIpLocation(record.ipAddress)}`">
                <EnvironmentOutlined class="location-icon" />
              </a-tooltip>
            </div>
          </template>

          <template v-else-if="column.key === 'operationTime'">
            <div class="time-cell">
              <div>{{ formatDateTime(record.operationTime) }}</div>
              <div class="time-ago">{{ formatTimeAgo(record.operationTime) }}</div>
            </div>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="viewDetails(record)">详情</a>
              <a-divider type="vertical" />
              <a @click="viewUserLogs(record)">用户日志</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="showDetailModal"
      title="操作日志详情"
      :footer="null"
      width="720px"
    >
      <div v-if="selectedLog" class="detail-content">
        <a-descriptions bordered :column="1">
          <a-descriptions-item label="日志 ID">{{ selectedLog.id }}</a-descriptions-item>
          <a-descriptions-item label="用户 ID">{{ selectedLog.userId || '-' }}</a-descriptions-item>
          <a-descriptions-item label="用户名">{{ selectedLog.username || '-' }}</a-descriptions-item>
          <a-descriptions-item label="操作类型">
            <a-tag :color="getOperationTypeColor(selectedLog.operationType)">
              {{ getOperationTypeLabel(selectedLog.operationType) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="模块">
            <a-tag :color="getModuleColor(selectedLog.module)">
              {{ getModuleLabel(selectedLog.module) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="操作描述">{{ selectedLog.operationDesc || '-' }}</a-descriptions-item>
          <a-descriptions-item label="IP 地址">
            {{ selectedLog.ipAddress || '-' }} ({{ getIpLocation(selectedLog.ipAddress) }})
          </a-descriptions-item>
          <a-descriptions-item label="User Agent">
            <div class="detail-block">{{ selectedLog.userAgent || '-' }}</div>
          </a-descriptions-item>
          <a-descriptions-item label="请求路径">{{ selectedLog.requestPath || '-' }}</a-descriptions-item>
          <a-descriptions-item label="请求方法">{{ selectedLog.requestMethod || '-' }}</a-descriptions-item>
          <a-descriptions-item label="请求参数">
            <div class="detail-block">{{ selectedLog.requestParams || '-' }}</div>
          </a-descriptions-item>
          <a-descriptions-item label="响应结果">
            <div class="detail-block">{{ selectedLog.responseResult || '-' }}</div>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="selectedLog.status === 1 ? 'success' : 'error'">
              {{ selectedLog.status === 1 ? '成功' : '失败' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item v-if="selectedLog.errorMessage" label="错误信息">
            <div class="detail-block error-message">{{ selectedLog.errorMessage }}</div>
          </a-descriptions-item>
          <a-descriptions-item label="操作时间">{{ formatDateTime(selectedLog.operationTime) }}</a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  EnvironmentOutlined,
  ExportOutlined,
  FileTextOutlined,
  ReloadOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import { getOperationLogs, getOperationLogStats } from '@/api/admin'

const searchKeyword = ref('')
const moduleFilter = ref(undefined)
const operationFilter = ref(undefined)
const statusFilter = ref(undefined)
const dateRange = ref([])

const stats = reactive({
  totalOperations: 0,
  successOperations: 0,
  failedOperations: 0,
  activeUsers: 0,
  todayOperations: 0
})

const logList = ref([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条，共 ${total} 条`
})

const showDetailModal = ref(false)
const selectedLog = ref(null)

const columns = [
  { title: '用户', dataIndex: 'username', key: 'username', width: 140 },
  { title: '操作类型', dataIndex: 'operationType', key: 'operationType', width: 110 },
  { title: '模块', dataIndex: 'module', key: 'module', width: 130 },
  { title: '操作描述', dataIndex: 'operationDesc', key: 'operationDesc', ellipsis: true, width: 220 },
  { title: 'IP 地址', dataIndex: 'ipAddress', key: 'ipAddress', width: 160 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '操作时间', dataIndex: 'operationTime', key: 'operationTime', width: 190 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]

const getAvatarColor = (username) => {
  const colors = ['#f56a00', '#7265e6', '#ffbf00', '#00a2ae', '#7cb305', '#1890ff']
  const index = username.charCodeAt(0) % colors.length
  return colors[index]
}

const getOperationTypeColor = (type) => {
  const colorMap = {
    CREATE: 'green',
    UPDATE: 'blue',
    DELETE: 'red',
    QUERY: 'cyan',
    VIEW: 'cyan',
    LOGIN: 'purple',
    LOGOUT: 'orange'
  }
  return colorMap[type] || 'default'
}

const getOperationTypeLabel = (type) => {
  const labelMap = {
    CREATE: '创建',
    UPDATE: '更新',
    DELETE: '删除',
    QUERY: '查询',
    VIEW: '查看',
    LOGIN: '登录',
    LOGOUT: '登出'
  }
  return labelMap[type] || type || '-'
}

const getModuleColor = (module) => {
  const colorMap = {
    USER_MANAGEMENT: 'blue',
    URL_MANAGEMENT: 'green',
    SYSTEM_MONITOR: 'purple',
    SYSTEM_CONFIG: 'purple',
    LOGIN_LOGOUT: 'orange',
    AUTH: 'gold'
  }
  return colorMap[module] || 'default'
}

const getModuleLabel = (module) => {
  const labelMap = {
    USER_MANAGEMENT: '用户管理',
    URL_MANAGEMENT: '短链管理',
    SYSTEM_MONITOR: '系统监控',
    SYSTEM_CONFIG: '系统配置',
    LOGIN_LOGOUT: '登录登出',
    AUTH: '认证授权'
  }
  return labelMap[module] || module || '-'
}

const getIpLocation = (ip) => {
  if (!ip) return '未知'
  const tags = ['华北', '华东', '华南', '西南', '西北', '东北']
  const score = ip
    .split('.')
    .map((part) => Number(part) || 0)
    .reduce((sum, value) => sum + value, 0)
  return tags[score % tags.length]
}

const buildParams = () => {
  const params = {
    page: pagination.current,
    size: pagination.pageSize,
    keyword: searchKeyword.value || undefined,
    module: moduleFilter.value,
    operationType: operationFilter.value,
    status: statusFilter.value
  }

  if (dateRange.value?.length === 2) {
    params.startDate = dateRange.value[0]?.toISOString?.()
    params.endDate = dateRange.value[1]?.toISOString?.()
  }

  return params
}

const loadStats = async () => {
  try {
    const response = await getOperationLogStats()
    if (response.code === 200) {
      Object.assign(stats, response.data || {})
    }
  } catch (error) {
    console.error('Failed to load log stats:', error)
    message.error('加载日志统计失败')
  }
}

const loadLogList = async () => {
  loading.value = true
  try {
    const response = await getOperationLogs(buildParams())
    if (response.code === 200) {
      logList.value = response.data?.list || []
      pagination.total = response.data?.total || 0
    }
  } catch (error) {
    console.error('Failed to load log list:', error)
    logList.value = []
    pagination.total = 0
    message.error('加载日志列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadLogList()
}

const handleFilter = () => {
  pagination.current = 1
  loadLogList()
}

const handleDateFilter = () => {
  pagination.current = 1
  loadLogList()
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadLogList()
}

const handleRefresh = () => {
  searchKeyword.value = ''
  moduleFilter.value = undefined
  operationFilter.value = undefined
  statusFilter.value = undefined
  dateRange.value = []
  pagination.current = 1
  loadStats()
  loadLogList()
}

const viewDetails = (record) => {
  selectedLog.value = record
  showDetailModal.value = true
}

const viewUserLogs = (record) => {
  searchKeyword.value = record.username || ''
  pagination.current = 1
  loadLogList()
}

const handleExport = async () => {
  try {
    const params = {
      ...buildParams(),
      page: 1,
      size: Math.max(pagination.total || 100, 100)
    }
    const response = await getOperationLogs(params)
    if (response.code === 200 && response.data?.list?.length) {
      exportLogsToCSV(response.data.list, '操作日志')
      message.success('日志导出成功')
      return
    }
    message.warning('没有可导出的日志数据')
  } catch (error) {
    console.error('Failed to export logs:', error)
    message.error('导出失败，请稍后重试')
  }
}

const exportLogsToCSV = (data, filename) => {
  const headers = ['日志ID', '用户ID', '用户名', '操作类型', '操作模块', '操作描述', 'IP地址', '状态', '操作时间']
  const csvRows = data.map((item) => [
    item.id || '',
    item.userId || '',
    item.username || '',
    getOperationTypeLabel(item.operationType),
    getModuleLabel(item.module),
    item.operationDesc || '',
    item.ipAddress || '',
    item.status === 1 ? '成功' : '失败',
    formatDateTime(item.operationTime)
  ])

  const content = [headers.join(','), ...csvRows.map((row) => row.map((field) => `"${String(field).replace(/"/g, '""')}"`).join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + content], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${filename}_${new Date().toISOString().slice(0, 10)}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString()
}

const formatTimeAgo = (dateTime) => {
  if (!dateTime) return '-'
  const diff = Date.now() - new Date(dateTime).getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (days > 0) return `${days} 天前`
  if (hours > 0) return `${hours} 小时前`
  if (minutes > 0) return `${minutes} 分钟前`
  return '刚刚'
}

onMounted(() => {
  loadStats()
  loadLogList()
})
</script>

<style scoped>
.operation-logs {
  min-height: 100%;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 600;
  color: #262626;
}

.page-subtitle {
  margin: 0;
  color: #8c8c8c;
  font-size: 16px;
}

.filter-card,
.table-card,
.stat-card {
  border-radius: 8px;
}

.filter-card {
  margin-bottom: 24px;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.stats-row {
  margin-bottom: 24px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
}

.total-ops {
  background: linear-gradient(135deg, #1677ff, #69b1ff);
}

.success-ops {
  background: linear-gradient(135deg, #52c41a, #95de64);
}

.failed-ops {
  background: linear-gradient(135deg, #ff4d4f, #ff7875);
}

.active-users {
  background: linear-gradient(135deg, #722ed1, #b37feb);
}

.today-ops {
  background: linear-gradient(135deg, #fa8c16, #ffc069);
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #262626;
}

.stat-label {
  margin-top: 4px;
  color: #8c8c8c;
  font-size: 13px;
}

.user-cell,
.ip-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.username {
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.location-icon {
  color: #1677ff;
}

.time-cell {
  line-height: 1.4;
}

.time-ago {
  color: #8c8c8c;
  font-size: 12px;
}

.detail-block {
  white-space: pre-wrap;
  word-break: break-word;
}

.error-message {
  color: #ff4d4f;
}

@media (max-width: 768px) {
  .action-buttons {
    justify-content: flex-start;
  }
}
</style>
