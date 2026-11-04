<script setup>
import { ref, computed, onMounted } from 'vue'
import '@/assets/finance.css'
import { financeApi } from '@/api/financeApi'
import { ariary, dateFr, messageErreur } from '@/utils/format'
import GrilleForm from '@/components/finance/GrilleForm.vue'
import Pagination from '@/components/ui/Pagination.vue'
import { usePagination } from '@/composable/usePagination'

const grilles = ref([])
const postes = ref([])
const formOuvert = ref(false)
const enEdition = ref(null)      // null = nouvelle grille
const erreurServeur = ref('')

async function charger() {
  const [g, p] = await Promise.all([financeApi.getGrilles(), financeApi.getPostes()])
  grilles.value = g
  postes.value = p
}

function nouvelle() { enEdition.value = null; erreurServeur.value = ''; formOuvert.value = true }
function modifier(g) { enEdition.value = g; erreurServeur.value = ''; formOuvert.value = true }

async function enregistrer(valeurs) {
  erreurServeur.value = ''
  try {
    if (enEdition.value) await financeApi.modifierGrille(enEdition.value.id, valeurs)
    else await financeApi.creerGrille(valeurs)
    formOuvert.value = false
    await charger()
  } catch (e) {
    erreurServeur.value = messageErreur(e)   // ex. chevauchement de période (409)
  }
}

// Recherche, filtre « période » et pagination
const recherche = ref('')
const filtrePeriode = ref('')   // '' | 'vigueur' | 'passee' | 'future'
const aujourdhui = () => new Date().toISOString().slice(0, 10)
const filtrees = computed(() => {
  const q = recherche.value.trim().toLowerCase()
  const jour = aujourdhui()
  return grilles.value.filter((g) => {
    const debut = String(g.dateDebutValidite ?? '').slice(0, 10)
    const fin = g.dateFinValidite ? String(g.dateFinValidite).slice(0, 10) : null
    const etat = debut > jour ? 'future' : fin && fin < jour ? 'passee' : 'vigueur'
    if (filtrePeriode.value && etat !== filtrePeriode.value) return false
    return !q || `${g.poste ?? ''} ${g.departement ?? ''}`.toLowerCase().includes(q)
  })
})
const pg = usePagination(filtrees, 10)

onMounted(charger)
</script>

<template>
  <section class="fin-page">
    <h1>Grille salariale</h1>
    <p class="fin-sub">Fourchette de salaire mensuel par poste et sa période de validité.</p>

    <div class="fin-toolbar">
      <div style="display:flex;gap:8px;flex-wrap:wrap">
        <input v-model="recherche" type="search" placeholder="Rechercher un poste ou un département…"
          aria-label="Rechercher une grille" style="min-width:260px" />
        <select v-model="filtrePeriode" aria-label="Filtrer par période">
          <option value="">Toutes les périodes</option>
          <option value="vigueur">En vigueur</option>
          <option value="future">À venir</option>
          <option value="passee">Passées</option>
        </select>
      </div>
      <button class="fin-btn fin-btn-pri" type="button" @click="nouvelle">+ Nouvelle grille</button>
    </div>

    <GrilleForm v-if="formOuvert" :postes="postes" :grille="enEdition" :erreur-serveur="erreurServeur"
      @enregistrer="enregistrer" @annuler="formOuvert = false" />

    <div class="fin-card fin-scroll">
      <table class="fin-table">
        <thead>
          <tr>
            <th>Poste</th><th>Département</th>
            <th class="fin-num">Minimum</th><th class="fin-num">Maximum</th>
            <th>Début</th><th>Fin</th><th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="g in pg.items" :key="g.id">
            <td><strong>{{ g.poste }}</strong></td>
            <td>{{ g.departement }}</td>
            <td class="fin-num">{{ ariary(g.valeurMin) }}</td>
            <td class="fin-num">{{ ariary(g.valeurMax) }}</td>
            <td>{{ dateFr(g.dateDebutValidite) }}</td>
            <td>{{ dateFr(g.dateFinValidite) }}</td>
            <td><button class="fin-btn fin-btn-sec" type="button" @click="modifier(g)">Modifier</button></td>
          </tr>
          <tr v-if="!filtrees.length"><td colspan="7">{{ grilles.length ? 'Aucune grille ne correspond aux filtres.' : 'Aucune grille définie.' }}</td></tr>
        </tbody>
      </table>
    </div>
    <Pagination v-bind="pg.barre" @update:page="pg.aller" />
  </section>
</template>