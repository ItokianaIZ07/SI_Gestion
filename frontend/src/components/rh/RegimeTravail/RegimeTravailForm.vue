<script setup>
import { reactive, watch } from "vue";

const props = defineProps({
  regime: { type: Object, default: null },
  envoi: { type: Boolean, default: false },
});

const emit = defineEmits(["enregistrer", "annuler"]);

const form = reactive({
  code: "",
  libelle: "",
  description: "",
  heuresHebdomadaires: "",
});

watch(
  () => props.regime,
  (regime) => {
    form.code = regime?.code ?? "";
    form.libelle = regime?.libelle ?? "";
    form.description = regime?.description ?? "";
    form.heuresHebdomadaires = regime?.heuresHebdomadaires ?? "";
  },
  { immediate: true },
);

const estTempsPlein = () => props.regime?.code === "TEMPS_PLEIN";

function soumettre() {
  const code = form.code.trim();
  const libelle = form.libelle.trim();
  const heures = Number(form.heuresHebdomadaires);

  if (!code || /\s/.test(code)) {
    return;
  }
  if (!libelle || !Number.isFinite(heures) || heures <= 0 || heures > 60) {
    return;
  }

  emit("enregistrer", {
    code,
    libelle,
    description: form.description.trim() || null,
    heuresHebdomadaires: heures,
  });
}
</script>

<template>
  <form class="g-carte g-form rt-form" @submit.prevent="soumettre">
    <div class="g-champ">
      <label for="rt-code" class="g-obligatoire">Code</label>
      <input
        id="rt-code"
        v-model="form.code"
        class="g-input"
        type="text"
        maxlength="50"
        pattern="\S+"
        title="Le code ne doit contenir aucun espace."
        autocomplete="off"
        :readonly="estTempsPlein()"
        required
      />
      <small v-if="estTempsPlein()">Le code du régime TEMPS_PLEIN est fixe.</small>
    </div>

    <div class="g-champ">
      <label for="rt-libelle" class="g-obligatoire">Libellé</label>
      <input
        id="rt-libelle"
        v-model="form.libelle"
        class="g-input"
        type="text"
        maxlength="100"
        required
      />
    </div>

    <div class="g-champ">
      <label for="rt-heures" class="g-obligatoire">Heures hebdomadaires</label>
      <input
        id="rt-heures"
        v-model="form.heuresHebdomadaires"
        class="g-input"
        type="number"
        min="0.01"
        max="60"
        step="0.01"
        required
      />
      <small>La durée doit être supérieure à 0 et ne pas dépasser 60 heures.</small>
    </div>

    <div class="g-champ g-champ--large">
      <label for="rt-description">Description</label>
      <textarea id="rt-description" v-model="form.description" class="g-input" rows="4" />
    </div>

    <div class="g-form__actions">
      <button class="g-btn" type="button" :disabled="envoi" @click="emit('annuler')">
        Annuler
      </button>
      <button class="g-btn g-btn--primaire" type="submit" :disabled="envoi">
        {{ envoi ? "Enregistrement…" : regime ? "Enregistrer les modifications" : "Créer le régime" }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.rt-form {
  width: 100%;
}

.rt-form input[readonly] {
  cursor: not-allowed;
}
</style>
