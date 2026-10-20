import { createRouter, createWebHistory } from 'vue-router'
import ChoiceUploadView from '../views/ChoiceUploadView.vue'
import FormulaireUploadCVView from '../views/FormulaireUploadCVView.vue'
import EditCVView from '../views/EditCVView.vue'

import HomeView from "../views/HomeView.vue";
import AppLayout from "@/layouts/AppLayout.vue";
import DashboardView from "@/views/modules/gestion/DashboardView.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      name: "home",
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

    {
      path: '/editor-cv',
      name: 'editeurEnLigne',
      component: EditCVView
    {
      path: "/si/app",
      component: AppLayout,
      children: [
        {
          path: "",
          component: DashboardView,
        },
        // Ajouter ici les routes RH, Finance,
        // Manager et Direction.
      ],
    },
  ],
});

export default router;
