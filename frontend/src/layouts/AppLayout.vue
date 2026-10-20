<script setup>
import "../assets/theme.css"

</script>

<template>
  <div class="app-layout">
    <aside>
      <nav>
        <RouterLink to="/si/app">Dashboard</RouterLink>
        <RouterLink to="/si/app/rh/demandes">
          Demandes RH
        </RouterLink>
        <RouterLink to="/si/app/finance">
          Finance
        </RouterLink>
      </nav>
    </aside>

    <main>
        <!-- eto no injecter ny vue rehetra momban'le app sauf ny view an'ny candidat -->
      <RouterView /> 
    </main>
  </div>
</template>

<style scoped>
/* ==============================
   Layout général
============================== */

.app-layout {
    min-height: 100vh;
    background-color: var(--color-background);
}

/* ==============================
   Sidebar
============================== */

aside {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;

    display: flex;
    flex-direction: column;

    width: 250px;
    height: 100vh;
    box-sizing: border-box;

    background-color: var(--color-card);
    border-right: 1px solid #e5e7eb;

    padding: 24px 16px;
    overflow-y: auto;
    z-index: 100;
}

/* Navigation */

nav {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

/* Liens */

nav a {
    display: flex;
    align-items: center;

    padding: 12px 14px;
    border-radius: 8px;

    color: #4b5563;
    text-decoration: none;
    font-size: 14px;
    font-weight: 500;

    transition:
        background-color 0.2s ease,
        color 0.2s ease;
}

/* Survol */

nav a:hover {
    background-color: var(--color-background);
    color: var(--color-primary);
}

/* Route active */

nav a.router-link-active {
    background-color: #e8f3e9;
    color: var(--color-primary);
    font-weight: 600;
}

/* ==============================
   Contenu principal
============================== */

main {
    margin-left: 250px;
    min-width: 0;
    min-height: 100vh;
    box-sizing: border-box;

    padding: 32px;
    background-color: var(--color-background);
}

/* ==============================
   Responsive : tablette et mobile
============================== */

@media (max-width: 768px) {
    aside {
        width: 200px;
        padding: 20px 12px;
    }

    main {
        margin-left: 200px;
        padding: 20px;
    }
}

@media (max-width: 480px) {
    aside {
        width: 72px;
        padding: 20px 8px;
    }

    nav a {
        justify-content: center;
        padding: 12px 4px;
        font-size: 0;
    }

    main {
        margin-left: 72px;
        padding: 16px;
    }
}
</style>