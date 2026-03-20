<template>
  <div class="dashboard">
    <AdminPageHeader
      title="数据看板"
      subtitle="查看系统概览、访问趋势和实时运行状态"
    />

    <a-row :gutter="[16, 16]" class="stats-row">
      <a-col :xs="24" :sm="12" :md="6">
        <a-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon total-urls">
              <LinkOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalUrls }}</div>
              <div class="stat-label">短链总数</div>
              <div class="stat-change positive">
                <ArrowUpOutlined />
                <span>今日新增 {{ stats.todayNewUrls }}</span>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24" :sm="12" :md="6">
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
                <span>今日访问 {{ stats.todayClicks }}</span>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24" :sm="12" :md="6">
        <a-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon active-users">
              <UserOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalUsers }}</div>
              <div class="stat-label">注册用户数</div>
              <div class="stat-change neutral">
                <span>在线用户 {{ stats.onlineUsers }}</span>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24" :sm="12" :md="6">
        <a-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon system-health">
              <HeartOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ systemUptime }}</div>
              <div class="stat-label">系统运行时长</div>
              <div class="stat-change healthy">
                <CheckCircleOutlined />
                <span>运行正常</span>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]" class="charts-row">
      <a-col :xs="24" :lg="16">
        <a-card title="访问趋势" :bordered="false" class="chart-card">
          <template #extra>
            <a-radio-group v-model:value="trendPeriod" size="small" @change="loadDashboardData">
              <a-radio-button value="7">7天</a-radio-button>
              <a-radio-button value="30">30天</a-radio-button>
              <a-radio-button value="90">90天</a-radio-button>
            </a-radio-group>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="8">
        <a-card title="热门短链" :bordered="false" class="hot-urls-card">
          <a-empty v-if="!hotUrls.length" description="暂无数据" />
          <a-list v-else :data-source="hotUrls" class="hot-urls-list">
            <template #renderItem="{ item, index }">
              <a-list-item class="hot-url-item">
                <div class="rank-badge" :class="[`rank-${index + 1}`]">{{ index + 1 }}</div>
                <div class="url-info">
                  <div class="short-url">
                    <a :href="item.shortUrl" target="_blank" rel="noreferrer" @click.prevent="copyUrl(item.shortUrl)">
                      {{ item.shortKey }}
                    </a>
                  </div>
                  <div class="url-title" :title="item.title || ''">
                    {{ item.title || '未设置标题' }}
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

    <a-row :gutter="[16, 16]" class="details-row">
      <a-col :xs="24" :lg="12">
        <a-card title="最近访问" :bordered="false" class="recent-activity-card">
          <a-empty v-if="!recentAccess.length" description="暂无访问记录" />
          <a-list v-else :data-source="recentAccess" class="activity-list">
            <template #renderItem="{ item }">
              <a-list-item class="activity-item">
                <div class="activity-info">
                  <div class="activity-title">
                    <LinkOutlined class="activity-icon" />
                    <span class="short-key">{{ item.shortKey }}</span>
                  </div>
                  <div class="activity-meta">
                    <span class="ip-address">{{ item.ipAddress || '未知 IP' }}</span>
                    <span class="separator">|</span>
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

      <a-col :xs="24" :lg="12">
        <a-card title="系统状态" :bordered="false" class="system-status-card">
          <div class="system-metrics">
            <div class="metric-item">
              <div class="metric-label">CPU 使用率</div>
              <div class="metric-value">
                <a-progress
                  :percent="systemMetrics.cpuUsage"
                  size="small"
                  :status="getProgressStatus(systemMetrics.cpuUsage)"
                  :format="formatPercent"
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
                  :format="formatPercent"
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
                  :format="formatPercent"
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
              <a-descriptions-item label="系统版本">{{ stats.version || 'v1.0.0' }}</a-descriptions-item>
              <a-descriptions-item label="启动时间">{{ formatDateTime(stats.systemStartTime) }}</a-descriptions-item>
              <a-descriptions-item label="运行时长">{{ systemUptime }}</a-descriptions-item>
            </a-descriptions>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ArrowUpOutlined,
  CheckCircleOutlined,
  EyeOutlined,
  HeartOutlined,
  LinkOutlined,
  UserOutlined
} from '@ant-design/icons-vue'
import echarts from '@/utils/echarts'
import { getDashboardOverview, getSystemStats } from '@/api/admin'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'

const router = useRouter()

const stats = reactive({
  totalUrls: 0,
  totalClicks: 0,
  todayNewUrls: 0,
  todayClicks: 0,
  totalUsers: 0,
  onlineUsers: 0,
  systemStartTime: null,
  version: ''
})

const trendChartRef = ref(null)
const trendChart = ref(null)
const trendPeriod = ref('7')
const hotUrls = ref([])
const recentAccess = ref([])
const systemMetrics = reactive({
  cpuUsage: 0,
  memoryUsage: 0,
  diskUsage: 0,
  networkLatency: 0
})

const systemUptime = computed(() => {
  if (!stats.systemStartTime) {
    return '0 分钟'
  }

  const now = new Date()
  const start = new Date(stats.systemStartTime)
  const diffMs = Math.max(now.getTime() - start.getTime(), 0)
  const diffMinutes = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMinutes / 60)
  const diffDays = Math.floor(diffHours / 24)

  if (diffDays > 0) {
    return `${diffDays} 天 ${diffHours % 24} 小时`
  }
  if (diffHours > 0) {
    return `${diffHours} 小时 ${diffMinutes % 60} 分钟`
  }
  return `${diffMinutes} 分钟`
})

const ensureChart = () => {
  if (!trendChart.value && trendChartRef.value) {
    trendChart.value = echarts.init(trendChartRef.value)
  }
}

const updateTrendChart = (trend = {}) => {
  ensureChart()
  if (!trendChart.value) {
    return
  }

  trendChart.value.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: ['访问量', '新增短链']
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
      data: trend.dates || []
    },
    yAxis: [
      {
        type: 'value',
        name: '访问量'
      },
      {
        type: 'value',
        name: '新增数量'
      }
    ],
    series: [
      {
        name: '访问量',
        type: 'line',
        smooth: true,
        yAxisIndex: 0,
        data: trend.clicks || [],
        lineStyle: {
          color: '#1890ff'
        },
        areaStyle: {
          color: 'rgba(24, 144, 255, 0.12)'
        }
      },
      {
        name: '新增短链',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: trend.newUrls || [],
        lineStyle: {
          color: '#52c41a'
        }
      }
    ]
  })
}

const loadStats = async () => {
  const response = await getSystemStats()
  if (response.code === 200) {
    Object.assign(stats, response.data || {})
  }
}

const loadDashboardData = async () => {
  const response = await getDashboardOverview(Number(trendPeriod.value))
  if (response.code !== 200) {
    return
  }

  const data = response.data || {}
  updateTrendChart(data.trend)
  hotUrls.value = data.hotUrls || []
  recentAccess.value = data.recentAccess || []
  Object.assign(systemMetrics, data.systemMetrics || {})
}

const refreshDashboard = async () => {
  try {
    await Promise.all([loadStats(), loadDashboardData()])
  } catch (error) {
    console.error('Failed to load dashboard data:', error)
    message.error('加载数据看板失败')
  }
}

const getProgressStatus = (value) => {
  if (value >= 80) return 'exception'
  if (value >= 60) return 'active'
  return 'normal'
}

const formatPercent = (percent) => `${Math.max(0, Math.min(100, Number(percent) || 0))}%`

const getLatencyStatus = (latency) => {
  if (latency < 50) return 'good'
  if (latency < 100) return 'normal'
  return 'poor'
}

const getLatencyLabel = (latency) => {
  if (latency < 50) return '优秀'
  if (latency < 100) return '良好'
  return '较高'
}

const copyUrl = async (url) => {
  try {
    await navigator.clipboard.writeText(url)
    message.success('短链已复制到剪贴板')
  } catch (error) {
    message.error('复制失败，请手动复制')
  }
}

const viewUrlDetails = (shortKey) => {
  router.push(`/admin/urls?key=${encodeURIComponent(shortKey)}`)
}

const formatTime = (time) => {
  if (!time) {
    return '--'
  }

  const now = new Date()
  const target = new Date(time)
  const diffMinutes = Math.floor((now.getTime() - target.getTime()) / 60000)
  const diffHours = Math.floor(diffMinutes / 60)
  const diffDays = Math.floor(diffHours / 24)

  if (diffDays > 0) return `${diffDays} 天前`
  if (diffHours > 0) return `${diffHours} 小时前`
  if (diffMinutes > 0) return `${diffMinutes} 分钟前`
  return '刚刚'
}

const formatDateTime = (dateTime) => {
  if (!dateTime) {
    return '--'
  }
  return new Date(dateTime).toLocaleString()
}

let refreshTimer = null

const startAutoRefresh = () => {
  refreshTimer = window.setInterval(() => {
    refreshDashboard()
  }, 30000)
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
    refreshTimer = null
  }
}

const handleResize = () => {
  trendChart.value?.resize()
}

onMounted(async () => {
  await refreshDashboard()
  startAutoRefresh()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  stopAutoRefresh()
  window.removeEventListener('resize', handleResize)
  trendChart.value?.dispose()
})
</script>

<style scoped>
.dashboard {
  min-height: 100%;
  padding: 4px;
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card,
.chart-card,
.hot-urls-card,
.recent-activity-card,
.system-status-card {
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

.stat-card:hover {
  transform: translateY(-2px);
  transition: all 0.3s;
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
  margin-right: 16px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.total-urls {
  background: linear-gradient(135deg, #1890ff, #40a9ff);
}

.total-clicks {
  background: linear-gradient(135deg, #52c41a, #73d13d);
}

.active-users {
  background: linear-gradient(135deg, #722ed1, #b37feb);
}

.system-health {
  background: linear-gradient(135deg, #fa8c16, #ffa940);
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
  margin: 4px 0;
  font-size: 14px;
  color: #8c8c8c;
}

.stat-change {
  display: flex;
  align-items: center;
  font-size: 12px;
}

.stat-change span {
  margin-left: 4px;
}

.stat-change.positive,
.stat-change.healthy {
  color: #52c41a;
}

.stat-change.neutral {
  color: #1890ff;
}

.charts-row,
.details-row {
  margin-bottom: 24px;
}

.chart-container {
  width: 100%;
  height: 400px;
}

.hot-urls-list {
  max-height: 400px;
  overflow-y: auto;
}

.hot-url-item,
.activity-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.rank-badge {
  width: 24px;
  height: 24px;
  margin-right: 12px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
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

.url-info,
.activity-info {
  flex: 1;
}

.short-url,
.short-key,
.latency-value {
  font-family: "Monaco", "Menlo", "Ubuntu Mono", monospace;
}

.short-url {
  margin-bottom: 2px;
  font-size: 14px;
  font-weight: 500;
}

.short-url a {
  color: #1890ff;
  text-decoration: none;
}

.short-url a:hover {
  text-decoration: underline;
}

.url-title {
  max-width: 200px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 12px;
  color: #8c8c8c;
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
  margin-left: 2px;
  font-size: 12px;
  color: #8c8c8c;
}

.activity-list {
  max-height: 300px;
  overflow-y: auto;
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

.activity-meta {
  display: flex;
  align-items: center;
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
  margin-bottom: 8px;
  font-size: 14px;
  color: #595959;
}

.metric-value.latency {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.latency-status {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
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
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

@media (max-width: 768px) {
  .dashboard {
    padding: 0;
  }

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
    max-width: 140px;
  }
}

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
