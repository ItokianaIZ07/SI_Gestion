import { createRouter, createWebHistory } from 'vue-router'
import ChoiceUploadView from '@/views/modules/client/ChoiceUploadView.vue'
import ClientLayout from '@/layouts/ClientLayout.vue'
import FormulaireUploadCVView from '@/views/modules/client/FormulaireUploadCVView.vue'
import EditCVView from '@/views/modules/client/EditCVView.vue'

import LoginView from '@/views/LoginView.vue'
import AppLayout from "@/layouts/AppLayout.vue";
import DashboardView from "@/views/modules/gestion/DashboardView.vue";
import ManifestationView from '@/views/modules/gestion/Manager/ManifestationView.vue'
import { useAuthStore } from '@/stores/authStore'
import CVView from '@/views/modules/gestion/rh/CVView.vue'
import ManifestationView from '@/views/modules/gestion/Manager/ManifestationView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      name: "login",
      component: LoginView,
    },

    {
      path: "/si/client",
      component: ClientLayout,
      children: [
        {
          path: "",
          component: null
        },
        {
          path: "upload-selection",
          name: 'upload',
          component: ChoiceUploadView
        },
        {
          path: 'upload-cv',
          name: 'uploadCVManuel',
          component: FormulaireUploadCVView
        },
        {
          path: 'editor-cv',
          name: 'editeurEnLigne',
          component: EditCVView
        },
      ]
    },

    {
      path: "/si/app",
      component: AppLayout,
      beforeEnter: (to, from) => {
        const authStore = useAuthStore()

        if (!authStore.estAuthentifie) {
          return { name: 'login' }
        }
      },
      children: [
        {
          path: "",
          component: DashboardView,
        },
        {
          path: 'manager/demandes/creation',
          name: 'DemandeManifestation',
          component: ManifestationView
        },
        {
          path: "rh/cv",
          component: CVView
        },
        {
          path: 'manager/demandes/creation',
          name: 'DemandeManifestation',
          component: ManifestationView
        },
        // Ajouter ici les routes RH, Finance,
        // Manager et Direction.
      ],
    },
  ],
});

export default router;
