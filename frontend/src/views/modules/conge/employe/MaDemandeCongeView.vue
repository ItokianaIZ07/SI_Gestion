<script setup>
// Page 7 : l'employé fait sa propre demande de congé.
// Son identifiant vient de la session (GET /auth/employe/me) et ne peut pas être modifié.
import { computed } from 'vue'
import '@/assets/finance.css'
import { useAuthStore } from '@/stores/authStore'
import FormulaireDemandeConge from '@/components/conge/FormulaireDemandeConge.vue'

const auth = useAuthStore()
const moi = computed(() => auth.employe)
</script>

<template>
  <section class="fin-page">
    <h1>Faire une demande de congé</h1>
    <p v-if="!moi" class="fin-error" role="alert">Session employé introuvable. Reconnectez-vous.</p>
    <template v-else>
      <p class="fin-sub">{{ moi.prenom }} {{ moi.nom }} · matricule {{ moi.matricule }}. Le nombre de jours compte du lundi au vendredi ; le backend contrôle le décompte final.</p>
      <FormulaireDemandeConge :id-employe-fixe="moi.id" lien-demandes="/si/app/employe/conges" libelle-lien="Voir mes congés" />
    </template>
  </section>
</template>
