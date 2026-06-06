import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/** 开发时 API 代理目标，与 dataplatform-api application.yml server.port 一致 */
const API_TARGET = 'http://127.0.0.1:3001'
const SIM_TARGET = 'http://127.0.0.1:3002'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 3000,
    strictPort: true,
    proxy: {
      '/v1': {
        target: API_TARGET,
        changeOrigin: true,
        ws: true
      },
      '/sim': {
        target: SIM_TARGET,
        changeOrigin: true
      }
    }
  },
  preview: {
    host: '0.0.0.0',
    port: 3000,
    proxy: {
      '/v1': {
        target: API_TARGET,
        changeOrigin: true,
        ws: true
      },
      '/sim': {
        target: SIM_TARGET,
        changeOrigin: true
      }
    }
  }
})
