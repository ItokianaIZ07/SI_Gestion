<script setup>
// Formulaire de demande de congé, partagé par la page RH (choix de l'employé) et l'espace employé
// (identifiant fixé).
// Données : GET /soldes, GET /type-conge (avec règles), GET /manager/employes/par-departement (remplaçant).
import { ref, reactive, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import '@/assets/finance.css'
import '@/assets/conge.css'
import { congeApi } from '@/api/congeApi'
import { messageErreur } from '@/utils/format'
import { jours, joursOuvres, nomComplet } from '@/utils/conge'

const props = defineProps({
  idEmployeFixe: { type: Number, default: null },   // renseigné dans l'espace employé
  lienDemandes: { type: String, default: '' },
  libelleLien: { type: String, default: 'Voir les demandes' }
})

const soldes = ref([])
const types = ref([])
const departements = ref([])
const chargement = ref(true)
const erreurChargement = ref('')
const erreurRemplacants = ref('')

const form = reactive({ idEmploye: '', idTypeConge: '', idEmployeRemplacant: '', dateDebut: '', dateFin: '', motif: '' })
const tentative = ref(false)      // les erreurs de saisie ne s'affichent qu'après un premier envoi
const envoi = ref(false)
const erreurServeur = ref('')
const succes = ref('')

const idEmploye = computed(() => props.idEmployeFixe ?? form.idEmploye)

const employes = computed(() => {
  const map = new Map()
  soldes.value.forEach(s => map.set(s.idEmploye, nomComplet(s)))
  return [...map].map(([id, nom]) => ({ id, nom })).sort((a, b) => a.nom.localeCompare(b.nom))
})

const nombreJours = computed(() => joursOuvres(form.dateDebut, form.dateFin))
const typeChoisi = computed(() => types.value.find(t => t.id === form.idTypeConge))
const regle = computed(() => typeChoisi.value?.regle ?? null)
const deduit = computed(() => !!regle.value?.deductionSolde)
const remplacantObligatoire = computed(() => !!regle.value?.remplacantObligatoire)

// Solde de cet employé pour ce type ; sans objet si le type n'est pas déduit du solde.
const soldeDispo = computed(() => {
  if (!deduit.value) return null
  const ligne = soldes.value.find(s => s.idEmploye === idEmploye.value && s.typeConge === typeChoisi.value?.libelle)
  return ligne ? ligne.solde : null
})
const soldeApres = computed(() => (soldeDispo.value == null ? null : soldeDispo.value - nombreJours.value))

// Remplaçants possibles : tous les collègues sauf le demandeur, ceux de son département d'abord.
const departementDuDemandeur = computed(() =>
  departements.value.find(d => d.employes.some(e => e.id === idEmploye.value))?.idDepartement ?? null)
const groupesRemplacants = computed(() =>
  departements.value
    .map(d => ({
      id: d.idDepartement,
      nom: d.nomDepartement + (d.idDepartement === departementDuDemandeur.value ? ' (même département)' : ''),
      employes: d.employes.filter(e => e.id !== idEmploye.value)
    }))
    .filter(g => g.employes.length)
    .sort((a, b) => (b.id === departementDuDemandeur.value) - (a.id === departementDuDemandeur.value)))

const erreurs = computed(() => {
  const e = {}
  if (!idEmploye.value) e.idEmploye = 'Choisissez un employé.'
  if (!form.idTypeConge) e.idTypeConge = 'Choisissez un type de congé.'
  if (!form.dateDebut) e.dateDebut = 'La date de début est obligatoire.'
  if (!form.dateFin) e.dateFin = 'La date de fin est obligatoire.'
  else if (form.dateDebut && form.dateFin < form.dateDebut) e.dateFin = 'La date de fin doit être après la date de début.'
  if (form.dateDebut && form.dateFin && !e.dateFin) {
    if (nombreJours.value <= 0) e.dateFin = 'Cette période ne contient aucun jour ouvré.'
    else if (deduit.value && soldeDispo.value != null && nombreJours.value > soldeDispo.value)
      e.dateFin = `Solde insuffisant : ${jours(nombreJours.value)} demandés, ${jours(soldeDispo.value)} disponibles.`
  }
  if (remplacantObligatoire.value && !form.idEmployeRemplacant) e.idEmployeRemplacant = 'Ce type de congé exige un remplaçant.'
  return e
})
const voir = (champ) => (tentative.value ? erreurs.value[champ] : '')

async function charger() {
  try { ;[soldes.value, types.value] = await Promise.all([congeApi.getSoldes(), congeApi.getTypesConge()]) }
  catch (e) { erreurChargement.value = messageErreur(e) }
  finally { chargement.value = false }
  // La liste des collègues peut être refusée (403) pour certains rôles : le reste du formulaire reste utilisable.
  try { departements.value = await congeApi.getEmployesParDepartement() }
  catch (e) {
    erreurRemplacants.value = e?.response?.status === 403
      ? 'La liste des collègues n’est pas accessible avec ce compte : le remplaçant ne peut pas être choisi.'
      : 'La liste des collègues n’a pas pu être chargée.'
  }
}

async function envoyer() {
  tentative.value = true
  erreurServeur.value = ''
  succes.value = ''
  if (Object.keys(erreurs.value).length) return
  envoi.value = true
  try {
    await congeApi.creerDemande({
      idEmploye: idEmploye.value,
      idTypeConge: form.idTypeConge,
      idEmployeRemplacant: form.idEmployeRemplacant || null,
      dateDebut: form.dateDebut,
      dateFin: form.dateFin,
      nombreJours: nombreJours.value,
      motif: form.motif.trim() || null
    })
    succes.value = 'La demande a été enregistrée. Elle attend la validation du manager.'
    Object.assign(form, { idTypeConge: '', idEmployeRemplacant: '', dateDebut: '', dateFin: '', motif: '' })
    tentative.value = false
    soldes.value = await congeApi.getSoldes()          // le solde affiché reste à jour
  } catch (e) {
    erreurServeur.value = messageErreur(e)             // ex. « Solde insuffisant » renvoyé par le backend
  } finally {
    envoi.value = false
  }
}

onMounted(charger)
</script>

<template>
  <div>
    <p v-if="erreurChargement" class="fin-error" role="alert">{{ erreurChargement }}</p>
    <p v-if="chargement">Chargement…</p>

    <div v-else class="cg-grille">
      <form class="fin-card cg-form" novalidate @submit.prevent="envoyer">
        <div class="fin-grid">
          <label v-if="idEmployeFixe == null" class="fin-label">Employé *
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

        <label v-if="typeChoisi" class="fin-label">
          Remplaçant {{ remplacantObligatoire ? '*' : '(facultatif)' }}
          <select v-model="form.idEmployeRemplacant" class="fin-champ" :disabled="!groupesRemplacants.length"
                  :aria-invalid="!!voir('idEmployeRemplacant')">
            <option value="">{{ remplacantObligatoire ? 'Choisir un collègue…' : 'Aucun remplaçant' }}</option>
            <optgroup v-for="g in groupesRemplacants" :key="g.id" :label="g.nom">
              <option v-for="e in g.employes" :key="e.id" :value="e.id">{{ e.prenom }} {{ e.nom }} ({{ e.matricule }})</option>
            </optgroup>
          </select>
          <span v-if="voir('idEmployeRemplacant')" class="cg-err">{{ voir('idEmployeRemplacant') }}</span>
          <span v-else-if="erreurRemplacants" class="fin-hint">{{ erreurRemplacants }}</span>
          <span v-else class="fin-hint">Le remplaçant reçoit une proposition qu’il peut accepter ou refuser.</span>
        </label>

        <label class="fin-label">Motif
          <textarea v-model="form.motif" class="fin-champ" rows="3" maxlength="500" placeholder="Facultatif"></textarea>
        </label>

        <p v-if="erreurServeur" class="fin-error" role="alert">{{ erreurServeur }}</p>
        <p v-if="succes" class="cg-ok" role="status">{{ succes }}
          <RouterLink v-if="lienDemandes" :to="lienDemandes">{{ libelleLien }}</RouterLink>
        </p>

        <div class="cg-actions">
          <button type="submit" class="fin-btn fin-btn-pri" :disabled="envoi">{{ envoi ? 'Envoi…' : 'Envoyer la demande' }}</button>
        </div>
      </form>

      <aside class="fin-card">
        <h2>{{ typeChoisi ? typeChoisi.libelle : 'Solde et règles' }}</h2>
        <p v-if="!typeChoisi" class="fin-hint">Choisissez un type de congé pour voir son solde et ses règles.</p>
        <template v-else>
          <div class="cg-ligne"><span>Disponible</span><strong>{{ deduit ? jours(soldeDispo) : 'Non déduit' }}</strong></div>
          <div class="cg-ligne"><span>Cette demande</span><strong>{{ nombreJours ? jours(nombreJours) : '—' }}</strong></div>
          <div v-if="deduit" class="cg-ligne"><span>Après la demande</span>
            <strong :class="{ 'cg-neg': soldeApres != null && soldeApres < 0 }">{{ soldeApres == null || !nombreJours ? '—' : jours(soldeApres) }}</strong>
          </div>
          <ul class="cg-regles">
            <li>{{ deduit ? '✓ Déduit du solde de congés' : '• Pas de déduction du solde' }}</li>
            <li v-if="regle.remplacantObligatoire">✓ Remplaçant obligatoire</li>
            <li v-if="regle.justificatifObligatoire">✓ Justificatif obligatoire : à remettre au RH</li>
          </ul>
        </template>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.cg-form h2, aside h2 { margin: 0 0 6px; font-size: 16px; }
</style>
