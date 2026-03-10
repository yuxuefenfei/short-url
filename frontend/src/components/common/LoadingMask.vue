<template>
  <a-spin
    v-if="isLoading"
    :spinning="isLoading"
    :tip="loadingTip"
    size="large"
    class="global-loading"
  >
    <div class="loading-content">
      <!-- 自定义加载动画 -->
      <div class="custom-spinner">
        <div class="spinner-ring"></div>
        <div class="spinner-ring"></div>
        <div class="spinner-ring"></div>
        <div class="spinner-ring"></div>
      </div>

      <!-- 加载文本 -->
      <div class="loading-text">{{ loadingTip || '加载中...' }}</div>

      <!-- 进度条（可选） -->
      <div v-if="showProgress" class="loading-progress">
        <a-progress
          :percent="progress"
          :showInfo="false"
          strokeColor="#1890ff"
        />
      </div>
    </div>
  </a-spin>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useUrlStore } from '@/stores/url'
import { useUserStore } from '@/stores/user'

// Props
const props = defineProps({
  // 是否显示加载遮罩
  visible: {
    type: Boolean,
    default: false
  },

  // 加载提示文本
  tip: {
    type: String,
    default: ''
  },

  // 是否显示进度条
  showProgress: {
    type: Boolean,
    default: false
  },

  // 进度百分比
  progress: {
    type: Number,
    default: 0,
    validator: (value) => value >= 0 && value <= 100
  }
})

// 状态管理
const urlStore = useUrlStore()
const userStore = useUserStore()

// 响应式数据
const localVisible = ref(props.visible)
const localTip = ref(props.tip)
const localProgress = ref(props.progress)

// 计算属性
const isLoading = computed(() => {
  return localVisible.value || urlStore.isLoading
})

const loadingTip = computed(() => {
  return localTip.value || '加载中...'
})

const showProgress = computed(() => {
  return props.showProgress
})

const progress = computed(() => {
  return localProgress.value
})

// 监听Props变化
watch(() => props.visible, (newValue) => {
  localVisible.value = newValue
})

watch(() => props.tip, (newValue) => {
  localTip.value = newValue
})

watch(() => props.progress, (newValue) => {
  localProgress.value = newValue
})

// 暴露方法给父组件
defineExpose({
  show: (tip = '') => {
    localTip.value = tip
    localVisible.value = true
  },

  hide: () => {
    localVisible.value = false
    localTip.value = ''
    localProgress.value = 0
  },

  updateProgress: (percent) => {
    localProgress.value = Math.max(0, Math.min(100, percent))
  }
})
</script>

<style scoped>
.global-loading {
  position: fixed !important;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(2px);
}

.loading-content {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  min-width: 200px;
}

.custom-spinner {
  position: relative;
  width: 50px;
  height: 50px;
  margin: 0 auto 20px;
}

.spinner-ring {
  box-sizing: border-box;
  position: absolute;
  width: 100%;
  height: 100%;
  border: 3px solid transparent;
  border-top-color: #1890ff;
  border-radius: 50%;
  animation: spinner-ring 1.2s cubic-bezier(0.5, 0, 0.5, 1) infinite;
}

.spinner-ring:nth-child(1) {
  animation-delay: -0.45s;
}

.spinner-ring:nth-child(2) {
  animation-delay: -0.3s;
}

.spinner-ring:nth-child(3) {
  animation-delay: -0.15s;
}

@keyframes spinner-ring {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.loading-text {
  color: #666;
  font-size: 14px;
  margin-bottom: 16px;
  font-weight: 500;
}

.loading-progress {
  width: 200px;
  margin: 0 auto;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .loading-content {
    min-width: 150px;
  }

  .custom-spinner {
    width: 40px;
    height: 40px;
  }

  .loading-text {
    font-size: 12px;
  }

  .loading-progress {
    width: 150px;
  }
}

/* 暗色主题支持 */
@media (prefers-color-scheme: dark) {
  .global-loading {
    background: rgba(0, 0, 0, 0.8);
  }

  .loading-text {
    color: #fff;
  }
}
</style>