<script setup>
import { computed } from "vue";
import { useAuthStore } from "@/stores/authStore";
import { getSidebarLinks } from "@/config/sidebar";
import { useRouter } from "vue-router";

const authStore = useAuthStore();
const router = useRouter();

const role = authStore.user.role; // rôle reçu du serveur, utilisé pour choisir les liens
const email = authStore.user.email ?? "";
const links = getSidebarLinks(role);

/* Regroupe les liens par domaine (rh / manager / finance) d'après leur URL */
const SECTIONS = {
  rh: "Ressources humaines",
  manager: "Managers",
  finance: "Finance",
};

const groupes = computed(() => {
  const map = new Map();
  for (const link of links) {
    const url = link.to ?? link.children?.[0]?.to ?? "";
    const cle = url.split("/")[3] ?? "autre"; // /si/app/<domaine>/...
    if (!map.has(cle)) map.set(cle, { cle, titre: SECTIONS[cle] ?? "Général", liens: [] });
    map.get(cle).liens.push(link);
  }
  return [...map.values()];
});

const avecTitres = computed(() => groupes.value.length > 1);
const initiale = computed(() => (email[0] ?? role?.[0] ?? "?").toUpperCase());

const logout = async () => {
  const response = await authStore.logout();
  if (response.success) {
    router.replace("/");
  }
};
</script>

<template>
  <div class="app-layout">
    <aside>
      <div class="brand">
        <span class="brand__logo">RH</span>
        <div class="brand__texte">
          <strong>Gestion RH</strong>
          <small>{{ role }}</small>
        </div>
      </div>

      <nav aria-label="Navigation principale">
        <RouterLink to="/si/app" exact-active-class="router-link-exact-active">Dashboard</RouterLink>

        <div v-for="groupe in groupes" :key="groupe.cle" class="nav-groupe">
          <p v-if="avecTitres" class="nav-groupe__titre">{{ groupe.titre }}</p>
          <template v-for="link in groupe.liens" :key="link.to ?? link.label">
            <!-- Lien simple -->
            <RouterLink v-if="link.to" :to="link.to">
              {{ link.label }}
            </RouterLink>

            <!-- Sous-menu (children) -->
            <details v-else class="nav-sous-menu">
              <summary>
                <span>{{ link.label }}</span>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"
                  stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <polyline points="6 9 12 15 18 9" />
                </svg>
              </summary>
              <div class="nav-sous-menu__liste">
                <RouterLink v-for="child in link.children" :key="child.to" :to="child.to" class="nav-sous-menu__lien">
                  {{ child.label }}
                </RouterLink>
              </div>
            </details>
          </template>
        </div>
      </nav>

      <div class="profil">
        <span class="profil__avatar" aria-hidden="true">{{ initiale }}</span>
        <span class="profil__email" :title="email">{{ email }}</span>
        <button class="btn-logout" type="button" @click="logout()" aria-label="Se déconnecter" title="Se déconnecter">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
            stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
            <path d="M16 17l5-5-5-5" />
            <path d="M21 12H9" />
          </svg>
        </button>
      </div>
    </aside>

    <main>
      <!-- Toutes les vues de l'app, sauf celles du candidat -->
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-layout {
  min-height: 100vh;
  margin-top: -68px;
  /* annule le padding-top du body (réservé à la navbar candidat) */
  background: var(--color-background);
}

/* ---------- Sidebar ---------- */
aside {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  width: 260px;
  background: var(--color-card);
  border-right: 1px solid var(--color-border);
}

.brand {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 12px 4px;
  padding: 16px;
  overflow: hidden;
  color: #fff;
  background:
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='200' height='80' viewBox='0 0 200 80'%3E%3Ccircle cx='180' cy='6' r='44' fill='none' stroke='white' stroke-opacity='.08' stroke-width='14'/%3E%3Ccircle cx='10' cy='84' r='28' fill='white' fill-opacity='.06'/%3E%3C/svg%3E") right / cover no-repeat,
    linear-gradient(120deg, #014d07, #015f09 60%, #0a7a14);
  border-radius: var(--radius-md);
  box-shadow: 0 6px 16px rgba(1, 70, 8, 0.2);
}

.brand__logo {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: #fff;
  color: var(--color-primary);
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.brand__texte {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
  min-width: 0;
}

.brand__texte strong {
  font-size: 15px;
  color: #fff;
}

.brand__texte small {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.75);
}

nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 12px 16px;
  overflow-y: auto;
}

.nav-groupe {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 14px;
}

.nav-groupe__titre {
  margin: 0 0 4px;
  padding: 0 12px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-faint);
}

nav a {
  position: relative;
  display: flex;
  align-items: center;
  padding: 9px 12px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.35;
  color: var(--color-muted);
  transition: background-color 0.15s, color 0.15s;
}

nav a:hover {
  background: var(--color-subtle);
  color: var(--color-ink);
}

nav a.router-link-exact-active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 600;
}

nav a.router-link-exact-active::before {
  content: "";
  position: absolute;
  left: -12px;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--color-primary);
}

/* ---------- Profil + déconnexion ---------- */
.profil {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 12px 12px;
  padding: 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-background);
}

.profil__avatar {
  display: grid;
  place-items: center;
  flex: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
}

.profil__email {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12.5px;
  color: var(--color-muted);
}

.btn-logout {
  display: grid;
  place-items: center;
  flex: none;
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-muted);
  cursor: pointer;
  transition: background-color 0.15s, color 0.15s;
}

.btn-logout:hover {
  background: var(--color-danger-background);
  color: var(--color-danger);
}

/* ---------- Sous-menu ---------- */
.nav-sous-menu {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-sous-menu summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 9px 12px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  font-weight: 500;
  line-height: 1.35;
  color: var(--color-muted);
  cursor: pointer;
  list-style: none;
  transition: background-color 0.15s, color 0.15s;
}

.nav-sous-menu summary::-webkit-details-marker { display: none; }

.nav-sous-menu summary:hover {
  background: var(--color-subtle);
  color: var(--color-ink);
}

.nav-sous-menu summary svg {
  flex: none;
  color: var(--color-faint);
  transition: transform 0.15s;
}

.nav-sous-menu[open] summary svg { transform: rotate(180deg); }

.nav-sous-menu__liste {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin: 2px 0 2px 12px;
  padding-left: 12px;
  border-left: 1px solid var(--color-border);
}

.nav-sous-menu__lien {
  font-size: 13.5px;
  padding: 7px 12px;
}

.nav-sous-menu__lien.router-link-exact-active::before {
  display: none;
}

/* ---------- Contenu ---------- */
main {
  min-width: 0;
  min-height: 100vh;
  margin-left: 260px;
  padding: 36px clamp(20px, 3vw, 44px) 56px;
}

/* ---------- Tablette / mobile : la sidebar devient une barre horizontale ---------- */
@media (max-width: 900px) {
  aside {
    position: sticky;
    inset: 0 0 auto 0;
    flex-direction: row;
    align-items: center;
    width: 100%;
    height: auto;
    border-right: 0;
    border-bottom: 1px solid var(--color-border);
  }

  .brand {
    margin: 8px;
    padding: 8px 12px;
    box-shadow: none;
  }

  .brand__texte small,
  .profil__email {
    display: none;
  }

  nav {
    flex-direction: row;
    align-items: center;
    gap: 4px;
    padding: 8px;
    overflow-x: auto;
    overflow-y: hidden;
  }

  .nav-groupe {
    flex-direction: row;
    margin: 0;
    gap: 4px;
  }

  .nav-groupe__titre {
    display: none;
  }

  nav a {
    white-space: nowrap;
  }

  nav a.router-link-exact-active::before {
    display: none;
  }

  .profil {
    margin: 0 10px 0 0;
    padding: 4px;
    border: 0;
    background: transparent;
  }

  .profil__avatar {
    display: none;
  }

  main {
    margin-left: 0;
    padding: 20px 16px 40px;
  }
}
</style>
