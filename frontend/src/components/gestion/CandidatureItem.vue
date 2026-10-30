<script setup>
import { computed } from "vue";
import CandidatureActions from "./CandidatureActions.vue";
import { formatDate } from "@/config/libelles";

const props = defineProps({
  ligne: { type: Object, required: true },
  actif: { type: Boolean, default: false },
  afficherOffre: { type: Boolean, default: true }, // masqué quand toute la liste concerne la même offre
});
const emit = defineEmits(["select", "statut-change", "erreur"]);

const initiales = computed(
  () => `${props.ligne.prenom?.charAt(0) ?? ""}${props.ligne.nom?.charAt(0) ?? ""}`.toUpperCase() || "?",
);
</script>

<template>
  <article class="cand" :class="{ 'cand--actif': actif }">
    <!-- Zone cliquable : ouvre l'aperçu du CV -->
    <button type="button" class="cand__select" :aria-pressed="actif" @click="emit('select', ligne)">
      <span class="cand__avatar" aria-hidden="true">{{ initiales }}</span>
      <span class="cand__info">
        <span class="cand__nom">
          {{ ligne.prenom }} {{ ligne.nom }}
          <span v-if="ligne.statutLecture === 'NON_LU'" class="cand__nouveau">Nouveau</span>
        </span>
        <span v-if="ligne.email" class="cand__detail">{{ ligne.email }}</span>
        <span v-if="ligne.telephone" class="cand__detail">{{ ligne.telephone }}</span>
        <span v-if="afficherOffre" class="cand__offre">
          <template v-if="ligne.candidatureId">{{ ligne.poste }} · {{ ligne.recrutementReference }}</template>
          <template v-else>Candidature spontanée</template>
        </span>
        <span class="cand__date">
          <template v-if="ligne.experience !== null && ligne.experience !== undefined">{{ ligne.experience }} an(s) d'expérience · </template>
          Reçu le {{ formatDate(ligne.dateCandidature ?? ligne.dateDepot) }}
        </span>
      </span>
    </button>

    <!-- Les boutons sont hors du <button> ci-dessus (un bouton ne peut pas en contenir un autre) -->
    <footer class="cand__pied">
      <CandidatureActions
        :ligne="ligne"
        @statut-change="emit('statut-change', $event)"
        @erreur="emit('erreur', $event)"
      />
    </footer>
  </article>
</template>

<style scoped>
.cand {
  display: flex; flex-direction: column; gap: 10px; padding: 12px;
  background: #fff; border: 1px solid transparent; border-radius: var(--radius-md);
  transition: background-color .2s, border-color .2s;
}
.cand:hover { background: var(--color-primary-soft); border-color: var(--color-primary-line); }
.cand--actif { background: var(--color-primary-soft); border-color: var(--color-primary); }

.cand__select {
  display: flex; align-items: flex-start; gap: 12px; width: 100%; padding: 0;
  background: none; border: 0; text-align: left; cursor: pointer; font: inherit; color: inherit;
}
.cand__select:focus-visible { outline: 2px solid var(--color-primary); outline-offset: 4px; border-radius: 8px; }

.cand__avatar {
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  width: 42px; height: 42px; border: 1px solid #e8ebe6; border-radius: 10px;
  background: var(--color-primary-soft); color: var(--color-primary); font-size: 13px; font-weight: 700;
}
.cand--actif .cand__avatar { background: #fff; }

.cand__info { display: flex; flex-direction: column; gap: 3px; min-width: 0; flex: 1; }
.cand__nom { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; color: #292e2a; font-size: 14px; font-weight: 650; overflow-wrap: anywhere; }
.cand__nouveau { padding: 2px 7px; border-radius: 5px; background: #eaf5e9; color: #24752b; font-size: 10px; font-weight: 700; }
.cand__detail { color: #777e78; font-size: 12px; overflow-wrap: anywhere; }
.cand__offre { margin-top: 3px; color: var(--color-primary); font-size: 12px; font-weight: 600; }
.cand__date { margin-top: 3px; color: #989e98; font-size: 11px; }

.cand__pied { padding-left: 54px; }
</style>
