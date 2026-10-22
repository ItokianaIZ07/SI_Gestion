import './assets/style.css'
import './assets/formulaire.css'
import './assets/editeur.css'

// import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate' // 1. Importer le plugin

// Harena : reglage de l'erreur requet api
import api from './services/api.js'
import { useAuthStore } from './stores/authStore'


import App from './App.vue'
import router from './router'

const app = createApp(App)
const pinia = createPinia();

pinia.use(piniaPluginPersistedstate)
// Harena : reglage de l'erreur requet api
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url
    if (
      error.response?.status === 401 &&
      url !== '/auth/login' &&
      url !== '/auth/me' &&
      url !== '/auth/csrf'
    ) {
      useAuthStore(pinia).clearSession()
      if (router.currentRoute.value.name !== 'login') {
        void router.replace({ name: 'login' })
      }
    }

    return Promise.reject(error)
  },
)

app.use(pinia)
app.use(router)

app.mount('#app')
