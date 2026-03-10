import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由配置
 *
 * 模块职责：
 * - 定义应用路由结构
 * - 配置路由守卫
 * - 实现懒加载
 * - 处理404页面
 */

// 路由定义
const routes = [
    {
        path: '/',
        name: 'Home',
        component: () => import('@/pages/Home.vue'),
        meta: {
            title: '短网址生成器',
            requiresAuth: false
        }
    },
    {
        path: '/stats',
        name: 'Stats',
        component: () => import('@/pages/Stats.vue'),
        meta: {
            title: '访问统计',
            requiresAuth: false
        }
    },
    {
        path: '/admin',
        name: 'Admin',
        component: () => import('@/pages/admin/Dashboard.vue'),
        meta: {
            title: '管理后台',
            requiresAuth: true,
            role: 'ADMIN'
        }
    },
    {
        path: '/admin/dashboard',
        name: 'Dashboard',
        component: () => import('@/pages/admin/Dashboard.vue'),
        meta: {
            title: '数据面板',
            requiresAuth: true,
            role: 'ADMIN'
        }
    },
    {
        path: '/admin/urls',
        name: 'UrlManage',
        component: () => import('@/pages/admin/UrlManage.vue'),
        meta: {
            title: '短网址管理',
            requiresAuth: true,
            role: 'ADMIN'
        }
    },
    {
        path: '/admin/users',
        name: 'UserManage',
        component: () => import('@/pages/admin/UserManage.vue'),
        meta: {
            title: '用户管理',
            requiresAuth: true,
            role: 'ADMIN'
        }
    },
    {
        path: '/admin/logs',
        name: 'OperationLogs',
        component: () => import('@/pages/admin/OperationLogs.vue'),
        meta: {
            title: '操作日志',
            requiresAuth: true,
            role: 'ADMIN'
        }
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/pages/Login.vue'),
        meta: {
            title: '用户登录',
            requiresAuth: false
        }
    },
    {
        path: '/:pathMatch(.*)*',
        name: 'NotFound',
        component: () => import('@/pages/NotFound.vue'),
        meta: {
            title: '页面未找到'
        }
    }
]

// 创建路由实例
const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes,

    // 滚动行为
    scrollBehavior(to, from, savedPosition) {
        if (savedPosition) {
            return savedPosition
        } else {
            return { top: 0 }
        }
    }
})

// 路由守卫
router.beforeEach(async (to, from, next) => {debugger
    // 设置页面标题
    document.title = to.meta.title ? `${to.meta.title} - 短网址管理系统` : '短网址管理系统'

    // 获取用户状态
    const userStore = useUserStore()
    const isLoggedIn = userStore.isLoggedIn
    const userRole = userStore.userInfo?.role

    // 检查是否需要登录
    if (to.meta.requiresAuth && !isLoggedIn) {
        // 需要登录但未登录，跳转到登录页
        next({
            name: 'Login',
            query: { redirect: to.fullPath }
        })
        return
    }

    // 检查角色权限
    if (to.meta.role && userRole !== to.meta.role) {
        // 角色权限不足
        next(false)
        return
    }

    // 已登录用户访问登录页，跳转到首页
    if (isLoggedIn && to.name === 'Login') {
        next({ name: 'Home' })
        return
    }

    next()
})

// 路由错误处理
router.onError((error) => {
    console.error('路由错误:', error)
})

export default router

// 需要导入useUserStore
import { useUserStore } from '@/stores/user'