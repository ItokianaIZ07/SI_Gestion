<script setup>
import Pagination from "@/components/ui/Pagination.vue";
import { usePagination } from "@/composable/usePagination";
import { ref, computed, onMounted } from "vue";
import { chargerAnnonces } from "@/services/annonces";
import HeroBanner from "@/components/ui/HeroBanner.vue";
import OffreCard from '../../../components/client/OffreCard.vue'

const annonces = ref([]);
const chargement = ref(true);
const erreur = ref("");
const recherche = ref("");

onMounted(async () => {
  try {
    annonces.value = await chargerAnnonces();
  } catch (e) {
    erreur.value = e.message;
  } finally {
    chargement.value = false;
  }
});

const resultats = computed(() => {
  const q = recherche.value.trim().toLowerCase();
  if (!q) return annonces.value;
  return annonces.value.filter(({ offre }) =>
    [offre.titre, offre.descriptionPoste, offre.competencesRequises, offre.niveauEtude]
      .join(" ").toLowerCase().includes(q),
  );
});


const pg = usePagination(resultats, 6);
</script>

<template>
  <div class="principal principal--formulaire">
    <HeroBanner class="offres__hero" eyebrow="Carrières" titre="Trouvez le poste qui vous correspond"
      texte="Parcourez les offres ouvertes et postulez en déposant votre CV.">
      <input v-model="recherche" class="offres__recherche" type="search"
        placeholder="Rechercher un poste, une compétence…" aria-label="Rechercher une offre" />
    </HeroBanner>

    <p v-if="chargement" class="offres__info">Chargement des offres…</p>
    <p v-else-if="erreur" class="champ__erreur offres__info" role="alert">{{ erreur }}</p>
    <p v-else-if="!resultats.length" class="offres__info">
      {{ annonces.length ? "Aucune offre ne correspond à votre recherche." :
        "Aucune offre n'est ouverte pour le moment." }}
    </p>

    <ul v-else class="offres__grille">
      <li v-for="{ offre, publication } in pg.items" :key="offre.id">
        <OffreCard :offre="offre", :publication="publication"/>
      </li>
    </ul>

    <Pagination v-if="!chargement && !erreur" class="offres__pagination" v-bind="pg.barre" @update:page="pg.aller" />

    <p class="offres__spontanee">
      Aucune offre ne vous correspond ?
      <RouterLink to="/si/client/upload-selection">Déposez une candidature spontanée</RouterLink>.
    </p>
  </div>
</template>

<style scoped>
.offres__hero { max-width: 960px; }

.offres__recherche {
  width: 100%;
  max-width: 460px;
  box-shadow: 0 4px 14px rgba(0, 40, 6, .18);
  padding: 12px 16px;
  font: inherit;
  color: var(--texte);
  background: var(--fond-card);
  border: 1.5px solid var(--couleur-texte);
  border-radius: 8px;
}

.offres__recherche:focus {
  outline: none;
  border-color: var(--secondaire);
  box-shadow: 0 0 0 1px var(--texte-success);
}

.offres__info {
  margin: 0;
  color: var(--texte-doux);
}

.offres__grille {
  list-style: none;
  margin: 0;
  padding: 0;
  width: 100%;
  max-width: 960px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(290px, 1fr));
  gap: 24px;
}

.offres__pagination { width: 100%; max-width: 960px; border: 1px solid var(--color-border); border-radius: var(--radius-md); }

.offres__spontanee {
  margin: 0;
  color: var(--texte-doux);
}

.offres__spontanee a {
  text-decoration: underline;
  text-decoration-color: var(--secondaire);
  text-underline-offset: 3px;
}
</style>
