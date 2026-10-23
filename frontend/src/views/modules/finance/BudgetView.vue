<script setup>
import { ref, watch, onMounted } from 'vue'
import '@/assets/finance.css'
import { financeApi } from '@/api/financeApi'
import { ariary, messageErreur } from '@/utils/format'

const annee = ref(new Date().getFullYear())
const annees = [annee.value, annee.value - 1, annee.value - 2]
const budget = ref(null)     // { total: {...}, departements: [...] }
const erreur = ref('')

const taux = (b) => (b.budget ? Math.min(100, Math.round(((b.masseSalariale + b.depenses) / b.budget) * 100)) : 0)

async function charger() {
  erreur.value = ''
  try { budget.value = await financeApi.getBudget(annee.value) }
  catch (e) { erreur.value = messageErreur(e) }
}

watch(annee, charger)
onMounted(charger)
</script>

<template>
  <section class="fin-page">
    <div class="fin-toolbar">
      <div>
        <h1>Budget annuel</h1>
        <p class="fin-sub" style="margin:0">Budget = masse salariale (contrats actifs × 12) + dépenses internes.</p>
      </div>
      <label class="fin-label">Année
        <select v-model.number="annee" class="fin-champ">
          <option v-for="a in annees" :key="a" :value="a">{{ a }}</option>
        </select>
      </label>
    </div>

    <p v-if="erreur" class="fin-error" role="alert">{{ erreur }}</p>

    <template v-if="budget">
      <div class="fin-kpis">
        <div class="fin-card fin-kpi"><small>Budget total</small><strong>{{ ariary(budget.total.budget) }}</strong></div>
        <div class="fin-card fin-kpi"><small>Masse salariale annuelle</small><strong>{{ ariary(budget.total.masseSalariale) }}</strong></div>
        <div class="fin-card fin-kpi"><small>Dépenses internes</small><strong>{{ ariary(budget.total.depenses) }}</strong></div>
        <div class="fin-card fin-kpi"><small>Budget restant</small><strong style="color:var(--fin-primary)">{{ ariary(budget.total.restant) }}</strong></div>
      </div>

      <div class="fin-card fin-scroll">
        <table class="fin-table">
          <thead>
            <tr>
              <th>Département</th><th class="fin-num">Budget</th><th class="fin-num">Masse salariale</th>
              <th class="fin-num">Dépenses</th><th class="fin-num">Restant</th><th>Utilisation</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="d in budget.departements" :key="d.departement">
              <td><strong>{{ d.departement }}</strong></td>
              <td class="fin-num">{{ ariary(d.budget) }}</td>
              <td class="fin-num">{{ ariary(d.masseSalariale) }}</td>
              <td class="fin-num">{{ ariary(d.depenses) }}</td>
              <td class="fin-num">{{ ariary(d.restant) }}</td>
              <td>
                <div class="fin-bar" role="img" :aria-label="`${taux(d)} %`">
                  <i :class="{ hot: taux(d) >= 80 }" :style="{ width: taux(d) + '%' }"></i>
                </div>{{ taux(d) }} %
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </section>
</template>