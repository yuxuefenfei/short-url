<template>
  <div class="operation-logs">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">操作日志</h1>
      <p class="page-subtitle">查看系统操作记录和安全审计</p>
    </div>

    <!-- 搜索和过滤 -->
    <a-card class="filter-card">
      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :sm="6" :md="4">
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索用户或操作"
            enter-button="搜索"
            @search="handleSearch"
            allow-clear
          />
        </a-col>
        <a-col :xs="12" :sm="6" :md="4">
          <a-select
            v-model:value="moduleFilter"
            placeholder="模块筛选"
            style="width: 100%"
            @change="handleFilter"
            allow-clear
          >
            <a-select-option value="USER_MANAGEMENT">用户管理</a-select-option>
            <a-select-option value="URL_MANAGEMENT">短网址管理</a-select-option>
            <a-select-option value="SYSTEM_CONFIG">系统配置</a-select-option>
            <a-select-option value="LOGIN_LOGOUT">登录登出</a-select-option>
          </a-select>
        </a-col>
        <a-col :xs="12" :sm="6" :md="4">
          <a-select
            v-model:value="operationFilter"
            placeholder="操作类型"
            style="width: 100%"
            @change="handleFilter"
            allow-clear
          >
            <a-select-option value="CREATE">创建</a-select-option>
            <a-select-option value="UPDATE">更新</a-select-option>
            <a-select-option value="DELETE">删除</a-select-option>
            <a-select-option value="VIEW">查看</a-select-option>
            <a-select-option value="LOGIN">登录</a-select-option>
            <a-select-option value="LOGOUT">登出</a-select-option>
          </a-select>
        </a-col>
        <a-col :xs="12" :sm="6" :md="4">
          <a-select
            v-model:value="statusFilter"
            placeholder="状态筛选"
            style="width: 100%"
            @change="handleFilter"
            allow-clear
          >
            <a-select-option value="1">成功</a-select-option>
            <a-select-option value="0">失败</a-select-option>
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
        <a-col :xs="24" :sm="6" :md="10" class="action-buttons">
          <a-button @click="handleExport" style="margin-left: 8px;">
            <ExportOutlined />
            导出日志
          </a-button>
          <a-button @click="handleRefresh" style="margin-left: 8px;">
            <ReloadOutlined />
            刷新
          </a-button>
        </a-col>
      </a-row>
    </a-card>

    <!-- 统计卡片 -->
    <a-row :gutter="[16, 16]" class="stats-row">
      <a-col :xs="24" :sm="6" :md="3">
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
      <a-col :xs="24" :sm="6" :md="3">
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
      <a-col :xs="24" :sm="6" :md="3">
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
      <a-col :xs="24" :sm="6" :md="3">
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
      <a-col :xs="24" :sm="12" :md="6">
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

    <!-- 数据表格 -->
    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="logList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        :scroll="{ x: 1400 }"
        :row-key="record => record.id"
      >
        <!-- 用户名列 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'username'">
            <div class="user-cell">
              <a-avatar :style="{ backgroundColor: getAvatarColor(record.username) }" size="small">
                {{ record.username.charAt(0).toUpperCase() }}
              </a-avatar>
              <span class="username">{{ record.username }}</span>
            </div>
          </template>

          <!-- 操作类型列 -->
          <template v-else-if="column.key === 'operationType'">
            <a-tag :color="getOperationTypeColor(record.operationType)">
              {{ getOperationTypeLabel(record.operationType) }}
            </a-tag>
          </template>

          <!-- 操作模块列 -->
          <template v-else-if="column.key === 'module'">
            <a-tag :color="getModuleColor(record.module)">
              {{ getModuleLabel(record.module) }}
            </a-tag>
          </template>

          <!-- 操作描述列 -->
          <template v-else-if="column.key === 'operationDesc'">
            <div class="operation-desc" :title="record.operationDesc">
              {{ record.operationDesc }}
            </div>
          </template>

          <!-- 状态列 -->
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'success' : 'error'">
              {{ record.status === 1 ? '成功' : '失败' }}
            </a-tag>
          </template>

          <!-- IP地址列 -->
          <template v-else-if="column.key === 'ipAddress'">
            <div class="ip-cell">
              <span class="ip">{{ record.ipAddress }}</span>
              <a-tooltip v-if="record.ipAddress" :title="'地理位置: ' + getIpLocation(record.ipAddress)" placement="top">
                <EnvironmentOutlined class="location-icon" />
              </a-tooltip>
            </div>
          </template>

          <!-- 操作时间列 -->
          <template v-else-if="column.key === 'operationTime'">
            <div class="time-cell">
              <div>{{ formatDateTime(record.operationTime) }}</div>
              <div class="time-ago">{{ formatTimeAgo(record.operationTime) }}</div>
            </div>
          </template>

          <!-- 操作列 -->
          <template v-else-if="column.key === 'action'">
            <div class="action-cell">
              <a-space>
                <a @click="viewDetails(record)">详情</a>
                <a-divider type="vertical" />
                <a @click="viewUserLogs(record)">用户日志</a>
              </a-space>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 日志详情模态框 -->
    <a-modal
      v-model:visible="showDetailModal"
      title="操作日志详情"
      :footer="null"
      width="700px"
    >
      <div v-if="selectedLog" class="detail-content">
        <a-descriptions bordered :column="1">
          <a-descriptions-item label="日志ID">
            {{ selectedLog.id }}
          </a-descriptions-item>
          <a-descriptions-item label="用户ID">
            {{ selectedLog.userId }}
          </a-descriptions-item>
          <a-descriptions-item label="用户名">
            {{ selectedLog.username }}
          </a-descriptions-item>
          <a-descriptions-item label="操作类型">
            <a-tag :color="getOperationTypeColor(selectedLog.operationType)">
              {{ getOperationTypeLabel(selectedLog.operationType) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="操作模块">
            <a-tag :color="getModuleColor(selectedLog.module)">
              {{ getModuleLabel(selectedLog.module) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="操作描述">
            {{ selectedLog.operationDesc }}
          </a-descriptions-item>
          <a-descriptions-item label="IP地址">
            <div class="ip-detail">
              {{ selectedLog.ipAddress }}
              <span class="location">({{ getIpLocation(selectedLog.ipAddress) }})</span>
            </div>
          </a-descriptions-item>
          <a-descriptions-item label="用户代理">
            <div class="user-agent">
              {{ selectedLog.userAgent || '未记录' }}
            </div>
          </a-descriptions-item>
          <a-descriptions-item label="请求路径">
            {{ selectedLog.requestPath || '未记录' }}
          </a-descriptions-item>
          <a-descriptions-item label="请求方法">
            {{ selectedLog.requestMethod || '未记录' }}
          </a-descriptions-item>
          <a-descriptions-item label="请求参数">
            <div class="request-params">
              {{ selectedLog.requestParams || '无' }}
            </div>
          </a-descriptions-item>
          <a-descriptions-item label="响应结果">
            <div class="response-result">
              {{ selectedLog.responseResult || '无' }}
            </div>
          </a-descriptions-item>
          <a-descriptions-item label="操作状态">
            <a-tag :color="selectedLog.status === 1 ? 'success' : 'error'">
              {{ selectedLog.status === 1 ? '成功' : '失败' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="错误信息" v-if="selectedLog.errorMessage">
            <div class="error-message">
              {{ selectedLog.errorMessage }}
            </div>
          </a-descriptions-item>
          <a-descriptions-item label="操作时间">
            {{ formatDateTime(selectedLog.operationTime) }}
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  FileTextOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  UserOutlined,
  ClockCircleOutlined,
  ExportOutlined,
  ReloadOutlined,
  EnvironmentOutlined
} from '@ant-design/icons-vue'
import { getOperationLogs, getOperationLogStats } from '@/api/admin'

// 搜索和过滤
const searchKeyword = ref('')
const moduleFilter = ref(undefined)
const operationFilter = ref(undefined)
const statusFilter = ref(undefined)
const dateRange = ref([])

// 统计数据
const stats = reactive({
  totalOperations: 0,
  successOperations: 0,
  failedOperations: 0,
  activeUsers: 0,
  todayOperations: 0
})

// 表格数据
const logList = ref([])
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
    title: '用户',
    dataIndex: 'username',
    key: 'username',
    width: 120
  },
  {
    title: '操作类型',
    dataIndex: 'operationType',
    key: 'operationType',
    width: 100
  },
  {
    title: '模块',
    dataIndex: 'module',
    key: 'module',
    width: 120
  },
  {
    title: '操作描述',
    dataIndex: 'operationDesc',
    key: 'operationDesc',
    ellipsis: true,
    width: 200
  },
  {
    title: 'IP地址',
    dataIndex: 'ipAddress',
    key: 'ipAddress',
    width: 140
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 80
  },
  {
    title: '操作时间',
    dataIndex: 'operationTime',
    key: 'operationTime',
    width: 180,
    sorter: true
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
    fixed: 'right'
  }
]

// 详情模态框
const showDetailModal = ref(false)
const selectedLog = ref(null)

/**
 * 生成头像颜色
 */
const getAvatarColor = (username) => {
  const colors = ['#f56a00', '#7265e6', '#ffbf00', '#00a2ae', '#7cb305', '#1890ff']
  const index = username.charCodeAt(0) % colors.length
  return colors[index]
}

/**
 * 获取操作类型颜色
 */
const getOperationTypeColor = (type) => {
  const colorMap = {
    'CREATE': 'green',
    'UPDATE': 'blue',
    'DELETE': 'red',
    'VIEW': 'cyan',
    'LOGIN': 'purple',
    'LOGOUT': 'orange'
  }
  return colorMap[type] || 'default'
}

/**
 * 获取操作类型标签
 */
const getOperationTypeLabel = (type) => {
  const labelMap = {
    'CREATE': '创建',
    'UPDATE': '更新',
    'DELETE': '删除',
    'VIEW': '查看',
    'LOGIN': '登录',
    'LOGOUT': '登出'
  }
  return labelMap[type] || type
}

/**
 * 获取模块颜色
 */
const getModuleColor = (module) => {
  const colorMap = {
    'USER_MANAGEMENT': 'blue',
    'URL_MANAGEMENT': 'green',
    'SYSTEM_CONFIG': 'purple',
    'LOGIN_LOGOUT': 'orange'
  }
  return colorMap[module] || 'default'
}

/**
 * 获取模块标签
 */
const getModuleLabel = (module) => {
  const labelMap = {
    'USER_MANAGEMENT': '用户管理',
    'URL_MANAGEMENT': '短网址管理',
    'SYSTEM_CONFIG': '系统配置',
    'LOGIN_LOGOUT': '登录登出'
  }
  return labelMap[module] || module
}

/**
 * 获取IP地理位置（模拟）
 */
const getIpLocation = (ip) => {
  if (!ip) return '未知'
  // 模拟IP地理位置查询
  const locations = ['北京市', '上海市', '广州市', '深圳市', '杭州市', '南京市']
  const index = ip.split('.').reduce((sum, part) => sum + parseInt(part), 0) % locations.length
  return locations[index]
}

/**
 * 加载统计数据
 */
const loadStats = async () => {
  try {
    const response = await getOperationLogStats()
    if (response.code === 200) {
      Object.assign(stats, response.data)
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    // 使用模拟数据
    stats.totalOperations = 1250
    stats.successOperations = 1180
    stats.failedOperations = 70
    stats.activeUsers = 45
    stats.todayOperations = 89
  }
}

/**
 * 加载日志列表
 */
const loadLogList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.current,
      size: pagination.pageSize,
      keyword: searchKeyword.value,
      module: moduleFilter.value,
      operationType: operationFilter.value,
      status: statusFilter.value
    }

    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    const response = await getOperationLogs(params)
    if (response.code === 200) {
      logList.value = response.data.list
      pagination.total = response.data.total
    }
  } catch (error) {
    console.error('加载日志列表失败:', error)
    // 使用模拟数据
    logList.value = generateMockLogs()
    pagination.total = logList.value.length
  } finally {
    loading.value = false
  }
}

/**
 * 生成模拟日志数据
 */
const generateMockLogs = () => {
  const operations = ['CREATE', 'UPDATE', 'DELETE', 'VIEW', 'LOGIN', 'LOGOUT']
  const modules = ['USER_MANAGEMENT', 'URL_MANAGEMENT', 'SYSTEM_CONFIG', 'LOGIN_LOGOUT']
  const users = ['admin', 'user1', 'user2', 'testuser']
  const logs = []

  for (let i = 1; i <= 50; i++) {
    const operationTime = new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000)
    logs.push({
      id: i,
      userId: Math.floor(Math.random() * 100) + 1,
      username: users[Math.floor(Math.random() * users.length)],
      operationType: operations[Math.floor(Math.random() * operations.length)],
      module: modules[Math.floor(Math.random() * modules.length)],
      operationDesc: `执行${getOperationTypeLabel(operations[Math.floor(Math.random() * operations.length)])}操作`,
      ipAddress: `192.168.1.${Math.floor(Math.random() * 255)}`,
      status: Math.random() > 0.1 ? 1 : 0,
      operationTime: operationTime,
      userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    })
  }

  return logs.sort((a, b) => new Date(b.operationTime) - new Date(a.operationTime))
}

/**
 * 处理搜索
 */
const handleSearch = () => {
  pagination.current = 1
  loadLogList()
}

/**
 * 处理筛选
 */
const handleFilter = () => {
  pagination.current = 1
  loadLogList()
}

/**
 * 处理日期筛选
 */
const handleDateFilter = () => {
  pagination.current = 1
  loadLogList()
}

/**
 * 处理表格变化
 */
const handleTableChange = (pag, filters, sorter) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadLogList()
}

/**
 * 处理刷新
 */
const handleRefresh = () => {
  searchKeyword.value = ''
  moduleFilter.value = undefined
  operationFilter.value = undefined
  statusFilter.value = undefined
  dateRange.value = []
  pagination.current = 1
  loadLogList()
}

/**
 * 查看详情
 */
const viewDetails = (record) => {
  selectedLog.value = record
  showDetailModal.value = true
}

/**
 * 查看用户日志
 */
const viewUserLogs = (record) => {
  searchKeyword.value = record.username
  pagination.current = 1
  loadLogList()
}

/**
 * 导出日志
 */
const handleExport = async () => {
  try {
    message.loading('正在导出日志...', 0)

    // 构建导出参数
    const params = {
      keyword: searchKeyword.value,
      module: moduleFilter.value,
      operationType: operationFilter.value,
      status: statusFilter.value,
      exportAll: true // 标记为导出全部数据
    }

    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    // 调用导出API
    const response = await getOperationLogs(params)

    if (response.code === 200 && response.data.list) {
      exportLogsToCSV(response.data.list, '操作日志')
      message.success('日志导出成功')
    } else {
      message.error('导出失败：未获取到数据')
    }
  } catch (error) {
    console.error('导出日志失败:', error)
    message.error('导出失败，请稍后重试')
  }
}

/**
 * 导出日志数据为CSV
 */
const exportLogsToCSV = (data, filename) => {
  if (!data || data.length === 0) {
    message.warning('没有数据可导出')
    return
  }

  // CSV表头
  const headers = [
    '日志ID',
    '用户ID',
    '用户名',
    '操作类型',
    '操作模块',
    '操作描述',
    'IP地址',
    '操作状态',
    '操作时间'
  ]

  // 数据转换
  const csvData = data.map(item => [
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
  margin-bottom: 24px;
}
</style>