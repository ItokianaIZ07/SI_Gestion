<script setup>
// Page 2 : liste des demandes de congé et fenêtre de détail.
// Détail : remplaçant, historique des décisions (GET /demandes/{id}/validations) et annulation
// avec motif (POST /demandes/{id}/annulation, réservée au RH).
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import '@/assets/finance.css'
import '@/assets/conge.css'
import { congeApi } from '@/api/congeApi'
import { useAuthStore } from '@/stores/authStore'
import { dateFr, messageErreur } from '@/utils/format'
import { jours, nomComplet, statutDe, STATUTS_LISTE, etapeDe, decisionDe, dateHeure } from '@/utils/conge'
import { usePagination } from '@/composable/usePagination'
import Pagination from '@/components/ui/Pagination.vue'
import AnnulationDemande from '@/components/conge/AnnulationDemande.vue'

const auth = useAuthStore()
const estRh = computed(() => auth.user?.role === 'RH')

const demandes = ref([])
const chargement = ref(true)
const erreur = ref('')
const confirmation = ref('')

const recherche = ref('')
const statut = ref('')
const du = ref('')
const au = ref('')

// Fenêtre de détail
const selection = ref(null)
const modal = ref(null)
const historique = ref([])
const historiqueErreur = ref('')
const chargementHistorique = ref(false)
watch(selection, async (v) => { if (v) { await nextTick(); modal.value?.focus() } })

// Noms des employés (pour afficher le remplaçant) : facultatif, sans incidence si la liste est refusée.
const noms = ref(new Map())
const nomRemplacant = (id) => (id ? noms.value.get(id) ?? `Employé n°${id}` : null)

const sansStatut = computed(() => demandes.value.length > 0 && !demandes.value.some(d => d.statut))

// Annulable : demande VALIDÉE (si l'API ne renvoie pas le statut, le backend refusera ce qui ne l'est pas).
const peutAnnuler = computed(() => estRh.value && !!selection.value && (selection.value.statut === 'VALIDEE' || !selection.value.statut))

const filtrees = computed(() => {
  const q = recherche.value.trim().toLowerCase()
  return demandes.value.filter(d =>
    (!q || [nomComplet(d), d.matriculeEmploye, d.typeConge, d.motif].join(' ').toLowerCase().includes(q)) &&
    (!statut.value || d.statut === statut.value) &&
    (!du.value || d.dateFinConge >= du.value) &&        // la demande chevauche la période choisie
    (!au.value || d.dateDebutConge <= au.value))
})
const pg = usePagination(filtrees, 8)

async function charger() {
  chargement.value = true
  erreur.value = ''
  try { demandes.value = await congeApi.getDemandes() }
  catch (e) { erreur.value = messageErreur(e) }
  finally { chargement.value = false }
}

async function ouvrir(d) {
  selection.value = d
  historique.value = []
  historiqueErreur.value = ''
  chargementHistorique.value = true
  try {
    const h = await congeApi.getHistoriqueValidations(d.idDemande)
    if (selection.value?.idDemande === d.idDemande) historique.value = h
  } catch (e) {
    if (selection.value?.idDemande === d.idDemande) {
      historiqueErreur.value = e?.response?.status === 403
        ? 'L’historique des décisions est réservé aux RH et aux managers.'
        : 'L’historique des décisions n’a pas pu être chargé.'
    }
  } finally {
    chargementHistorique.value = false
  }
}

function apresAnnulation(r) {
  confirmation.value = `La demande #${r.idDemande} est annulée : ${jours(r.joursRestitues)} restitués au solde.`
  selection.value = null
  charger()
}

onMounted(async () => {
  charger()
  try {
    const deps = await congeApi.getEmployesParDepartement()
    noms.value = new Map(deps.flatMap(d => d.employes.map(e => [e.id, `${e.prenom} ${e.nom}`])))
  } catch { /* le remplaçant s'affichera par son numéro */ }
})
</script>

<template>
  <section class="fin-page">
    <h1>Demandes de congé</h1>
    <p class="fin-sub">Recherchez une demande, filtrez-la, puis ouvrez son détail.</p>

    <p v-if="confirmation" class="cg-ok" role="status">{{ confirmation }}</p>
    <p v-if="erreur" class="fin-error" role="alert">{{ erreur }}</p>
    <p v-if="chargement">Chargement…</p>

    <template v-else>
      <div class="fin-toolbar">
        <label class="fin-label">Recherche
          <input v-model="recherche" class="fin-champ" type="search" placeholder="Nom, matricule, type, motif…">
        </label>
        <label class="fin-label">Statut
          <select v-model="statut" class="fin-champ" :disabled="sansStatut">
            <option value="">Tous les statuts</option>
            <option v-for="s in STATUTS_LISTE" :key="s.code" :value="s.code">{{ s.libelle }}</option>
          </select>
        </label>
        <label class="fin-label">Du <input v-model="du" class="fin-champ" type="date"></label>
        <label class="fin-label">Au <input v-model="au" class="fin-champ" type="date" :min="du || null"></label>
      </div>
      <p v-if="sansStatut" class="fin-hint">Le statut des demandes n’est pas encore fourni par l’API : le filtre par statut sera actif dès qu’il le sera.</p>

      <div class="fin-card cg-liste">
        <div class="fin-scroll">
          <table class="fin-table">
            <thead>
              <tr><th>Employé</th><th>Type</th><th>Période</th><th class="fin-num">Jours</th><th>Statut</th><th></th></tr>
            </thead>
            <tbody>
              <tr v-for="d in pg.items" :key="d.idDemande">
                <td><strong>{{ nomComplet(d) }}</strong><br><small>{{ d.matriculeEmploye }}</small></td>
                <td>{{ d.typeConge }}</td>
                <td>{{ dateFr(d.dateDebutConge) }} → {{ dateFr(d.dateFinConge) }}</td>
                <td class="fin-num">{{ jours(d.nombreJourConge) }}</td>
                <td><span class="fin-badge" :class="statutDe(d.statut).classe">{{ statutDe(d.statut).libelle }}</span></td>
                <td class="fin-num"><button type="button" class="fin-btn fin-btn-sec" @click="ouvrir(d)">Voir</button></td>
              </tr>
              <tr v-if="!filtrees.length"><td colspan="6">Aucune demande ne correspond.</td></tr>
            </tbody>
          </table>
        </div>
        <Pagination v-bind="pg.barre" @update:page="pg.aller" />
      </div>
    </template>

    <!-- Fenêtre de détail -->
    <div v-if="selection" class="cg-fond" @click.self="selection = null" @keydown.esc="selection = null">
      <div ref="modal" class="fin-card cg-modal" role="dialog" aria-modal="true" aria-labelledby="cg-titre" tabindex="-1">
        <h2 id="cg-titre">Demande #{{ selection.idDemande }}</h2>
        <dl class="cg-dl">
          <dt>Employé</dt><dd>{{ nomComplet(selection) }} ({{ selection.matriculeEmploye }})</dd>
          <dt>Email</dt><dd>{{ selection.emailEmploye }}</dd>
          <dt>Type de congé</dt><dd>{{ selection.typeConge }}</dd>
          <dt>Période</dt><dd>{{ dateFr(selection.dateDebutConge) }} → {{ dateFr(selection.dateFinConge) }}</dd>
          <dt>Nombre de jours</dt><dd>{{ jours(selection.nombreJourConge) }}</dd>
          <dt>Statut</dt><dd><span class="fin-badge" :class="statutDe(selection.statut).classe">{{ statutDe(selection.statut).libelle }}</span></dd>
          <dt>Remplaçant</dt><dd>{{ nomRemplacant(selection.idEmployeRemplacant) || '—' }}</dd>
          <dt>Motif</dt><dd>{{ selection.motif || '—' }}</dd>
        </dl>

        <h3>Historique des décisions</h3>
        <p v-if="chargementHistorique" class="fin-hint">Chargement…</p>
        <p v-else-if="historiqueErreur" class="fin-hint">{{ historiqueErreur }}</p>
        <ul v-else class="cg-hist">
          <li v-for="h in historique" :key="h.idValidation">
            <span class="fin-badge" :class="decisionDe(h.decision).classe">{{ etapeDe(h.etape) }} · {{ decisionDe(h.decision).libelle }}</span>
            <small v-if="h.emailValidateur"> {{ h.emailValidateur }} · {{ dateHeure(h.dateValidation) }}</small>
            <p v-if="h.commentaire">« {{ h.commentaire }} »</p>
          </li>
          <li v-if="!historique.length" class="fin-hint">Aucune décision enregistrée pour le moment.</li>
        </ul>

        <template v-if="peutAnnuler">
          <p v-if="!selection.statut" class="fin-hint">Le statut n’est pas fourni par l’API : le serveur vérifiera que la demande est bien validée.</p>
          <AnnulationDemande :id-demande="selection.idDemande" @annulee="apresAnnulation" />
        </template>

        <div class="cg-actions"><button type="button" class="fin-btn fin-btn-sec" @click="selection = null">Fermer</button></div>
      </div>
    </div>
  </section>
</template>
