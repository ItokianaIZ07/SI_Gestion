import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // Proxy : le navigateur appelle /api/... sur localhost:5173,
    // et Vite transmet la requête à l'API PHP sur localhost:8000.
    // Pour le navigateur, front et API ont la même origine : plus de CORS.
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})
