<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import { chargerAnnonces } from "@/services/annonces";
import BoutonRetour from "@/components/BoutonRetour.vue";
import OffreDetailCard from "@/components/client/OffreDetailCard.vue";

const route = useRoute();
const publicationId = Number(route.params.publicationId);

const annonce = ref(null);
const chargement = ref(true);
const erreur = ref("");

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
    <p v-if="chargement" class="detail__info">Chargement…</p>
    <p v-else-if="erreur" class="champ__erreur" role="alert">{{ erreur }}</p>

    <div v-else-if="!annonce" class="intro">
      <h1>Offre introuvable</h1>
      <p>Cette offre n'est plus disponible ou a expiré.</p>
      <p>
        <RouterLink class="bouton" to="/si/client/offres">Voir les offres ouvertes</RouterLink>
      </p>
    </div>
    <OffreDetailCard v-else :annonce="annonce" />
  </div>
</template>

<style scoped>
.detail__info {
  color: var(--texte-doux);
}

.detail {
  width: 100%;
  max-width: 760px;
  display: flex;
  flex-direction: column;
  gap: 28px;
  padding: 36px;
  background: var(--fond-card);
  border: 1.5px solid var(--couleur-texte);
  border-radius: 12px;
}

.detail__entete h1 {
  margin: 0 0 12px;
  font-size: 1.9rem;
  line-height: 1.2;
}

.detail__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0;
}

.detail__meta span,
.detail__tags li {
  padding: 4px 12px;
  font-size: .9rem;
  color: var(--texte-doux);
  background: var(--fond);
  border-radius: 999px;
}

.detail__limite {
  margin: 12px 0 0;
  color: var(--texte-doux);
}

.detail h2 {
  margin: 0 0 8px;
  font-size: 1.15rem;
}

.detail__texte {
  margin: 0;
  white-space: pre-line;
  color: var(--texte-doux);
}

.detail__tags {
  list-style: none;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0;
  padding: 0;
}

.detail__pied {
  display: flex;
  justify-content: flex-end;
  padding-top: 8px;
  border-top: 1px solid var(--fond);
}

.bouton--accent {
  color: #fff;
  background: var(--principale);
  border-color: var(--principale);
}

.bouton--accent:hover {
  background: #014d07;
  border-color: #014d07;
}

@media (max-width: 800px) {
  .detail {
    padding: 24px 20px;
  }
}
</style>
