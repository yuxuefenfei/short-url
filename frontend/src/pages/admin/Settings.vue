<template>
  <div class="settings-page">
    <AdminPageHeader
      title="系统设置"
      subtitle="查看前端运行配置和后台入口状态"
    />

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="12">
        <a-card title="运行配置" class="settings-card">
          <a-descriptions bordered :column="1">
            <a-descriptions-item label="应用名称"
              >短网址管理系统</a-descriptions-item
            >
            <a-descriptions-item label="前端版本">1.0.0</a-descriptions-item>
            <a-descriptions-item label="接口地址">{{
              apiBaseUrl
            }}</a-descriptions-item>
            <a-descriptions-item label="构建模式">{{
              mode
            }}</a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="12">
        <a-card title="快捷入口" class="settings-card">
          <a-space direction="vertical" class="quick-actions">
            <a-button block @click="go('/admin/dashboard')"
              >打开数据看板</a-button
            >
            <a-button block @click="go('/admin/urls')">管理短网址</a-button>
            <a-button block @click="go('/admin/logs')">查看操作日志</a-button>
          </a-space>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";
import AdminPageHeader from "@/components/admin/AdminPageHeader.vue";

const router = useRouter();
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "/";
const mode = import.meta.env.MODE;

const go = (path) => {
  router.push(path);
};
</script>

<style scoped>
.settings-page {
  min-height: 100%;
  padding: 4px;
}

.settings-card {
  border-radius: 8px;
}

.quick-actions {
  width: 100%;
}

@media (max-width: 768px) {
  .settings-page {
    padding: 0;
  }
}
</style>
