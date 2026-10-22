<script setup>
import FormulaireManifestation from '@/components/Manager/FormulaireManifestation.vue'
import { computed, onMounted, ref } from 'vue'
import api  from '@/services/api.js'

const departements = ref([])
const typeBesoin = ref([])
const postes = ref([])
const utilisateurs = ref([])
const employes = ref([])
const typeContrat = ref([])
const departementSelectionne = ref('')

const erreurChargement = ref('')

const appartientAuDepartement = (element, departementId) =>
  Number(element.departement?.id ?? element.departementId ?? element.idDepartement) ===
  Number(departementId)

const postesFiltres = computed(() =>
  departementSelectionne.value
    ? postes.value.filter((poste) =>
        appartientAuDepartement(poste, departementSelectionne.value),
      )
    : [],
)

const employesFiltres = computed(() =>
  departementSelectionne.value
    ? employes.value.filter((employe) =>
        appartientAuDepartement(employe, departementSelectionne.value),
      )
    : [],
)

const utilisateursFiltres = computed(() => {
  if (!departementSelectionne.value) return []

  const departement = departements.value.find(
    (item) => Number(item.id) === Number(departementSelectionne.value),
  )
  const idManager =
    departement?.manager?.id ??
    departement?.idUtilisateurManager ??
    departement?.id_utilisateur_manager

  return utilisateurs.value.filter((utilisateur) =>
    Number(utilisateur.id) === Number(idManager),
  )
})

const chargerDonnees = async () => {
  try {
    const typeBesoinReponse = await api.get('/manager/types-besoin')
    const departementsReponse = await api.get('/manager/departements')
    const postesReponse = await api.get('/manager/postes')
    const employesReponse = await api.get('/manager/employes')
    const typeContratReponse = await api.get('/manager/types-contrat')
    const utilisateursReponse = await api.get('/manager/utilisateurs')

    typeBesoin.value = typeBesoinReponse.data
    departements.value = departementsReponse.data
    postes.value = postesReponse.data
    employes.value = employesReponse.data
    typeContrat.value = typeContratReponse.data
    utilisateurs.value = utilisateursReponse.data
  } catch (error) {
    erreurChargement.value = `Impossible de charger les données : ${error.response?.status ?? error.message}`
  }
}

onMounted(chargerDonnees)
</script>

<template>
  <main class="principal" style="margin-right: 20%;">
    <div class="intro">
      <h1>Manifester un besoin</h1>
      <p>Décrivez le besoin en personnel de votre département. Il sera transmis aux ressources humaines pour traitement.</p>
    </div>

    <p v-if="erreurChargement" role="alert">{{ erreurChargement }}</p>
    <FormulaireManifestation
      :departements="departements"
      v-model:departement-selectionne="departementSelectionne"
      :types-besoin="typeBesoin"
      :postes="postesFiltres"
      :utilisateurs="utilisateursFiltres"
      :employes="employesFiltres"
      :types-contrat="typeContrat"
    />
  </main>
</template>