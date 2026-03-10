<template>
  <div class="dashboard">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">数据面板</h1>
      <p class="page-subtitle">系统概览和关键指标</p>
    </div>

    <!-- 统计卡片 -->
    <a-row :gutter="[16, 16]" class="stats-row">
      <a-col :xs="24" :sm="12" :md="6" :lg="6">
        <a-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon total-urls">
              <LinkOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalUrls }}</div>
              <div class="stat-label">总短网址数</div>
              <div class="stat-change positive">
                <ArrowUpOutlined />
                <span>+{{ stats.todayNewUrls }} 今日新增</span>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24" :sm="12" :md="6" :lg="6">
        <a-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon total-clicks">
              <EyeOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalClicks }}</div>
              <div class="stat-label">总访问次数</div>
              <div class="stat-change positive">
                <ArrowUpOutlined />
                <span>+{{ stats.todayClicks }} 今日访问</span>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24" :sm="12" :md="6" :lg="6">
        <a-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon active-users">
              <UserOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalUsers }}</div>
              <div class="stat-label">注册用户数</div>
              <div class="stat-change neutral">
                <span>{{ stats.onlineUsers }} 在线用户</span>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24" :sm="12" :md="6" :lg="6">
        <a-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon system-health">
              <HeartOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ systemUptime }}</div>
              <div class="stat-label">系统运行</div>
              <div class="stat-change healthy">
                <CheckCircleOutlined />
                <span>运行正常</span>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 图表区域 -->
    <a-row :gutter="[16, 16]" class="charts-row">
      <!-- 访问趋势图 -->
      <a-col :xs="24" :lg="16">
        <a-card title="访问趋势" :bordered="false" class="chart-card">
          <template #extra>
            <a-radio-group v-model:value="trendPeriod" size="small" @change="loadTrendData">
              <a-radio-button value="7">7天</a-radio-button>
              <a-radio-button value="30">30天</a-radio-button>
              <a-radio-button value="90">90天</a-radio-button>
            </a-radio-group>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </a-card>
      </a-col>

      <!-- 热门短网址 -->
      <a-col :xs="24" :lg="8">
        <a-card title="热门短网址" :bordered="false" class="hot-urls-card">
          <a-list :data-source="hotUrls" class="hot-urls-list">
            <template #renderItem="{ item, index }">
              <a-list-item class="hot-url-item">
                <div class="rank-badge" :class="['rank-' + (index + 1)]">
                  {{ index + 1 }}
                </div>
                <div class="url-info">
                  <div class="short-url">
                    <a :href="item.shortUrl" target="_blank" @click.prevent="copyUrl(item.shortUrl)">
                      {{ item.shortKey }}
                    </a>
                  </div>
                  <div class="url-title" :title="item.title">
                    {{ item.title || '无标题' }}
                  </div>
                </div>
                <div class="click-count">
                  <span class="count">{{ item.clickCount }}</span>
                  <span class="label">次</span>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>

    <!-- 详细信息区域 -->
    <a-row :gutter="[16, 16]" class="details-row">
      <!-- 最近活动 -->
      <a-col :xs="24" :lg="12">
        <a-card title="最近访问" :bordered="false" class="recent-activity-card">
          <a-list :data-source="recentAccess" class="activity-list">
            <template #renderItem="{ item }">
              <a-list-item class="activity-item">
                <div class="activity-info">
                  <div class="activity-title">
                    <LinkOutlined class="activity-icon" />
                    <span class="short-key">{{ item.shortKey }}</span>
                  </div>
                  <div class="activity-meta">
                    <span class="ip-address">{{ item.ipAddress }}</span>
                    <span class="separator">·</span>
                    <span class="access-time">{{ formatTime(item.accessTime) }}</span>
                  </div>
                </div>
                <div class="activity-action">
                  <a @click="viewUrlDetails(item.shortKey)">查看详情</a>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>

      <!-- 系统状态 -->
      <a-col :xs="24" :lg="12">
        <a-card title="系统状态" :bordered="false" class="system-status-card">
          <div class="system-metrics">
            <div class="metric-item">
              <div class="metric-label">CPU使用率</div>
              <div class="metric-value">
                <a-progress
                  :percent="systemMetrics.cpuUsage"
                  size="small"
                  :status="getProgressStatus(systemMetrics.cpuUsage)"
                />
              </div>
            </div>
            <div class="metric-item">
              <div class="metric-label">内存使用率</div>
              <div class="metric-value">
                <a-progress
                  :percent="systemMetrics.memoryUsage"
                  size="small"
                  :status="getProgressStatus(systemMetrics.memoryUsage)"
                />
              </div>
            </div>
            <div class="metric-item">
              <div class="metric-label">磁盘使用率</div>
              <div class="metric-value">
                <a-progress
                  :percent="systemMetrics.diskUsage"
                  size="small"
                  :status="getProgressStatus(systemMetrics.diskUsage)"
                />
              </div>
            </div>
            <div class="metric-item">
              <div class="metric-label">网络延迟</div>
              <div class="metric-value latency">
                <span class="latency-value">{{ systemMetrics.networkLatency }}ms</span>
                <span :class="['latency-status', getLatencyStatus(systemMetrics.networkLatency)]">
                  {{ getLatencyLabel(systemMetrics.networkLatency) }}
                </span>
              </div>
            </div>
          </div>

          <div class="system-info">
            <a-descriptions size="small" :column="1">
              <a-descriptions-item label="系统版本">v1.0.0</a-descriptions-item>
              <a-descriptions-item label="启动时间">{{ formatDateTime(stats.systemStartTime) }}</a-descriptions-item>
              <a-descriptions-item label="运行时间">{{ systemUptime }}</a-descriptions-item>
            </a-descriptions>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  LinkOutlined,
  EyeOutlined,
  UserOutlined,
  HeartOutlined,
  CheckCircleOutlined,
  ArrowUpOutlined,
  CopyOutlined
} from '@ant-design/icons-vue'
import * as echarts from 'echarts'
import { getSystemStats } from '@/api/admin'

const router = useRouter()

// 统计数据
const stats = reactive({
  totalUrls: 0,
  totalClicks: 0,
  todayNewUrls: 0,
  todayClicks: 0,
  totalUsers: 0,
  onlineUsers: 0,
  systemStartTime: null
})

// 图表相关
const trendChartRef = ref(null)
const trendChart = ref(null)
const trendPeriod = ref('7')

// 热门短网址
const hotUrls = ref([])

// 最近访问
const recentAccess = ref([])

// 系统指标
const systemMetrics = reactive({
  cpuUsage: 0,
  memoryUsage: 0,
  diskUsage: 0,
  networkLatency: 0
})

// 计算属性
const systemUptime = computed(() => {
  if (!stats.systemStartTime) return '0分钟'

  const now = new Date()
  const start = new Date(stats.systemStartTime)
  const diffMs = now - start
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMins / 60)
  const diffDays = Math.floor(diffHours / 24)

  if (diffDays > 0) {
    return `${diffDays}天${diffHours % 24}小时`
  } else if (diffHours > 0) {
    return `${diffHours}小时${diffMins % 60}分钟`
  } else {
    return `${diffMins}分钟`
  }
})

/**
 * 加载统计数据
 */
const loadStats = async () => {
  try {
    const response = await getSystemStats()
    if (response.code === 200) {
      Object.assign(stats, response.data)
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    message.error('加载统计数据失败')
  }
}

/**
 * 加载趋势数据
 */
const loadTrendData = async () => {
  try {
    // TODO: 实现趋势数据加载
    const mockData = generateMockTrendData(parseInt(trendPeriod.value))
    updateTrendChart(mockData)
  } catch (error) {
    console.error('加载趋势数据失败:', error)
  }
}

/**
 * 生成模拟趋势数据
 */
const generateMockTrendData = (days) => {
  const data = {
    dates: [],
    clicks: [],
    newUrls: []
  }

  for (let i = days - 1; i >= 0; i--) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    data.dates.push(date.toLocaleDateString())
    data.clicks.push(Math.floor(Math.random() * 1000) + 100)
    data.newUrls.push(Math.floor(Math.random() * 50) + 10)
  }

  return data
}

/**
 * 更新趋势图表
 */
const updateTrendChart = (data) => {
  if (!trendChart.value) {
    trendChart.value = echarts.init(trendChartRef.value)
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      }
    },
    legend: {
      data: ['访问量', '新增短网址']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.dates
    },
    yAxis: [
      {
        type: 'value',
        name: '访问量',
        position: 'left'
      },
      {
        type: 'value',
        name: '新增数量',
        position: 'right'
      }
    ],
    series: [
      {
        name: '访问量',
        type: 'line',
        yAxisIndex: 0,
        data: data.clicks,
        smooth: true,
        lineStyle: {
          color: '#1890ff'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
              { offset: 1, color: 'rgba(24, 144, 255, 0.05)' }
            ]
          }
        }
      },
      {
        name: '新增短网址',
        type: 'line',
        yAxisIndex: 1,
        data: data.newUrls,
        smooth: true,
        lineStyle: {
          color: '#52c41a'
        }
      }
    ]
  }

  trendChart.value.setOption(option)
}

/**
 * 加载热门短网址
 */
const loadHotUrls = () => {
  // 模拟数据
  hotUrls.value = [
    {
      shortKey: 'abc123',
      shortUrl: 'https://short.ly/abc123',
      title: '示例网站',
      clickCount: 1250
    },
    {
      shortKey: 'def456',
      shortUrl: 'https://short.ly/def456',
      title: '另一个链接',
      clickCount: 890
    },
    {
      shortKey: 'ghi789',
      shortUrl: 'https://short.ly/ghi789',
      title: '测试页面',
      clickCount: 654
    }
  ]
}

/**
 * 加载最近访问
 */
const loadRecentAccess = () => {
  // 模拟数据
  recentAccess.value = [
    {
      shortKey: 'abc123',
      ipAddress: '192.168.1.100',
      accessTime: new Date(Date.now() - 300000)
    },
    {
      shortKey: 'def456',
      ipAddress: '10.0.0.50',
      accessTime: new Date(Date.now() - 600000)
    },
    {
      shortKey: 'ghi789',
      ipAddress: '172.16.0.25',
      accessTime: new Date(Date.now() - 900000)
    }
  ]
}

/**
 * 加载系统指标
 */
const loadSystemMetrics = () => {
  // 模拟数据
  systemMetrics.cpuUsage = Math.floor(Math.random() * 40) + 10
  systemMetrics.memoryUsage = Math.floor(Math.random() * 60) + 20
  systemMetrics.diskUsage = Math.floor(Math.random() * 30) + 40
  systemMetrics.networkLatency = Math.floor(Math.random() * 50) + 10
}

/**
 * 获取进度条状态
 */
const getProgressStatus = (value) => {
  if (value >= 80) return 'exception'
  if (value >= 60) return 'active'
  return 'normal'
}

/**
 * 获取延迟状态
 */
const getLatencyStatus = (latency) => {
  if (latency < 50) return 'good'
  if (latency < 100) return 'normal'
  return 'poor'
}

/**
 * 获取延迟标签
 */
const getLatencyLabel = (latency) => {
  if (latency < 50) return '优秀'
  if (latency < 100) return '良好'
  return '较差'
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
 * 查看URL详情
 */
const viewUrlDetails = (shortKey) => {
  router.push(`/admin/urls?key=${shortKey}`)
}

/**
 * 格式化时间
 */
const formatTime = (time) => {
  const now = new Date()
  const diff = now - new Date(time)
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (days > 0) return `${days}天前`
  if (hours > 0) return `${hours}小时前`
  if (minutes > 0) return `${minutes}分钟前`
  return '刚刚'
}

/**
 * 格式化日期时间
 */
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return new Date(dateTime).toLocaleString()
}

/**
 * 定时刷新数据
 */
let refreshTimer = null

const startAutoRefresh = () => {
  refreshTimer = setInterval(() => {
    loadSystemMetrics()
  }, 30000) // 每30秒刷新一次系统指标
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

/**
 * 生命周期钩子
 */
onMounted(() => {
  loadStats()
  loadTrendData()
  loadHotUrls()
  loadRecentAccess()
  loadSystemMetrics()
  startAutoRefresh()

  // 监听窗口大小变化，重新调整图表
  window.addEventListener('resize', () => {
    trendChart.value?.resize()
  })
})

onUnmounted(() => {
  stopAutoRefresh()
  window.removeEventListener('resize', () => {
    trendChart.value?.resize()
  })
  trendChart.value?.dispose()
})
</script>

<style scoped>
.dashboard {
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

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  padding: 8px 0;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 16px;
}

.total-urls {
  background: linear-gradient(135deg, #1890ff, #40a9ff);
  color: white;
}

.total-clicks {
  background: linear-gradient(135deg, #52c41a, #73d13d);
  color: white;
}

.active-users {
  background: linear-gradient(135deg, #722ed1, #b37feb);
  color: white;
}

.system-health {
  background: linear-gradient(135deg, #fa8c16, #ffa940);
  color: white;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #262626;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #8c8c8c;
  margin: 4px 0;
}

.stat-change {
  font-size: 12px;
  display: flex;
  align-items: center;
}

.stat-change.positive {
  color: #52c41a;
}

.stat-change.neutral {
  color: #1890ff;
}

.stat-change.healthy {
  color: #52c41a;
}

.stat-change span {
  margin-left: 4px;
}

.charts-row {
  margin-bottom: 24px;
}

.chart-card {
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.chart-container {
  height: 400px;
  width: 100%;
}

.hot-urls-card {
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.hot-urls-list {
  max-height: 400px;
  overflow-y: auto;
}

.hot-url-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
}

.rank-badge {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: white;
  margin-right: 12px;
}

.rank-1 {
  background: #ffd700;
}

.rank-2 {
  background: #c0c0c0;
}

.rank-3 {
  background: #cd7f32;
}

.rank-4,
.rank-5 {
  background: #8c8c8c;
}

.url-info {
  flex: 1;
  margin-right: 12px;
}

.short-url {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 2px;
}

.short-url a {
  color: #1890ff;
  text-decoration: none;
}

.short-url a:hover {
  text-decoration: underline;
}

.url-title {
  font-size: 12px;
  color: #8c8c8c;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.click-count {
  text-align: right;
}

.count {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.label {
  font-size: 12px;
  color: #8c8c8c;
  margin-left: 2px;
}

.details-row {
  margin-bottom: 24px;
}

.recent-activity-card,
.system-status-card {
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.activity-list {
  max-height: 300px;
  overflow-y: auto;
}

.activity-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
}

.activity-info {
  flex: 1;
}

.activity-title {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
}

.activity-icon {
  margin-right: 8px;
  color: #1890ff;
}

.short-key {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-weight: 500;
}

.activity-meta {
  font-size: 12px;
  color: #8c8c8c;
}

.separator {
  margin: 0 8px;
}

.activity-action a {
  font-size: 12px;
}

.system-metrics {
  margin-bottom: 24px;
}

.metric-item {
  margin-bottom: 16px;
}

.metric-label {
  font-size: 14px;
  color: #595959;
  margin-bottom: 8px;
}

.metric-value {
  display: flex;
  align-items: center;
}

.metric-value.latency {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.latency-value {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-weight: 500;
}

.latency-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.latency-status.good {
  background: #f6ffed;
  color: #52c41a;
  border: 1px solid #b7eb8f;
}

.latency-status.normal {
  background: #fff7e6;
  color: #fa8c16;
  border: 1px solid #ffd591;
}

.latency-status.poor {
  background: #fff2f0;
  color: #ff4d4f;
  border: 1px solid #ffccc7;
}

.system-info {
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .stat-content {
    flex-direction: column;
    text-align: center;
  }

  .stat-icon {
    margin-right: 0;
    margin-bottom: 12px;
  }

  .chart-container {
    height: 300px;
  }

  .url-title {
    max-width: 150px;
  }
}

/* 暗色主题适配 */
:deep(.ant-card) {
  background: #fff;
}

:deep(.ant-card-head-title) {
  font-weight: 600;
}

:deep(.ant-list-item) {
  border-bottom: 1px solid #f0f0f0;
}

:deep(.ant-list-item:last-child) {
  border-bottom: none;
}
</style>