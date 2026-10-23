<script setup>
import { ref, watch, computed } from 'vue'

const props = defineProps({
  postes: { type: Array, default: () => [] },   // [{ id, nom }]
  grille: { type: Object, default: null },      // null = création, objet = modification
  erreurServeur: { type: String, default: '' }
})
const emit = defineEmits(['enregistrer', 'annuler'])

const vide = () => ({ idPoste: '', valeurMin: null, valeurMax: null, dateDebutValidite: '', dateFinValidite: '' })
const form = ref(vide())
watch(() => props.grille, (g) => { form.value = g ? { ...g } : vide() }, { immediate: true })

const erreur = computed(() => {
  const f = form.value
  if (!f.idPoste) return 'Choisissez un poste.'
  if (f.valeurMin == null || f.valeurMax == null) return 'Renseignez le minimum et le maximum.'
  if (f.valeurMin < 0) return 'Le minimum ne peut pas être négatif.'
  if (f.valeurMax < f.valeurMin) return 'Le maximum doit être supérieur ou égal au minimum.'
  if (!f.dateDebutValidite) return 'Renseignez la date de début.'
  if (f.dateFinValidite && f.dateFinValidite < f.dateDebutValidite) return 'La date de fin doit suivre la date de début.'
  return ''
})
</script>

<template>
  <div class="fin-card" style="margin-bottom:16px">
    <div class="fin-grid">
      <label class="fin-label">Poste
        <select v-model="form.idPoste" class="fin-champ" :disabled="!!grille">
          <option value="" disabled>Choisir…</option>
          <option v-for="p in postes" :key="p.id" :value="p.id">{{ p.nom }}</option>
        </select>
      </label>
      <label class="fin-label">Salaire minimum (Ar)
        <input v-model.number="form.valeurMin" class="fin-champ" type="number" min="0">
      </label>
      <label class="fin-label">Salaire maximum (Ar)
        <input v-model.number="form.valeurMax" class="fin-champ" type="number" min="0">
      </label>
      <label class="fin-label">Début de validité
        <input v-model="form.dateDebutValidite" class="fin-champ" type="date">
      </label>
      <label class="fin-label">Fin de validité (optionnel)
        <input v-model="form.dateFinValidite" class="fin-champ" type="date">
      </label>
    </div>
    <p class="fin-hint">Les périodes d’un même poste ne peuvent pas se chevaucher.</p>
    <p v-if="erreur" class="fin-error">{{ erreur }}</p>
    <p v-else-if="erreurServeur" class="fin-error">{{ erreurServeur }}</p>
    <div class="fin-acts">
      <button class="fin-btn fin-btn-pri" type="button" :disabled="!!erreur"
        @click="emit('enregistrer', { ...form, dateFinValidite: form.dateFinValidite || null })">Enregistrer</button>
      <button class="fin-btn fin-btn-sec" type="button" @click="emit('annuler')">Annuler</button>
    </div>
  </div>
</template>