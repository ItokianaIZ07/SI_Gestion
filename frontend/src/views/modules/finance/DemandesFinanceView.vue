
<script setup>
import { ref, onMounted } from 'vue'
import '@/views/modules/finance/finance.css'
import { financeApi } from '@/api/financeApi'
import { ariary, messageErreur } from '@/utils/format'
import ValidationSteps from '@/views/modules/finance/components/ValidationSteps.vue'
import RefusModal from '@/views/modules/finance/components/RefusModal.vue'

const demandes = ref([])
const chargement = ref(false)
const erreur = ref('')
const aRefuser = ref(null)   // la demande en cours de refus

async function charger() {
  chargement.value = true
  erreur.value = ''
  try { demandes.value = await financeApi.getDemandes() }
  catch (e) { erreur.value = messageErreur(e) }
  finally { chargement.value = false }
}

async function valider(d) {
  erreur.value = ''
  try { await financeApi.valider(d.id); await charger() }
  catch (e) { erreur.value = messageErreur(e) }   // le backend revérifie budget et ordre
}

async function confirmerRefus(remarque) {
  const d = aRefuser.value
  try { await financeApi.refuser(d.id, remarque); aRefuser.value = null; await charger() }
  catch (e) { erreur.value = messageErreur(e); aRefuser.value = null }
}

onMounted(charger)
</script>

<template>
  <section class="fin-page">
    <h1>Demandes de recrutement</h1>
    <p class="fin-sub">Montant défini par les RH, analyse du budget et validation Finance.</p>

    <p v-if="erreur" class="fin-error" role="alert">{{ erreur }}</p>
    <p v-if="chargement">Chargement…</p>

    <div v-else class="fin-card fin-scroll">
      <table class="fin-table">
        <thead>
          <tr>
            <th>Poste</th><th>Département</th>
            <th class="fin-num">Effectif</th><th class="fin-num">Salaire estimé</th>
            <th class="fin-num">Mois</th><th class="fin-num">Coût total</th>
            <th class="fin-num">Budget restant</th><th>Budget</th>
            <th>Validations</th><th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="d in demandes" :key="d.id">
            <td><strong>{{ d.poste }}</strong></td>
            <td>{{ d.departement }}</td>
            <td class="fin-num">{{ d.effectif }}</td>
            <td class="fin-num">{{ ariary(d.salaireEstime) }}</td>
            <td class="fin-num">{{ d.nbMois }}</td>
            <td class="fin-num"><strong>{{ ariary(d.coutTotal) }}</strong></td>
            <td class="fin-num">{{ ariary(d.budgetRestant) }}</td>
            <td>
              <span class="fin-badge" :class="d.couvert ? 'fin-ok' : 'fin-ko'">
                {{ d.couvert ? '✓ Couvert' : '✕ Dépasse le budget' }}
              </span>
            </td>
            <td><ValidationSteps :etapes="d.etapes" /></td>
            <td>
              <div class="fin-acts">
                <button class="fin-btn fin-btn-pri" :disabled="!d.peutValider"
                  :title="d.peutValider ? '' : d.motifBlocage" @click="valider(d)">Valider</button>
                <button class="fin-btn fin-btn-dan" :disabled="!d.peutRefuser"
                  :title="d.peutRefuser ? '' : d.motifBlocage" @click="aRefuser = d">Refuser</button>
              </div>
            </td>
          </tr>
          <tr v-if="!demandes.length"><td colspan="10">Aucune demande à traiter.</td></tr>
        </tbody>
      </table>
    </div>

    <RefusModal :open="!!aRefuser" :titre="aRefuser?.poste"
      @confirmer="confirmerRefus" @annuler="aRefuser = null" />
  </section>
</template>