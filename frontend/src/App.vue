<template>
  <a-config-provider :locale="zhCN">
    <div id="app">
      <notification-container />

      <router-view v-slot="{ Component, route }">
        <transition name="fade" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>

      <loading-mask />
      <a-back-top :visibility-height="400" />
    </div>
  </a-config-provider>
</template>

<script setup>
import { computed, provide } from 'vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import NotificationContainer from '@/components/common/NotificationContainer.vue'
import LoadingMask from '@/components/common/LoadingMask.vue'
import { useUserStore } from '@/stores/user'
import { useUrlStore } from '@/stores/url'

const userStore = useUserStore()
const urlStore = useUrlStore()

provide('userStore', userStore)
provide('urlStore', urlStore)

const appConfig = computed(() => ({
  version: '1.0.0',
  name: '短网址管理系统',
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '/api',
  shortUrlDomain: import.meta.env.VITE_SHORT_URL_DOMAIN || 'https://short.ly'
}))

provide('appConfig', appConfig)

const handleError = (error) => {
  console.error('应用级错误:', error)
}

provide('handleError', handleError)
</script>

<style scoped>
#app {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

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

@media (max-width: 768px) {
  #app {
    font-size: 14px;
  }
}
</style>
