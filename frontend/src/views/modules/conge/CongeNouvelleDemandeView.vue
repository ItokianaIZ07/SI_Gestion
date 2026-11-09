<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import '@/assets/finance.css'
import { congeApi } from '@/api/congeApi'
import { messageErreur } from '@/utils/format'
import { jours, joursOuvres, nomComplet } from '@/utils/conge'

const soldes = ref([])
const types = ref([])
const chargement = ref(true)
const erreurChargement = ref('')

const form = reactive({ idEmploye: '', idTypeConge: '', dateDebut: '', dateFin: '', motif: '' })
const tentative = ref(false)      // les erreurs de saisie ne s'affichent qu'après un premier envoi
const envoi = ref(false)
const erreurServeur = ref('')
const succes = ref('')

const employes = computed(() => {
  const map = new Map()
  soldes.value.forEach(s => map.set(s.idEmploye, nomComplet(s)))
  return [...map].map(([id, nom]) => ({ id, nom })).sort((a, b) => a.nom.localeCompare(b.nom))
})

const nombreJours = computed(() => joursOuvres(form.dateDebut, form.dateFin))
const typeChoisi = computed(() => types.value.find(t => t.id === form.idTypeConge))

// Solde de cet employé pour ce type (null si l'API ne le fournit pas).
const soldeDispo = computed(() => {
  const ligne = soldes.value.find(s => s.idEmploye === form.idEmploye && s.typeConge === typeChoisi.value?.libelle)
  return ligne ? ligne.solde : null
})
const soldeApres = computed(() => (soldeDispo.value == null ? null : soldeDispo.value - nombreJours.value))

const erreurs = computed(() => {
  const e = {}
  if (!form.idEmploye) e.idEmploye = 'Choisissez un employé.'
  if (!form.idTypeConge) e.idTypeConge = 'Choisissez un type de congé.'
  if (!form.dateDebut) e.dateDebut = 'La date de début est obligatoire.'
  if (!form.dateFin) e.dateFin = 'La date de fin est obligatoire.'
  else if (form.dateDebut && form.dateFin < form.dateDebut) e.dateFin = 'La date de fin doit être après la date de début.'
  if (form.dateDebut && form.dateFin && !e.dateFin) {
    if (nombreJours.value <= 0) e.dateFin = 'Cette période ne contient aucun jour ouvré.'
    else if (soldeDispo.value != null && nombreJours.value > soldeDispo.value)
      e.dateFin = `Solde insuffisant : ${jours(nombreJours.value)} demandés, ${jours(soldeDispo.value)} disponibles.`
  }
  return e
})
const voir = (champ) => (tentative.value ? erreurs.value[champ] : '')

async function envoyer() {
  tentative.value = true
  erreurServeur.value = ''
  succes.value = ''
  if (Object.keys(erreurs.value).length) return
  envoi.value = true
  try {
    await congeApi.creerDemande({
      idEmploye: form.idEmploye,
      idTypeConge: form.idTypeConge,
      dateDebut: form.dateDebut,
      dateFin: form.dateFin,
      nombreJours: nombreJours.value,
      motif: form.motif.trim() || null
    })
    succes.value = 'La demande a été enregistrée. Elle attend la validation du manager.'
    Object.assign(form, { idTypeConge: '', dateDebut: '', dateFin: '', motif: '' })
    tentative.value = false
    soldes.value = await congeApi.getSoldes()          // le solde affiché reste à jour
  } catch (e) {
    erreurServeur.value = messageErreur(e)             // ex. « Solde insuffisant » renvoyé par le backend
  } finally {
    envoi.value = false
  }
}

onMounted(async () => {
  try { ;[soldes.value, types.value] = await Promise.all([congeApi.getSoldes(), congeApi.getTypesConge()]) }
  catch (e) { erreurChargement.value = messageErreur(e) }
  finally { chargement.value = false }
})
</script>

<template>
  <section class="fin-page">
    <h1>Nouvelle demande de congé</h1>
    <p class="fin-sub">Le nombre de jours compte du lundi au vendredi ; le backend contrôle le décompte final.</p>

    <p v-if="erreurChargement" class="fin-error" role="alert">{{ erreurChargement }}</p>
    <p v-if="chargement">Chargement…</p>

    <div v-else class="cg-grille">
      <form class="fin-card" novalidate @submit.prevent="envoyer">
        <div class="fin-grid">
          <label class="fin-label">Employé *
            <select v-model="form.idEmploye" class="fin-champ" :aria-invalid="!!voir('idEmploye')">
              <option value="">Choisir…</option>
              <option v-for="e in employes" :key="e.id" :value="e.id">{{ e.nom }}</option>
            </select>
            <span v-if="voir('idEmploye')" class="cg-err">{{ voir('idEmploye') }}</span>
          </label>
          <label class="fin-label">Type de congé *
            <select v-model="form.idTypeConge" class="fin-champ" :aria-invalid="!!voir('idTypeConge')">
              <option value="">Choisir…</option>
              <option v-for="t in types" :key="t.id" :value="t.id">{{ t.libelle }}</option>
            </select>
            <span v-if="voir('idTypeConge')" class="cg-err">{{ voir('idTypeConge') }}</span>
          </label>
          <label class="fin-label">Date de début *
            <input v-model="form.dateDebut" class="fin-champ" type="date" :aria-invalid="!!voir('dateDebut')">
            <span v-if="voir('dateDebut')" class="cg-err">{{ voir('dateDebut') }}</span>
          </label>
          <label class="fin-label">Date de fin *
            <input v-model="form.dateFin" class="fin-champ" type="date" :min="form.dateDebut || null" :aria-invalid="!!voir('dateFin')">
            <span v-if="voir('dateFin')" class="cg-err">{{ voir('dateFin') }}</span>
          </label>
        </div>

        <label class="fin-label">Nombre de jours (calculé)
          <input class="fin-champ" :value="nombreJours ? jours(nombreJours) : '—'" readonly>
        </label>
        <label class="fin-label cg-motif">Motif
          <textarea v-model="form.motif" class="fin-champ" rows="3" maxlength="500" placeholder="Facultatif"></textarea>
        </label>

        <p v-if="erreurServeur" class="fin-error" role="alert">{{ erreurServeur }}</p>
        <p v-if="succes" class="cg-ok" role="status">{{ succes }} <RouterLink to="/si/app/rh/conges/demandes">Voir les demandes</RouterLink></p>

        <div class="cg-actions">
          <button type="submit" class="fin-btn fin-btn-pri" :disabled="envoi">{{ envoi ? 'Envoi…' : 'Envoyer la demande' }}</button>
        </div>
      </form>

      <aside class="fin-card">
        <h2>Solde de l’employé</h2>
        <p class="fin-hint">{{ typeChoisi && form.idEmploye ? typeChoisi.libelle : 'Choisissez un employé et un type.' }}</p>
        <div class="cg-ligne"><span>Disponible</span><strong>{{ jours(soldeDispo) }}</strong></div>
        <div class="cg-ligne"><span>Cette demande</span><strong>{{ nombreJours ? jours(nombreJours) : '—' }}</strong></div>
        <div class="cg-ligne"><span>Après la demande</span>
          <strong :class="{ 'cg-neg': soldeApres != null && soldeApres < 0 }">{{ soldeApres == null || !nombreJours ? '—' : jours(soldeApres) }}</strong>
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.cg-grille { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 20px; align-items: start; }
.cg-grille form { display: grid; gap: 14px; }
.cg-grille textarea { padding: 10px 12px; resize: vertical; }
.cg-err { color: #9B1C13; font-size: 13px; font-weight: 400; }
.cg-ok { background: #E3F3EA; color: #14633B; border-radius: 8px; padding: 10px 14px; margin: 0; font-size: 14px; }
.cg-actions { display: flex; justify-content: flex-end; }
.cg-ligne { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #E7EEE9; }
.cg-neg { color: #9B1C13; }
h2 { margin: 0 0 6px; font-size: 16px; }
@media (max-width: 900px) { .cg-grille { grid-template-columns: 1fr; } }
</style>