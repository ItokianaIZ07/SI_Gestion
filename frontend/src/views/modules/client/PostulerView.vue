<script setup>
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { chargerAnnonces } from "@/services/annonces";
import FormulaireCV from "@/components/FormulaireCV.vue";
import BoutonRetour from "@/components/BoutonRetour.vue";

const route = useRoute();
const publicationId = Number(route.params.publicationId);

const annonce = ref(null);
const chargement = ref(true);
const erreur = ref("");
const depose = ref(false);

onMounted(async () => {
  try {
    const annonces = await chargerAnnonces();
    annonce.value = annonces.find((a) => a.publication.id === publicationId) ?? null;
  } catch (e) {
    erreur.value = e.message;
  } finally {
    chargement.value = false;
  }
});
</script>

<template>
  <BoutonRetour />

  <div class="principal principal--formulaire">
    <p v-if="chargement">Chargement…</p>
    <p v-else-if="erreur" class="champ__erreur" role="alert">{{ erreur }}</p>

    <div v-else-if="!annonce" class="intro">
      <h1>Offre introuvable</h1>
      <p>Cette offre n'est plus disponible ou a expiré.</p>
      <p><RouterLink class="bouton" to="/si/client/offres">Voir les offres ouvertes</RouterLink></p>
    </div>

    <template v-else>
      <div class="intro">
        <h1>Postuler : {{ annonce.offre.titre }}</h1>
        <p>Renseignez vos informations et joignez votre CV au format PDF. Il s'affichera en dessous dès que vous l'aurez choisi.</p>
      </div>

      <FormulaireCV
        :recrutement-id="annonce.offre.recrutementId"
        :publication-id="annonce.publication.id"
        @depose="depose = true"
      />

      <p v-if="depose">
        <RouterLink class="bouton" to="/si/client/offres">Voir les autres offres</RouterLink>
      </p>
    </template>
  </div>
</template>
