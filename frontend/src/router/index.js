import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const appTitle = '短网址管理系统'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/pages/Home.vue'),
    meta: { title: '短网址生成器' }
  },
  {
    path: '/stats',
    name: 'Stats',
    component: () => import('@/pages/Stats.vue'),
    meta: { title: '访问统计' }
  },
  {
    path: '/admin',
    redirect: '/admin/dashboard'
  },
  {
    path: '/admin/dashboard',
    name: 'Dashboard',
    component: () => import('@/pages/admin/Dashboard.vue'),
    meta: { title: '数据看板', requiresAuth: true, role: 'ADMIN' }
  },
  {
    path: '/admin/urls',
    name: 'UrlManage',
    component: () => import('@/pages/admin/UrlManage.vue'),
    meta: { title: '短链管理', requiresAuth: true, role: 'ADMIN' }
  },
  {
    path: '/admin/users',
    name: 'UserManage',
    component: () => import('@/pages/admin/UserManage.vue'),
    meta: { title: '用户管理', requiresAuth: true, role: 'ADMIN' }
  },
  {
    path: '/admin/logs',
    name: 'OperationLogs',
    component: () => import('@/pages/admin/OperationLogs.vue'),
    meta: { title: '操作日志', requiresAuth: true, role: 'ADMIN' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/Login.vue'),
    meta: { title: '用户登录' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/pages/NotFound.vue'),
    meta: { title: '页面未找到' }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(_, __, savedPosition) {
    return savedPosition || { top: 0 }
  }
})

router.beforeEach((to, _, next) => {
  document.title = to.meta.title ? `${to.meta.title} - ${appTitle}` : appTitle

  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.role && userStore.userInfo?.role !== to.meta.role) {
    next({ name: 'Home' })
    return
  }

  if (to.name === 'Login' && userStore.isLoggedIn) {
    next({ name: 'Dashboard' })
    return
  }

  next()
})

export default router
