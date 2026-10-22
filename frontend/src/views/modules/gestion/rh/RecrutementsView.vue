<script setup>
import { ref, computed, onMounted } from "vue";
import { recrutementsApi, offresApi } from "@/services/ressources";
import { STATUTS_RECRUTEMENT, formatDate } from "@/config/libelles";
import PageEntete from "@/components/gestion/PageEntete.vue";
import StatutBadge from "@/components/gestion/StatutBadge.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import "@/assets/gestion.css";

const recrutements = ref([]);
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
    const [r, o] = await Promise.all([recrutementsApi.liste(), offresApi.liste()]);
    recrutements.value = r ?? [];
    offres.value = o ?? [];
  } catch (e) {
    erreur.value = e.message;
  } finally {
    chargement.value = false;
  }
}

const offreParRecrutement = computed(() => new Map(offres.value.map((o) => [o.recrutementId, o])));

const lignes = computed(() => {
  const q = recherche.value.trim().toLowerCase();
  return recrutements.value
    .filter((r) => {
      if (filtreStatut.value && r.statut !== filtreStatut.value) return false;
      return !q || [r.reference, r.titre, r.description].join(" ").toLowerCase().includes(q);
    })
    .sort((a, b) => (b.dateCreation ?? "").localeCompare(a.dateCreation ?? ""));
});

async function supprimer(r) {
  if (!window.confirm(`Supprimer le recrutement « ${r.reference} » ? Cette action est définitive.`)) return;
  erreur.value = "";
  succes.value = "";
  try {
    await recrutementsApi.supprimer(r.id);
    succes.value = `Recrutement ${r.reference} supprimé.`;
    await charger();
  } catch (e) {
    erreur.value = e.message;
  }
}

onMounted(charger);
</script>

<template>
  <div class="g-page">
    <PageEntete titre="Recrutements" sous-titre="Suivez chaque recrutement ouvert, de sa création jusqu'à la publication de son offre.">
      <RouterLink class="g-btn g-btn--primaire" to="/si/app/rh/recrutements/creation">Nouveau recrutement</RouterLink>
    </PageEntete>

    <MessageAlerte :message="erreur" />
    <MessageAlerte :message="succes" type="succes" />

    <section class="g-carte">
      <div class="g-filtres">
        <input v-model="recherche" class="g-input g-input--recherche" type="search" placeholder="Rechercher par référence ou titre…" />
        <select v-model="filtreStatut" class="g-input" aria-label="Filtrer par statut">
          <option value="">Tous les statuts</option>
          <option v-for="o in STATUTS_RECRUTEMENT" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
        <span class="g-compteur">{{ lignes.length }} recrutement(s)</span>
      </div>

      <p v-if="chargement" class="g-vide">Chargement…</p>
      <div v-else-if="!lignes.length" class="g-vide">
        <strong>Aucun recrutement</strong>
        Partez d'un besoin accepté pour en créer un.
        <p><RouterLink class="g-btn g-btn--primaire" to="/si/app/rh/demandes">Voir les besoins</RouterLink></p>
      </div>

      <div v-else class="g-table-wrap">
        <table class="g-table">
          <thead>
            <tr>
              <th>Référence</th>
              <th>Intitulé</th>
              <th>Ouverture</th>
              <th>Clôture</th>
              <th>Statut</th>
              <th>Offre d'emploi</th>
              <th class="g-droite">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in lignes" :key="r.id">
              <td class="g-principal">{{ r.reference }}</td>
              <td>{{ r.titre }}</td>
              <td>{{ formatDate(r.dateOuverture) }}</td>
              <td>{{ formatDate(r.dateCloture) }}</td>
              <td><StatutBadge :statut="r.statut" /></td>
              <td>
                <StatutBadge v-if="offreParRecrutement.get(r.id)" :statut="offreParRecrutement.get(r.id).statut" />
                <RouterLink v-else class="g-btn g-btn--petit" :to="{ path: '/si/app/rh/offres/creation', query: { recrutementId: r.id } }">
                  Créer l'offre
                </RouterLink>
              </td>
              <td>
                <div class="g-actions-cellule">
                  <RouterLink class="g-btn g-btn--petit" :to="`/si/app/rh/recrutements/${r.id}/modifier`">Modifier</RouterLink>
                  <button class="g-btn g-btn--petit g-btn--danger" type="button" @click="supprimer(r)">Supprimer</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
