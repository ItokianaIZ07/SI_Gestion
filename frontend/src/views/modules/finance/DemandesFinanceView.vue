<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import '@/assets/finance.css'
import { financeApi } from '@/api/financeApi'
import { ariary, messageErreur } from '@/utils/format'
import { usePagination } from '@/composable/usePagination'
import Pagination from '@/components/ui/Pagination.vue'
import ValidationSteps from '@/components/finance/ValidationSteps.vue'

const route = useRoute()
const router = useRouter()

const demandes = ref([])
const chargement = ref(false)
const erreur = ref('')

// 5 demandes par page ; la pagination revient à la page 1 quand la liste est rechargée.
const pg = usePagination(demandes, 5)

// Message de confirmation affiché après une décision prise dans la page de détail.
const MESSAGES = {
  validee: 'La demande a été validée par la Finance et transmise à la Direction.',
  refusee: 'La demande a été refusée.'
}
const confirmation = ref(MESSAGES[route.query.resultat] ?? '')

// Montant vide tant que les RH n'ont pas défini le salaire estimé.
function badgeBudget(d) {
  if (d.coutTotal == null) return { classe: 'fin-none', texte: 'Salaire non défini' }
  return d.couvert
    ? { classe: 'fin-ok', texte: '✓ Couvert' }
    : { classe: 'fin-ko', texte: '✕ Dépasse le budget' }
}

const voirDetails = (id) => router.push(`/si/app/finance/demandes/${id}`)

async function charger() {
  chargement.value = true
  erreur.value = ''
  try { demandes.value = await financeApi.getDemandes() }
  catch (e) { erreur.value = messageErreur(e) }
  finally { chargement.value = false }
}

onMounted(() => {
  if (confirmation.value) router.replace({ query: {} })   // le message ne revient pas au rechargement
  charger()
})
</script>

<template>
  <section class="fin-page">
    <h1>Demandes de recrutement</h1>
    <p class="fin-sub">Consultez les demandes, puis ouvrez le détail pour les accepter ou les refuser.</p>

    <p v-if="confirmation" class="demandes-ok" role="status">{{ confirmation }}</p>
    <p v-if="erreur" class="fin-error" role="alert">{{ erreur }}</p>
    <p v-if="chargement">Chargement…</p>

    <div v-else class="fin-card fin-liste">
      <div class="fin-scroll">
        <table class="fin-table">
          <thead>
            <tr>
              <th>Poste</th><th>Département</th>
              <th class="fin-num">Effectif</th>
              <!-- <th class="fin-num">Coût total</th>  -->
              <th>Budget</th>
              <th>Validations</th><th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="d in pg.items" :key="d.id">
              <td><strong>{{ d.poste }}</strong></td>
              <td>{{ d.departement }}</td>
              <td class="fin-num">{{ d.effectif }}</td>
              <!-- <td class="fin-num"><strong>{{ d.coutTotal == null ? '—' : ariary(d.coutTotal) }}</strong></td> -->
              <td><span class="fin-badge" :class="badgeBudget(d).classe">{{ badgeBudget(d).texte }}</span></td>
              <td><ValidationSteps :etapes="d.etapes" /></td>
              <td class="fin-num">
                <button type="button" class="fin-btn fin-btn-sec" @click="voirDetails(d.id)">Voir détails</button>
              </td>
            </tr>
            <tr v-if="!demandes.length"><td colspan="7">Aucune demande à afficher.</td></tr>
          </tbody>
        </table>
      </div>

      <Pagination v-bind="pg.barre" @update:page="pg.aller" />
    </div>
  </section>
</template>

<style scoped>
/* La carte garde son style .fin-card, mais sans marge interne pour que la pagination touche les bords. */
.fin-liste { padding: 0; max-height: none; overflow: hidden; }
.fin-liste .fin-scroll { padding: 8px 12px 0; }
.demandes-ok {
  background: #E3F3EA; color: #14633B; border-radius: 8px;
  padding: 10px 14px; margin-bottom: 16px; font-size: 14px;
}
</style>