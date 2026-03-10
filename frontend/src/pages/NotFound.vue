<template>
  <div class="not-found">
    <div class="container">
      <div class="error-code">404</div>
      <div class="error-title">页面未找到</div>
      <div class="error-description">
        抱歉，您访问的页面不存在或已被移除。
        请检查URL是否正确，或者返回首页继续浏览。
      </div>
      <div class="actions">
        <a-button type="primary" size="large" @click="goHome">
          <HomeOutlined />
          返回首页
        </a-button>
        <a-button size="large" @click="goBack" style="margin-left: 16px;">
          <ArrowLeftOutlined />
          返回上页
        </a-button>
      </div>
      <div class="search-box">
        <div class="search-title">或者尝试搜索：</div>
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索功能、页面或内容..."
          enter-button="搜索"
          size="large"
          @search="handleSearch"
          style="max-width: 400px; margin: 0 auto;"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { HomeOutlined, ArrowLeftOutlined } from '@ant-design/icons-vue'

const router = useRouter()
const searchKeyword = ref('')

/**
 * 返回首页
 */
const goHome = () => {
  router.push('/')
}

/**
 * 返回上页
 */
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

/**
 * 处理搜索
 */
const handleSearch = () => {
  if (!searchKeyword.value.trim()) {
    message.warning('请输入搜索关键词')
    return
  }

  // 根据关键词跳转到相应页面
  const keyword = searchKeyword.value.toLowerCase()

  if (keyword.includes('管理') || keyword.includes('后台')) {
    router.push('/admin')
  } else if (keyword.includes('统计') || keyword.includes('数据')) {
    router.push('/stats')
  } else if (keyword.includes('登录')) {
    router.push('/login')
  } else if (keyword.includes('生成') || keyword.includes('短网址')) {
    router.push('/')
  } else {
    message.info('未找到相关页面，请尝试其他关键词')
  }
}
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.container {
  text-align: center;
  max-width: 600px;
  background: rgba(255, 255, 255, 0.95);
  padding: 60px 40px;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  color: #ff4d4f;
  line-height: 1;
  margin-bottom: 20px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
  background: linear-gradient(45deg, #ff4d4f, #ff7875);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.error-title {
  font-size: 32px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 16px;
}

.error-description {
  font-size: 16px;
  color: #8c8c8c;
  line-height: 1.6;
  margin-bottom: 40px;
}

.actions {
  margin-bottom: 40px;
}

.search-box {
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.search-title {
  font-size: 14px;
  color: #595959;
  margin-bottom: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .container {
    padding: 40px 20px;
    margin: 20px;
  }

  .error-code {
    font-size: 80px;
  }

  .error-title {
    font-size: 24px;
  }

  .error-description {
    font-size: 14px;
  }

  .actions {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .actions .ant-btn {
    width: 100%;
    max-width: 200px;
    margin: 8px 0 !important;
  }
}

@media (max-width: 480px) {
  .container {
    padding: 30px 15px;
  }

  .error-code {
    font-size: 60px;
  }

  .error-title {
    font-size: 20px;
  }

  .error-description {
    font-size: 13px;
  }
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.container {
  animation: fadeIn 0.6s ease-out;
}

.error-code {
  animation: fadeIn 0.8s ease-out 0.2s both;
}

.error-title {
  animation: fadeIn 0.8s ease-out 0.4s both;
}

.error-description {
  animation: fadeIn 0.8s ease-out 0.6s both;
}

.actions {
  animation: fadeIn 0.8s ease-out 0.8s both;
}

.search-box {
  animation: fadeIn 0.8s ease-out 1s both;
}
</style>