<script setup>
import FormulaireManifestation from '@/components/Manager/FormulaireManifestation.vue'
import { onMounted, ref } from 'vue'

const departements = ref([])
const erreurChargement = ref('')

async function chargerDepartements() {
  try {
    const response = await fetch('/api/departements')
    if (!response.ok) {
      throw new Error(`Erreur HTTP ${response.status}`)
    }
    departements.value = await response.json()
  } catch (error) {
    erreurChargement.value = `Impossible de charger les départements : ${error.message}`
  }
}

const post = [
  'Nouveau poste',
  'Remplacement',
  'Renfort temporaire',
  'Stage'
]

onMounted(chargerDepartements)
</script>

<template>
  <main class="principal" style="margin-right: 20%;">
    <div class="intro">
      <h1>Manifester un besoin</h1>
      <p>Décrivez le besoin en personnel de votre département. Il sera transmis aux ressources humaines pour traitement.</p>
    </div>

    <p v-if="erreurChargement" role="alert">{{ erreurChargement }}</p>
    <FormulaireManifestation :departements="departements" :postes="post" />
  </main>
</template>