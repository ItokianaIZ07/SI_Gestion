<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import '@/assets/finance.css'
import { congeApi } from '@/api/congeApi'
import { useAuthStore } from '@/stores/authStore'
import { dateFr, messageErreur } from '@/utils/format'
import { jours, nomComplet, statutDe, etapeCourante, etapeDe, decisionDe, dateHeure } from '@/utils/conge'
import { usePagination } from '@/composable/usePagination'
import Pagination from '@/components/ui/Pagination.vue'

const auth = useAuthStore()
const estManager = computed(() => auth.user?.role === 'MANAGER')

const demandes = ref([])
const chargement = ref(true)
const erreur = ref('')
const confirmation = ref('')
const recherche = ref('')

// Fenêtre de décision
const selection = ref(null)
const historique = ref([])
const historiqueErreur = ref('')
const commentaire = ref('')
const aConfirmer = ref(null)          // null | 'ACCEPTE' | 'REFUSE' : étape de confirmation avant d'envoyer
const envoi = ref(false)
const erreurDecision = ref('')
const modal = ref(null)
watch(selection, async (v) => { if (v) { await nextTick(); modal.value?.focus() } })

// 401 : la session a disparu (redémarrage du serveur, déconnexion…) — sans corps de réponse exploitable.
const erreurTexte = (e) => (e?.response?.status === 401 ? 'Votre session a expiré. Reconnectez-vous.' : messageErreur(e))

// Les plus anciennes demandes en premier.
const filtrees = computed(() => {
  const q = recherche.value.trim().toLowerCase()
  return demandes.value
    .filter(d => !q || [nomComplet(d), d.matriculeEmploye, d.typeConge, d.motif].join(' ').toLowerCase().includes(q))
    .sort((a, b) => (a.dateDemande ?? '').localeCompare(b.dateDemande ?? ''))
})
const pg = usePagination(filtrees, 8)

async function charger(initial = false) {
  if (initial) chargement.value = true
  erreur.value = ''
  try { demandes.value = await congeApi.getDemandesEnAttente() }
  catch (e) { erreur.value = erreurTexte(e) }
  finally { chargement.value = false }
}

async function ouvrir(d) {
  selection.value = d
  commentaire.value = ''
  aConfirmer.value = null
  erreurDecision.value = ''
  historique.value = []
  historiqueErreur.value = ''
  try {
    const h = await congeApi.getHistoriqueValidations(d.idDemande)
    if (selection.value?.idDemande === d.idDemande) historique.value = h
  } catch {
    if (selection.value?.idDemande === d.idDemande) historiqueErreur.value = 'L’historique des décisions n’a pas pu être chargé.'
  }
}

function fermer() {
  if (!envoi.value) selection.value = null
}

// 1) On vérifie la saisie puis on demande confirmation.
function demander(decision) {
  erreurDecision.value = ''
  if (decision === 'REFUSE' && !commentaire.value.trim()) {
    erreurDecision.value = 'Le commentaire est obligatoire pour refuser une demande.'
    return
  }
  aConfirmer.value = decision
}

// 2) Après confirmation, on envoie la décision au backend (il revérifie l'étape, le rôle et le solde).
async function confirmer() {
  const d = selection.value
  envoi.value = true
  erreurDecision.value = ''
  try {
    const r = await congeApi.deciderDemande(d.idDemande, aConfirmer.value, commentaire.value.trim())
    const suite = r.statutGlobal === 'EN_ATTENTE_RH' ? ' et transmise aux RH'
      : r.statutGlobal === 'VALIDEE' ? ' : le congé est validé' : ''
    confirmation.value = r.decision === 'REFUSE'
      ? `La demande de ${nomComplet(d)} a été refusée.`
      : `La demande de ${nomComplet(d)} a été acceptée${suite}.`
    envoi.value = false
    selection.value = null
    await charger()
  } catch (e) {
    erreurDecision.value = erreurTexte(e)       // ex. « Solde insuffisant », étape déjà traitée, accès refusé
    aConfirmer.value = null
    envoi.value = false
    if ([404, 409].includes(e?.response?.status)) charger()    // la demande a changé : on rafraîchit la liste
  }
}

onMounted(() => charger(true))
</script>

<template>
  <section class="fin-page">
    <h1>Demandes de congé à valider</h1>
    <p class="fin-sub">
      Étape {{ estManager ? 'manager' : 'RH' }} :
      {{ estManager ? 'les demandes des employés de votre département' : 'les demandes déjà acceptées par les managers' }}.
    </p>

    <p v-if="confirmation" class="cg-ok" role="status">{{ confirmation }}</p>
    <p v-if="erreur" class="fin-error" role="alert">{{ erreur }}</p>
    <p v-if="chargement">Chargement…</p>

    <template v-else>
      <div class="fin-toolbar">
        <label class="fin-label">Recherche
          <input v-model="recherche" class="fin-champ" type="search" placeholder="Nom, matricule, type, motif…">
        </label>
      </div>

      <div class="fin-card cg-liste">
        <div class="fin-scroll">
          <table class="fin-table">
            <thead>
              <tr><th>Employé</th><th>Type</th><th>Période</th><th class="fin-num">Jours</th><th>Étape</th><th>Demandée le</th><th></th></tr>
            </thead>
            <tbody>
              <tr v-for="d in pg.items" :key="d.idDemande">
                <td><strong>{{ nomComplet(d) }}</strong><br><small>{{ d.matriculeEmploye }}</small></td>
                <td>{{ d.typeConge }}</td>
                <td>{{ dateFr(d.dateDebut) }} → {{ dateFr(d.dateFin) }}</td>
                <td class="fin-num">{{ jours(d.nombreJours) }}</td>
                <td><span class="fin-badge" :class="statutDe(d.statutGlobal).classe">{{ etapeCourante(d.statutGlobal) }}</span></td>
                <td>{{ dateFr(d.dateDemande) }}</td>
                <td class="fin-num"><button type="button" class="fin-btn fin-btn-pri" @click="ouvrir(d)">Traiter</button></td>
              </tr>
              <tr v-if="!filtrees.length">
                <td colspan="7">{{ demandes.length ? 'Aucune demande ne correspond.' : 'Aucune demande à valider pour le moment.' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <Pagination v-bind="pg.barre" @update:page="pg.aller" />
      </div>
    </template>

    <!-- Fenêtre de décision -->
    <div v-if="selection" class="cg-fond" @click.self="fermer" @keydown.esc="fermer">
      <div ref="modal" class="fin-card cg-modal" role="dialog" aria-modal="true" aria-labelledby="cg-titre" tabindex="-1">
        <h2 id="cg-titre">Demande #{{ selection.idDemande }} — {{ nomComplet(selection) }}</h2>

        <dl class="cg-dl">
          <dt>Matricule</dt><dd>{{ selection.matriculeEmploye }} · {{ selection.emailEmploye }}</dd>
          <dt>Type de congé</dt><dd>{{ selection.typeConge }}</dd>
          <dt>Période</dt><dd>{{ dateFr(selection.dateDebut) }} → {{ dateFr(selection.dateFin) }}</dd>
          <dt>Nombre de jours</dt><dd>{{ jours(selection.nombreJours) }}</dd>
          <dt>Motif</dt><dd>{{ selection.motif || '—' }}</dd>
          <dt>Étape actuelle</dt>
          <dd><span class="fin-badge" :class="statutDe(selection.statutGlobal).classe">{{ statutDe(selection.statutGlobal).libelle }}</span></dd>
        </dl>

        <h3>Historique des décisions</h3>
        <p v-if="historiqueErreur" class="fin-hint">{{ historiqueErreur }}</p>
        <ul v-else class="cg-hist">
          <li v-for="h in historique" :key="h.idValidation">
            <span class="fin-badge" :class="decisionDe(h.decision).classe">{{ etapeDe(h.etape) }} · {{ decisionDe(h.decision).libelle }}</span>
            <small v-if="h.emailValidateur"> {{ h.emailValidateur }} · {{ dateHeure(h.dateValidation) }}</small>
            <p v-if="h.commentaire">« {{ h.commentaire }} »</p>
          </li>
          <li v-if="!historique.length" class="fin-hint">Aucune décision enregistrée pour le moment.</li>
        </ul>

        <label class="fin-label cg-commentaire">Commentaire <span class="fin-hint">(obligatoire pour refuser)</span>
          <textarea v-model="commentaire" class="fin-champ" rows="3" maxlength="2000" :disabled="envoi || !!aConfirmer"
                    placeholder="Motif ou précision sur votre décision…"></textarea>
        </label>

        <p v-if="erreurDecision" class="fin-error" role="alert">{{ erreurDecision }}</p>

        <!-- Confirmation avant envoi -->
        <div v-if="aConfirmer" class="cg-confirm" role="alert">
          {{ aConfirmer === 'ACCEPTE'
              ? `Confirmer l’acceptation de la demande de ${nomComplet(selection)} ?`
              : `Confirmer le refus de la demande de ${nomComplet(selection)} ? Cette décision est définitive.` }}
        </div>

        <div class="cg-actions">
          <template v-if="!aConfirmer">
            <button type="button" class="fin-btn fin-btn-sec" @click="fermer">Fermer</button>
            <button type="button" class="fin-btn fin-btn-dan" @click="demander('REFUSE')">Refuser</button>
            <button type="button" class="fin-btn fin-btn-pri" @click="demander('ACCEPTE')">Accepter</button>
          </template>
          <template v-else>
            <button type="button" class="fin-btn fin-btn-sec" :disabled="envoi" @click="aConfirmer = null">Retour</button>
            <button type="button" class="fin-btn" :class="aConfirmer === 'ACCEPTE' ? 'fin-btn-pri' : 'fin-btn-dan'" :disabled="envoi" @click="confirmer">
              {{ envoi ? 'Envoi…' : aConfirmer === 'ACCEPTE' ? 'Confirmer l’acceptation' : 'Confirmer le refus' }}
            </button>
          </template>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.cg-liste { padding: 0; max-height: none; overflow: hidden; }
.cg-liste .fin-scroll { padding: 8px 12px 0; }
.cg-ok { background: #E3F3EA; color: #14633B; border-radius: 8px; padding: 10px 14px; margin: 0 0 16px; font-size: 14px; }
.cg-fond { position: fixed; inset: 0; background: rgba(20, 30, 24, .45); display: grid; place-items: center; padding: 16px; z-index: 50; }
.cg-modal { width: min(580px, 100%); max-height: 92vh; overflow: auto; outline: none; }
.cg-modal h2 { margin: 0 0 14px; font-size: 18px; }
.cg-modal h3 { margin: 18px 0 8px; font-size: 14px; }
.cg-dl { display: grid; grid-template-columns: 130px 1fr; gap: 8px 12px; margin: 0; }
.cg-dl dt { color: #4B5563; font-size: 13px; }
.cg-dl dd { margin: 0; font-weight: 600; }
.cg-hist { list-style: none; margin: 0; padding: 0; display: grid; gap: 10px; }
.cg-hist p { margin: 4px 0 0; color: #374151; }
.cg-commentaire { margin-top: 16px; }
.cg-commentaire textarea { padding: 10px 12px; resize: vertical; }
.cg-confirm { background: #FFF3D6; color: #7A4B00; border-radius: 8px; padding: 10px 14px; margin-top: 12px; font-size: 14px; }
.cg-actions { display: flex; justify-content: flex-end; flex-wrap: wrap; gap: 10px; margin-top: 18px; }
</style>