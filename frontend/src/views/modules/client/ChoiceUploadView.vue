<script setup>
import { useRoute } from 'vue-router';
import NavBar from '@/components/NavBar.vue';
import UploadCV from '@/components/UploadCV.vue';
import EditCV from '@/components/EditCV.vue';

const route = useRoute();

const publicationId = Number(route.params.publicationId);
</script>


<template>

  <NavBar />

  <main class="principal">
    <div class="intro">
      <h1>Votre CV, à votre façon</h1>
      <p>Importez un CV que vous avez déjà, ou créez-le directement en ligne avec nos modèles. Dans les deux cas, vous
        pouvez le télécharger en PDF quand il est prêt.</p>
    </div>

    <div class="choix">
      <UploadCV :id-publication="publicationId" />
      <EditCV :id-publication="publicationId" />
    </div>
  </main>



</template>