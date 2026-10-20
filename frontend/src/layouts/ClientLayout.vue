<script setup>
import NavBar from '@/components/NavBar.vue';

</script>

<template>
  <header>
    <NavBar />

    <!-- <nav>
      <RouterLink to="/si/client">Accueil</RouterLink>
      <RouterLink to="/si/client/offres">Offres</RouterLink>
    </nav> -->
  </header>

  <main>
    <RouterView />
  </main>
</template>

<style scoped>
    
</style>