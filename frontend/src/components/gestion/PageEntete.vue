<script setup>
defineProps({
  titre: { type: String, required: true },
  sousTitre: { type: String, default: "" },
});
</script>

<template>
  <header class="g-entete">
    <div>
      <h1>{{ titre }}</h1>
      <p v-if="sousTitre">{{ sousTitre }}</p>
    </div>
    <div class="g-entete__actions"><slot /></div>
  </header>
</template>
