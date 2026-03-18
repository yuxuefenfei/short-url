<template>
  <div class="notification-container">
    <div id="global-notification"></div>

    <div v-if="showQuickTips" class="quick-tips">
      <a-alert
        message="使用提示"
        description="输入长网址并点击生成，即可获得短链地址，同时支持访问统计和数据分析。"
        type="info"
        closable
        banner
        @close="closeQuickTips"
      />
    </div>

    <div v-if="showMaintenanceNotice" class="maintenance-notice">
      <a-alert
        message="系统维护通知"
        description="系统将于今晚 22:00 - 23:00 进行维护升级，期间可能短暂影响正常使用。"
        type="warning"
        closable
        banner
        @close="closeMaintenanceNotice"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'

const showQuickTips = ref(true)
const showMaintenanceNotice = ref(false)

const closeQuickTips = () => {
  showQuickTips.value = false
  localStorage.setItem('hideQuickTips', 'true')
}

const closeMaintenanceNotice = () => {
  showMaintenanceNotice.value = false
  localStorage.setItem('hideMaintenanceNotice', 'true')
}

onMounted(() => {
  if (localStorage.getItem('hideQuickTips') === 'true') {
    showQuickTips.value = false
  }

  if (localStorage.getItem('hideMaintenanceNotice') !== 'true') {
    showMaintenanceNotice.value = false
  }
})
</script>

<style scoped>
.notification-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
}

#global-notification {
  position: relative;
}

.quick-tips,
.maintenance-notice {
  margin: 0;
}

:deep(.ant-alert) {
  border-radius: 0;
  margin-bottom: 0;
}

:deep(.ant-alert-banner) {
  margin-bottom: 0;
}

@media (max-width: 768px) {
  .notification-container {
    font-size: 14px;
  }

  :deep(.ant-alert-message) {
    font-size: 14px;
  }

  :deep(.ant-alert-description) {
    font-size: 12px;
  }
}
</style>
