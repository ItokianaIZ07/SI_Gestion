<script setup>
// Décision sur une candidature.
//  - EN_ATTENTE            -> boutons « Présélectionner » / « Rejeter »
//  - déjà décidée          -> uniquement son statut, sans bouton
//  - dépôt spontané        -> pas de candidature, donc rien à décider
import { ref } from "vue";
import StatutBadge from "./StatutBadge.vue";
import { candidaturesRhApi } from "@/services/candidaturesRh";

const props = defineProps({
  ligne: { type: Object, required: true }, // une ligne de /api/rh/candidatures
});
const emit = defineEmits(["statut-change", "erreur"]);

const enCours = ref("");

async function agir(action) {
  if (enCours.value) return;
  const { candidatureId, prenom, nom } = props.ligne;
  if (action === "rejeter" && !window.confirm(`Rejeter la candidature de ${prenom} ${nom} ? Cette décision est définitive.`)) {
    return;
  }
  enCours.value = action;
  try {
    const resultat =
      action === "rejeter"
        ? await candidaturesRhApi.rejeter(candidatureId)
        : await candidaturesRhApi.preselectionner(candidatureId);
    emit("statut-change", { candidatureId, statut: resultat.statut, remarque: resultat.remarque });
  } catch (e) {
    emit("erreur", { message: e.message, status: e.status });
  } finally {
    enCours.value = "";
  }
}
</script>

<template>
  <div class="cand-actions">
    <span v-if="!ligne.candidatureId" class="g-badge g-badge--neutral">Dépôt spontané</span>

    <template v-else-if="ligne.statut === 'EN_ATTENTE'">
      <button
        type="button"
        class="g-btn g-btn--petit g-btn--primaire"
        :disabled="!!enCours"
        :aria-label="`Présélectionner ${ligne.prenom} ${ligne.nom}`"
        @click="agir('preselectionner')"
      >
        {{ enCours === "preselectionner" ? "…" : "Présélectionner" }}
      </button>
      <button
        type="button"
        class="g-btn g-btn--petit g-btn--danger"
        :disabled="!!enCours"
        :aria-label="`Rejeter ${ligne.prenom} ${ligne.nom}`"
        @click="agir('rejeter')"
      >
        {{ enCours === "rejeter" ? "…" : "Rejeter" }}
      </button>
    </template>

    <StatutBadge v-else :statut="ligne.statut" />
  </div>
</template>

<style scoped>
.cand-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
</style>
