<script setup>
// Page 6 : espace employé — mon solde, mes demandes, annulation d'une demande validée.
// Données : GET /soldes (filtré sur mon id), GET /demandes/{idEmploye}, GET /remplacements/moi.
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import '@/assets/finance.css'
import '@/assets/conge.css'
import { congeApi } from '@/api/congeApi'
import { useAuthStore } from '@/stores/authStore'
import { dateFr, messageErreur } from '@/utils/format'
import { jours, statutDe, STATUTS_LISTE } from '@/utils/conge'
import { usePagination } from '@/composable/usePagination'
import Pagination from '@/components/ui/Pagination.vue'
import AnnulationDemande from '@/components/conge/AnnulationDemande.vue'

const auth = useAuthStore()
const moi = computed(() => auth.employe)

const soldes = ref([])
const demandes = ref([])
const propositions = ref([])
const chargement = ref(true)
const erreur = ref('')
const confirmation = ref('')

const statut = ref('')
const du = ref('')
const au = ref('')

const selection = ref(null)
const modal = ref(null)
watch(selection, async (v) => { if (v) { await nextTick(); modal.value?.focus() } })

const sansStatut = computed(() => demandes.value.length > 0 && !demandes.value.some(d => d.statut))
const enAttenteRemplacement = computed(() => propositions.value.filter(p => p.decision === 'EN_ATTENTE').length)

const aujourdhui = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
// Annulable : demande VALIDÉE (si le statut n'est pas fourni, le serveur tranche) et congé pas encore commencé.
const annulable = (d) => (d.statut === 'VALIDEE' || !d.statut) && d.dateDebutConge > aujourdhui()

const filtrees = computed(() => demandes.value
  .filter(d =>
    (!statut.value || d.statut === statut.value) &&
    (!du.value || d.dateFinConge >= du.value) &&
    (!au.value || d.dateDebutConge <= au.value))
  .sort((a, b) => b.dateDebutConge.localeCompare(a.dateDebutConge)))
const pg = usePagination(filtrees, 8)

async function charger() {
  if (!moi.value) { chargement.value = false; return }
  chargement.value = true
  erreur.value = ''
  try {
    const [s, d] = await Promise.all([congeApi.getSoldes(), congeApi.getDemandesEmploye(moi.value.id)])
    soldes.value = s.filter(x => x.idEmploye === moi.value.id)
    demandes.value = d
  } catch (e) { erreur.value = messageErreur(e) }
  finally { chargement.value = false }
  try { propositions.value = await congeApi.getMesPropositions() } catch { propositions.value = [] }
}

function apresAnnulation(r) {
  confirmation.value = `Votre demande #${r.idDemande} est annulée : ${jours(r.joursRestitues)} restitués à votre solde.`
  selection.value = null
  charger()
}

onMounted(charger)
</script>

<template>
  <section class="fin-page">
    <h1>Mes congés</h1>
    <p v-if="!moi" class="fin-error" role="alert">Session employé introuvable. Reconnectez-vous.</p>
    <template v-else>
      <p class="fin-sub">{{ moi.prenom }} {{ moi.nom }} · matricule {{ moi.matricule }}</p>

      <p v-if="confirmation" class="cg-ok" role="status">{{ confirmation }}</p>
      <p v-if="erreur" class="fin-error" role="alert">{{ erreur }}</p>
      <p v-if="chargement">Chargement…</p>

      <template v-else>
        <div class="cg-soldes">
          <article v-for="s in soldes" :key="s.typeConge" class="cg-solde" :class="{ 'cg-solde--princ': s.typeConge === 'Congé annuel' }">
            <small>{{ s.typeConge === 'Congé annuel' ? 'Mon solde · ' : '' }}{{ s.typeConge }}</small>
            <strong>{{ jours(s.solde) }}</strong>
          </article>
          <article v-if="enAttenteRemplacement" class="cg-solde">
            <small>Propositions de remplacement</small>
            <strong>{{ enAttenteRemplacement }} en attente</strong>
            <RouterLink class="cg-lien" to="/si/app/employe/remplacements">Répondre</RouterLink>
          </article>
        </div>

        <div class="fin-toolbar">
          <label class="fin-label">Statut
            <select v-model="statut" class="fin-champ" :disabled="sansStatut">
              <option value="">Tous les statuts</option>
              <option v-for="s in STATUTS_LISTE" :key="s.code" :value="s.code">{{ s.libelle }}</option>
            </select>
          </label>
          <label class="fin-label">Du <input v-model="du" class="fin-champ" type="date"></label>
          <label class="fin-label">Au <input v-model="au" class="fin-champ" type="date" :min="du || null"></label>
          <RouterLink class="fin-btn fin-btn-pri" to="/si/app/employe/conges/nouvelle">Faire une demande</RouterLink>
        </div>
        <p v-if="sansStatut" class="fin-hint">Le statut de vos demandes n’est pas encore fourni par l’API : le filtre sera actif dès qu’il le sera.</p>

        <div class="fin-card cg-liste">
          <div class="fin-scroll">
            <table class="fin-table">
              <thead>
                <tr><th>Type</th><th>Période</th><th class="fin-num">Jours</th><th>Statut</th><th></th></tr>
              </thead>
              <tbody>
                <tr v-for="d in pg.items" :key="d.idDemande">
                  <td>{{ d.typeConge }}</td>
                  <td>{{ dateFr(d.dateDebutConge) }} → {{ dateFr(d.dateFinConge) }}</td>
                  <td class="fin-num">{{ jours(d.nombreJourConge) }}</td>
                  <td><span class="fin-badge" :class="statutDe(d.statut).classe">{{ statutDe(d.statut).libelle }}</span></td>
                  <td class="fin-num"><button type="button" class="fin-btn fin-btn-sec" @click="selection = d">Détail</button></td>
                </tr>
                <tr v-if="!filtrees.length"><td colspan="5">{{ demandes.length ? 'Aucune demande ne correspond.' : 'Vous n’avez fait aucune demande de congé.' }}</td></tr>
              </tbody>
            </table>
          </div>
          <Pagination v-bind="pg.barre" @update:page="pg.aller" />
        </div>
      </template>
    </template>

    <div v-if="selection" class="cg-fond" @click.self="selection = null" @keydown.esc="selection = null">
      <div ref="modal" class="fin-card cg-modal" role="dialog" aria-modal="true" aria-labelledby="cg-titre" tabindex="-1">
        <h2 id="cg-titre">Demande #{{ selection.idDemande }}</h2>
        <dl class="cg-dl">
          <dt>Type de congé</dt><dd>{{ selection.typeConge }}</dd>
          <dt>Période</dt><dd>{{ dateFr(selection.dateDebutConge) }} → {{ dateFr(selection.dateFinConge) }}</dd>
          <dt>Nombre de jours</dt><dd>{{ jours(selection.nombreJourConge) }}</dd>
          <dt>Statut</dt><dd><span class="fin-badge" :class="statutDe(selection.statut).classe">{{ statutDe(selection.statut).libelle }}</span></dd>
          <dt>Motif</dt><dd>{{ selection.motif || '—' }}</dd>
        </dl>
        <template v-if="annulable(selection)">
          <p v-if="!selection.statut" class="fin-hint">Le statut n’est pas fourni par l’API : le serveur vérifiera que la demande est bien validée.</p>
          <AnnulationDemande :id-demande="selection.idDemande" @annulee="apresAnnulation" />
        </template>
        <div class="cg-actions"><button type="button" class="fin-btn fin-btn-sec" @click="selection = null">Fermer</button></div>
      </div>
    </div>
  </section>
</template>
