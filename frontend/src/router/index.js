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
import DemandeView from '@/views/modules/gestion/DemandeView.vue'

import EntretienView from "@/views/modules/gestion/rh/EntretienView.vue";
import ContratView from "@/views/modules/gestion/rh/ContratView.vue";

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
        // Parcours candidat : offres publiées -> détail -> dépôt de CV
        {
          path: 'offres',
          name: 'offresPubliques',
          component: () => import('@/views/modules/client/OffresPubliquesView.vue')
        },
        {
          path: 'offres/:publicationId',
          name: 'offreDetail',
          component: () => import('@/views/modules/client/OffreDetailView.vue')
        },
        {
          path: 'offres/:publicationId/postuler',
          name: 'postuler',
          component: () => import('@/views/modules/client/PostulerView.vue')
        },
      ]
    },

    {
      path: "/si/app",
      component: AppLayout,
      beforeEnter: async () => {
        const authStore = useAuthStore()

        if (!await authStore.restaurerSession()) {
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
        {
          path: "rh/demandes",
          component: DemandeView
          //           component: () => DemandeView,
        },
        {
          path: "rh/recrutements",
          name: "rhRecrutements",
          component: () => import("@/views/modules/gestion/rh/RecrutementsView.vue"),
        },
        {
          path: "rh/recrutements/creation",
          name: "rhRecrutementCreation",
          component: () => import("@/views/modules/gestion/rh/RecrutementFormView.vue"),
        },
        {
          path: "rh/recrutements/:id/modifier",
          name: "rhRecrutementModification",
          component: () => import("@/views/modules/gestion/rh/RecrutementFormView.vue"),
        },
        {
          path: "rh/offres",
          name: "rhOffres",
          component: () => import("@/views/modules/gestion/rh/OffresView.vue"),
        },
        {
          path: "rh/offres/creation",
          name: "rhOffreCreation",
          component: () => import("@/views/modules/gestion/rh/OffreFormView.vue"),
        },
        {
          path: "rh/offres/:id/modifier",
          name: "rhOffreModification",
          component: () => import("@/views/modules/gestion/rh/OffreFormView.vue"),
        },
        {
          path: "rh/publications",
          name: "rhPublications",
          component: () => import("@/views/modules/gestion/rh/PublicationsView.vue"),
        },
        {
          path: "rh/publications/creation",
          name: "rhPublicationCreation",
          component: () => import("@/views/modules/gestion/rh/PublicationFormView.vue"),
        },
        {
          path: "rh/publications/:id/modifier",
          name: "rhPublicationModification",
          component: () => import("@/views/modules/gestion/rh/PublicationFormView.vue"),
        },
        {
          path: "rh/entretiens",
          component: EntretienView,
        },
        {
          path: "rh/contrats",
          component: ContratView,
        }
        // Ajouter ici les routes RH, Finance,
        // Manager et Direction.
      ],
    },
  ],
});

export default router;
