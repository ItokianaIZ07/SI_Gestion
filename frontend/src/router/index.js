import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ChoiceUploadView from '../views/ChoiceUploadView.vue'
import FormulaireUploadCVView from '../views/FormulaireUploadCVView.vue'


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
      component: ChoiceUploadView
    },

    {
      path: '/upload-cv',
      name: 'uploadCVManuel',
      component: FormulaireUploadCVView
    },
  ],
})

export default router
