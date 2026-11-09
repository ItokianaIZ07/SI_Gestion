<script setup>
import { computed, reactive, watch } from "vue";

const props = defineProps({
  horaire: { type: Object, default: null },
  envoi: { type: Boolean, default: false },
});

const emit = defineEmits(["enregistrer", "annuler"]);
const nomsJours = ["L", "M", "M", "J", "V", "S", "D"];

const form = reactive({
  code: "",
  libelle: "",
  heureDebut: "",
  heureFin: "",
  debutPause: "",
  finPause: "",
  toleranceRetardMinutes: 0,
  joursTravail: [],
});

const codeStandard = computed(() => props.horaire?.code === "STANDARD");

function heureEnMinutes(heure) {
  if (!heure) return null;
  const [h, m] = heure.split(":").map(Number);
  return h * 60 + m;
}

const dureePrevue = computed(() => {
  const debut = heureEnMinutes(form.heureDebut);
  const fin = heureEnMinutes(form.heureFin);
  if (debut === null || fin === null || fin <= debut) return null;
  const pauseDebut = heureEnMinutes(form.debutPause);
  const pauseFin = heureEnMinutes(form.finPause);
  const dureePause = pauseDebut !== null && pauseFin !== null && pauseFin > pauseDebut
    ? pauseFin - pauseDebut
    : 0;
  return fin - debut - dureePause;
});

const erreurs = computed(() => {
  const resultat = [];
  const debut = heureEnMinutes(form.heureDebut);
  const fin = heureEnMinutes(form.heureFin);
  const pauseDebut = heureEnMinutes(form.debutPause);
  const pauseFin = heureEnMinutes(form.finPause);

  if (!form.code.trim()) resultat.push("Le code est obligatoire.");
  else if (/\s/.test(form.code.trim())) resultat.push("Le code ne doit contenir aucun espace.");
  if (!form.libelle.trim()) resultat.push("Le libellé est obligatoire.");
  if (debut === null || fin === null) resultat.push("Les heures de début et de fin sont obligatoires.");
  else if (fin <= debut) resultat.push("L'heure de fin doit être après l'heure de début.");
  if ((form.debutPause && !form.finPause) || (!form.debutPause && form.finPause)) {
    resultat.push("La pause doit avoir un début et une fin, ou être absente.");
  } else if (pauseDebut !== null && pauseFin !== null
      && (debut === null || fin === null || pauseDebut < debut || pauseFin <= pauseDebut || pauseFin > fin)) {
    resultat.push("La pause doit être comprise entre le début et la fin de la journée.");
  }
  if (String(form.toleranceRetardMinutes).trim() === ""
      || !Number.isInteger(Number(form.toleranceRetardMinutes))
      || Number(form.toleranceRetardMinutes) < 0) {
    resultat.push("La tolérance doit être supérieure ou égale à 0.");
  }
  if (!form.joursTravail.length) resultat.push("Sélectionnez au moins un jour travaillé.");
  return resultat;
});

watch(
  () => props.horaire,
  (horaire) => {
    form.code = horaire?.code ?? "";
    form.libelle = horaire?.libelle ?? "";
    form.heureDebut = horaire?.heureDebut?.slice(0, 5) ?? "";
    form.heureFin = horaire?.heureFin?.slice(0, 5) ?? "";
    form.debutPause = horaire?.debutPause?.slice(0, 5) ?? "";
    form.finPause = horaire?.finPause?.slice(0, 5) ?? "";
    form.toleranceRetardMinutes = horaire?.toleranceRetardMinutes ?? 0;
    form.joursTravail = [...(horaire?.joursTravail ?? [])].map(Number);
  },
  { immediate: true },
);

function formatterDuree(minutes) {
  if (minutes === null) return "—";
  return `${Math.floor(minutes / 60)} h ${String(minutes % 60).padStart(2, "0")} min`;
}

function soumettre() {
  if (erreurs.value.length) return;
  emit("enregistrer", {
    code: form.code.trim(),
    libelle: form.libelle.trim(),
    heureDebut: form.heureDebut,
    heureFin: form.heureFin,
    debutPause: form.debutPause || null,
    finPause: form.finPause || null,
    toleranceRetardMinutes: Number(form.toleranceRetardMinutes),
    joursTravail: [...form.joursTravail].sort((a, b) => a - b),
  });
}
</script>

<template>
  <form class="g-carte g-form ht-form" novalidate @submit.prevent="soumettre">
    <div class="g-champ">
      <label for="ht-code" class="g-obligatoire">Code</label>
      <input id="ht-code" v-model="form.code" class="g-input" maxlength="50" :readonly="codeStandard" required />
      <small v-if="codeStandard">Le code de l’horaire STANDARD est fixe.</small>
    </div>
    <div class="g-champ">
      <label for="ht-libelle" class="g-obligatoire">Libellé</label>
      <input id="ht-libelle" v-model="form.libelle" class="g-input" maxlength="100" required />
    </div>

    <div class="g-champ">
      <label for="ht-debut" class="g-obligatoire">Début</label>
      <input id="ht-debut" v-model="form.heureDebut" class="g-input" type="time" required />
    </div>
    <div class="g-champ">
      <label for="ht-fin" class="g-obligatoire">Fin</label>
      <input id="ht-fin" v-model="form.heureFin" class="g-input" type="time" required />
    </div>

    <div class="g-champ">
      <label for="ht-pause-debut">Début de pause (facultatif)</label>
      <input id="ht-pause-debut" v-model="form.debutPause" class="g-input" type="time" />
    </div>
    <div class="g-champ">
      <label for="ht-pause-fin">Fin de pause (facultatif)</label>
      <input id="ht-pause-fin" v-model="form.finPause" class="g-input" type="time" />
    </div>

    <div class="g-champ">
      <label for="ht-tolerance" class="g-obligatoire">Tolérance de retard (minutes)</label>
      <input
        id="ht-tolerance"
        v-model.number="form.toleranceRetardMinutes"
        class="g-input"
        type="number"
        min="0"
        step="1"
        required
      />
    </div>
    <div class="g-champ">
      <label>Durée prévue</label>
      <output class="ht-duree">{{ formatterDuree(dureePrevue) }}</output>
    </div>

    <fieldset class="g-champ g-champ--large ht-jours">
      <legend class="g-obligatoire">Jours travaillés</legend>
      <label v-for="(jour, index) in nomsJours" :key="index" class="ht-jour">
        <input v-model="form.joursTravail" type="checkbox" :value="index + 1" />
        <span>{{ jour }}</span>
      </label>
    </fieldset>

    <ul v-if="erreurs.length" class="ht-erreurs g-champ--large" aria-live="polite">
      <li v-for="erreur in erreurs" :key="erreur">{{ erreur }}</li>
    </ul>

    <div class="g-form__actions">
      <button class="g-btn" type="button" :disabled="envoi" @click="emit('annuler')">Annuler</button>
      <button class="g-btn g-btn--primaire" type="submit" :disabled="envoi || erreurs.length > 0">
        {{ envoi ? "Enregistrement…" : horaire ? "Enregistrer les modifications" : "Créer l'horaire" }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.ht-form input[readonly] {
  cursor: not-allowed;
}

.ht-duree {
  min-height: 42px;
  display: flex;
  align-items: center;
  padding: 10px 12px;
  color: var(--color-ink);
  background: var(--color-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-weight: 700;
}

.ht-jours {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin: 0;
  padding: 0;
  border: 0;
}

.ht-jours legend {
  width: 100%;
  margin-bottom: 4px;
  font-size: 13.5px;
  font-weight: 600;
}

.ht-jour {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.ht-erreurs {
  margin: 0;
  padding: 12px 16px 12px 36px;
  color: var(--color-danger);
  background: var(--color-danger-background);
  border: 1px solid var(--color-danger-line);
  border-radius: var(--radius-sm);
  font-size: 13px;
}
</style>
