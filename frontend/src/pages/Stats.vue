<template>
  <div class="stats-page">
    <header class="page-header">
      <div class="container">
        <h1 class="page-title">访问统计分析</h1>
        <p class="page-subtitle">查看短链的详细访问数据、趋势和浏览器分布</p>
      </div>
    </header>

    <main class="page-content">
      <div class="container">
        <div class="query-section">
          <a-card :bordered="false" class="query-card">
            <a-form layout="inline" :model="queryForm" @finish="handleQuery">
              <a-form-item name="shortKey" label="短链 Key">
                <a-input
                  v-model:value="queryForm.shortKey"
                  placeholder="请输入短链 Key"
                  style="width: 220px"
                  allow-clear
                >
                  <template #prefix>
                    <LinkOutlined />
                  </template>
                </a-input>
              </a-form-item>

              <a-form-item>
                <a-button type="primary" html-type="submit" :loading="loading">
                  <SearchOutlined />
                  查询统计
                </a-button>
              </a-form-item>

              <a-form-item>
                <a-button @click="resetQuery">
                  <ReloadOutlined />
                  重置
                </a-button>
              </a-form-item>
            </a-form>
          </a-card>
        </div>

        <div v-if="stats" class="stats-section">
          <a-row :gutter="[24, 24]" class="stats-row">
            <a-col :xs="24" :sm="12" :md="6">
              <a-card class="stat-card" :bordered="false">
                <div class="stat-content">
                  <div class="stat-icon total-clicks">
                    <EyeOutlined />
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
                    <CalendarOutlined />
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
                    <ClockCircleOutlined />
                  </div>
                  <div class="stat-info">
                    <div class="stat-value">{{ formatDate(stats.createdTime) }}</div>
                    <div class="stat-label">创建日期</div>
                  </div>
                </div>
              </a-card>
            </a-col>

            <a-col :xs="24" :sm="12" :md="6">
              <a-card class="stat-card" :bordered="false">
                <div class="stat-content">
                  <div class="stat-icon status">
                    <CheckCircleOutlined />
                  </div>
                  <div class="stat-info">
                    <div class="stat-value">{{ getStatusText(stats.status) }}</div>
                    <div class="stat-label">当前状态</div>
                  </div>
                </div>
              </a-card>
            </a-col>
          </a-row>

          <a-card :bordered="false" class="detail-card">
            <template #title>
              <div class="card-title">
                <InfoCircleOutlined class="title-icon" />
                详细信息
              </div>
            </template>

            <a-descriptions bordered :column="1">
              <a-descriptions-item label="短链 Key">
                <a-tag color="blue">{{ stats.shortKey }}</a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="完整短链">
                <a :href="stats.shortUrl" target="_blank" rel="noreferrer">
                  {{ stats.shortUrl }}
                </a>
              </a-descriptions-item>
              <a-descriptions-item label="原始网址">
                <a-tooltip :title="stats.originalUrl">
                  <a :href="stats.originalUrl" target="_blank" rel="noreferrer">
                    {{ stats.originalUrl }}
                  </a>
                </a-tooltip>
              </a-descriptions-item>
              <a-descriptions-item v-if="stats.title" label="网址标题">
                {{ stats.title }}
              </a-descriptions-item>
              <a-descriptions-item label="创建时间">
                {{ formatDateTime(stats.createdTime) }}
              </a-descriptions-item>
              <a-descriptions-item label="最近访问">
                {{ formatDateTime(stats.lastAccessTime) }}
              </a-descriptions-item>
              <a-descriptions-item label="状态">
                <a-tag :color="getStatusColor(stats.status)">
                  {{ getStatusText(stats.status) }}
                </a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-card>

          <a-card :bordered="false" class="chart-card">
            <template #title>
              <div class="card-title">
                <LineChartOutlined class="title-icon" />
                最近 7 天访问趋势
              </div>
            </template>
            <div class="chart-container">
              <div ref="chartRef" class="chart"></div>
            </div>
          </a-card>

          <a-card :bordered="false" class="source-card">
            <template #title>
              <div class="card-title">
                <GlobalOutlined class="title-icon" />
                浏览器分布
              </div>
            </template>

            <a-empty v-if="!accessSources.length" description="暂无来源分析数据" />

            <a-list v-else :data-source="accessSources" size="small">
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta>
                    <template #title>
                      <span>{{ item.source || 'Unknown' }}</span>
                    </template>
                    <template #description>
                      <span>访问次数：{{ item.count }}</span>
                    </template>
                  </a-list-item-meta>
                  <template #actions>
                    <span>{{ getSourcePercent(item.count) }}%</span>
                  </template>
                </a-list-item>
              </template>
            </a-list>
          </a-card>

          <div class="action-section">
            <a-space>
              <a-button @click="exportStats">
                <ExportOutlined />
                导出报告
              </a-button>
              <a-button :loading="loading" @click="refreshStats">
                <ReloadOutlined />
                刷新数据
              </a-button>
              <a-button type="primary" @click="openShortUrl(stats.shortUrl)">
                <ExportOutlined />
                访问短链
              </a-button>
            </a-space>
          </div>
        </div>

        <div v-else class="empty-section">
          <a-empty description="请输入短链 Key 查询统计信息">
            <template #image>
              <BarChartOutlined class="empty-icon" />
            </template>
          </a-empty>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import echarts from '@/utils/echarts'
import {
  BarChartOutlined,
  CalendarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  ExportOutlined,
  EyeOutlined,
  GlobalOutlined,
  InfoCircleOutlined,
  LineChartOutlined,
  LinkOutlined,
  ReloadOutlined,
  SearchOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { getUrlStats } from '@/api/url'

const route = useRoute()

const loading = ref(false)
const stats = ref(null)
const accessSources = ref([])
const chartRef = ref(null)
const chartInstance = ref(null)

const queryForm = reactive({
  shortKey: route.query.key || ''
})

const handleResize = () => {
  chartInstance.value?.resize()
}

const handleQuery = async () => {
  if (!queryForm.shortKey) {
    message.warning('请输入短链 Key')
    return
  }

  await loadStats(queryForm.shortKey)
}

const resetQuery = () => {
  queryForm.shortKey = ''
  stats.value = null
  accessSources.value = []
  chartInstance.value?.dispose()
  chartInstance.value = null
}

const loadStats = async (shortKey) => {
  try {
    loading.value = true
    const response = await getUrlStats(shortKey)
    if (response.code !== 200) {
      return
    }

    stats.value = response.data
    accessSources.value = response.data?.accessSources || []

    await nextTick()
    renderChart()
    message.success('统计数据加载成功')
  } catch (error) {
    console.error('Failed to load stats:', error)
    message.error(error?.message || '加载统计数据失败')
  } finally {
    loading.value = false
  }
}

const refreshStats = async () => {
  if (stats.value?.shortKey) {
    await loadStats(stats.value.shortKey)
  }
}

const renderChart = () => {
  if (!chartRef.value || !stats.value) {
    return
  }

  chartInstance.value?.dispose()
  chartInstance.value = echarts.init(chartRef.value)

  const trend = stats.value.trend || []
  chartInstance.value.setOption({
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
      data: trend.map((item) => item.date)
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
        data: trend.map((item) => item.clicks),
        lineStyle: {
          color: '#1890ff',
          width: 2
        },
        itemStyle: {
          color: '#1890ff'
        },
        areaStyle: {
          color: 'rgba(24, 144, 255, 0.12)'
        }
      }
    ]
  })
}

const exportStats = () => {
  if (!stats.value) {
    return
  }

  const data = {
    ...stats.value,
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

  message.success('统计报告已导出')
}

const openShortUrl = (url) => {
  window.open(url, '_blank', 'noopener,noreferrer')
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
  if (status === 1) return '正常'
  if (status === 0) return '禁用'
  return '未知'
}

const getStatusColor = (status) => {
  if (status === 1) return 'success'
  if (status === 0) return 'error'
  return 'default'
}

const getSourcePercent = (count) => {
  const total = stats.value?.totalClicks || 0
  if (!total) {
    return '0.0'
  }
  return ((count / total) * 100).toFixed(1)
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  if (route.query.key) {
    queryForm.shortKey = route.query.key
    handleQuery()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance.value?.dispose()
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
  margin: 0 0 10px;
  font-size: 2.5rem;
  font-weight: 600;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
}

.page-subtitle {
  margin: 0;
  font-size: 1.1rem;
  opacity: 0.9;
}

.page-content {
  padding: 20px 0 60px;
}

.query-section,
.stats-section {
  margin-bottom: 24px;
}

.query-card,
.stat-card,
.detail-card,
.chart-card,
.source-card,
.empty-section {
  border-radius: 8px;
  background: white;
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
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
  margin-top: 4px;
  font-size: 12px;
  color: #8c8c8c;
}

.detail-card,
.chart-card,
.source-card {
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
  width: 100%;
  height: 400px;
}

.chart {
  width: 100%;
  height: 100%;
}

.action-section {
  padding: 20px;
  text-align: center;
}

.empty-section {
  padding: 80px 0;
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  color: #d9d9d9;
}

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

