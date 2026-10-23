<script setup>
import { computed } from 'vue'

// etapes = { rh, finance, direction } avec 'EN_ATTENTE' | 'ACCEPTE' | 'REFUSE'
const props = defineProps({ etapes: { type: Object, required: true } })

const LIBELLES = {
  ACCEPTE: { icone: '✓', classe: 'fin-ok' },
  REFUSE: { icone: '✕', classe: 'fin-ko' },
  EN_ATTENTE: { icone: '⏳', classe: 'fin-wait' },
  BLOQUE: { icone: '–', classe: 'fin-none' } // étape pas encore atteinte
}

// Une étape n'est « atteinte » que si la précédente est acceptée (workflow séquentiel).
const affichage = computed(() => {
  const { rh, finance, direction } = props.etapes
  const f = rh === 'ACCEPTE' ? finance : 'BLOQUE'
  const d = f === 'ACCEPTE' ? direction : 'BLOQUE'
  return [
    { nom: 'RH', etat: rh },
    { nom: 'Finance', etat: f },
    { nom: 'Direction', etat: d }
  ]
})
</script>

<template>
  <div class="fin-steps">
    <span v-for="e in affichage" :key="e.nom" class="fin-badge" :class="LIBELLES[e.etat].classe">
      {{ e.nom }} {{ LIBELLES[e.etat].icone }}
    </span>
  </div>
</template>