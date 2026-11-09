<script setup>
// Annulation d'une demande de congé VALIDÉE : motif obligatoire, puis confirmation.
// POST /api/conge/demandes/{id}/annulation (rôles EMPLOYE concerné ou RH ; le backend revérifie tout :
// demande validée, congé non commencé, droit de l'utilisateur).
import { ref } from 'vue'
import '@/assets/finance.css'
import '@/assets/conge.css'
import { congeApi } from '@/api/congeApi'
import { messageErreur } from '@/utils/format'

const props = defineProps({ idDemande: { type: Number, required: true } })
const emit = defineEmits(['annulee'])

const etape = ref('ferme')      // 'ferme' | 'saisie' | 'confirmation'
const motif = ref('')
const envoi = ref(false)
const erreur = ref('')

const texteErreur = (e) => (e?.response?.status === 401 ? 'Votre session a expiré. Reconnectez-vous.' : messageErreur(e))

function ouvrir() { etape.value = 'saisie'; erreur.value = '' }
function annuler() { etape.value = 'ferme'; motif.value = ''; erreur.value = '' }

function continuer() {
  if (!motif.value.trim()) { erreur.value = 'Le motif de l’annulation est obligatoire.'; return }
  erreur.value = ''
  etape.value = 'confirmation'
}

async function confirmer() {
  envoi.value = true
  erreur.value = ''
  try {
    const r = await congeApi.annulerDemande(props.idDemande, motif.value.trim())
    emit('annulee', r)
    annuler()
  } catch (e) {
    erreur.value = texteErreur(e)      // ex. congé déjà commencé, demande non validée…
    etape.value = 'saisie'
  } finally {
    envoi.value = false
  }
}
</script>

<template>
  <div class="ann">
    <button v-if="etape === 'ferme'" type="button" class="fin-btn fin-btn-dan" @click="ouvrir">Annuler cette demande</button>

    <div v-else class="ann__panneau">
      <label class="fin-label">Motif de l’annulation <span class="fin-hint">(obligatoire)</span>
        <textarea v-model="motif" class="fin-champ" rows="3" maxlength="2000" :disabled="envoi || etape === 'confirmation'"
                  placeholder="Pourquoi cette demande est-elle annulée ?"></textarea>
      </label>
      <p v-if="erreur" class="fin-error" role="alert">{{ erreur }}</p>
      <p v-if="etape === 'confirmation'" class="cg-confirm" role="alert">
        Confirmer l’annulation ? Les jours déduits seront restitués au solde et cette action est définitive.
      </p>
      <div class="cg-actions">
        <template v-if="etape === 'saisie'">
          <button type="button" class="fin-btn fin-btn-sec" @click="annuler">Retour</button>
          <button type="button" class="fin-btn fin-btn-dan" @click="continuer">Continuer</button>
        </template>
        <template v-else>
          <button type="button" class="fin-btn fin-btn-sec" :disabled="envoi" @click="etape = 'saisie'">Retour</button>
          <button type="button" class="fin-btn fin-btn-dan" :disabled="envoi" @click="confirmer">{{ envoi ? 'Envoi…' : 'Confirmer l’annulation' }}</button>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ann { margin-top: 18px; padding-top: 14px; border-top: 1px solid #E7EEE9; }
.ann__panneau { display: grid; gap: 10px; }
.ann textarea { padding: 10px 12px; resize: vertical; }
</style>
