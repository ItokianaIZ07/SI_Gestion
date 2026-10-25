<script setup>
const props = defineProps({
  modelValue: { type: Array, required: true },
  validationLancee: { type: Boolean, default: false },
  desactive: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

function modifier(index, champ, valeur) {
  const lignes = props.modelValue.map((poste) => ({ ...poste }))
  lignes[index][champ] = valeur
  emit('update:modelValue', lignes)
}

function ajouterLigne() {
  emit('update:modelValue', [
    ...props.modelValue,
    {
      nom: '',
      valeurMin: null,
      valeurMax: null,
      dateDebutValidite: new Date().toISOString().slice(0, 10),
      dateFinValidite: '',
    },
  ])
}

function supprimerLigne(index) {
  emit('update:modelValue', props.modelValue.filter((_, ligne) => ligne !== index))
}

function erreurPour(poste, champ) {
  if (!props.validationLancee) return ''

  if (champ === 'nom' && !poste.nom.trim()) return 'Le nom du poste est obligatoire.'
  if (champ === 'valeurMin' && (poste.valeurMin === null || poste.valeurMin === '' || Number(poste.valeurMin) < 0)) {
    return 'Saisissez un salaire minimum positif ou nul.'
  }
  if (champ === 'valeurMax') {
    if (poste.valeurMax === null || poste.valeurMax === '' || Number(poste.valeurMax) < 0) {
      return 'Saisissez un salaire maximum positif ou nul.'
    }
    if (Number(poste.valeurMax) < Number(poste.valeurMin)) {
      return 'Le maximum doit être supérieur ou égal au minimum.'
    }
  }
  if (champ === 'dateDebutValidite' && !poste.dateDebutValidite) {
    return 'La date de début est obligatoire.'
  }
  if (
    champ === 'dateFinValidite' &&
    poste.dateFinValidite &&
    poste.dateDebutValidite &&
    poste.dateFinValidite < poste.dateDebutValidite
  ) {
    return 'La date de fin doit être postérieure ou égale à la date de début.'
  }

  return ''
}
</script>

<template>
  <section class="postes" aria-labelledby="titre-postes">
    <header class="postes__entete">
      <div>
        <h2 id="titre-postes">Postes et grilles salariales</h2>
        <p>Ajoutez les postes du département et leur première grille de salaire.</p>
      </div>
      <button class="g-btn" type="button" :disabled="props.desactive" @click="ajouterLigne">
        Ajouter un poste
      </button>
    </header>

    <p v-if="!props.modelValue.length" class="postes__vide">
      Aucun poste ajouté. Vous pouvez créer le département sans poste.
    </p>

    <fieldset
      v-for="(poste, index) in props.modelValue"
      :key="index"
      class="poste"
      :disabled="props.desactive"
    >
      <legend>Poste {{ index + 1 }}</legend>

      <label class="g-champ">
        <span class="g-obligatoire">Nom du poste</span>
        <input
          class="g-input"
          type="text"
          maxlength="100"
          :value="poste.nom"
          :aria-invalid="Boolean(erreurPour(poste, 'nom'))"
          @input="modifier(index, 'nom', $event.target.value)"
        >
        <small v-if="erreurPour(poste, 'nom')" class="g-erreur" role="alert">
          {{ erreurPour(poste, 'nom') }}
        </small>
      </label>

      <label class="g-champ">
        <span class="g-obligatoire">Salaire minimum (Ar)</span>
        <input
          class="g-input"
          type="number"
          min="0"
          step="0.01"
          :value="poste.valeurMin ?? ''"
          :aria-invalid="Boolean(erreurPour(poste, 'valeurMin'))"
          @input="modifier(index, 'valeurMin', $event.target.value === '' ? null : Number($event.target.value))"
        >
        <small v-if="erreurPour(poste, 'valeurMin')" class="g-erreur" role="alert">
          {{ erreurPour(poste, 'valeurMin') }}
        </small>
      </label>

      <label class="g-champ">
        <span class="g-obligatoire">Salaire maximum (Ar)</span>
        <input
          class="g-input"
          type="number"
          min="0"
          step="0.01"
          :value="poste.valeurMax ?? ''"
          :aria-invalid="Boolean(erreurPour(poste, 'valeurMax'))"
          @input="modifier(index, 'valeurMax', $event.target.value === '' ? null : Number($event.target.value))"
        >
        <small v-if="erreurPour(poste, 'valeurMax')" class="g-erreur" role="alert">
          {{ erreurPour(poste, 'valeurMax') }}
        </small>
      </label>

      <label class="g-champ">
        <span class="g-obligatoire">Début de validité</span>
        <input
          class="g-input"
          type="date"
          :value="poste.dateDebutValidite"
          :aria-invalid="Boolean(erreurPour(poste, 'dateDebutValidite'))"
          @input="modifier(index, 'dateDebutValidite', $event.target.value)"
        >
        <small v-if="erreurPour(poste, 'dateDebutValidite')" class="g-erreur" role="alert">
          {{ erreurPour(poste, 'dateDebutValidite') }}
        </small>
      </label>

      <label class="g-champ">
        <span>Fin de validité (optionnel)</span>
        <input
          class="g-input"
          type="date"
          :value="poste.dateFinValidite"
          :aria-invalid="Boolean(erreurPour(poste, 'dateFinValidite'))"
          @input="modifier(index, 'dateFinValidite', $event.target.value)"
        >
        <small v-if="erreurPour(poste, 'dateFinValidite')" class="g-erreur" role="alert">
          {{ erreurPour(poste, 'dateFinValidite') }}
        </small>
      </label>

      <button
        class="g-btn g-btn--danger poste__supprimer"
        type="button"
        :aria-label="`Supprimer le poste ${index + 1}`"
        @click="supprimerLigne(index)"
      >
        Supprimer ce poste
      </button>
    </fieldset>
  </section>
</template>

<style scoped>
.postes {
  grid-column: 1 / -1;
  display: grid;
  gap: 16px;
}

.postes__entete {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.postes__entete h2 {
  margin: 0;
  color: var(--color-ink);
  font-size: 1.1rem;
}

.postes__entete p,
.postes__vide {
  margin: 6px 0 0;
  color: var(--color-muted);
  font-size: 13px;
}

.poste {
  min-width: 0;
  margin: 0;
  padding: 18px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.poste legend {
  padding: 0 6px;
  color: var(--color-ink);
  font-weight: 600;
}

.poste__supprimer {
  grid-column: 1 / -1;
  justify-self: end;
}

.g-erreur {
  color: var(--color-danger);
}

@media (max-width: 650px) {
  .postes__entete {
    align-items: flex-start;
    flex-direction: column;
  }

  .poste {
    grid-template-columns: 1fr;
    padding: 14px;
  }
}
</style>
