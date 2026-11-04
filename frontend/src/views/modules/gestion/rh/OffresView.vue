<script setup>
import Pagination from "@/components/ui/Pagination.vue";
import { usePagination } from "@/composable/usePagination";
import { ref, computed, onMounted } from "vue";
import { offresApi, recrutementsApi, publicationsApi } from "@/services/ressources";
import { STATUTS_OFFRE, formatDate, formatSalaire } from "@/config/libelles";
import PageEntete from "@/components/gestion/PageEntete.vue";
import StatutBadge from "@/components/gestion/StatutBadge.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import "@/assets/gestion.css";

const offres = ref([]);
const recrutements = ref([]);
const publications = ref([]);
const chargement = ref(true);
const erreur = ref("");
const succes = ref("");

const recherche = ref("");
const filtreStatut = ref("");

async function charger() {
  chargement.value = true;
  erreur.value = "";
  try {
    const [o, r, p] = await Promise.all([offresApi.liste(), recrutementsApi.liste(), publicationsApi.liste()]);
    offres.value = o ?? [];
    recrutements.value = r ?? [];
    publications.value = p ?? [];
  } catch (e) {
    erreur.value = e.message;
  } finally {
    chargement.value = false;
  }
}

const refParRecrutement = computed(() => new Map(recrutements.value.map((r) => [r.id, r.reference])));
const nbPublications = computed(() => {
  const m = new Map();
  for (const p of publications.value) {
    if (p.statut === "PUBLIEE") m.set(p.offreId, (m.get(p.offreId) ?? 0) + 1);
  }
  return m;
});

// Recrutements qui n'ont pas encore d'offre
const sansOffre = computed(() => {
  const avecOffre = new Set(offres.value.map((o) => o.recrutementId));
  return recrutements.value.filter((r) => !avecOffre.has(r.id)).length;
});

const lignes = computed(() => {
  const q = recherche.value.trim().toLowerCase();
  return offres.value
    .filter((o) => {
      if (filtreStatut.value && o.statut !== filtreStatut.value) return false;
      return !q || [o.titre, o.competencesRequises, o.niveauEtude].join(" ").toLowerCase().includes(q);
    })
    .sort((a, b) => (b.dateCreation ?? "").localeCompare(a.dateCreation ?? ""));
});

async function supprimer(o) {
  if (!window.confirm(`Supprimer l'offre « ${o.titre} » ? Cette action est définitive.`)) return;
  erreur.value = "";
  succes.value = "";
  try {
    await offresApi.supprimer(o.id);
    succes.value = "Offre supprimée.";
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
    <PageEntete titre="Offres d'emploi" sous-titre="Rédigez, validez puis publiez les offres liées à vos recrutements.">
      <RouterLink class="g-btn g-btn--primaire" to="/si/app/rh/offres/creation">Nouvelle offre</RouterLink>
    </PageEntete>

    <MessageAlerte :message="erreur" />
    <MessageAlerte :message="succes" type="succes" />

    <section class="g-carte">
      <div class="g-filtres">
        <input v-model="recherche" class="g-input g-input--recherche" type="search" placeholder="Rechercher un titre, une compétence…" />
        <select v-model="filtreStatut" class="g-input" aria-label="Filtrer par statut">
          <option value="">Tous les statuts</option>
          <option v-for="o in STATUTS_OFFRE" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
        <span class="g-compteur">
          {{ lignes.length }} offre(s)
          <template v-if="sansOffre"> · {{ sansOffre }} recrutement(s) sans offre</template>
        </span>
      </div>

      <p v-if="chargement" class="g-vide">Chargement…</p>
      <div v-else-if="!lignes.length" class="g-vide">
        <strong>Aucune offre d'emploi</strong>
        Créez une offre à partir d'un recrutement.
      </div>

      <div v-else class="g-table-wrap">
        <table class="g-table">
          <thead>
            <tr>
              <th>Offre</th>
              <th>Recrutement</th>
              <th>Niveau / Expérience</th>
              <th>Salaire</th>
              <th>Créée le</th>
              <th>Statut</th>
              <th class="g-droite">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="o in pg.items" :key="o.id">
              <td>
                <RouterLink class="g-principal" :to="`/si/app/rh/offres/${o.id}/candidatures`" title="Voir les candidatures reçues">{{ o.titre }}</RouterLink>
                <span class="g-secondaire">{{ nbPublications.get(o.id) ?? 0 }} publication(s) active(s)</span>
              </td>
              <td>{{ refParRecrutement.get(o.recrutementId) ?? `#${o.recrutementId}` }}</td>
              <td>
                {{ o.niveauEtude || "—" }}
                <span class="g-secondaire">{{ o.experienceMinAnnees }} an(s) min.</span>
              </td>
              <td>{{ formatSalaire(o.salaireMin, o.salaireMax) }}</td>
              <td>{{ formatDate(o.dateCreation) }}</td>
              <td><StatutBadge :statut="o.statut" /></td>
              <td>
                <div class="g-actions-cellule">
                  <RouterLink v-if="o.statut === 'VALIDEE'" class="g-btn g-btn--petit g-btn--primaire" :to="{ path: '/si/app/rh/publications/creation', query: { offreId: o.id } }">
                    Publier
                  </RouterLink>
                  <RouterLink class="g-btn g-btn--petit" :to="`/si/app/rh/offres/${o.id}/candidatures`">Candidatures</RouterLink>
                  <RouterLink class="g-btn g-btn--petit" :to="`/si/app/rh/offres/${o.id}/modifier`">Modifier</RouterLink>
                  <button class="g-btn g-btn--petit g-btn--danger" type="button" @click="supprimer(o)">Supprimer</button>
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
