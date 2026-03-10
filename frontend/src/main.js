import { createApp } from 'vue'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import './assets/styles/global.css'

/**
 * 应用入口文件
 *
 * 模块职责：
 * - 初始化Vue应用
 * - 配置全局插件
 * - 设置路由和状态管理
 * - 挂载应用到DOM
 *
 * 加载顺序：
 * 1. 创建Vue应用实例
 * 2. 配置Pinia状态管理
 * 3. 配置路由
 * 4. 配置Ant Design Vue
 * 5. 挂载应用
 */

// 创建应用实例
const app = createApp(App)

// 配置Pinia状态管理
const pinia = createPinia()
app.use(pinia)

// 配置路由
app.use(router)

// 配置Ant Design Vue
app.use(Antd)

// 全局错误处理
app.config.errorHandler = (err, instance, info) => {
    console.error('Vue应用错误:', err)
    console.error('错误信息:', info)

    // TODO: 可以集成错误监控服务
    // 例如：Sentry、LogRocket等
}

// 全局属性配置
app.config.globalProperties = {
    // 可以添加全局方法或属性
    appVersion: '1.0.0',
    apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '/api'
}

// 挂载应用
app.mount('#app')

// 性能监控
if (import.meta.env.PROD) {
    // 生产环境性能监控
    window.addEventListener('load', () => {
        if ('performance' in window) {
            const loadTime = performance.now()
            console.log(`应用加载时间: ${loadTime}ms`)

            // TODO: 发送性能指标到监控服务
        }
    })
}