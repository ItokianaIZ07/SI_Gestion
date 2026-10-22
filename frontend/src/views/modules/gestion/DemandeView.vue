<script setup>
import Pagination from "@/components/ui/Pagination.vue";
import { usePagination } from "@/composable/usePagination";
import { ref, computed, onMounted } from "vue";
import { getBesoins } from "@/services/api";
import {
  prioriteLabel,
  formatDate,
  statutLabel,
  normaliserBesoin,
} from "@/utils/demandeUtils";
import DemandeComponent from "@/components/DemandeComponent.vue";
import DemandeSelectComponent from "@/components/DemandeSelectComponent.vue";

const besoins = ref([]);
const chargement = ref(false);
const erreur = ref("");
const recherche = ref("");
const filtreActif = ref("TOUS");
const filtrePriorite = ref("TOUTES");
const demandeSelectionneeId = ref(null);

const filtres = [
  { label: "Toutes", value: "TOUS" },
  { label: "En attente RH", value: "EN_ATTENTE_RH" },
  { label: "En attente Finance", value: "EN_ATTENTE_FINANCE" },
  { label: "En attente Direction", value: "EN_ATTENTE_DIRECTION" },
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
  () =>
    demandes.value.filter((d) =>
      ["EN_ATTENTE_RH", "EN_ATTENTE_FINANCE", "EN_ATTENTE_DIRECTION"].includes(
        d.statut,
      ),
    ).length,
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

function formatSalaire(montant) {
  if (montant === null || montant === undefined || montant === "") {
    return "Non estimé";
  }

  const valeur = Number(montant);
  if (!Number.isFinite(valeur)) return "Non estimé";

  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency: "MGA",
    maximumFractionDigits: 0,
  }).format(valeur);
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
            :title="Statut"
            v-model="filtreActif"
            :data="filtres"
          />
          <DemandeSelectComponent
            :title="Priorité"
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
        <template v-if="demandeSelectionnee">
          <header class="detail-header">
            <div>
              <span class="eyebrow">DEMANDE #{{ demandeSelectionnee.id }}</span>
              <h2>{{ demandeSelectionnee.poste }}</h2>
              <p>{{ demandeSelectionnee.departement }}</p>
            </div>
            <span
              class="priority"
              :class="String(demandeSelectionnee.priorite).toLowerCase()"
            >
              {{ prioriteLabel(demandeSelectionnee.priorite) }}
            </span>
          </header>

          <div class="detail-body">
            <div class="status-banner">
              <span>Statut actuel</span>
              <strong>{{ statutLabel(demandeSelectionnee.statut) }}</strong>
            </div>

            <h3 class="section-title">Informations générales</h3>
            <div class="detail-grid">
              <div class="detail-field">
                <span>Demandeur</span>
                <strong>{{ demandeSelectionnee.demandeur }}</strong>
              </div>
              <div class="detail-field">
                <span>Type de besoin</span>
                <strong>{{ demandeSelectionnee.type }}</strong>
              </div>
              <div class="detail-field">
                <span>Nombre de postes</span>
                <strong>{{ demandeSelectionnee.effectif }}</strong>
              </div>
              <div class="detail-field">
                <span>Lieu de travail</span>
                <strong>{{ demandeSelectionnee.lieu }}</strong>
              </div>
              <div class="detail-field">
                <span>Date de demande</span>
                <strong>{{
                  formatDate(demandeSelectionnee.dateDemande)
                }}</strong>
              </div>
              <div class="detail-field">
                <span>Arrivée souhaitée</span>
                <strong>{{
                  formatDate(demandeSelectionnee.dateArrivee)
                }}</strong>
              </div>
            </div>

            <h3 class="section-title">Motif de la demande</h3>
            <p class="motif">{{ demandeSelectionnee.motif }}</p>

            <h3 class="section-title">Profil recherché</h3>
            <div class="detail-grid">
              <div class="detail-field">
                <span>Expérience</span>
                <strong>{{ demandeSelectionnee.experience }}</strong>
              </div>
              <div class="detail-field">
                <span>Niveau d'étude</span>
                <strong>{{ demandeSelectionnee.niveauEtude }}</strong>
              </div>
            </div>

            <div class="detail-field full-field">
              <span>Compétences requises</span>
              <strong>{{ demandeSelectionnee.competences }}</strong>
            </div>

            <div class="salary-box">
              <span>Salaire mensuel estimé</span>
              <strong>{{ formatSalaire(demandeSelectionnee.salaire) }}</strong>
            </div>

            <div class="read-only-notice">
              Cette page permet actuellement de consulter les demandes. Les
              actions de validation et de refus seront reliées aux routes
              backend correspondantes lorsqu'elles seront définies.
            </div>
          </div>
        </template>

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
.panel-heading p,
.detail-header p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}

.role-selector {
  display: flex;
  flex-direction: column;
  gap: 7px;
  color: var(--muted);
  font-size: 12px;
}

.role-selector select {
  min-width: 210px;
  padding: 11px 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: white;
  color: var(--ink);
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

.panel-heading h2,
.detail-header h2 {
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

/* Filtres */
.filter-tabs {
  display: flex;
  gap: 5px;
  overflow-x: auto;
  padding: 0 14px 14px;
  border-bottom: 1px solid #f0f1ef;
}

.filter-tabs button {
  flex-shrink: 0;
  padding: 8px 10px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  color: #7c827c;
  cursor: pointer;
  font-size: 11px;
}

.filter-tabs button.selected {
  border-color: #dce9d9;
  background: var(--green-soft);
  color: var(--color-primary);
  font-weight: 700;
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

/* Détails */
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 14px;
  padding: 22px;
  border-bottom: 1px solid var(--line);
}

.detail-header h2 {
  margin-top: 9px;
  font-size: 21px;
}

.detail-body {
  padding: 22px;
}

.status-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 13px 15px;
  border: 1px solid #e7ece4;
  border-radius: 8px;
  background: #fafbf9;
  font-size: 12px;
}

.status-banner span {
  color: var(--muted);
}

.status-banner strong {
  color: var(--color-primary);
}

.section-title {
  margin: 25px 0 14px;
  font-size: 13px;
  font-weight: 750;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 20px;
}

.detail-field {
  display: flex;
  flex-direction: column;
  gap: 7px;
  min-width: 0;
}

.detail-field span {
  color: var(--muted);
  font-size: 11px;
}

.detail-field strong {
  color: #363c36;
  font-size: 12px;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.motif {
  margin: 0;
  padding: 14px;
  border: 1px solid #eff0ed;
  border-radius: 8px;
  background: #fafbf9;
  color: #656c65;
  font-size: 12px;
  line-height: 1.8;
}

.full-field {
  margin-top: 19px;
}

/* Salaire */
.salary-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 22px;
  padding: 16px;
  border: 1px solid #e1ebdd;
  border-radius: 9px;
  background: #f6f9f4;
}

.salary-box span {
  color: #727a70;
  font-size: 12px;
}

.salary-box strong {
  color: var(--color-primary);
  font-size: 19px;
}

/* Décision */
.remark-label {
  display: block;
  margin-bottom: 8px;
  color: #747b74;
  font-size: 12px;
}

.detail-body textarea {
  display: block;
  width: 100%;
  box-sizing: border-box;
  resize: vertical;
  padding: 12px;
  border: 1px solid #e4e7e2;
  border-radius: 8px;
  background: #fcfcfb;
  color: var(--ink);
  font: inherit;
  font-size: 12px;
}

.detail-body textarea:focus,
.role-selector select:focus {
  outline: 2px solid #d4e6d0;
  border-color: #9bc296;
}

.decision-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.decision-actions button {
  flex: 1;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
}

.reject-button {
  border: 1px solid #f0d7d3;
  background: #fff7f6;
  color: #b74736;
}

.reject-button:hover {
  background: #ffebe8;
}

.approve-button {
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: white;
}

.approve-button:hover {
  background: #014b07;
}

.decision-hint {
  margin: 12px 0 0;
  color: #969c96;
  font-size: 11px;
  line-height: 1.6;
}

.error-message {
  color: var(--color-danger);
  font-size: 12px;
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

.success-feedback {
  border: 1px solid #d6ead4;
  background: #f0f8ee;
  color: #246d2a;
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

.read-only-notice {
  margin-top: 20px;
  padding: 13px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fafbf9;
  color: #737a73;
  font-size: 12px;
  line-height: 1.7;
}

/* Tablette */
@media (max-width: 1000px) {
  .workspace {
    grid-template-columns: minmax(240px, 0.8fr) minmax(0, 1.4fr);
  }
}

/* Mobile */
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

  .detail-body,
  .detail-header {
    padding: 17px;
  }

  .page-header h1 {
    font-size: 22px;
  }
}

@media (max-width: 480px) {
  .summary-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .detail-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .decision-actions {
    flex-direction: column;
  }
}

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
.refresh-button {
  padding: 11px 15px;
  border-radius: 8px;
  cursor: pointer;
}
.refresh-button:disabled {
  opacity: 0.6;
  cursor: wait;
}
.error-feedback {
  border: 1px solid #f0d7d3;
  background: #fff7f6;
  color: #a43b2e;
}
@media (max-width: 480px) {
  .filter-selects {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
