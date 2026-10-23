<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ open: Boolean, titre: String, chargement: Boolean })
const emit = defineEmits(['confirmer', 'annuler'])

const remarque = ref('')
watch(() => props.open, (o) => { if (o) remarque.value = '' })
</script>

<template>
  <div v-if="open" class="overlay" @click.self="emit('annuler')">
    <div class="modal" role="dialog" aria-modal="true" aria-labelledby="refus-titre">
      <h2 id="refus-titre">Refuser la demande</h2>
      <p class="poste">{{ titre }}</p>
      <label class="fin-label">
        Remarque (obligatoire)
        <textarea v-model="remarque" placeholder="Motif du refus..."></textarea>
      </label>
      <div class="fin-acts bas">
        <button class="fin-btn fin-btn-sec" type="button" @click="emit('annuler')">Annuler</button>
        <button
          class="fin-btn fin-btn-dan"
          type="button"
          :disabled="!remarque.trim() || chargement"
          @click="emit('confirmer', remarque.trim())"
        >Confirmer le refus</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.overlay{position:fixed;inset:0;background:rgba(18,48,31,.5);display:grid;place-items:center;z-index:1000}
.modal{background:#fff;border-radius:12px;padding:24px;width:min(440px,90vw)}
h2{margin:0 0 4px;font-size:20px}
.poste{margin:0 0 12px;color:#4B5563}
textarea{min-height:90px;padding:10px;border:1px solid #9DB3A5;border-radius:8px;font:400 14px inherit;font-family:inherit}
.bas{justify-content:flex-end;margin-top:16px}
</style>