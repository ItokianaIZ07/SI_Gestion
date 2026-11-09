<script setup>
// Page 8 : propositions de remplacement reçues — accepter ou refuser, avec commentaire.
// // GET /remplacements/moi, POST /remplacements/{id}/decision { decision: ACCEPTE|REFUSE, commentaire? }
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import '@/assets/finance.css'
import '@/assets/conge.css'
import { congeApi } from '@/api/congeApi'
import { dateFr, messageErreur } from '@/utils/format'
import { jours, decisionDe, dateHeure } from '@/utils/conge'
import { usePagination } from '@/composable/usePagination'
import Pagination from '@/components/ui/Pagination.vue'

const propositions = ref([])
const chargement = ref(true)
const erreur = ref('')
const confirmation = ref('')
const filtre = ref('EN_ATTENTE')      // par défaut : celles qui attendent l réponse

const selection = ref(null)
const commentaire = ref('')
const aConfirmer = ref(null)          // null | 'ACCEPTE' | 'REFUSE'
const envoi = ref(false)
const erreurDecision = ref('')
const modal = ref(null)
watch(selection, async (v) => { if (v) { await nextTick(); modal.value?.focus() } })

const enAttente = computed(() => propositions.value.filter(p => p.decision === 'EN_ATTENTE').length)
const filtrees = computed(() => propositions.value
  .filter(p => !filtre.value || p.decision === filtre.value)
  .sort((a, b) => (b.dateProposition ?? '').localeCompare(a.dateProposition ?? '')))
const pg = usePagination(filtrees, 8)

const demandeur = (p) => `${p.prenomEmployeDemandeur} ${p.nomEmployeDemandeur}`
const repondable = computed(() => selection.value?.decision === 'EN_ATTENTE')
const texteErreur = (e) => (e?.response?.status === 401 ? 'Votre session a expiré. Reconnectez-vous.' : messageErreur(e))

async function charger(initial = false) {
  if (initial) chargement.value = true
  erreur.value = ''
  try { propositions.value = await congeApi.getMesPropositions() }
  catch (e) { erreur.value = texteErreur(e) }
  finally { chargement.value = false }
}

function ouvrir(p) {
  selection.value = p
  commentaire.value = ''
  aConfirmer.value = null
  erreurDecision.value = ''
}
function fermer() { if (!envoi.value) selection.value = null }

async function confirmer() {
  const p = selection.value
  envoi.value = true
  erreurDecision.value = ''
  try {
    const r = await congeApi.deciderRemplacement(p.idRemplacement, aConfirmer.value, commentaire.value.trim())
    confirmation.value = r.decision === 'ACCEPTE'
      ? `Vous remplacerez ${demandeur(p)} du ${dateFr(p.dateDebut)} au ${dateFr(p.dateFin)}.`
      : `Vous avez refusé de remplacer ${demandeur(p)}.`
    envoi.value = false
    selection.value = null
    await charger()
  } catch (e) {
    erreurDecision.value = texteErreur(e)       // ex. proposition déjà traitée, remplaçant indisponible
    aConfirmer.value = null
    envoi.value = false
    if ([404, 409].includes(e?.response?.status)) charger()
  }
}

onMounted(() => charger(true))
</script>

<template>
  <section class="fin-page">
    <h1>Mes propositions de remplacement</h1>
    <p class="fin-sub">Des collègues vous ont proposé comme remplaçant pendant leur congé. {{ enAttente ? `${enAttente} proposition(s) attendent votre réponse.` : '' }}</p>

    <p v-if="confirmation" class="cg-ok" role="status">{{ confirmation }}</p>
    <p v-if="erreur" class="fin-error" role="alert">{{ erreur }}</p>
    <p v-if="chargement">Chargement…</p>

    <template v-else>
      <div class="fin-toolbar">
        <label class="fin-label">Réponse
          <select v-model="filtre" class="fin-champ">
            <option value="">Toutes</option>
            <option value="EN_ATTENTE">En attente de ma réponse</option>
            <option value="ACCEPTE">Acceptées</option>
            <option value="REFUSE">Refusées</option>
          </select>
        </label>
      </div>

      <div class="fin-card cg-liste">
        <div class="fin-scroll">
          <table class="fin-table">
            <thead>
              <tr><th>Collègue à remplacer</th><th>Type</th><th>Période</th><th class="fin-num">Jours</th><th>Proposée le</th><th>Réponse</th><th></th></tr>
            </thead>
            <tbody>
              <tr v-for="p in pg.items" :key="p.idRemplacement">
                <td><strong>{{ demandeur(p) }}</strong><br><small>{{ p.matriculeEmployeDemandeur }}</small></td>
                <td>{{ p.typeConge }}</td>
                <td>{{ dateFr(p.dateDebut) }} → {{ dateFr(p.dateFin) }}</td>
                <td class="fin-num">{{ jours(p.nombreJours) }}</td>
                <td>{{ dateFr(p.dateProposition) }}</td>
                <td><span class="fin-badge" :class="decisionDe(p.decision).classe">{{ decisionDe(p.decision).libelle }}</span></td>
                <td class="fin-num">
                  <button type="button" class="fin-btn" :class="p.decision === 'EN_ATTENTE' ? 'fin-btn-pri' : 'fin-btn-sec'" @click="ouvrir(p)">
                    {{ p.decision === 'EN_ATTENTE' ? 'Répondre' : 'Voir' }}
                  </button>
                </td>
              </tr>
              <tr v-if="!filtrees.length">
                <td colspan="7">{{ propositions.length ? 'Aucune proposition ne correspond.' : 'Vous n’avez reçu aucune proposition de remplacement.' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <Pagination v-bind="pg.barre" @update:page="pg.aller" />
      </div>
    </template>

    <div v-if="selection" class="cg-fond" @click.self="fermer" @keydown.esc="fermer">
      <div ref="modal" class="fin-card cg-modal" role="dialog" aria-modal="true" aria-labelledby="cg-titre" tabindex="-1">
        <h2 id="cg-titre">Remplacer {{ demandeur(selection) }}</h2>
        <dl class="cg-dl">
          <dt>Collègue</dt><dd>{{ demandeur(selection) }} ({{ selection.matriculeEmployeDemandeur }})</dd>
          <dt>Type de congé</dt><dd>{{ selection.typeConge }}</dd>
          <dt>Période</dt><dd>{{ dateFr(selection.dateDebut) }} → {{ dateFr(selection.dateFin) }}</dd>
          <dt>Nombre de jours</dt><dd>{{ jours(selection.nombreJours) }}</dd>
          <dt>Proposée le</dt><dd>{{ dateHeure(selection.dateProposition) }}</dd>
          <template v-if="!repondable">
            <dt>Votre réponse</dt>
            <dd><span class="fin-badge" :class="decisionDe(selection.decision).classe">{{ decisionDe(selection.decision).libelle }}</span>
              <small v-if="selection.dateDecision"> {{ dateHeure(selection.dateDecision) }}</small></dd>
            <dt v-if="selection.commentaire">Commentaire</dt><dd v-if="selection.commentaire">« {{ selection.commentaire }} »</dd>
          </template>
        </dl>

        <template v-if="repondable">
          <label class="fin-label cg-commentaire">Commentaire <span class="fin-hint">(facultatif)</span>
            <textarea v-model="commentaire" class="fin-champ" rows="3" maxlength="2000" :disabled="envoi || !!aConfirmer"
                      placeholder="Une précision pour votre collègue…"></textarea>
          </label>
          <p v-if="erreurDecision" class="fin-error" role="alert">{{ erreurDecision }}</p>
          <div v-if="aConfirmer" class="cg-confirm" role="alert">
            {{ aConfirmer === 'ACCEPTE'
                ? `Confirmer que vous remplacerez ${demandeur(selection)} du ${dateFr(selection.dateDebut)} au ${dateFr(selection.dateFin)} ?`
                : `Confirmer le refus de remplacer ${demandeur(selection)} ?` }}
          </div>
          <div class="cg-actions">
            <template v-if="!aConfirmer">
              <button type="button" class="fin-btn fin-btn-sec" @click="fermer">Fermer</button>
              <button type="button" class="fin-btn fin-btn-dan" @click="aConfirmer = 'REFUSE'">Refuser</button>
              <button type="button" class="fin-btn fin-btn-pri" @click="aConfirmer = 'ACCEPTE'">Accepter</button>
            </template>
            <template v-else>
              <button type="button" class="fin-btn fin-btn-sec" :disabled="envoi" @click="aConfirmer = null">Retour</button>
              <button type="button" class="fin-btn" :class="aConfirmer === 'ACCEPTE' ? 'fin-btn-pri' : 'fin-btn-dan'" :disabled="envoi" @click="confirmer">
                {{ envoi ? 'Envoi…' : aConfirmer === 'ACCEPTE' ? 'Confirmer l’acceptation' : 'Confirmer le refus' }}
              </button>
            </template>
          </div>
        </template>
        <div v-else class="cg-actions"><button type="button" class="fin-btn fin-btn-sec" @click="fermer">Fermer</button></div>
      </div>
    </div>
  </section>
</template>
