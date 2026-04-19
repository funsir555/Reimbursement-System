import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import vueDevTools from 'vite-plugin-vue-devtools'

const frontendPort = Number.parseInt(process.env.FINEX_FRONTEND_PORT ?? '5173', 10)
const gatewayProxyTarget = process.env.FINEX_GATEWAY_PROXY_TARGET ?? 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    Components({
      resolvers: [
        ElementPlusResolver({
          importStyle: 'css'
        })
      ]
    }),
    vueDevTools(),
  ],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return
          }
          const isPackage = (name: string) => new RegExp(`[\\\\/]${name}[\\\\/]`).test(id)
          if (isPackage('vue') || isPackage('pinia') || isPackage('vue-router')) {
            return 'framework'
          }
        }
      }
    }
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  server: {
    port: frontendPort,
    proxy: {
      '/api': {
        target: gatewayProxyTarget,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api')
      }
    }
  },
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/**/__tests__/**/*.{test,spec}.ts'],
    server: {
      deps: {
        inline: ['element-plus', '@element-plus/icons-vue']
      }
    }
  }
})
