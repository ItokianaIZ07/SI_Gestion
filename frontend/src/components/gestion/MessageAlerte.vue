<script setup>
defineProps({
  message: { type: String, default: "" },
  type: { type: String, default: "erreur" }, // 'erreur' | 'succes'
});
</script>

<template>
  <p v-if="message" class="g-alerte" :class="`g-alerte--${type}`" role="alert">{{ message }}</p>
</template>
