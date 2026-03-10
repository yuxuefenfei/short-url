<template>
  <div class="notification-container">
    <!-- 全局通知容器 -->
    <div id="global-notification"></div>

    <!-- 快捷操作提示 -->
    <div v-if="showQuickTips" class="quick-tips">
      <a-alert
        message="使用提示"
        description="输入长网址，点击生成即可获得短网址。支持访问统计和数据分析。"
        type="info"
        closable
        @close="closeQuickTips"
        banner
      />
    </div>

    <!-- 系统维护通知 -->
    <div v-if="showMaintenanceNotice" class="maintenance-notice">
      <a-alert
        message="系统维护通知"
        description="系统将于今晚22:00-23:00进行维护升级，期间可能影响正常使用。"
        type="warning"
        closable
        @close="closeMaintenanceNotice"
        banner
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

// 响应式数据
const showQuickTips = ref(true)
const showMaintenanceNotice = ref(false)

// 方法
const closeQuickTips = () => {
  showQuickTips.value = false
  localStorage.setItem('hideQuickTips', 'true')
}

const closeMaintenanceNotice = () => {
  showMaintenanceNotice.value = false
  localStorage.setItem('hideMaintenanceNotice', 'true')
}

// 生命周期
onMounted(() => {
  // 检查用户是否已经关闭过提示
  const hideQuickTips = localStorage.getItem('hideQuickTips')
  if (hideQuickTips === 'true') {
    showQuickTips.value = false
  }

  const hideMaintenanceNotice = localStorage.getItem('hideMaintenanceNotice')
  if (hideMaintenanceNotice !== 'true') {
    // 可以添加逻辑来判断是否需要显示维护通知
    // 例如：检查维护时间、用户权限等
    showMaintenanceNotice.value = false
  }

  // 可以在这里添加其他通知逻辑
  // 例如：版本更新通知、新功能介绍等
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

/* 确保通知不被其他元素遮挡 */
:deep(.ant-alert) {
  border-radius: 0;
  margin-bottom: 0;
}

:deep(.ant-alert-banner) {
  margin-bottom: 0;
}

/* 移动端适配 */
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