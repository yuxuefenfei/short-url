<template>
  <div class="login-container">
    <div class="login-background">
      <div class="login-form-wrapper">
        <a-card class="login-card" :bordered="false">
          <div class="login-header">
            <div class="logo">
              <LinkIcon class="logo-icon" />
              <h1>短网址管理系统</h1>
            </div>
            <p class="subtitle">管理员登录</p>
          </div>

          <a-form
            :model="loginForm"
            @finish="handleLogin"
            @finishFailed="handleLoginFailed"
            class="login-form"
            autocomplete="off"
          >
            <a-form-item
              name="username"
              :rules="[
                { required: true, message: '请输入用户名' },
                { min: 3, max: 50, message: '用户名长度必须在3-50个字符之间' }
              ]"
            >
              <a-input
                v-model:value="loginForm.username"
                placeholder="用户名"
                size="large"
                autocomplete="username"
              >
                <template #prefix>
                  <UserOutlined class="input-icon" />
                </template>
              </a-input>
            </a-form-item>

            <a-form-item
              name="password"
              :rules="[
                { required: true, message: '请输入密码' },
                { min: 6, message: '密码长度不能少于6个字符' }
              ]"
            >
              <a-input-password
                v-model:value="loginForm.password"
                placeholder="密码"
                size="large"
                autocomplete="current-password"
                @pressEnter="handleLogin"
              >
                <template #prefix>
                  <LockOutlined class="input-icon" />
                </template>
              </a-input-password>
            </a-form-item>

            <a-form-item>
              <a-button
                type="primary"
                html-type="submit"
                size="large"
                block
                :loading="loading"
                class="login-button"
              >
                {{ loading ? '登录中...' : '登录' }}
              </a-button>
            </a-form-item>
          </a-form>

          <div class="login-footer">
            <p>© 2024 短网址管理系统. All rights reserved.</p>
          </div>
        </a-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { login } from '@/api/auth'

// 图标组件
const LinkIcon = {
  template: `
    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
    </svg>
  `
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: ''
})

// 加载状态
const loading = ref(false)

/**
 * 处理登录提交
 */
const handleLogin = async () => {
  if (loading.value) return

  loading.value = true

  try {
    // 调用登录API
    const response = await login({
      username: loginForm.username.trim(),
      password: loginForm.password
    })
    debugger

    const { token, refreshToken, userInfo } = response

    // 保存用户信息到store
    userStore.login({
      token,
      refreshToken,
      userInfo
    })

    message.success('登录成功！')

    // 跳转到之前访问的页面或默认页面
    const redirect = route.query.redirect || '/admin/dashboard'
    router.push(redirect)

  } catch (error) {
    console.error('登录失败:', error)

    // 显示错误信息
    const errorMessage = error.response?.data?.message || '登录失败，请检查用户名和密码'
    message.error(errorMessage)

    // 清空密码
    loginForm.password = ''
  } finally {
    loading.value = false
  }
}

/**
 * 处理登录失败（表单验证失败）
 */
const handleLoginFailed = (errorInfo) => {
  console.log('表单验证失败:', errorInfo)
  message.warning('请检查输入信息')
}

/**
 * 检查是否已登录
 */
const checkLoginStatus = () => {
  if (userStore.isLoggedIn && userStore.userInfo?.role === 'ADMIN') {
    // 如果已登录且是管理员，跳转到管理页面
    const redirect = route.query.redirect || '/admin/dashboard'
    router.push(redirect)
  }
}

/**
 * 初始化
 */
onMounted(() => {
  checkLoginStatus()
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-background {
  width: 100%;
  max-width: 420px;
}

.login-form-wrapper {
  display: flex;
  justify-content: center;
}

.login-card {
  width: 100%;
  border-radius: 12px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.logo-icon {
  color: #1890ff;
  margin-right: 12px;
}

.logo h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #262626;
  background: linear-gradient(135deg, #1890ff, #722ed1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  margin: 0;
  color: #8c8c8c;
  font-size: 16px;
}

.login-form {
  max-width: 320px;
  margin: 0 auto;
}

.input-icon {
  color: #bfbfbf;
}

.login-button {
  height: 44px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
}

.login-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.login-footer p {
  margin: 0;
  color: #bfbfbf;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-background {
    max-width: 100%;
  }

  .login-card {
    margin: 0;
    border-radius: 0;
    box-shadow: none;
  }

  .login-container {
    padding: 0;
    background: white;
  }

  .logo h1 {
    font-size: 20px;
  }

  .subtitle {
    font-size: 14px;
  }
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-card {
  animation: fadeIn 0.6s ease-out;
}

/* 表单元素样式优化 */
:deep(.ant-input-affix-wrapper) {
  border-radius: 8px;
  border: 1px solid #d9d9d9;
  transition: all 0.3s;
}

:deep(.ant-input-affix-wrapper:hover) {
  border-color: #40a9ff;
}

:deep(.ant-input-affix-wrapper:focus),
:deep(.ant-input-affix-wrapper-focused) {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

:deep(.ant-form-item-explain-error) {
  font-size: 12px;
  margin-top: 4px;
}

:deep(.ant-btn-primary) {
  background: linear-gradient(135deg, #1890ff, #40a9ff);
  border: none;
}

:deep(.ant-btn-primary:hover) {
  background: linear-gradient(135deg, #40a9ff, #69c0ff);
}
</style>