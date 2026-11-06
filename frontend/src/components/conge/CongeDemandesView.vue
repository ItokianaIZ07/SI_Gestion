<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import '@/assets/finance.css'
import { congeApi } from '@/api/congeApi'
import { dateFr, messageErreur } from '@/utils/format'
import { jours, nomComplet, statutDe, STATUTS_LISTE } from '@/utils/conge'
import { usePagination } from '@/composable/usePagination'
import Pagination from '@/components/ui/Pagination.vue'

const demandes = ref([])
const chargement = ref(true)
const erreur = ref('')

const recherche = ref('')
const statut = ref('')
const du = ref('')
const au = ref('')

// Fenêtre de détail
const selection = ref(null)
const modal = ref(null)
watch(selection, async (v) => { if (v) { await nextTick(); modal.value?.focus() } })

const sansStatut = computed(() => demandes.value.length > 0 && !demandes.value.some(d => d.statut))

const filtrees = computed(() => {
  const q = recherche.value.trim().toLowerCase()
  return demandes.value.filter(d =>
    (!q || [nomComplet(d), d.matriculeEmploye, d.typeConge, d.motif].join(' ').toLowerCase().includes(q)) &&
    (!statut.value || d.statut === statut.value) &&
    (!du.value || d.dateFinConge >= du.value) &&        // la demande chevauche la période choisie
    (!au.value || d.dateDebutConge <= au.value))
})
const pg = usePagination(filtrees, 8)

onMounted(async () => {
  try { demandes.value = await congeApi.getDemandes() }
  catch (e) { erreur.value = messageErreur(e) }
  finally { chargement.value = false }
})
</script>

<template>
  <section class="fin-page">
    <h1>Demandes de congé</h1>
    <p class="fin-sub">Recherchez une demande, filtrez-la, puis ouvrez son détail.</p>

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
                <td class="fin-num"><button type="button" class="fin-btn fin-btn-sec" @click="selection = d">Voir</button></td>
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
          <dt>Motif</dt><dd>{{ selection.motif || '—' }}</dd>
        </dl>
        <div class="cg-actions"><button type="button" class="fin-btn fin-btn-sec" @click="selection = null">Fermer</button></div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.cg-liste { padding: 0; max-height: none; overflow: hidden; }
.cg-liste .fin-scroll { padding: 8px 12px 0; }
.cg-fond { position: fixed; inset: 0; background: rgba(20, 30, 24, .45); display: grid; place-items: center; padding: 16px; z-index: 50; }
.cg-modal { width: min(520px, 100%); max-height: 90vh; overflow: auto; outline: none; }
.cg-modal h2 { margin: 0 0 14px; font-size: 18px; }
.cg-dl { display: grid; grid-template-columns: 140px 1fr; gap: 8px 12px; margin: 0; }
.cg-dl dt { color: #4B5563; font-size: 13px; }
.cg-dl dd { margin: 0; font-weight: 600; }
.cg-actions { display: flex; justify-content: flex-end; margin-top: 18px; }
</style>