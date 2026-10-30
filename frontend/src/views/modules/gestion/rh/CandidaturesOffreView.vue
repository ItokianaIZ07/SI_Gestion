<script setup>
// Candidatures reçues pour une offre : /si/app/rh/offres/:offreId/candidatures
// Présélectionner / Rejeter uniquement pour les candidatures EN_ATTENTE ; sinon on affiche le statut.
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import "@/assets/gestion.css";
import Pagination from "@/components/ui/Pagination.vue";
import { usePagination } from "@/composable/usePagination";
import PageEntete from "@/components/gestion/PageEntete.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import CandidatItem from "@/components/CandidatItem.vue";
import { offresApi } from "@/services/ressources";
import { candidaturesRhApi } from "@/services/candidaturesRh";
import { STATUTS_CANDIDATURE } from "@/config/libelles";

const route = useRoute();
let offreId = Number(route.params.offreId);

const offre = ref(null);
const lignes = ref([]);
const chargement = ref(true);
const erreur = ref("");

const recherche = ref("");
const filtreStatut = ref("");
const ordre = ref("desc");

async function charger() {
  chargement.value = true;
  erreur.value = "";
  try {
    offre.value = offre.value ?? (await offresApi.parId(offreId));
    if (!offre.value?.recrutementId) throw new Error("Cette offre n'est associée à aucun recrutement.");
    lignes.value = (await candidaturesRhApi.liste(offre.value.recrutementId)) ?? [];
  } catch (e) {
    erreur.value = e.message ?? "Impossible de charger les candidatures.";
  } finally {
    chargement.value = false;
  }
}

const compter = (statut) => lignes.value.filter((l) => l.statut === statut).length;
const synthese = computed(() => [
  { cle: "", libelle: "Total", nombre: lignes.value.length },
  { cle: "EN_ATTENTE", libelle: "En attente", nombre: compter("EN_ATTENTE") },
  { cle: "PRESELECTIONNE", libelle: "Présélectionnées", nombre: compter("PRESELECTIONNE") },
  { cle: "RETENU", libelle: "Retenues", nombre: compter("RETENU") },
  { cle: "REJETE", libelle: "Rejetées", nombre: compter("REJETE") },
]);

const date = (l) => new Date(l.dateCandidature ?? l.dateDepot ?? 0).getTime();
const filtrees = computed(() => {
  const q = recherche.value.trim().toLowerCase();
  return lignes.value
    .filter((l) => {
      if (filtreStatut.value && l.statut !== filtreStatut.value) return false;
      return !q || [l.prenom, l.nom, l.email, l.telephone].join(" ").toLowerCase().includes(q);
    })
    .sort((a, b) => (ordre.value === "asc" ? date(a) - date(b) : date(b) - date(a)));
});
const pg = usePagination(filtrees, 10);

function majStatut({ candidatureId, statut, remarque }) {
  erreur.value = "";
  lignes.value = lignes.value.map((l) => (l.candidatureId === candidatureId ? { ...l, statut, remarque } : l));
}

// Déjà traitée ailleurs (409) : on recharge pour afficher son vrai statut.
async function surErreur({ message, status }) {
  if (status === 409 || status === 404) await charger();
  erreur.value = message;
}

onMounted(charger);

// Même composant réutilisé avec une autre offre dans l'adresse : on recharge.
watch(
  () => route.params.offreId,
  (nouveau) => {
    if (nouveau === undefined) return;
    offreId = Number(nouveau);
    offre.value = null;
    recherche.value = "";
    filtreStatut.value = "";
    charger();
  },
);
</script>

<template>
  <div class="g-page">
    <PageEntete
      :titre="offre ? `Candidatures — ${offre.titre}` : 'Candidatures'"
      sous-titre="Lisez les CV reçus pour cette offre, puis présélectionnez ou rejetez les candidatures en attente."
    >
      <RouterLink class="g-btn" to="/si/app/rh/offres">← Retour aux offres</RouterLink>
    </PageEntete>

    <MessageAlerte :message="erreur" />

    <ul class="synthese" aria-label="Répartition des candidatures">
      <li v-for="s in synthese" :key="s.libelle">
        <button type="button" :class="{ actif: filtreStatut === s.cle }" @click="filtreStatut = s.cle">
          <strong>{{ s.nombre }}</strong><span>{{ s.libelle }}</span>
        </button>
      </li>
    </ul>

    <section class="g-carte">
      <div class="g-filtres">
        <input v-model="recherche" class="g-input g-input--recherche" type="search" placeholder="Rechercher un nom, un e-mail, un téléphone…" aria-label="Rechercher un candidat" />
        <select v-model="filtreStatut" class="g-input" aria-label="Filtrer par statut">
          <option value="">Tous les statuts</option>
          <option v-for="o in STATUTS_CANDIDATURE" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
        <button type="button" class="g-btn" @click="ordre = ordre === 'asc' ? 'desc' : 'asc'">
          {{ ordre === "asc" ? "↑ Plus ancien" : "↓ Plus récent" }}
        </button>
        <span class="g-compteur">{{ filtrees.length }} candidature(s)</span>
      </div>

      <p v-if="chargement" class="g-vide">Chargement des candidatures…</p>
      <div v-else-if="!filtrees.length" class="g-vide">
        <strong>{{ lignes.length ? "Aucun résultat" : "Aucune candidature" }}</strong>
        {{ lignes.length ? "Modifiez vos filtres." : "Aucun candidat n'a encore postulé à cette offre." }}
      </div>

      <div v-else class="g-table-wrap">
        <table class="g-table">
          <thead>
            <tr>
              <th>Candidat</th>
              <th>Expérience</th>
              <th>Reçue le</th>
              <th>Décision</th>
              <th class="g-droite">CV</th>
            </tr>
          </thead>
          <tbody>
            <CandidatItem
              v-for="l in pg.items"
              :key="l.candidatureId"
              :ligne="l"
              @statut-change="majStatut"
              @erreur="surErreur"
            />
          </tbody>
        </table>
      </div>

      <Pagination v-bind="pg.barre" @update:page="pg.aller" />
    </section>
  </div>
</template>

<style scoped>
.synthese { display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 12px; margin: 0; padding: 0; list-style: none; }
.synthese button {
  display: flex; flex-direction: column; align-items: flex-start; gap: 2px; width: 100%;
  padding: 14px 16px; text-align: left; font: inherit; cursor: pointer;
  background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm);
  transition: border-color .15s, background-color .15s;
}
.synthese button:hover { border-color: var(--color-primary-line); background: var(--color-primary-soft); }
.synthese button.actif { border-color: var(--color-primary); background: var(--color-primary-soft); }
.synthese strong { font-size: 1.5rem; line-height: 1.1; color: var(--color-ink); }
.synthese span { font-size: 12.5px; color: var(--color-muted); }
</style>
