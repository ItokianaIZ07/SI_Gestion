<script setup>
import { reactive, ref } from 'vue'
import FormulairePosteGrille from '@/components/Manager/FormulairePosteGrille.vue'

const props = defineProps({
  utilisateurs: { type: Array, required: true },
  envoiEnCours: { type: Boolean, default: false },
})

const emit = defineEmits(['submit'])

const formulaire = reactive({
  nom: '',
  managerId: '',
  effectifMax: 1,
  postes: [{
    nom: '',
    valeurMin: null,
    valeurMax: null,
    dateDebutValidite: new Date().toISOString().slice(0, 10),
    dateFinValidite: '',
  }],
})
const erreurs = ref({})
const validationLancee = ref(false)

function valider() {
  const nouvellesErreurs = {}
  if (!formulaire.nom.trim()) {
    nouvellesErreurs.nom = 'Le nom du département est obligatoire.'
  }
  if (!formulaire.managerId) {
    nouvellesErreurs.managerId = 'Choisissez un manager.'
  }
  if (!Number.isInteger(Number(formulaire.effectifMax)) || Number(formulaire.effectifMax) < 1) {
    nouvellesErreurs.effectifMax = 'L’effectif maximal doit être un entier supérieur ou égal à 1.'
  }
  formulaire.postes.forEach((poste, index) => {
    if (
      !poste.nom.trim() ||
      poste.valeurMin === null ||
      poste.valeurMin === '' ||
      Number(poste.valeurMin) < 0 ||
      poste.valeurMax === null ||
      poste.valeurMax === '' ||
      Number(poste.valeurMax) < Number(poste.valeurMin) ||
      !poste.dateDebutValidite ||
      (poste.dateFinValidite && poste.dateFinValidite < poste.dateDebutValidite)
    ) {
      nouvellesErreurs[`poste-${index}`] = 'Complétez les champs du poste et vérifiez sa grille salariale.'
    }
  })
  erreurs.value = nouvellesErreurs
  return Object.keys(nouvellesErreurs).length === 0
}

function actualiserErreurs() {
  if (validationLancee.value) valider()
}

function envoyer() {
  validationLancee.value = true
  if (!valider()) return

  emit('submit', {
    nom: formulaire.nom.trim(),
    managerId: Number(formulaire.managerId),
    effectifMax: Number(formulaire.effectifMax),
    postes: formulaire.postes.map((poste) => ({
      nom: poste.nom.trim(),
      grilleSalaire: {
        valeurMin: Number(poste.valeurMin),
        valeurMax: Number(poste.valeurMax),
        dateDebutValidite: poste.dateDebutValidite,
        dateFinValidite: poste.dateFinValidite || null,
      },
    })),
  })
}
</script>

<template>
  <form class="g-form" novalidate @submit.prevent="envoyer">
    <div class="g-champ g-champ--large">
      <label class="g-obligatoire" for="departement-nom">Nom du département</label>
      <input
        id="departement-nom"
        v-model="formulaire.nom"
        class="g-input"
        type="text"
        maxlength="100"
        required
        autocomplete="organization"
        :aria-invalid="Boolean(erreurs.nom)"
        aria-describedby="departement-nom-erreur"
        @input="actualiserErreurs"
      >
      <small v-if="erreurs.nom" id="departement-nom-erreur" class="g-erreur" role="alert">
        {{ erreurs.nom }}
      </small>
    </div>

    <div class="g-champ">
      <label class="g-obligatoire" for="departement-manager">Manager</label>
      <select
        id="departement-manager"
        v-model="formulaire.managerId"
        class="g-input"
        required
        :disabled="!props.utilisateurs.length || props.envoiEnCours"
        :aria-invalid="Boolean(erreurs.managerId)"
        aria-describedby="departement-manager-erreur"
        @change="actualiserErreurs"
      >
        <option value="" disabled>
          {{ props.utilisateurs.length ? 'Sélectionner un manager' : 'Aucun utilisateur disponible' }}
        </option>
        <option v-for="utilisateur in props.utilisateurs" :key="utilisateur.id" :value="utilisateur.id">
          {{ utilisateur.nom }} {{ utilisateur.prenom }}
        </option>
      </select>
      <small v-if="erreurs.managerId" id="departement-manager-erreur" class="g-erreur" role="alert">
        {{ erreurs.managerId }}
      </small>
    </div>

    <div class="g-champ">
      <label class="g-obligatoire" for="departement-effectif-max">Effectif maximal</label>
      <input
        id="departement-effectif-max"
        v-model.number="formulaire.effectifMax"
        class="g-input"
        type="number"
        min="1"
        step="1"
        required
        :aria-invalid="Boolean(erreurs.effectifMax)"
        aria-describedby="departement-effectif-erreur"
        @input="actualiserErreurs"
      >
      <small v-if="erreurs.effectifMax" id="departement-effectif-erreur" class="g-erreur" role="alert">
        {{ erreurs.effectifMax }}
      </small>
    </div>

    <FormulairePosteGrille
      v-model="formulaire.postes"
      :validation-lancee="validationLancee"
      :desactive="props.envoiEnCours"
    />

    <div class="g-form__actions">
      <button class="g-btn g-btn--primaire" type="submit" :disabled="props.envoiEnCours || !props.utilisateurs.length">
        {{ props.envoiEnCours ? 'Enregistrement…' : 'Créer le département' }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.g-erreur {
  color: var(--color-danger);
}
</style>
