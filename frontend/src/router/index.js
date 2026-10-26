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
import EmbaucheView from '@/views/modules/gestion/rh/EmbaucheView.vue'
import ListeDepartementsView from "@/views/modules/gestion/Manager/ListeDepartementsView.vue";
import VisionDepartementView from "@/views/modules/gestion/Manager/VisionDepartementView.vue";
import DepartementFormView from "@/views/modules/gestion/Manager/DepartementFormView.vue";
import PostulerView from '@/views/modules/client/PostulerView.vue'

import SignerContratView from '@/views/modules/client/SignerContratView.vue'


// Autorise la route seulement si le rôle de l'utilisateur est dans la liste.
const exigerRole = (...rolesAutorises) => () => {
  const authStore = useAuthStore()
  const role = authStore.user?.role?.toUpperCase()
  if (!rolesAutorises.includes(role)) {
    return { path: '/si/app' }
  }
}
const financeEtDirection = exigerRole('FINANCE', 'DIRECTION')
const rhEtDirection = exigerRole('RH', 'DIRECTION')
const rhUniquement = exigerRole('RH')

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
        // {
        //   path: "upload-selection",
        //   name: 'upload',
        //   component: ChoiceUploadView
        // },
        {
          path: 'offres/:publicationId/upload-cv',
          name: 'uploadCVManuel',
          component: PostulerView
        },
        {
          path: 'offres/:publicationId/editor-cv',
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
          component: ChoiceUploadView
          // component: () => import('@/views/modules/client/PostulerView.vue')
        },
        {
          path: 'signer-contrat/:token',
          name: 'signerContrat',
          component: SignerContratView
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
        { path: "", component: DashboardView },
        { path: "rh/cv", component: CVView },

        // ----- Module Finance -----
        {
          path: "finance/demandes",
          component: () => import('@/views/modules/finance/DemandesFinanceView.vue'),
          beforeEnter: financeEtDirection,
        },
        {
          path: "finance/grille-salaire",
          component: () => import('@/views/modules/finance/GrilleSalaireView.vue'),
          beforeEnter: financeEtDirection,
        },
        {
          path: "finance/budget",
          component: () => import('@/views/modules/finance/BudgetView.vue'),
          beforeEnter: financeEtDirection,
        },

        {
        path: "finance/demandes/:id",
        component: () => import('@/views/modules/finance/DemandeFinanceDetailView.vue'),
        beforeEnter: financeEtDirection,
        },
        // À décommenter quand la page Dépenses sera créée :
        // {
        //   path: "finance/depenses",
        //   component: () => import('@/views/modules/finance/DepensesView.vue'),
        //   beforeEnter: financeEtDirection,
        // },

        // Ajouter ici les routes RH, Manager et Direction.
        {
          path: 'manager/demandes/creation',
          name: 'DemandeManifestation',
          component: ManifestationView
        },
        {
          path: 'manager/departements',
          name: 'ListeDepartements',
          component: ListeDepartementsView
        },
        {
          path: 'manager/departements/:departementId',
          name: 'DetailVisionDepartement',
          component: VisionDepartementView
        },
        {
          path: 'manager/departements/creation',
          name: 'CreationDepartement',
          component: DepartementFormView
        },
        {
          path: "rh/cv",
          component: CVView
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
          name: "rhEntretiens",
          component: EntretienView,
          beforeEnter: rhUniquement,
        },
        {
          path: "rh/embauche",
          name: "rhEmbauche",
          component: EmbaucheView,
          beforeEnter: rhUniquement,
        },
        {
          path: "rh/contrats",
          name: "rhContrats",
          component: ContratView,
          beforeEnter: rhEtDirection,
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
        // Ajouter ici les routes RH, Finance,
        // Manager et Direction.
      ],
    },
  ],
});

export default router;