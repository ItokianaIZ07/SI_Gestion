<script setup>
// Une ligne du tableau des candidatures d'une offre.
//  - candidature EN_ATTENTE : boutons Présélectionner / Rejeter
//  - déjà décidée           : uniquement son statut (aucun bouton)
import { computed } from "vue";
import CandidatureActions from "@/components/gestion/CandidatureActions.vue";
import { formatDate } from "@/config/libelles";

const props = defineProps({
  ligne: { type: Object, required: true }, // une ligne de /api/rh/candidatures
});
const emit = defineEmits(["statut-change", "erreur"]);

const initiales = computed(
  () => `${props.ligne.prenom?.charAt(0) ?? ""}${props.ligne.nom?.charAt(0) ?? ""}`.toUpperCase() || "?",
);
</script>

<template>
  <tr>
    <td>
      <div class="candidat">
        <span class="candidat__avatar" aria-hidden="true">{{ initiales }}</span>
        <div>
          <span class="g-principal">
            {{ ligne.prenom }} {{ ligne.nom }}
            <span v-if="ligne.statutLecture === 'NON_LU'" class="candidat__nouveau">Nouveau</span>
          </span>
          <span class="g-secondaire">{{ ligne.email }}</span>
          <span v-if="ligne.telephone" class="g-secondaire">{{ ligne.telephone }}</span>
        </div>
      </div>
    </td>
    <td>
      <template v-if="ligne.experience !== null && ligne.experience !== undefined">{{ ligne.experience }} an(s)</template>
      <template v-else>—</template>
    </td>
    <td>{{ formatDate(ligne.dateCandidature ?? ligne.dateDepot) }}</td>
    <td>
      <CandidatureActions :ligne="ligne" @statut-change="emit('statut-change', $event)" @erreur="emit('erreur', $event)" />
    </td>
    <td>
      <div class="g-actions-cellule">
        <RouterLink
          class="g-btn g-btn--petit"
          :to="{ name: 'CvCandidat', params: { candidatId: ligne.candidatId }, query: { cvId: ligne.cvId } }"
          :aria-label="`Voir le CV de ${ligne.prenom} ${ligne.nom}`"
        >Voir le CV</RouterLink>
      </div>
    </td>
  </tr>
</template>

<style scoped>
.candidat { display: flex; align-items: center; gap: 12px; }
.candidat__avatar {
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  width: 40px; height: 40px; border-radius: 10px; font-size: 13px; font-weight: 700;
  background: var(--color-primary-soft); color: var(--color-primary); border: 1px solid var(--color-primary-line);
}
.candidat__nouveau { margin-left: 8px; padding: 2px 7px; border-radius: 5px; background: var(--color-success-background); color: var(--color-success); font-size: 10px; font-weight: 700; }
</style>
