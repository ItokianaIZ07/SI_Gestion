<script setup>
import { useRouter } from 'vue-router';

const router = useRouter()

function retourner(){
    router.back()
}
</script>

<template>
    <button type="button" class="bouton-retour" @click="retourner">
    ← Retour
  </button>
</template>