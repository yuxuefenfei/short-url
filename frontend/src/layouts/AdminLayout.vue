<template>
  <a-layout class="admin-layout">
    <!-- 侧边栏 -->
    <a-layout-sider
      v-model:collapsed="collapsed"
      collapsible
      class="admin-sider"
      :trigger="null"
    >
      <div class="logo">
        <LinkIcon v-if="collapsed" class="collapsed-logo" />
        <span v-else class="logo-text">短网址管理</span>
      </div>

      <a-menu
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        theme="dark"
        mode="inline"
        :items="menuItems"
        @click="handleMenuClick"
      />
    </a-layout-sider>

    <!-- 主内容区 -->
    <a-layout class="admin-content">
      <!-- 顶部导航栏 -->
      <a-layout-header class="admin-header">
        <div class="header-left">
          <menu-unfold-outlined
            v-if="collapsed"
            class="trigger"
            @click="toggleCollapsed"
          />
          <menu-fold-outlined v-else class="trigger" @click="toggleCollapsed" />

          <a-breadcrumb class="breadcrumb">
            <a-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.title }}
            </a-breadcrumb-item>
          </a-breadcrumb>
        </div>

        <div class="header-right">
          <a-space :size="16">
            <!-- 系统状态指示器 -->
            <div class="system-status">
              <a-tooltip title="系统状态">
                <div class="status-indicator" :class="systemStatusClass">
                  <CheckCircleOutlined v-if="systemStatus === 'healthy'" />
                  <ExclamationCircleOutlined v-else-if="systemStatus === 'warning'" />
                  <CloseCircleOutlined v-else />
                </div>
              </a-tooltip>
            </div>

            <!-- 用户菜单 -->
            <a-dropdown>
              <div class="user-info">
                <a-avatar size="small" class="user-avatar">
                  <template #icon><UserOutlined /></template>
                </a-avatar>
                <span class="user-name">{{ userStore.userInfo?.username }}</span>
                <DownOutlined class="dropdown-arrow" />
              </div>

              <template #overlay>
                <a-menu>
                  <a-menu-item key="profile" @click="goToProfile">
                    <UserOutlined />
                    个人资料
                  </a-menu-item>
                  <a-menu-item key="settings" @click="goToSettings">
                    <SettingOutlined />
                    系统设置
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="logout" @click="handleLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </div>
      </a-layout-header>

      <!-- 主内容 -->
      <a-layout-content class="admin-main">
        <div class="content-wrapper">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </a-layout-content>

      <!-- 页脚 -->
      <a-layout-footer class="admin-footer">
        <div class="footer-content">
          <p>© 2024 短网址管理系统. All rights reserved.</p>
          <p class="version">Version 1.0.0</p>
        </div>
      </a-layout-footer>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { message, Modal } from 'ant-design-vue'
import {
  MenuUnfoldOutlined,
  MenuFoldOutlined,
  UserOutlined,
  SettingOutlined,
  LogoutOutlined,
  DownOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  CloseCircleOutlined
} from '@ant-design/icons-vue'
import { logout } from '@/api/auth'

// 图标组件
const LinkIcon = {
  template: `
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>
  `
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 侧边栏状态
const collapsed = ref(false)
const selectedKeys = ref(['dashboard'])
const openKeys = ref(['sub1'])

// 系统状态 (healthy: 正常, warning: 警告, error: 错误)
const systemStatus = ref('healthy')

// 计算属性
const systemStatusClass = computed(() => {
  return {
    'status-healthy': systemStatus.value === 'healthy',
    'status-warning': systemStatus.value === 'warning',
    'status-error': systemStatus.value === 'error'
  }
})

// 面包屑导航
const breadcrumbs = computed(() => {
  const matched = route.matched
  return matched.map(item => ({
    path: item.path,
    title: item.meta?.title || item.name || '未知页面'
  })).filter(item => item.title)
})

// 菜单项配置
const menuItems = reactive([
  {
    key: 'dashboard',
    icon: () => h(AreaChartOutlined),
    label: '数据面板',
    title: '数据面板'
  },
  {
    key: 'urls',
    icon: () => h(LinkOutlined),
    label: '短网址管理',
    title: '短网址管理'
  },
  {
    key: 'users',
    icon: () => h(TeamOutlined),
    label: '用户管理',
    title: '用户管理'
  },
  {
    key: 'logs',
    icon: () => h(FileTextOutlined),
    label: '操作日志',
    title: '操作日志'
  },
  {
    key: 'settings',
    icon: () => h(SettingOutlined),
    label: '系统设置',
    title: '系统设置'
  }
])

// 需要导入的图标
import {
  AreaChartOutlined,
  LinkOutlined,
  TeamOutlined,
  FileTextOutlined
} from '@ant-design/icons-vue'

import { h } from 'vue'

/**
 * 切换侧边栏折叠状态
 */
const toggleCollapsed = () => {
  collapsed.value = !collapsed.value
}

/**
 * 处理菜单点击
 */
const handleMenuClick = ({ key }) => {
  selectedKeys.value = [key]

  // 根据菜单key跳转到对应页面
  const routeMap = {
    dashboard: '/admin/dashboard',
    urls: '/admin/urls',
    users: '/admin/users',
    logs: '/admin/logs',
    settings: '/admin/settings'
  }

  const targetRoute = routeMap[key]
  if (targetRoute && route.path !== targetRoute) {
    router.push(targetRoute)
  }
}

/**
 * 跳转到个人资料页面
 */
const goToProfile = () => {
  router.push('/admin/profile')
}

/**
 * 跳转到系统设置页面
 */
const goToSettings = () => {
  router.push('/admin/settings')
}

/**
 * 处理退出登录
 */
const handleLogout = () => {
  Modal.confirm({
    title: '确认退出',
    content: '您确定要退出登录吗？',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        // 调用登出API
        await logout(userStore.token)

        // 清除本地存储的用户信息
        userStore.logout()

        message.success('已退出登录')

        // 跳转到登录页面
        router.push('/login')
      } catch (error) {
        console.error('退出登录失败:', error)

        // 即使API调用失败，也清除本地状态
        userStore.logout()
        router.push('/login')
      }
    }
  })
}

/**
 * 检查用户登录状态
 */
const checkAuthStatus = () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }

  if (userStore.userInfo?.role !== 'ADMIN') {
    message.error('权限不足，需要管理员权限')
    router.push('/unauthorized')
    return
  }
}

/**
 * 同步当前路由到菜单选中状态
 */
const syncMenuWithRoute = () => {
  const pathMap = {
    '/admin/dashboard': 'dashboard',
    '/admin/urls': 'urls',
    '/admin/users': 'users',
    '/admin/logs': 'logs',
    '/admin/settings': 'settings'
  }

  const currentKey = pathMap[route.path]
  if (currentKey) {
    selectedKeys.value = [currentKey]
  }
}

/**
 * 初始化
 */
onMounted(() => {
  checkAuthStatus()
  syncMenuWithRoute()
})

/**
 * 监听路由变化
 */
watch(() => route.path, () => {
  checkAuthStatus()
  syncMenuWithRoute()
})
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.admin-sider {
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.35);
  z-index: 10;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  margin: 16px 0;
}

.collapsed-logo {
  color: #1890ff;
  font-size: 24px;
}

.logo-text {
  color: white;
  font-size: 18px;
  font-weight: 600;
  margin-left: 8px;
}

.admin-content {
  display: flex;
  flex-direction: column;
}

.admin-header {
  background: #fff;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.12);
  z-index: 9;
}

.header-left {
  display: flex;
  align-items: center;
}

.trigger {
  font-size: 18px;
  cursor: pointer;
  transition: color 0.3s;
  padding: 8px;
  margin-right: 16px;
}

.trigger:hover {
  color: #1890ff;
}

.breadcrumb {
  display: none;
}

@media (min-width: 768px) {
  .breadcrumb {
    display: block;
  }
}

.header-right {
  display: flex;
  align-items: center;
}

.system-status {
  display: flex;
  align-items: center;
}

.status-indicator {
  display: flex;
  align-items: center;
  font-size: 16px;
  padding: 4px;
  border-radius: 50%;
}

.status-healthy {
  color: #52c41a;
  background-color: rgba(82, 196, 26, 0.1);
}

.status-warning {
  color: #faad14;
  background-color: rgba(250, 173, 20, 0.1);
}

.status-error {
  color: #ff4d4f;
  background-color: rgba(255, 77, 79, 0.1);
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 6px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f5f5;
}

.user-avatar {
  margin-right: 8px;
  background-color: #1890ff;
}

.user-name {
  margin-right: 4px;
  font-weight: 500;
}

.dropdown-arrow {
  font-size: 12px;
  color: #8c8c8c;
}

.admin-main {
  flex: 1;
  background: #f0f2f5;
  overflow: auto;
}

.content-wrapper {
  padding: 24px;
  min-height: calc(100vh - 112px); /* 减去header和footer的高度 */
}

.admin-footer {
  background: #f0f2f5;
  padding: 16px 24px;
  text-align: center;
}

.footer-content {
  color: #8c8c8c;
  font-size: 14px;
}

.footer-content p {
  margin: 4px 0;
}

.version {
  font-size: 12px;
  color: #bfbfbf;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .content-wrapper {
    padding: 16px;
  }

  .user-name {
    display: none;
  }

  .system-status {
    display: none;
  }
}

/* 暗色主题适配 */
:deep(.ant-layout-sider) {
  background: #001529;
}

:deep(.ant-menu-dark) {
  background: #001529;
}

:deep(.ant-menu-dark .ant-menu-item-selected) {
  background-color: #1890ff;
}

:deep(.ant-breadcrumb) {
  color: #8c8c8c;
}

:deep(.ant-breadcrumb-separator) {
  color: #d9d9d9;
}
</style>