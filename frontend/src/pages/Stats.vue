<template>
  <div class="stats-page">
    <!-- 页面头部 -->
    <header class="page-header">
      <div class="container">
        <h1 class="page-title">访问统计分析</h1>
        <p class="page-subtitle">查看短网址的详细访问数据和趋势分析</p>
      </div>
    </header>

    <!-- 主要内容区域 -->
    <main class="page-content">
      <div class="container">
        <!-- 查询表单 -->
        <div class="query-section">
          <a-card :bordered="false" class="query-card">
            <a-form layout="inline" @finish="handleQuery" :model="queryForm">
              <a-form-item name="shortKey" label="短网址Key">
                <a-input
                  v-model:value="queryForm.shortKey"
                  placeholder="请输入短网址Key"
                  style="width: 200px"
                  allow-clear
                >
                  <template #prefix>
                    <link-outlined />
                  </template>
                </a-input>
              </a-form-item>

              <a-form-item>
                <a-button type="primary" html-type="submit" :loading="loading">
                  <search-outlined />
                  查询统计
                </a-button>
              </a-form-item>

              <a-form-item>
                <a-button @click="resetQuery">
                  <reload-outlined />
                  重置
                </a-button>
              </a-form-item>
            </a-form>
          </a-card>
        </div>

        <!-- 统计结果 -->
        <div v-if="stats" class="stats-section">
          <!-- 基本信息卡片 -->
          <a-row :gutter="[24, 24]" class="stats-row">
            <a-col :xs="24" :sm="12" :md="6">
              <a-card class="stat-card" :bordered="false">
                <div class="stat-content">
                  <div class="stat-icon total-clicks">
                    <eye-outlined />
                  </div>
                  <div class="stat-info">
                    <div class="stat-value">{{ stats.totalClicks || 0 }}</div>
                    <div class="stat-label">总访问量</div>
                  </div>
                </div>
              </a-card>
            </a-col>

            <a-col :xs="24" :sm="12" :md="6">
              <a-card class="stat-card" :bordered="false">
                <div class="stat-content">
                  <div class="stat-icon today-clicks">
                    <calendar-outlined />
                  </div>
                  <div class="stat-info">
                    <div class="stat-value">{{ stats.todayClicks || 0 }}</div>
                    <div class="stat-label">今日访问</div>
                  </div>
                </div>
              </a-card>
            </a-col>

            <a-col :xs="24" :sm="12" :md="6">
              <a-card class="stat-card" :bordered="false">
                <div class="stat-content">
                  <div class="stat-icon created-time">
                    <clock-circle-outlined />
                  </div>
                  <div class="stat-info">
                    <div class="stat-value">{{ formatDate(stats.createdTime) }}</div>
                    <div class="stat-label">创建时间</div>
                  </div>
                </div>
              </a-card>
            </a-col>

            <a-col :xs="24" :sm="12" :md="6">
              <a-card class="stat-card" :bordered="false">
                <div class="stat-content">
                  <div class="stat-icon status">
                    <check-circle-outlined />
                  </div>
                  <div class="stat-info">
                    <div class="stat-value">{{ getStatusText(stats.status) }}</div>
                    <div class="stat-label">当前状态</div>
                  </div>
                </div>
              </a-card>
            </a-col>
          </a-row>

          <!-- 详细信息卡片 -->
          <a-card :bordered="false" class="detail-card">
            <template #title>
              <div class="card-title">
                <info-circle-outlined class="title-icon" />
                详细信息
              </div>
            </template>

            <a-descriptions bordered :column="1">
              <a-descriptions-item label="短网址Key">
                <a-tag color="blue">{{ stats.shortKey }}</a-tag>
              </a-descriptions-item>

              <a-descriptions-item label="完整短网址">
                <a :href="getFullShortUrl(stats.shortKey)" target="_blank">
                  {{ getFullShortUrl(stats.shortKey) }}
                </a>
              </a-descriptions-item>

              <a-descriptions-item label="原始网址">
                <a-tooltip :title="stats.originalUrl">
                  <a :href="stats.originalUrl" target="_blank">
                    {{ stats.originalUrl }}
                  </a>
                </a-tooltip>
              </a-descriptions-item>

              <a-descriptions-item label="网址标题" v-if="stats.title">
                {{ stats.title }}
              </a-descriptions-item>

              <a-descriptions-item label="创建时间">
                {{ formatDateTime(stats.createdTime) }}
              </a-descriptions-item>

              <a-descriptions-item label="状态">
                <a-tag :color="getStatusColor(stats.status)">
                  {{ getStatusText(stats.status) }}
                </a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-card>

          <!-- 访问趋势图表 -->
          <a-card :bordered="false" class="chart-card">
            <template #title>
              <div class="card-title">
                <line-chart-outlined class="title-icon" />
                访问趋势
              </div>
            </template>

            <div class="chart-container">
              <div ref="chartRef" class="chart"></div>
            </div>
          </a-card>

          <!-- 访问来源分析 -->
          <a-card :bordered="false" class="source-card">
            <template #title>
              <div class="card-title">
                <global-outlined class="title-icon" />
                访问来源分析
              </div>
            </template>

            <a-empty v-if="!accessSources || accessSources.length === 0" description="暂无访问来源数据" />

            <a-list v-else :data-source="accessSources" size="small">
              <template #renderItem="item">
                <a-list-item>
                  <a-list-item-meta>
                    <template #title>
                      <span>{{ item.source || '直接访问' }}</span>
                    </template>
                    <template #description>
                      <span>访问次数: {{ item.count }}</span>
                    </template>
                  </a-list-item-meta>
                  <template #actions>
                    <span>{{ ((item.count / stats.totalClicks) * 100).toFixed(1) }}%</span>
                  </template>
                </a-list-item>
              </template>
            </a-list>
          </a-card>

          <!-- 操作按钮 -->
          <div class="action-section">
            <a-space>
              <a-button @click="exportStats">
                <export-outlined />
                导出报告
              </a-button>

              <a-button @click="refreshStats" :loading="loading">
                <reload-outlined />
                刷新数据
              </a-button>

              <a-button type="primary" @click="openShortUrl(getFullShortUrl(stats.shortKey))">
                <export-outlined />
                访问短网址
              </a-button>
            </a-space>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-section">
          <a-empty
            description="请输入短网址Key查询统计信息"
            :image="emptyImage"
          >
            <template #image>
              <bar-chart-outlined class="empty-icon" />
            </template>
          </a-empty>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  LinkOutlined,
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  InfoCircleOutlined,
  LineChartOutlined,
  GlobalOutlined,
  ExportOutlined,
  BarChartOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { getUrlStats } from '@/api/url'
import { useUrlStore } from '@/stores/url'

const route = useRoute()
const router = useRouter()
const urlStore = useUrlStore()

// 响应式数据
const loading = ref(false)
const stats = ref(null)
const chartRef = ref(null)
const chartInstance = ref(null)
const accessSources = ref([])

const queryForm = reactive({
  shortKey: route.query.key || ''
})

// 方法
const handleQuery = async () => {
  if (!queryForm.shortKey) {
    message.warning('请输入短网址Key')
    return
  }

  await loadStats(queryForm.shortKey)
}

const resetQuery = () => {
  queryForm.shortKey = ''
  stats.value = null
  accessSources.value = []

  if (chartInstance.value) {
    chartInstance.value.dispose()
    chartInstance.value = null
  }
}

const loadStats = async (shortKey) => {
  try {
    loading.value = true

    const response = await getUrlStats(shortKey)
    stats.value = response

    // 缓存统计数据
    urlStore.setUrlStats(shortKey, response)

    // 模拟访问来源数据（实际应该从API获取）
    generateMockAccessSources(response.totalClicks)

    // 渲染图表
    await nextTick()
    renderChart()

    message.success('统计数据加载成功')

  } catch (error) {
    console.error('加载统计数据失败:', error)
    message.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

const refreshStats = async () => {
  if (stats.value) {
    await loadStats(stats.value.shortKey)
  }
}

const generateMockAccessSources = (totalClicks) => {
  if (!totalClicks || totalClicks === 0) {
    accessSources.value = []
    return
  }

  const sources = [
    { source: 'Google', count: Math.floor(totalClicks * 0.4) },
    { source: 'Baidu', count: Math.floor(totalClicks * 0.3) },
    { source: 'Direct', count: Math.floor(totalClicks * 0.2) },
    { source: 'Social Media', count: Math.floor(totalClicks * 0.1) }
  ]

  accessSources.value = sources.filter(item => item.count > 0)
}

const renderChart = () => {
  if (!chartRef.value || !stats.value) return

  // 销毁之前的图表实例
  if (chartInstance.value) {
    chartInstance.value.dispose()
  }

  // 创建新的图表实例
  chartInstance.value = echarts.init(chartRef.value)

  // 生成模拟的访问趋势数据
  const chartData = generateChartData()

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>访问次数: {c}'
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
      data: chartData.dates
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        name: '访问次数',
        type: 'line',
        smooth: true,
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
        },
        lineStyle: {
          color: '#1890ff',
          width: 2
        },
        itemStyle: {
          color: '#1890ff'
        },
        data: chartData.values
      }
    ]
  }

  chartInstance.value.setOption(option)

  // 响应式处理
  const handleResize = () => {
    chartInstance.value?.resize()
  }

  window.addEventListener('resize', handleResize)

  // 组件卸载时清理
  onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    chartInstance.value?.dispose()
  })
}

const generateChartData = () => {
  const dates = []
  const values = []
  const now = new Date()

  // 生成最近7天的数据
  for (let i = 6; i >= 0; i--) {
    const date = new Date(now)
    date.setDate(date.getDate() - i)

    dates.push(date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }))

    // 生成模拟数据
    const baseValue = Math.floor((stats.value.totalClicks || 0) / 7)
    const randomValue = Math.floor(Math.random() * baseValue * 0.5)
    values.push(baseValue + randomValue)
  }

  return { dates, values }
}

const exportStats = () => {
  if (!stats.value) return

  const data = {
    shortKey: stats.value.shortKey,
    originalUrl: stats.value.originalUrl,
    title: stats.value.title,
    totalClicks: stats.value.totalClicks,
    todayClicks: stats.value.todayClicks,
    createdTime: stats.value.createdTime,
    status: stats.value.status,
    accessSources: accessSources.value,
    exportTime: new Date().toISOString()
  }

  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)

  const link = document.createElement('a')
  link.href = url
  link.download = `stats-${stats.value.shortKey}-${new Date().toISOString().split('T')[0]}.json`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  URL.revokeObjectURL(url)

  message.success('统计数据已导出')
}

const openShortUrl = (url) => {
  window.open(url, '_blank')
}

const getFullShortUrl = (shortKey) => {
  return urlStore.getFullShortUrl(shortKey)
}

const formatDate = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleDateString('zh-CN')
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

const getStatusText = (status) => {
  switch (status) {
    case 1: return '正常'
    case 0: return '禁用'
    default: return '未知'
  }
}

const getStatusColor = (status) => {
  switch (status) {
    case 1: return 'success'
    case 0: return 'error'
    default: return 'default'
  }
}

// 空状态图标
const emptyImage = BarChartOutlined

// 生命周期
onMounted(() => {
  // 如果URL中有key参数，自动查询
  if (route.query.key) {
    queryForm.shortKey = route.query.key
    handleQuery()
  }
})

onUnmounted(() => {
  if (chartInstance.value) {
    chartInstance.value.dispose()
  }
})
</script>

<style scoped>
.stats-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.page-header {
  padding: 40px 0 20px;
  text-align: center;
  color: white;
}

.page-title {
  font-size: 2.5rem;
  font-weight: 600;
  margin: 0 0 10px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
}

.page-subtitle {
  font-size: 1.1rem;
  opacity: 0.9;
  margin: 0;
}

.page-content {
  padding: 20px 0 60px;
}

.query-section {
  margin-bottom: 24px;
}

.query-card {
  border-radius: 8px;
  background: white;
}

.stats-section {
  margin-bottom: 24px;
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 8px;
  background: white;
  transition: transform 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
}

.total-clicks {
  background: linear-gradient(135deg, #1890ff, #40a9ff);
}

.today-clicks {
  background: linear-gradient(135deg, #52c41a, #73d13d);
}

.created-time {
  background: linear-gradient(135deg, #faad14, #ffc53d);
}

.status {
  background: linear-gradient(135deg, #722ed1, #9254de);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}

.detail-card,
.chart-card,
.source-card {
  border-radius: 8px;
  background: white;
  margin-bottom: 24px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
}

.title-icon {
  font-size: 20px;
  color: #1890ff;
}

.chart-container {
  height: 400px;
  width: 100%;
}

.chart {
  width: 100%;
  height: 100%;
}

.action-section {
  text-align: center;
  padding: 20px;
}

.empty-section {
  text-align: center;
  padding: 80px 0;
  background: white;
  border-radius: 8px;
}

.empty-icon {
  font-size: 64px;
  color: #d9d9d9;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .page-title {
    font-size: 2rem;
  }

  .page-subtitle {
    font-size: 1rem;
  }

  .stat-content {
    flex-direction: column;
    text-align: center;
    gap: 12px;
  }

  .stat-value {
    font-size: 20px;
  }

  .chart-container {
    height: 300px;
  }

  .action-section {
    padding: 16px;
  }

  .query-card .ant-form {
    flex-direction: column;
    align-items: stretch;
  }

  .query-card .ant-form-item {
    margin-bottom: 16px;
  }
}
</style>