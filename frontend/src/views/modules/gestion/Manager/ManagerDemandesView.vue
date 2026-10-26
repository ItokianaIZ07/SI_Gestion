<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '@/services/api.js'

const dashboard = ref(null)
const chargement = ref(true)
const erreur = ref('')
const demandes = computed(() => dashboard.value?.demandes ?? [])
const recherche = ref('')
const statutSelectionne = ref('')
const anneeSelectionnee = ref('')

const libellesStatuts = {
  EN_ATTENTE_RH: 'En attente RH',
  EN_ATTENTE_FINANCE: 'En attente Finance',
  EN_ATTENTE_DIRECTION: 'En attente Direction',
  ACCEPTE: 'Acceptée',
  REFUSE: 'Refusée',
  ANNULE: 'Annulée',
}

function libelleStatut(statut) {
  return libellesStatuts[statut] ?? statut
}

function classeStatut(statut) {
  if (statut === 'ACCEPTE') return 'statut--accepte'
  if (statut === 'REFUSE' || statut === 'ANNULE') return 'statut--ferme'
  return 'statut--attente'
}

function formaterDate(date) {
  if (!date) return '—'
  return new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium' }).format(new Date(date))
}

const anneesDisponibles = computed(() => [...new Set(
  demandes.value
    .map((demande) => demande.dateDemande ? new Date(demande.dateDemande).getFullYear() : null)
    .filter(Boolean),
)].sort((a, b) => b - a))

const demandesFiltrees = computed(() => {
  const texte = recherche.value.trim().toLocaleLowerCase('fr')
  return demandes.value.filter((demande) => {
    const correspondRecherche = !texte
      || String(demande.id).includes(texte)
      || demande.poste?.toLocaleLowerCase('fr').includes(texte)
    const correspondStatut = !statutSelectionne.value
      || demande.statut === statutSelectionne.value
    const correspondAnnee = !anneeSelectionnee.value
      || (demande.dateDemande
        && new Date(demande.dateDemande).getFullYear() === Number(anneeSelectionnee.value))

    return correspondRecherche && correspondStatut && correspondAnnee
  })
})

function reinitialiserFiltres() {
  recherche.value = ''
  statutSelectionne.value = ''
  anneeSelectionnee.value = ''
}

async function chargerDemandes() {
  chargement.value = true
  erreur.value = ''
  try {
    const { data } = await api.get('/manager/dashboard')
    dashboard.value = data
  } catch (error) {
    erreur.value = error.response?.data?.message
      ?? 'Impossible de charger les demandes de votre département.'
  } finally {
    chargement.value = false
  }
}

onMounted(chargerDemandes)
</script>

<template>
  <main class="page-demandes">
    <header class="entete">
      <div>
        <p class="sur-titre">Espace manager</p>
        <h1>Liste des demandes</h1>
        <p class="description">
          Besoins de recrutement liés au département
          <strong>{{ dashboard?.nomDepartement ?? '…' }}</strong>.
        </p>
      </div>
      <button class="actualiser" type="button" :disabled="chargement" @click="chargerDemandes">
        {{ chargement ? 'Chargement…' : 'Actualiser' }}
      </button>
    </header>

    <p v-if="chargement" class="message" role="status">
      Chargement des demandes du département…
    </p>
    <p v-else-if="erreur" class="message message--erreur" role="alert">
      {{ erreur }}
    </p>
    <section v-else class="bloc">
      <div class="bloc__entete">
        <div>
          <h2>Besoins du département</h2>
          <p>Suivi de toutes les demandes liées aux postes de votre département.</p>
        </div>
        <span class="compte">{{ demandesFiltrees.length }} sur {{ demandes.length }} demande(s)</span>
      </div>

      <div class="filtres" role="search" aria-label="Filtres des demandes">
        <label class="filtre">
          <span>Rechercher</span>
          <input v-model="recherche" type="search" placeholder="Identifiant ou poste">
        </label>
        <label class="filtre">
          <span>État</span>
          <select v-model="statutSelectionne">
            <option value="">Tous les états</option>
            <option v-for="(libelle, code) in libellesStatuts" :key="code" :value="code">
              {{ libelle }}
            </option>
          </select>
        </label>
        <label class="filtre">
          <span>Année</span>
          <select v-model="anneeSelectionnee">
            <option value="">Toutes les années</option>
            <option v-for="annee in anneesDisponibles" :key="annee" :value="String(annee)">
              {{ annee }}
            </option>
          </select>
        </label>
        <button
          v-if="recherche || statutSelectionne || anneeSelectionnee"
          class="reinitialiser"
          type="button"
          @click="reinitialiserFiltres"
        >
          Réinitialiser
        </button>
      </div>

      <div v-if="demandesFiltrees.length" class="table-conteneur">
        <table>
          <thead>
            <tr>
              <th>Demande</th>
              <th>Poste</th>
              <th>Effectif demandé</th>
              <th>État</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="demande in demandesFiltrees" :key="demande.id">
              <td>#{{ demande.id }}</td>
              <td>{{ demande.poste }}</td>
              <td>{{ demande.effectif }}</td>
              <td>
                <span class="statut" :class="classeStatut(demande.statut)">
                  {{ libelleStatut(demande.statut) }}
                </span>
              </td>
              <td>{{ formaterDate(demande.dateDemande) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else-if="demandes.length" class="vide">
        Aucune demande ne correspond aux filtres sélectionnés.
      </p>
      <p v-else class="vide">Aucune demande n’a encore été enregistrée pour ce département.</p>
    </section>
  </main>
</template>

<style scoped>
.page-demandes { display: grid; gap: 24px; max-width: 1440px; margin: 0 auto; color: #1f2937; }
.entete { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }
.sur-titre { margin: 0 0 8px; color: var(--color-primary, #015f09); font-size: .82rem; font-weight: 700; letter-spacing: .06em; text-transform: uppercase; }
h1 { margin: 0; color: #17211a; font-size: clamp(1.6rem, 3vw, 2.15rem); }
.description { margin: 10px 0 0; color: #64748b; line-height: 1.55; }
.actualiser { flex: 0 0 auto; padding: 10px 16px; border: 1px solid var(--color-primary, #015f09); border-radius: 9px; background: var(--color-primary, #015f09); color: #fff; font: inherit; font-weight: 600; cursor: pointer; }
.actualiser:hover:not(:disabled) { filter: brightness(.92); }
.actualiser:disabled { cursor: wait; opacity: .7; }
.message, .bloc { padding: 22px; border: 1px solid #e5e7eb; border-radius: 14px; background: var(--color-card, #fff); box-shadow: 0 3px 12px rgba(15, 23, 42, .04); }
.message { color: #475569; }
.message--erreur { border-color: #f3b9b9; background: #fff2f2; color: #a12828; }
.bloc__entete { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
h2 { margin: 0; color: #1f2937; font-size: 1.15rem; }
.bloc__entete p { margin: 6px 0 0; color: #64748b; font-size: .9rem; }
.compte { color: #64748b; font-size: .88rem; white-space: nowrap; }
.filtres { display: flex; align-items: flex-end; flex-wrap: wrap; gap: 12px; margin-bottom: 18px; }
.filtre { display: grid; min-width: 175px; gap: 6px; color: #475569; font-size: .84rem; font-weight: 600; }
.filtre input, .filtre select { min-height: 40px; padding: 8px 10px; border: 1px solid #d7e0d8; border-radius: 8px; background: #fff; color: #1f2937; font: inherit; }
.reinitialiser { min-height: 40px; padding: 8px 12px; border: 1px solid #d7e0d8; border-radius: 8px; background: #fff; color: var(--color-primary, #015f09); font: inherit; font-weight: 600; cursor: pointer; }
.reinitialiser:hover { background: #f1f8f1; }
.table-conteneur { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; text-align: left; }
th, td { padding: 13px 12px; border-bottom: 1px solid #e5e7eb; white-space: nowrap; }
th { color: #64748b; font-size: .8rem; font-weight: 600; text-transform: uppercase; }
td { color: #334155; font-size: .92rem; }
.statut { display: inline-flex; padding: 5px 9px; border-radius: 999px; font-size: .8rem; font-weight: 600; }
.statut--attente { background: #fff7dd; color: #8a5b00; }
.statut--accepte { background: #e5f6e8; color: #176c22; }
.statut--ferme { background: #f1f5f9; color: #475569; }
.vide { margin: 0; padding: 18px 0 4px; color: #64748b; }
@media (max-width: 620px) {
  .entete, .bloc__entete { align-items: flex-start; flex-direction: column; }
  .bloc { padding: 16px; }
}
</style>
