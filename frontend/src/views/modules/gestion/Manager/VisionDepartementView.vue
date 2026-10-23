<script setup>
import { ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import VisionDepartementPanel from '@/components/Manager/VisionDepartementPanel.vue'
import api from '@/services/api.js'

const route = useRoute()
const departement = ref(null)
const chargement = ref(true)
const erreur = ref('')

async function getVisionDepartement(departementId) {
  const { data } = await api.get(`/manager/departements/vision-globale/${departementId}`)
  return data
}

async function chargerVision(departementId) {
  chargement.value = true
  erreur.value = ''
  departement.value = null

  if (!departementId) {
    erreur.value = 'Aucun département sélectionné.'
    chargement.value = false
    return
  }

  try {
    departement.value = await getVisionDepartement(departementId)
  } catch (error) {
    erreur.value =
      error.response?.data?.message ??
      `Impossible de charger la vision départementale (${error.response?.status ?? error.message}).`
  } finally {
    chargement.value = false
  }
}

watch(
  () => route.params.departementId,
  (departementId) => chargerVision(departementId),
  { immediate: true },
)
</script>

<template>
  <div class="page-departement">
    <header class="entete">
      <div>
        <p class="sur-titre">Espace manager</p>
        <h1>{{ departement ? `Vision globale : ${departement.nom}` : 'Vision globale du département' }}</h1>
        <p class="description">
          Consultez les effectifs, les postes et l’avancement des demandes de votre département.
        </p>
      </div>
      <button
        class="actualiser"
        type="button"
        :disabled="chargement"
        @click="chargerVision(route.params.departementId)"
      >
        {{ chargement ? 'Chargement…' : 'Actualiser' }}
      </button>
    </header>

    <div v-if="erreur" class="message message--erreur" role="alert">
      {{ erreur }}
    </div>

    <div v-else-if="chargement" class="message" role="status">
      Chargement des données du département…
    </div>

    <div v-else-if="!departement" class="message">
      Le département demandé est introuvable.
    </div>

    <template v-else-if="departement">
      <RouterLink class="retour" to="/si/app/manager/departements">
        ← Retour à la liste des départements
      </RouterLink>
      <VisionDepartementPanel :departement="departement" />
    </template>
  </div>
</template>

<style scoped>
.page-departement {
  display: grid;
  gap: 24px;
  max-width: 1440px;
  margin: 0 auto;
  color: #1f2937;
}

.entete {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.sur-titre {
  margin: 0 0 8px;
  color: var(--color-primary, #015f09);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #17211a;
  font-size: clamp(1.6rem, 3vw, 2.15rem);
}

.description {
  max-width: 680px;
  margin: 10px 0 0;
  color: #64748b;
  line-height: 1.55;
}

.actualiser {
  flex: 0 0 auto;
  padding: 10px 16px;
  border: 1px solid var(--color-primary, #015f09);
  border-radius: 9px;
  background: var(--color-primary, #015f09);
  color: white;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.actualiser:hover:not(:disabled) {
  filter: brightness(0.92);
}

.actualiser:disabled {
  cursor: wait;
  opacity: 0.7;
}

.retour {
  width: fit-content;
  color: var(--color-primary, #015f09);
  font-weight: 600;
}

.retour:hover {
  text-decoration: underline;
}

.message {
  padding: 18px 20px;
  border: 1px solid #dbe5dc;
  border-radius: 12px;
  background: var(--color-card, #fff);
  color: #475569;
}

.message--erreur {
  border-color: #f3b9b9;
  background: #fff2f2;
  color: #a12828;
}

@media (max-width: 620px) {
  .entete {
    flex-direction: column;
  }
}
</style>
