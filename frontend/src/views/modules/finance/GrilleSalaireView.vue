<script setup>
import { ref, onMounted } from 'vue'
import '../finance.css'
import { financeApi } from '../api/financeApi'
import { ariary, dateFr, messageErreur } from '../utils/format'
import GrilleForm from '../components/GrilleForm.vue'

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

onMounted(charger)
</script>

<template>
  <section class="fin-page">
    <h1>Grille salariale</h1>
    <p class="fin-sub">Fourchette de salaire mensuel par poste et sa période de validité.</p>

    <div class="fin-toolbar">
      <span></span>
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
          <tr v-for="g in grilles" :key="g.id">
            <td><strong>{{ g.poste }}</strong></td>
            <td>{{ g.departement }}</td>
            <td class="fin-num">{{ ariary(g.valeurMin) }}</td>
            <td class="fin-num">{{ ariary(g.valeurMax) }}</td>
            <td>{{ dateFr(g.dateDebutValidite) }}</td>
            <td>{{ dateFr(g.dateFinValidite) }}</td>
            <td><button class="fin-btn fin-btn-sec" type="button" @click="modifier(g)">Modifier</button></td>
          </tr>
          <tr v-if="!grilles.length"><td colspan="7">Aucune grille définie.</td></tr>
        </tbody>
      </table>
    </div>
  </section>
</template>