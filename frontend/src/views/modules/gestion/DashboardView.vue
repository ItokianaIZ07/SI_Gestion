<script setup>
import { computed } from "vue";
import { useAuthStore } from "@/stores/authStore";
import HeroBanner from "@/components/ui/HeroBanner.vue";
import { getSidebarLinks } from "@/config/sidebar";

const authStore = useAuthStore();
/* Aplatit les liens avec `children` en une liste plate pour les raccourcis */
const raccourcis = computed(() => {
  const liens = getSidebarLinks(authStore.user?.role);
  return liens.slice(0, 8);
});
</script>

<template>
  <div class="dashboard">
    <HeroBanner
      eyebrow="Tableau de bord"
      titre="Bienvenue sur votre espace de gestion"
      texte="Accédez rapidement à vos espaces de travail depuis les raccourcis ci-dessous."
    />

    <ul class="dashboard__grille">
      <li v-for="lien in raccourcis" :key="lien.to ?? lien.label">
        <!-- Lien simple -->
        <RouterLink v-if="lien.to" class="dashboard__carte" :to="lien.to">
          <span>{{ lien.label }}</span>
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="M5 12h14M13 6l6 6-6 6" />
          </svg>
        </RouterLink>

        <!-- Groupe avec sous-liens (ex. Contrats → Embaucher / Liste des contrats) -->
        <details v-else class="dashboard__groupe">
          <summary class="dashboard__carte">
            <span>{{ lien.label }}</span>
            <svg
              class="dashboard__chevron"
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.2"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <polyline points="6 9 12 15 18 9" />
            </svg>
          </summary>

          <div class="dashboard__sous-liens">
            <RouterLink
              v-for="child in lien.children"
              :key="child.to"
              class="dashboard__sous-lien"
              :to="child.to"
            >
              {{ child.label }}
              <svg
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.2"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
              >
                <path d="M5 12h14M13 6l6 6-6 6" />
              </svg>
            </RouterLink>
          </div>
        </details>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1240px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.dashboard__entete h1 {
  margin: 0;
  font-size: 1.6rem;
}

.dashboard__entete p {
  margin: 6px 0 0;
  color: var(--color-muted);
}

.dashboard__grille {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
}

/* ---------- Carte simple ---------- */
.dashboard__carte {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 20px;
  font-weight: 600;
  color: var(--color-ink);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  transition: border-color 0.2s, box-shadow 0.2s, color 0.2s;
}

.dashboard__carte svg {
  color: var(--color-faint);
  transition: transform 0.2s, color 0.2s;
}

.dashboard__carte:hover {
  border-color: var(--color-primary-line);
  box-shadow: var(--shadow-md);
  color: var(--color-primary);
}

.dashboard__carte:hover svg {
  color: var(--color-primary);
  transform: translateX(3px);
}

/* ---------- Groupe (carte avec sous-liens) ---------- */
.dashboard__groupe {
  display: flex;
  flex-direction: column;
}

.dashboard__groupe > summary.dashboard__carte {
  list-style: none;
  cursor: pointer;
}

.dashboard__groupe > summary.dashboard__carte::-webkit-details-marker {
  display: none;
}

.dashboard__groupe > summary.dashboard__carte:hover .dashboard__chevron {
  color: var(--color-primary);
}

.dashboard__chevron {
  color: var(--color-faint);
  transition: transform 0.2s, color 0.2s;
}

.dashboard__groupe[open] .dashboard__chevron {
  transform: rotate(180deg);
  color: var(--color-primary);
}

.dashboard__groupe[open] > summary.dashboard__carte {
  border-color: var(--color-primary-line);
  box-shadow: var(--shadow-md);
  color: var(--color-primary);
}

/* ---------- Sous-liens ---------- */
.dashboard__sous-liens {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin: 6px 0 0 14px;
  padding-left: 12px;
  border-left: 1px solid var(--color-border);
}

.dashboard__sous-lien {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 11px 14px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-muted);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}

.dashboard__sous-lien:hover {
  color: var(--color-primary);
  border-color: var(--color-primary-line);
  background: var(--color-primary-soft);
}

.dashboard__sous-lien svg {
  color: var(--color-faint);
  transition: transform 0.15s, color 0.15s;
}

.dashboard__sous-lien:hover svg {
  color: var(--color-primary);
  transform: translateX(2px);
}
</style>