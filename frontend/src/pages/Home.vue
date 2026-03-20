<template>
  <div class="home-page">
    <!-- 页面头部 -->
    <header class="page-header">
      <div class="container">
        <h1 class="page-title">短网址生成器</h1>
        <p class="page-subtitle">将长网址转换为简短易记的短网址</p>
      </div>
    </header>

    <!-- 主要内容区域 -->
    <main class="page-content">
      <div class="container">
        <!-- 短网址生成表单 -->
        <div class="form-section">
          <a-card class="generator-card" :bordered="false">
            <template #title>
              <div class="card-title">
                <link-outlined class="title-icon" />
                生成短网址
              </div>
            </template>

            <a-form
              :model="form"
              :rules="formRules"
              ref="formRef"
              @finish="handleSubmit"
              layout="vertical"
            >
              <a-form-item name="originalUrl" label="原始网址" required>
                <a-input
                  v-model:value="form.originalUrl"
                  placeholder="请输入长网址，例如：https://example.com/very/long/url"
                  :maxLength="2048"
                  showCount
                  size="large"
                >
                  <template #prefix>
                    <global-outlined />
                  </template>
                </a-input>
              </a-form-item>

              <a-form-item name="title" label="网址标题（可选）">
                <a-input
                  v-model:value="form.title"
                  placeholder="为网址添加描述性标题"
                  :maxLength="255"
                  showCount
                  size="large"
                >
                  <template #prefix>
                    <tag-outlined />
                  </template>
                </a-input>
              </a-form-item>

              <a-form-item>
                <a-button
                  type="primary"
                  html-type="submit"
                  :loading="loading"
                  size="large"
                  block
                >
                  <template #icon>
                    <thunderbolt-outlined />
                  </template>
                  生成短网址
                </a-button>
              </a-form-item>
            </a-form>

            <!-- 生成结果展示 -->
            <div v-if="result" class="result-section">
              <a-divider>生成结果</a-divider>

              <a-alert
                message="短网址生成成功！"
                type="success"
                show-icon
                class="success-alert"
              />

              <div class="result-content">
                <div class="short-url-display">
                  <div class="url-item">
                    <span class="label">短网址：</span>
                    <a-input
                      :value="result.shortUrl"
                      readonly
                      size="large"
                      class="short-url-input"
                    >
                      <template #addonAfter>
                        <copy-outlined @click="copyToClipboard(result.shortUrl)" class="copy-icon" />
                      </template>
                    </a-input>
                  </div>

                  <div class="url-item">
                    <span class="label">短网址Key：</span>
                    <a-tag color="blue" class="short-key-tag">
                      {{ result.shortKey }}
                    </a-tag>
                  </div>

                  <div class="url-item">
                    <span class="label">原始网址：</span>
                    <a-tooltip :title="result.originalUrl">
                      <span class="original-url">{{ result.originalUrl }}</span>
                    </a-tooltip>
                  </div>

                  <div class="url-item" v-if="result.title">
                    <span class="label">标题：</span>
                    <span class="url-title">{{ result.title }}</span>
                  </div>

                  <div class="url-item">
                    <span class="label">创建时间：</span>
                    <span class="create-time">{{ formatDateTime(result.createdTime) }}</span>
                  </div>
                </div>

                <!-- 操作按钮 -->
                <div class="action-buttons">
                  <a-button type="default" @click="viewStats(result.shortKey)">
                    <bar-chart-outlined />
                    查看统计
                  </a-button>

                  <a-button type="default" @click="resetForm">
                    <reload-outlined />
                    重新生成
                  </a-button>

                  <a-button type="primary" @click="openShortUrl(result.shortUrl)">
                    <export-outlined />
                    访问短网址
                  </a-button>
                </div>
              </div>
            </div>
          </a-card>
        </div>

        <!-- 最近生成的短网址 -->
        <div class="recent-section" v-if="hasRecentUrls">
          <a-card :bordered="false" class="recent-card">
            <template #title>
              <div class="card-title">
                <history-outlined class="title-icon" />
                最近生成
              </div>
            </template>

            <div class="recent-list">
              <div
                v-for="item in recentUrls.slice(0, 5)"
                :key="item.shortKey"
                class="recent-item"
              >
                <div class="item-content">
                  <div class="item-url">
                    <a :href="item.shortUrl || '#'" target="_blank">
                      {{ item.shortUrl || item.shortKey }}
                    </a>
                  </div>
                  <div class="item-original" :title="item.originalUrl">
                    {{ item.originalUrl }}
                  </div>
                  <div class="item-time">
                    {{ formatDateTime(item.createdTime) }}
                  </div>
                </div>
                <div class="item-actions">
                  <a-button type="link" size="small" @click="viewStats(item.shortKey)">
                    统计
                  </a-button>
                  <a-button type="link" size="small" @click="copyToClipboard(item.shortUrl || item.shortKey)">
                    复制
                  </a-button>
                </div>
              </div>
            </div>

            <div class="recent-footer">
              <a-button type="link" @click="clearRecentUrls">
                清除历史记录
              </a-button>
            </div>
          </a-card>
        </div>

        <!-- 功能介绍 -->
        <div class="features-section">
          <a-row :gutter="[24, 24]">
            <a-col :xs="24" :sm="8">
              <a-card class="feature-card" :bordered="false">
                <link-outlined class="feature-icon" />
                <h3>快速生成</h3>
                <p>几秒钟内将长网址转换为简短的短网址</p>
              </a-card>
            </a-col>

            <a-col :xs="24" :sm="8">
              <a-card class="feature-card" :bordered="false">
                <bar-chart-outlined class="feature-icon" />
                <h3>访问统计</h3>
                <p>详细的访问数据统计和分析报告</p>
              </a-card>
            </a-col>

            <a-col :xs="24" :sm="8">
              <a-card class="feature-card" :bordered="false">
                <safety-outlined class="feature-icon" />
                <h3>安全可靠</h3>
                <p>多层安全防护，保障链接安全有效</p>
              </a-card>
            </a-col>
          </a-row>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  LinkOutlined,
  GlobalOutlined,
  TagOutlined,
  ThunderboltOutlined,
  CopyOutlined,
  BarChartOutlined,
  ReloadOutlined,
  ExportOutlined,
  HistoryOutlined,
  SafetyOutlined
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { shortenUrl } from '@/api/url'
import { useUrlStore } from '@/stores/url'

const router = useRouter()
const urlStore = useUrlStore()

// 表单数据
const formRef = ref(null)
const loading = ref(false)
const result = ref(null)

const form = reactive({
  originalUrl: '',
  title: ''
})

// 表单验证规则
const formRules = {
  originalUrl: [
    { required: true, message: '请输入原始网址', trigger: 'blur' },
    {
      pattern: /^https?:\/\/.+/,
      message: '请输入有效的网址（以http://或https://开头）',
      trigger: 'blur'
    }
  ]
}

// 计算属性
const recentUrls = computed(() => urlStore.recentUrls)
const hasRecentUrls = computed(() => urlStore.hasRecentUrls)

// 方法
const handleSubmit = async () => {
  try {
    loading.value = true

    const response = await shortenUrl(form)
    const payload = response.data
    result.value = {
      ...payload,
      shortUrl: payload.shortUrl,
      createdTime: new Date().toISOString()
    }

    // 添加到最近使用列表
    urlStore.addRecentUrl({
      shortKey: payload.shortKey,
      shortUrl: payload.shortUrl,
      originalUrl: form.originalUrl,
      title: form.title,
      createdTime: result.value.createdTime
    })

    message.success('短网址生成成功！')

  } catch (error) {
    console.error('生成短网址失败:', error)
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
  result.value = null
}

const copyToClipboard = async (text) => {
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    message.error('复制失败，请手动复制')
  }
}

const viewStats = (shortKey) => {
  router.push({ name: 'Stats', query: { key: shortKey } })
}

const openShortUrl = (url) => {
  window.open(url, '_blank')
}

const clearRecentUrls = () => {
  Modal.confirm({
    title: '确认清除',
    content: '确定要清除所有历史记录吗？此操作不可恢复。',
    onOk: () => {
      urlStore.clearRecentUrls()
      message.success('历史记录已清除')
    }
  })
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''

  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 生命周期
onMounted(() => {
  // 可以在这里添加页面初始化逻辑
})
</script>

<style scoped>
.home-page {
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

.form-section {
  margin-bottom: 40px;
}

.generator-card {
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  background: white;
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

.result-section {
  margin-top: 24px;
}

.success-alert {
  margin-bottom: 20px;
}

.result-content {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
}

.short-url-display {
  margin-bottom: 20px;
}

.url-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  gap: 12px;
}

.url-item .label {
  font-weight: 600;
  min-width: 80px;
  color: #595959;
}

.short-url-input {
  flex: 1;
}

.copy-icon {
  cursor: pointer;
  color: #1890ff;
  font-size: 16px;
}

.copy-icon:hover {
  color: #40a9ff;
}

.short-key-tag {
  font-family: monospace;
  font-weight: 600;
}

.original-url {
  color: #8c8c8c;
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.url-title {
  color: #52c41a;
  font-weight: 500;
}

.create-time {
  color: #8c8c8c;
  font-size: 12px;
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.recent-section {
  margin-bottom: 40px;
}

.recent-card {
  border-radius: 8px;
  background: white;
}

.recent-list {
  margin-bottom: 16px;
}

.recent-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.recent-item:last-child {
  border-bottom: none;
}

.item-content {
  flex: 1;
}

.item-url {
  font-weight: 600;
  margin-bottom: 4px;
}

.item-url a {
  color: #1890ff;
}

.item-url a:hover {
  color: #40a9ff;
}

.item-original {
  color: #8c8c8c;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}

.item-time {
  color: #bfbfbf;
  font-size: 11px;
  margin-top: 2px;
}

.item-actions {
  display: flex;
  gap: 8px;
}

.recent-footer {
  text-align: center;
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}

.features-section {
  margin-top: 40px;
}

.feature-card {
  text-align: center;
  padding: 24px;
  border-radius: 8px;
  background: white;
  transition: transform 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-4px);
}

.feature-icon {
  font-size: 32px;
  color: #1890ff;
  margin-bottom: 16px;
}

.feature-card h3 {
  margin: 0 0 8px;
  color: #2c3e50;
}

.feature-card p {
  color: #8c8c8c;
  margin: 0;
  font-size: 14px;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .page-title {
    font-size: 2rem;
  }

  .page-subtitle {
    font-size: 1rem;
  }

  .action-buttons {
    flex-direction: column;
  }

  .url-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .url-item .label {
    min-width: auto;
  }

  .item-original {
    max-width: 200px;
  }

  .recent-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .item-actions {
    align-self: flex-end;
  }
}
</style>
