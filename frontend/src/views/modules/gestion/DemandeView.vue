<script setup>
import Pagination from "@/components/ui/Pagination.vue";
import { usePagination } from "@/composable/usePagination";
import { ref, computed, onMounted } from "vue";
import { getBesoins } from "@/services/api";
import {
  normaliserBesoin,
} from "@/utils/demandeUtils";
import DemandeComponent from "@/components/demande/DemandeComponent.vue";
import DemandeSelectComponent from "@/components/demande/DemandeSelectComponent.vue";
import DemandeDetail from "@/components/demande/DemandeDetail.vue";

const besoins = ref([]);
const chargement = ref(false);
const erreur = ref("");
const recherche = ref("");
const filtreActif = ref("TOUS");
const filtrePriorite = ref("TOUTES");
const demandeSelectionneeId = ref(null);

const filtres = [
  { label: "Toutes", value: "TOUS" },
  { label: "En attente", value: "EN_ATTENTE" },
  { label: "Acceptées", value: "ACCEPTE" },
  { label: "Refusées", value: "REFUSE" },
  { label: "Annulées", value: "ANNULE" },
];

const priorites = [
  { label: "Toutes les priorités", value: "TOUTES" },
  { label: "Urgente", value: "URGENTE" },
  { label: "Haute", value: "HAUTE" },
  { label: "Normale", value: "NORMALE" },
  { label: "Basse", value: "BASSE" },
];

async function chargerBesoins() {
  chargement.value = true;
  erreur.value = "";

  try {
    const resultat = await getBesoins();
    // Accepte une liste directe ou une liste enveloppée dans { data: [...] }.
    const liste = Array.isArray(resultat)
      ? resultat
      : Array.isArray(resultat?.data)
        ? resultat.data
        : [];

    besoins.value = liste;

    if (
      demandeSelectionneeId.value !== null &&
      !besoins.value.some(
        (b) => String(b.id) === String(demandeSelectionneeId.value),
      )
    ) {
      demandeSelectionneeId.value = null;
    }

    if (demandeSelectionneeId.value === null && besoins.value.length > 0) {
      demandeSelectionneeId.value = besoins.value[0].id;
    }
  } catch (e) {
    console.error("Erreur de chargement des besoins :", e);

    if (e.response?.status === 401) {
      erreur.value = "Votre session a expiré. Veuillez vous reconnecter.";
    } else if (e.response?.status === 403) {
      erreur.value = "Vous n'avez pas les autorisations nécessaires.";
    } else {
      erreur.value =
        e.response?.data?.message ??
        "Impossible de récupérer les demandes. Vérifiez la connexion puis réessayez.";
    }
  } finally {
    chargement.value = false;
  }
}

const demandes = computed(() => besoins.value.map(normaliserBesoin));

const demandesFiltrees = computed(() => {
  const terme = recherche.value.trim().toLocaleLowerCase("fr");

  return demandes.value.filter((d) => {
    const contenu = [
      d.id,
      d.poste,
      d.departement,
      d.demandeur,
      d.motif,
      d.type,
      d.lieu,
      d.experience,
      d.niveauEtude,
    ]
      .filter(Boolean)
      .join(" ")
      .toLocaleLowerCase("fr");

    const correspondRecherche = !terme || contenu.includes(terme);
    const correspondStatut =
      filtreActif.value === "TOUS" || d.statut === filtreActif.value;
    const correspondPriorite =
      filtrePriorite.value === "TOUTES" || d.priorite === filtrePriorite.value;

    return correspondRecherche && correspondStatut && correspondPriorite;
  });
});

const demandeSelectionnee = computed(
  () =>
    demandes.value.find(
      (d) => String(d.id) === String(demandeSelectionneeId.value),
    ) ?? null,
);

const totalBesoins = computed(() => besoins.value.length);

const enAttente = computed(
  () => demandes.value.filter((d) => d.statut === "EN_ATTENTE").length,
);

const besoinsAcceptes = computed(
  () => demandes.value.filter((d) => d.statut === "ACCEPTE").length,
);

const besoinsRefuses = computed(
  () => demandes.value.filter((d) => d.statut === "REFUSE").length,
);

function selectionner(demande) {
  demandeSelectionneeId.value = demande.id;
}

onMounted(chargerBesoins);

const pg = usePagination(demandesFiltrees, 8);
</script>

<template>
  <main class="validation-page">
    <header class="page-header">
      <div>
        <span class="eyebrow">GESTION DES RESSOURCES HUMAINES</span>
        <h1>Demandes de recrutement</h1>
        <p>Consultez les demandes et leurs informations détaillées.</p>
      </div>
      <button
        type="button"
        class="approve-button refresh-button"
        :disabled="chargement"
        @click="chargerBesoins"
      >
        {{ chargement ? "Chargement..." : "Actualiser" }}
      </button>
    </header>

    <div v-if="erreur" class="feedback error-feedback" role="alert">
      <span>{{ erreur }}</span>
      <button type="button" @click="chargerBesoins">Réessayer</button>
    </div>

    <section class="summary-grid">
      <article class="summary-card">
        <span class="summary-label">Total des demandes</span>
        <strong>{{ totalBesoins }}</strong>
        <span class="summary-hint">Demandes enregistrées</span>
      </article>
      <article class="summary-card">
        <span class="summary-label">En attente</span>
        <strong>{{ enAttente }}</strong>
        <span class="summary-hint">Circuit de validation en cours</span>
      </article>
      <article class="summary-card">
        <span class="summary-label">Acceptées</span>
        <strong>{{ besoinsAcceptes }}</strong>
        <span class="summary-hint">Circuit terminé</span>
      </article>
    </section>

    <section class="workspace">
      <div class="request-panel">
        <header class="panel-heading">
          <div>
            <h2>Liste des demandes</h2>
            <p>{{ demandesFiltrees.length }} résultat(s)</p>
          </div>
        </header>

        <div class="search-box">
          <span aria-hidden="true">⌕</span>
          <input
            v-model="recherche"
            type="search"
            placeholder="Poste, département, demandeur..."
            aria-label="Rechercher une demande"
          />
        </div>

        <div class="filter-selects">
          <DemandeSelectComponent
            :title="'Statut'"
            v-model="filtreActif"
            :data="filtres"
          />
          <DemandeSelectComponent
            :title="'Priorité'"
            v-model="filtrePriorite"
            :data="priorites"
          />
        </div>

        <div v-if="chargement && besoins.length === 0" class="empty-list">
          <h3>Chargement des demandes...</h3>
          <p>Les informations sont en cours de récupération.</p>
        </div>

        <div v-else class="request-list">
          <DemandeComponent
            v-for="demande in pg.items"
            :key="demande.id"
            :demande="demande"
            :class="{ active: demandeSelectionnee?.id === demande.id }"
            @select="selectionner($event)"
          />

          <div v-if="demandesFiltrees.length === 0" class="empty-list">
            <span class="empty-symbol">✓</span>
            <h3>Aucune demande</h3>
            <p>
              {{
                besoins.length === 0
                  ? "Aucune demande n'a été renvoyée par le serveur."
                  : "Aucune demande ne correspond à ces critères."
              }}
            </p>
          </div>
        </div>
        <Pagination v-if="!chargement" v-bind="pg.barre" compact @update:page="pg.aller" />
      </div>

      <section class="detail-panel">
        <DemandeDetail v-if="demandeSelectionnee" :demande="demandeSelectionnee" :reload-besoin="chargerBesoins" />

        <div v-else class="empty-detail">
          <div class="empty-document">☷</div>
          <h2>Sélectionnez une demande</h2>
          <p>
            Choisissez une demande dans la liste pour consulter ses détails.
          </p>
        </div>
      </section>
    </section>
  </main>
</template>

<style scoped>
.validation-page {
  --ink: #292e2a;
  --muted: #818781;
  --line: #e9ece8;
  --surface: #ffffff;
  --canvas: #f7f8f6;
  --green-soft: #eef6ed;

  width: 100%;
  min-height: calc(100vh - 64px);
  padding: 0;
  margin: 0;
  box-sizing: border-box;
  background: var(--color-background);
  color: var(--ink);
}

/* En-tête */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 24px;
}

.eyebrow {
  color: var(--color-primary);
  font-size: 10px;
  font-weight: 750;
  letter-spacing: 1px;
}

.page-header h1 {
  margin: 8px 0;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.page-header p,
.panel-heading p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}

/* Cartes statistiques */
.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 22px;
}

.summary-card {
  display: flex;
  flex-direction: column;
  gap: 9px;
  min-width: 0;
  padding: 19px;
  border: 1px solid var(--line);
  border-radius: 11px;
  background: white;
}

.summary-label {
  color: #737a73;
  font-size: 12px;
}

.summary-card strong {
  font-size: 27px;
  font-weight: 700;
}

.summary-hint {
  color: #969c96;
  font-size: 11px;
}

/* Zone principale : liste + détails */
.workspace {
  display: grid;
  grid-template-columns: minmax(280px, 0.82fr) minmax(0, 1.8fr);
  align-items: stretch;
  gap: 18px;
  width: 100%;
  min-width: 0;
}

.request-panel,
.detail-panel {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--surface);
}

/* En-tête des panneaux */
.panel-heading {
  display: flex;
  justify-content: space-between;
  padding: 20px;
}

.panel-heading h2 {
  margin: 0 0 7px;
  font-size: 17px;
  font-weight: 700;
}

/* Recherche */
.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 14px 14px;
  padding: 0 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fafbf9;
}

.search-box > span {
  color: #818781;
  font-size: 22px;
}

.search-box input {
  width: 100%;
  min-width: 0;
  padding: 12px 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--ink);
  font: inherit;
  font-size: 12px;
}

/* Filtres (Selects) */
.filter-selects {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px;
  padding: 0 14px 14px;
  border-bottom: 1px solid #f0f1ef;
}

.filter-selects label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--muted);
  font-size: 11px;
}

.filter-selects select {
  width: 100%;
  min-width: 0;
  padding: 9px 8px;
  border: 1px solid var(--line);
  border-radius: 7px;
  background: #fff;
  color: var(--ink);
  font: inherit;
}

/* Liste des demandes */
.request-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
  max-height: 720px;
  overflow-y: auto;
  padding: 10px;
}

/* Messages et états vides */
.feedback {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
  padding: 13px 15px;
  border-radius: 8px;
  font-size: 13px;
}

.error-feedback {
  border: 1px solid #f0d7d3;
  background: #fff7f6;
  color: #a43b2e;
}

.feedback button {
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.empty-list,
.empty-detail {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 45px 22px;
  text-align: center;
}

.empty-list h3,
.empty-detail h2 {
  margin: 12px 0 7px;
  font-size: 15px;
}

.empty-list p,
.empty-detail p {
  max-width: 300px;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.8;
}

.empty-symbol,
.empty-document {
  display: grid;
  place-items: center;
  width: 50px;
  height: 50px;
  border-radius: 13px;
  background: var(--green-soft);
  color: var(--color-primary);
  font-size: 24px;
}

.refresh-button {
  padding: 11px 15px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: white;
}

.refresh-button:hover {
  background: #014b07;
}

.refresh-button:disabled {
  opacity: 0.6;
  cursor: wait;
}

/* Responsive */
@media (max-width: 1000px) {
  .workspace {
    grid-template-columns: minmax(240px, 0.8fr) minmax(0, 1.4fr);
  }
}

@media (max-width: 760px) {
  .workspace {
    grid-template-columns: minmax(0, 1fr);
  }

  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
  }

  .summary-card {
    padding: 13px;
  }

  .summary-card strong {
    font-size: 22px;
  }

  .page-header h1 {
    font-size: 22px;
  }
}

@media (max-width: 480px) {
  .summary-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .filter-selects {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>