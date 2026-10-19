import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ChoiceUpload from '../views/ChoiceUpload.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },

    {
      path: '/upload-selection',
      name: 'upload',
      component: ChoiceUpload
    },
  ],
})

export default router
