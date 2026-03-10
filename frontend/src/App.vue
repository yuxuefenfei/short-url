<template>
  <a-config-provider :locale="zhCN">
    <div id="app">
      <!-- 全局通知 -->
      <notification-container />

      <!-- 路由视图 -->
      <router-view v-slot="{ Component, route }">
        <transition name="fade" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>

      <!-- 全局加载遮罩 -->
      <loading-mask />

      <!-- 回到顶部按钮 -->
      <a-back-top :visibilityHeight="400" />
    </div>
  </a-config-provider>
</template>

<script setup>
import { provide, computed } from 'vue'
import { ConfigProvider } from 'ant-design-vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import NotificationContainer from '@/components/common/NotificationContainer.vue'
import LoadingMask from '@/components/common/LoadingMask.vue'
import { useUserStore } from '@/stores/user'
import { useUrlStore } from '@/stores/url'

// 全局提供状态管理
const userStore = useUserStore()
const urlStore = useUrlStore()

provide('userStore', userStore)
provide('urlStore', urlStore)

// 应用配置
const appConfig = computed(() => ({
  version: '1.0.0',
  name: '短网址管理系统',
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '/api',
  shortUrlDomain: import.meta.env.VITE_SHORT_URL_DOMAIN || 'https://short.ly'
}))

provide('appConfig', appConfig)

// 全局错误处理
const handleError = (error) => {
  console.error('应用级错误:', error)

  // TODO: 集成错误监控服务
  // 例如：Sentry、LogRocket等
}

// 提供错误处理方法
provide('handleError', handleError)
</script>

<style scoped>
#app {
  min-height: 100vh;
  background-color: #f5f7fa;
}

/* 路由切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 全局滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 响应式适配 */
@media (max-width: 768px) {
  #app {
    font-size: 14px;
  }
}
</style>