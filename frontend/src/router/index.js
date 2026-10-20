import { createRouter, createWebHistory } from "vue-router";
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
