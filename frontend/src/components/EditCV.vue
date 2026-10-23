<script setup>
defineProps({
  idPublication: {
    type: Number,
    required: true
  }
})
</script>

<template>
    <RouterLink class="carte" id="editer" :to="`/si/client/offres/${idPublication}/editor-cv`">
        <span class="carte__icone" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 20h9"/>
            <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"/>
          </svg>
        </span>
        <span class="carte__titre">Editer un CV en ligne</span>
        <span class="carte__texte">Créez ou modifiez votre CV directement dans l'éditeur.</span>
        <span class="carte__action">Ouvrir l'éditeur</span>
    </RouterLink>
</template>