import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

/**
 * Vite配置文件
 *
 * 模块职责：
 * - 配置Vite构建工具
 * - 设置开发服务器
 * - 配置路径别名
 * - 优化构建输出
 */
export default defineConfig({
  plugins: [vue()],

  // 路径别名配置
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },

  // 开发服务器配置
  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    open: true,

    // 代理配置（开发环境）
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      }
    }
  },

  // 构建配置
  build: {
    target: 'es2015',
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: false,
    minify: 'terser',

    // 分块策略
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['vue', 'vue-router', 'pinia'],
          antd: ['ant-design-vue'],
          echarts: ['echarts']
        }
      }
    },

    // 文件大小警告阈值
    chunkSizeWarningLimit: 1024
  },

  // CSS配置
  css: {
    devSourcemap: true,
    preprocessorOptions: {
      less: {
        javascriptEnabled: true,
        modifyVars: {
          // Ant Design Vue主题配置
          'primary-color': '#1890ff',
          'link-color': '#1890ff',
          'border-radius-base': '6px'
        }
      }
    }
  },

  // 环境变量配置
  define: {
    __APP_ENV__: JSON.stringify(process.env.NODE_ENV)
  }
})