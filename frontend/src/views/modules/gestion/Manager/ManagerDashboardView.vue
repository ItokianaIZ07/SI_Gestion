<script setup>
import { onMounted, ref } from 'vue'
import api from '@/services/api.js'
import HeroBanner from '@/components/ui/HeroBanner.vue'
import VisionDepartementPanel from '@/components/Manager/VisionDepartementPanel.vue'
import ManagerDepartmentBudget from '@/components/Manager/ManagerDepartmentBudget.vue'

const dashboard = ref(null)
const chargement = ref(true)
const erreur = ref('')

async function chargerDashboard() {
  chargement.value = true
  erreur.value = ''
  try {
    const { data } = await api.get('/manager/dashboard')
    dashboard.value = data
  } catch (error) {
    erreur.value = error.response?.data?.message
      ?? 'Impossible de charger les informations du département.'
  } finally {
    chargement.value = false
  }
}

onMounted(chargerDashboard)
</script>

<template>
  <main class="manager-dashboard">
    <HeroBanner
      eyebrow="Espace manager"
      titre="Tableau de bord départemental"
      texte="Retrouvez les indicateurs de votre département et le suivi de son budget."
    >
      <p v-if="dashboard" class="manager-dashboard__departement">
        Département : <strong>{{ dashboard.nomDepartement }}</strong>
      </p>
    </HeroBanner>

    <p v-if="chargement" class="message" role="status">Chargement des informations du département…</p>
    <p v-else-if="erreur" class="message message--erreur" role="alert">{{ erreur }}</p>

    <template v-else-if="dashboard">
      <VisionDepartementPanel :departement="dashboard" />
      <ManagerDepartmentBudget :budget="dashboard.budget" />
    </template>
  </main>
</template>

<style scoped>
.manager-dashboard { max-width: 1240px; margin: 0 auto; display: flex; flex-direction: column; gap: 24px; }
.manager-dashboard__departement { margin: 0; color: #fff; }
.message { padding: 16px; border-radius: var(--radius-md); background: var(--color-card); }
.message--erreur { color: #a12727; }
</style>
