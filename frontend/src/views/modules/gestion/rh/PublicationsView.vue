<script setup>
import Pagination from "@/components/ui/Pagination.vue";
import { usePagination } from "@/composable/usePagination";
// Publications des offres sur les plateformes (dont le site candidat).
// Seules les publications PUBLIEE non expirées sont visibles côté candidat.
import { ref, computed, onMounted } from "vue";
import { publicationsApi, offresApi } from "@/services/ressources";
import { STATUTS_PUBLICATION, formatDate } from "@/config/libelles";
import PageEntete from "@/components/gestion/PageEntete.vue";
import StatutBadge from "@/components/gestion/StatutBadge.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import "@/assets/gestion.css";

const publications = ref([]);
const offres = ref([]);
const chargement = ref(true);
const erreur = ref("");
const succes = ref("");

const recherche = ref("");
const filtreStatut = ref("");

async function charger() {
  chargement.value = true;
  erreur.value = "";
  try {
    const [p, o] = await Promise.all([publicationsApi.liste(), offresApi.liste()]);
    publications.value = p ?? [];
    offres.value = o ?? [];
  } catch (e) {
    erreur.value = e.message;
  } finally {
    chargement.value = false;
  }
}

const offreParId = computed(() => new Map(offres.value.map((o) => [o.id, o])));

const lignes = computed(() => {
  const q = recherche.value.trim().toLowerCase();
  return publications.value
    .filter((p) => {
      if (filtreStatut.value && p.statut !== filtreStatut.value) return false;
      return !q || [p.plateforme, offreParId.value.get(p.offreId)?.titre].join(" ").toLowerCase().includes(q);
    })
    .sort((a, b) => (b.datePublication ?? "").localeCompare(a.datePublication ?? ""));
});

async function changerStatut(p, statut) {
  erreur.value = "";
  succes.value = "";
  try {
    await publicationsApi.modifier(p.id, {
      offreId: p.offreId,
      plateforme: p.plateforme,
      urlPublication: p.urlPublication,
      datePublication: p.datePublication,
      dateExpiration: p.dateExpiration,
      statut,
    });
    succes.value = statut === "PUBLIEE" ? "Publication mise en ligne." : "Publication retirée.";
    await charger();
  } catch (e) {
    erreur.value = e.message;
  }
}

async function supprimer(p) {
  if (!window.confirm("Supprimer cette publication ? Cette action est définitive.")) return;
  erreur.value = "";
  succes.value = "";
  try {
    await publicationsApi.supprimer(p.id);
    succes.value = "Publication supprimée.";
    await charger();
  } catch (e) {
    erreur.value = e.message;
  }
}

onMounted(charger);

/* Pagination du tableau */
const pg = usePagination(lignes, 10);
</script>

<template>
  <div class="g-page">
    <PageEntete titre="Publications" sous-titre="Diffusez vos offres validées. Les candidats ne voient que les publications actives.">
      <RouterLink class="g-btn g-btn--primaire" to="/si/app/rh/publications/creation">Nouvelle publication</RouterLink>
    </PageEntete>

    <MessageAlerte :message="erreur" />
    <MessageAlerte :message="succes" type="succes" />

    <section class="g-carte">
      <div class="g-filtres">
        <input v-model="recherche" class="g-input g-input--recherche" type="search" placeholder="Rechercher une offre ou une plateforme…" />
        <select v-model="filtreStatut" class="g-input" aria-label="Filtrer par statut">
          <option value="">Tous les statuts</option>
          <option v-for="o in STATUTS_PUBLICATION" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
        <span class="g-compteur">{{ lignes.length }} publication(s)</span>
      </div>

      <p v-if="chargement" class="g-vide">Chargement…</p>
      <div v-else-if="!lignes.length" class="g-vide">
        <strong>Aucune publication</strong>
        Publiez une offre validée pour recevoir des candidatures.
      </div>

      <div v-else class="g-table-wrap">
        <table class="g-table">
          <thead>
            <tr>
              <th>Offre</th>
              <th>Plateforme</th>
              <th>Publiée le</th>
              <th>Expire le</th>
              <th>Statut</th>
              <th class="g-droite">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in pg.items" :key="p.id">
              <td class="g-principal">{{ offreParId.get(p.offreId)?.titre ?? `Offre #${p.offreId}` }}</td>
              <td>
                {{ p.plateforme }}
                <a v-if="p.urlPublication" class="g-secondaire" :href="p.urlPublication" target="_blank" rel="noopener">Ouvrir le lien</a>
              </td>
              <td>{{ formatDate(p.datePublication) }}</td>
              <td>{{ formatDate(p.dateExpiration) }}</td>
              <td><StatutBadge :statut="p.statut" /></td>
              <td>
                <div class="g-actions-cellule">
                  <button v-if="p.statut === 'BROUILLON' || p.statut === 'RETIREE'" class="g-btn g-btn--petit g-btn--primaire" type="button" @click="changerStatut(p, 'PUBLIEE')">Publier</button>
                  <button v-if="p.statut === 'PUBLIEE'" class="g-btn g-btn--petit" type="button" @click="changerStatut(p, 'RETIREE')">Retirer</button>
                  <RouterLink class="g-btn g-btn--petit" :to="`/si/app/rh/publications/${p.id}/modifier`">Modifier</RouterLink>
                  <button class="g-btn g-btn--petit g-btn--danger" type="button" @click="supprimer(p)">Supprimer</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination v-bind="pg.barre" @update:page="pg.aller" />
    </section>
  </div>
</template>
