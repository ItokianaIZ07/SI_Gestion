<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import FormulaireDepartement from '@/components/Manager/FormulaireDepartement.vue'
import api from '@/services/api.js'
import '@/assets/gestion.css'

const router = useRouter()
const utilisateurs = ref([])
const chargement = ref(true)
const envoiEnCours = ref(false)
const erreurChargement = ref('')
const erreurEnvoi = ref('')

async function chargerUtilisateurs() {
  chargement.value = true
  erreurChargement.value = ''

  try {
    const utilisateurResponse = await api.get('/manager/utilisateurs')
    utilisateurs.value = utilisateurResponse.data
  } catch (error) {
    erreurChargement.value = `Impossible de charger les utilisateurs : ${error.message}`
  } finally {
    chargement.value = false
  }
}

async function creerDepartement(donnees) {
  envoiEnCours.value = true
  erreurEnvoi.value = ''

  try {
    await api.post('/manager/departements', donnees)
    await router.push({ name: 'ListeDepartements' })
  } catch (error) {
    erreurEnvoi.value = `Impossible de créer le département : ${error.message}`
  } finally {
    envoiEnCours.value = false
  }
}

onMounted(chargerUtilisateurs)
</script>

<template>
  <main class="g-page">
    <header class="g-entete">
      <div>
        <h1>Créer un département</h1>
        <p>Renseignez le nom, le manager et l’effectif maximal du nouveau département.</p>
      </div>
      <div class="g-entete__actions">
        <RouterLink class="g-btn" to="/si/app/manager/departements">Retour à la liste</RouterLink>
      </div>
    </header>

    <p v-if="erreurChargement" class="g-alerte g-alerte--erreur" role="alert">
      {{ erreurChargement }}
    </p>
    <p v-else-if="chargement" class="g-alerte" role="status">Chargement des utilisateurs…</p>
    <p v-if="erreurEnvoi" class="g-alerte g-alerte--erreur" role="alert">
      {{ erreurEnvoi }}
    </p>

    <section v-if="!chargement && !erreurChargement" class="g-carte" aria-label="Formulaire de création du département">
      <FormulaireDepartement
        :utilisateurs="utilisateurs"
        :envoi-en-cours="envoiEnCours"
        @submit="creerDepartement"
      />
      <p v-if="!utilisateurs.length" class="g-vide">
        <strong>Aucun utilisateur disponible</strong>
        Impossible de créer un département sans manager.
      </p>
    </section>
  </main>
</template>
