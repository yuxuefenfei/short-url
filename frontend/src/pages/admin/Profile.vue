<template>
  <div class="profile-page">
    <AdminPageHeader title="个人资料" subtitle="查看当前登录账号与权限信息" />

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="8">
        <a-card class="profile-card">
          <div class="profile-summary">
            <a-avatar :size="72" class="profile-avatar">
              {{ avatarText }}
            </a-avatar>
            <h2>{{ userInfo?.username || "未登录用户" }}</h2>
            <a-tag :color="userInfo?.role === 'ADMIN' ? 'gold' : 'blue'">
              {{ userInfo?.role === "ADMIN" ? "管理员" : "普通用户" }}
            </a-tag>
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="16">
        <a-card title="账号信息" class="info-card">
          <a-descriptions bordered :column="1">
            <a-descriptions-item label="用户 ID">{{
              userInfo?.id || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="用户名">{{
              userInfo?.username || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="邮箱">{{
              userInfo?.email || "未设置"
            }}</a-descriptions-item>
            <a-descriptions-item label="角色">{{
              userInfo?.role || "-"
            }}</a-descriptions-item>
            <a-descriptions-item label="登录状态">
              <a-tag color="success">已登录</a-tag>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useUserStore } from "@/stores/user";
import AdminPageHeader from "@/components/admin/AdminPageHeader.vue";

const userStore = useUserStore();
const userInfo = computed(() => userStore.userInfo);
const avatarText = computed(() =>
  (userInfo.value?.username || "?").charAt(0).toUpperCase(),
);
</script>

<style scoped>
.profile-page {
  min-height: 100%;
  padding: 4px;
}

.profile-card,
.info-card {
  border-radius: 8px;
}

.profile-summary {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px 0;
}

.profile-summary h2 {
  margin: 8px 0 0;
  color: #262626;
}

.profile-avatar {
  background: #1677ff;
  font-size: 28px;
}

@media (max-width: 768px) {
  .profile-page {
    padding: 0;
  }
}
</style>
