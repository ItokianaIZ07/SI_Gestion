<script setup>
// Liste générale des CV reçus (toutes offres + dépôts spontanés).
// Pour chaque candidature EN_ATTENTE : boutons Présélectionner / Rejeter ; sinon uniquement le statut.
import { onMounted, ref } from "vue";
import "@/assets/gestion.css";
import PageEntete from "@/components/gestion/PageEntete.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import CandidaturesPanel from "@/components/gestion/CandidaturesPanel.vue";
import { candidaturesRhApi } from "@/services/candidaturesRh";

const lignes = ref([]);
const chargement = ref(true);
const erreur = ref("");

async function charger() {
  chargement.value = true;
  erreur.value = "";
  try {
    lignes.value = (await candidaturesRhApi.liste()) ?? [];
  } catch (e) {
    erreur.value = e.message ?? "Impossible de charger les CV.";
  } finally {
    chargement.value = false;
  }
}

function majStatut({ candidatureId, statut, remarque }) {
  erreur.value = "";
  lignes.value = lignes.value.map((l) =>
    l.candidatureId === candidatureId ? { ...l, statut, remarque } : l,
  );
}

function majLu(cvId) {
  lignes.value = lignes.value.map((l) => (l.cvId === cvId ? { ...l, statutLecture: "LU" } : l));
}

// Candidature déjà traitée par quelqu'un d'autre (409) : on recharge pour afficher son vrai statut.
async function surErreur({ message, status }) {
  if (status === 409 || status === 404) await charger();
  erreur.value = message;
}

onMounted(charger);
</script>

<template>
  <div class="g-page">
    <PageEntete
      titre="CV reçus"
      sous-titre="Tous les CV déposés. Présélectionnez ou rejetez les candidatures en attente ; les autres affichent leur statut."
    >
      <button class="g-btn" type="button" @click="charger">Actualiser</button>
    </PageEntete>

    <MessageAlerte :message="erreur" />

    <CandidaturesPanel
      :lignes="lignes"
      :chargement="chargement"
      @statut-change="majStatut"
      @cv-lu="majLu"
      @erreur="surErreur"
    />
  </div>
</template>
