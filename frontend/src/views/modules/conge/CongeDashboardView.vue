<script setup>
import { ref, computed, onMounted } from 'vue'
import '@/assets/finance.css'
import { congeApi } from '@/api/congeApi'
import { messageErreur } from '@/utils/format'
import { jours, nomComplet } from '@/utils/conge'
import { usePagination } from '@/composable/usePagination'
import Pagination from '@/components/ui/Pagination.vue'

const soldes = ref([])
const demandes = ref([])
const chargement = ref(true)
const erreur = ref('')

const idEmploye = ref('')
const typeConge = ref('')

// Listes des filtres, déduites des soldes (l'API ne fournit pas encore de liste d'employés ni de types).
const employes = computed(() => {
  const map = new Map()
  soldes.value.forEach(s => map.set(s.idEmploye, nomComplet(s)))
  return [...map].map(([id, nom]) => ({ id, nom })).sort((a, b) => a.nom.localeCompare(b.nom))
})
const types = computed(() => [...new Set(soldes.value.map(s => s.typeConge))])

const garde = (x) => (!idEmploye.value || x.idEmploye === idEmploye.value) && (!typeConge.value || x.typeConge === typeConge.value)
const soldesFiltres = computed(() => soldes.value.filter(garde))
const demandesFiltrees = computed(() => demandes.value.filter(garde))

const pg = usePagination(soldesFiltres, 8)

// Soldes : somme des lignes affichées (choisissez un type pour un total pertinent).
const disponible = computed(() => soldesFiltres.value.reduce((t, s) => t + (s.solde ?? 0), 0))

// Les statuts n'existent pas encore dans la réponse de /demandes : "—" tant qu'ils manquent.
const aDesStatuts = computed(() => demandesFiltrees.value.some(d => d.statut))
const compter = (test) => (aDesStatuts.value ? demandesFiltrees.value.filter(d => test(d.statut)).length : null)
const enAttente = computed(() => compter(s => s?.startsWith('EN_ATTENTE')))
const validees = computed(() => compter(s => s === 'VALIDEE'))
const consommes = computed(() => aDesStatuts.value
  ? demandesFiltrees.value.filter(d => d.statut === 'VALIDEE').reduce((t, d) => t + (d.nombreJourConge ?? 0), 0)
  : null)

const kpis = computed(() => [
  { titre: 'Solde disponible', valeur: jours(disponible.value), aide: typeConge.value || 'Tous types confondus' },
  { titre: 'Jours acquis', valeur: '—', aide: 'Fourni par une prochaine API' },
  { titre: 'Jours consommés', valeur: consommes.value == null ? '—' : jours(consommes.value), aide: 'Demandes validées' },
  { titre: 'Demandes en attente', valeur: enAttente.value ?? '—', aide: 'Manager ou RH' },
  { titre: 'Demandes validées', valeur: validees.value ?? '—', aide: 'Toutes périodes' }
])

onMounted(async () => {
  try {
    ;[soldes.value, demandes.value] = await Promise.all([congeApi.getSoldes(), congeApi.getDemandes()])
  } catch (e) {
    erreur.value = messageErreur(e)
  } finally {
    chargement.value = false
  }
})
</script>

<template>
  <section class="fin-page">
    <h1>Tableau de bord des congés</h1>
    <p class="fin-sub">Soldes et demandes de congé des employés.</p>

    <p v-if="erreur" class="fin-error" role="alert">{{ erreur }}</p>
    <p v-if="chargement">Chargement…</p>

    <template v-else>
      <div class="fin-toolbar">
        <label class="fin-label">Employé
          <select v-model="idEmploye" class="fin-champ">
            <option value="">Tous les employés</option>
            <option v-for="e in employes" :key="e.id" :value="e.id">{{ e.nom }}</option>
          </select>
        </label>
        <label class="fin-label">Type de congé
          <select v-model="typeConge" class="fin-champ">
            <option value="">Tous les types</option>
            <option v-for="t in types" :key="t" :value="t">{{ t }}</option>
          </select>
        </label>
      </div>

      <div class="fin-kpis">
        <div v-for="k in kpis" :key="k.titre" class="fin-card fin-kpi">
          <small>{{ k.titre }}</small>
          <strong>{{ k.valeur }}</strong>
          <small class="cg-aide">{{ k.aide }}</small>
        </div>
      </div>

      <div class="fin-card cg-liste">
        <div class="fin-scroll">
          <table class="fin-table">
            <thead>
              <tr><th>Employé</th><th>Matricule</th><th>Type de congé</th><th class="fin-num">Solde</th></tr>
            </thead>
            <tbody>
              <tr v-for="(s, i) in pg.items" :key="s.idEmploye + s.typeConge + i">
                <td><strong>{{ nomComplet(s) }}</strong><br><small>{{ s.emailEmploye }}</small></td>
                <td>{{ s.matriculeEmploye }}</td>
                <td>{{ s.typeConge }}</td>
                <td class="fin-num"><strong>{{ jours(s.solde) }}</strong></td>
              </tr>
              <tr v-if="!soldesFiltres.length"><td colspan="4">Aucun solde à afficher.</td></tr>
            </tbody>
          </table>
        </div>
        <Pagination v-bind="pg.barre" @update:page="pg.aller" />
      </div>
    </template>
  </section>
</template>

<style scoped>
.cg-aide { margin: 6px 0 0; }
.cg-liste { padding: 0; max-height: none; overflow: hidden; }
.cg-liste .fin-scroll { padding: 8px 12px 0; }
</style>