<!-- Brandon -->
<script setup>
import Pagination from "@/components/ui/Pagination.vue";
import { usePagination } from "@/composable/usePagination";
import { ref, computed, onMounted } from "vue";
import { useAuthStore } from "@/stores/authStore";
import { useEntretiens } from "@/composable/useEntretiens";

const authStore = useAuthStore();
const role = computed(() => authStore.user?.role?.toUpperCase());
const estRh = computed(() => role.value === "RH");

const {
  candidatures,
  intervieweurs,
  chargement,
  erreur,
  message,
  charger,
  sauvegarder,
  convoquer,
  notifierResultat,
} = useEntretiens();

const recherche = ref("");
const filtreStatut = ref("TOUS");
const filtreType = ref("TOUS");
const selectionId = ref(null);
const entretienEdite = ref(null);
const actionEnCours = ref(null);
const feedbackAnim = ref("");

const filtresStatut = [
  { label: "Tous les statuts", value: "TOUS" },
  { label: "Retenu", value: "RETENU" },
  { label: "Présélectionné", value: "PRESELECTIONNE" },
];

const filtresType = [
  { label: "Tous les types", value: "TOUS" },
  { label: "Téléphone", value: "TELEPHONE" },
  { label: "RH", value: "RH" },
  { label: "Technique", value: "TECHNIQUE" },
  { label: "Test technique", value: "TEST_TECHNIQUE" },
  { label: "Direction", value: "DIRECTION" },
];

const candidaturesFiltrees = computed(() => {
  const terme = recherche.value.trim().toLowerCase();
  return candidatures.value.filter((c) => {
    const contenu = [c.candidatNom, c.candidatPrenom, c.poste, c.departement]
      .filter(Boolean).join(" ").toLowerCase();
    const matchRecherche = !terme || contenu.includes(terme);
    const matchStatut = filtreStatut.value === "TOUS" || c.statut === filtreStatut.value;
    const matchType = filtreType.value === "TOUS"
      || (c.entretien && c.entretien.typeEntretien === filtreType.value);
    return matchRecherche && matchStatut && matchType;
  });
});

const candidatureSelectionnee = computed(() =>
  candidatures.value.find((c) => c.id === selectionId.value) ?? null
);

function selectionner(c) {
  if (!estRh.value) return;
  selectionId.value = c.id;
  if (c.entretien) {
    let date = c.entretien.dateEntretien || "";
    if (date.length > 19) date = date.slice(0, 19);
    entretienEdite.value = { ...c.entretien, dateEntretien: date };
  } else {
    entretienEdite.value = {
      id: null,
      idCandidature: c.id,
      typeEntretien: "RH",
      dateEntretien: "",
      lieu: "",
      idUtilisateurIntervieweur: null,
    };
  }
}

function afficherFeedback(texte, type = "success") {
  feedbackAnim.value = type;
  message.value = texte;
  setTimeout(() => {
    feedbackAnim.value = "";
  }, 4000);
}

async function enregistrer() {
  if (!estRh.value) return;
  if (!entretienEdite.value?.dateEntretien) {
    alert("Veuillez saisir une date d'entretien.");
    return;
  }

  actionEnCours.value = "save";
  let date = entretienEdite.value.dateEntretien;
  if (date.length === 16) date += ":00";

  const payload = { ...entretienEdite.value, dateEntretien: date };
  const resultat = await sauvegarder(payload);
  actionEnCours.value = null;

  if (resultat.success) {
    afficherFeedback(entretienEdite.value.id ? "Entretien mis à jour ✓" : "Entretien enregistré ✓");
    await charger();
    const c = candidatures.value.find((x) => x.id === selectionId.value);
    if (c) selectionner(c);
  } else {
    afficherFeedback(resultat.message, "error");
  }
}

async function convoquerEntretien() {
  if (!estRh.value) return;
  if (!entretienEdite.value?.id) {
    afficherFeedback("Enregistrez d'abord l'entretien.", "error");
    return;
  }
  actionEnCours.value = "convocation";
  const resultat = await convoquer(entretienEdite.value.id);
  actionEnCours.value = null;

  if (resultat.success) {
    afficherFeedback("Convocation envoyée ✓");
  } else {
    afficherFeedback(resultat.message, "error");
  }
}

async function envoyerResultat(decision) {
  if (!estRh.value || !candidatureSelectionnee.value) return;
  actionEnCours.value = decision;
  const resultat = await notifierResultat(candidatureSelectionnee.value.id, decision);
  actionEnCours.value = null;

  if (resultat.success) {
    afficherFeedback(
      decision === "RETENU" ? "Candidat notifié « Retenu » ✓" : "Candidat notifié « Rejeté » ✓"
    );
    await charger();
    const c = candidatures.value.find((x) => x.id === selectionId.value);
    if (c) selectionner(c);
  } else {
    afficherFeedback(resultat.message, "error");
  }
}

function formatDate(d) {
  if (!d) return "—";
  return new Intl.DateTimeFormat("fr-FR", {
    dateStyle: "short", timeStyle: "short",
  }).format(new Date(d));
}

function libelleStatut(statut) {
  return statut === "RETENU" ? "Retenu" : "Présélectionné";
}

onMounted(charger);

const pg = usePagination(candidaturesFiltrees, 8);
</script>

<template>
  <main class="validation-page">
    <header class="page-header">
      <div>
        <span class="eyebrow">MODULE RH — ENTRETIENS</span>
        <h1>Gestion des entretiens</h1>
        <p>Planifiez, convoquez et notifiez les résultats des candidats.</p>
      </div>
      <button class="approve-button refresh-button" :disabled="chargement" @click="charger">
        {{ chargement ? "Chargement..." : "Actualiser" }}
      </button>
    </header>

    <transition name="feedback">
      <div
        v-if="erreur || message"
        class="feedback"
        :class="erreur ? 'error-feedback' : 'success-feedback'"
      >
        <span>{{ erreur || message }}</span>
        <button v-if="erreur" @click="charger">Réessayer</button>
      </div>
    </transition>

    <section class="workspace">
      <div class="request-panel">
        <header class="panel-heading">
          <div>
            <h2>Candidatures</h2>
            <p>{{ candidaturesFiltrees.length }} résultat(s)</p>
          </div>
        </header>

        <div class="search-box">
          <span aria-hidden="true">⌕</span>
          <input v-model="recherche" type="search" placeholder="Nom, poste, département..." />
        </div>

        <div class="filter-selects">
          <label>
            <span>Statut</span>
            <select v-model="filtreStatut">
              <option v-for="f in filtresStatut" :key="f.value" :value="f.value">{{ f.label }}</option>
            </select>
          </label>
          <label>
            <span>Type d'entretien</span>
            <select v-model="filtreType">
              <option v-for="f in filtresType" :key="f.value" :value="f.value">{{ f.label }}</option>
            </select>
          </label>
        </div>

        <div class="request-list">
          <transition-group name="list" tag="div">
            <button
              v-for="c in pg.items"
              :key="c.id"
              type="button"
              class="request-item"
              :class="{ active: selectionId === c.id }"
              :disabled="!estRh"
              @click="selectionner(c)"
            >
              <div class="request-item-top">
                <span class="request-id">#{{ c.id }}</span>
                <span class="priority" :class="String(c.statut).toLowerCase()">
                  {{ libelleStatut(c.statut) }}
                </span>
              </div>
              <h3>{{ c.candidatPrenom }} {{ c.candidatNom }}</h3>
              <p class="department">{{ c.poste }} — {{ c.departement }}</p>
              <div class="request-item-bottom">
                <span>{{ c.entretien ? "Entretien planifié" : "Aucun entretien" }}</span>
                <span v-if="c.entretien">{{ formatDate(c.entretien.dateEntretien) }}</span>
              </div>
            </button>
          </transition-group>

          <div v-if="!chargement && candidaturesFiltrees.length === 0" class="empty-list">
            <h3>Aucune candidature</h3>
            <p>Modifiez vos filtres ou la recherche.</p>
          </div>
        </div>
        <Pagination v-bind="pg.barre" compact @update:page="pg.aller" />
      </div>

      <section class="detail-panel">
        <transition name="fade" mode="out-in">
          <div
            v-if="candidatureSelectionnee && entretienEdite"
            :key="'detail-' + candidatureSelectionnee.id"
          >
            <header class="detail-header">
              <div>
                <span class="eyebrow">CANDIDAT #{{ candidatureSelectionnee.id }}</span>
                <h2>{{ candidatureSelectionnee.candidatPrenom }} {{ candidatureSelectionnee.candidatNom }}</h2>
                <p>{{ candidatureSelectionnee.poste }} — {{ candidatureSelectionnee.departement }}</p>
              </div>
              <span class="priority" :class="String(candidatureSelectionnee.statut).toLowerCase()">
                {{ libelleStatut(candidatureSelectionnee.statut) }}
              </span>
            </header>

            <div class="detail-body">
              <h3 class="section-title">Planifier l'entretien</h3>
              <div class="detail-grid">
                <div class="detail-field">
                  <span>Type</span>
                  <select v-model="entretienEdite.typeEntretien">
                    <option value="TELEPHONE">Téléphone</option>
                    <option value="RH">RH</option>
                    <option value="TECHNIQUE">Technique</option>
                    <option value="TEST_TECHNIQUE">Test technique</option>
                    <option value="DIRECTION">Direction</option>
                  </select>
                </div>
                <div class="detail-field">
                  <span>Date et heure</span>
                  <input type="datetime-local" v-model="entretienEdite.dateEntretien" step="1" />
                </div>
                <div class="detail-field">
                  <span>Lieu</span>
                  <input type="text" v-model="entretienEdite.lieu" placeholder="Salle, visio..." />
                </div>
                <div class="detail-field">
                  <span>Intervieweur</span>
                  <select v-model="entretienEdite.idUtilisateurIntervieweur">
                    <option :value="null">—</option>
                    <option v-for="u in intervieweurs" :key="u.id" :value="u.id">
                      {{ u.prenom }} {{ u.nom }} ({{ u.role }})
                    </option>
                  </select>
                </div>
              </div>

              <!-- Boutons planification : RH uniquement -->
              <div v-if="estRh" class="decision-actions">
                <button
                  class="approve-button"
                  :disabled="actionEnCours === 'save'"
                  @click="enregistrer"
                >
                  {{ actionEnCours === "save" ? "..." : (entretienEdite.id ? "Mettre à jour" : "Enregistrer") }}
                </button>
                <button
                  class="reject-button"
                  :disabled="actionEnCours === 'convocation'"
                  @click="convoquerEntretien"
                >
                  {{ actionEnCours === "convocation" ? "..." : "Envoyer la convocation" }}
                </button>
              </div>

              <!-- Résultat : RH uniquement -->
              <template v-if="estRh">
                <h3 class="section-title">Résultat</h3>
                <div class="decision-actions">
                  <button
                    class="approve-button"
                    :disabled="actionEnCours === 'RETENU'"
                    @click="envoyerResultat('RETENU')"
                  >
                    {{ actionEnCours === "RETENU" ? "..." : "Notifier « Retenu »" }}
                  </button>
                  <button
                    class="reject-button"
                    :disabled="actionEnCours === 'REJETE'"
                    @click="envoyerResultat('REJETE')"
                  >
                    {{ actionEnCours === "REJETE" ? "..." : "Notifier « Rejeté »" }}
                  </button>
                </div>
              </template>

              <div v-if="candidatureSelectionnee.entretien" class="read-only-notice">
                Entretien actuel : {{ formatDate(candidatureSelectionnee.entretien.dateEntretien) }}
                — {{ candidatureSelectionnee.entretien.typeEntretien }}
              </div>
            </div>
          </div>

          <div v-else class="empty-detail" key="empty">
            <div class="empty-document">☷</div>
            <h2>Sélectionnez un candidat</h2>
            <p>Choisissez une candidature pour planifier ou modifier son entretien.</p>
          </div>
        </transition>
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
  --green-soft: #eef6ed;
  width: 100%;
  min-height: calc(100vh - 64px);
  padding: 0;
  margin: 0;
  box-sizing: border-box;
  background: var(--color-background);
  color: var(--ink);
}

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

.search-box {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 14px 14px;
  padding: 0 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fafbf9;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.search-box:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(1, 95, 9, 0.08);
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
  transition: border-color 0.2s;
}

.filter-selects select:focus {
  outline: none;
  border-color: var(--color-primary);
}

.request-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
  max-height: 720px;
  overflow-y: auto;
  padding: 10px;
}

.request-item {
  width: 100%;
  padding: 14px;
  border: 1px solid transparent;
  border-radius: 9px;
  background: white;
  text-align: left;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, transform 0.15s;
}

.request-item:disabled {
  cursor: default;
}

.request-item:not(:disabled):hover {
  background: #fafbf9;
  border-color: #e8ebe6;
  transform: translateX(2px);
}

.request-item.active {
  border-color: #bfd8bc;
  background: #f4f8f2;
  box-shadow: inset 3px 0 var(--color-primary);
}

.request-item-top,
.request-item-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
}

.request-id,
.request-item-bottom,
.department {
  color: var(--muted);
  font-size: 11px;
}

.request-item h3 {
  margin: 12px 0 5px;
  color: var(--ink);
  font-size: 14px;
}

.department {
  margin: 0 0 13px;
}

.request-item-bottom {
  padding-bottom: 12px;
  border-bottom: 1px solid #eff0ed;
}

.priority {
  display: inline-block;
  padding: 5px 8px;
  border-radius: 5px;
  background: #f0f1ef;
  color: #6d746d;
  font-size: 10px;
  font-weight: 700;
}

.priority.retenu {
  background: #eaf5e9;
  color: #24752b;
}

.priority.preselectionne {
  background: #eef2ff;
  color: #3b4fbf;
}

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

.detail-field input,
.detail-field select,
.detail-field textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 9px 11px;
  border: 1px solid #e4e7e2;
  border-radius: 8px;
  background: #fcfcfb;
  color: var(--ink);
  font: inherit;
  font-size: 12px;
  transition: border-color 0.2s, background 0.2s;
}

.detail-field input:focus,
.detail-field select:focus,
.detail-field textarea:focus {
  outline: 2px solid #d4e6d0;
  border-color: #9bc296;
  background: #fff;
}

.decision-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.decision-actions button {
  flex: 1;
  min-width: 140px;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  transition: transform 0.1s, box-shadow 0.2s, opacity 0.2s;
}

.decision-actions button:active:not(:disabled) {
  transform: scale(0.97);
}

.reject-button {
  border: 1px solid #f0d7d3;
  background: #fff7f6;
  color: #b74736;
}

.reject-button:hover:not(:disabled) {
  background: #ffebe8;
}

.approve-button {
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: white;
}

.approve-button:hover:not(:disabled) {
  background: #014b07;
}

.approve-button:disabled,
.reject-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.refresh-button {
  padding: 11px 15px;
  border-radius: 8px;
  cursor: pointer;
  flex: 0 0 auto;
  min-width: 0;
}

.feedback {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  text-decoration: underline;
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

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

.feedback-enter-active,
.feedback-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.feedback-enter-from,
.feedback-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.list-enter-active,
.list-leave-active {
  transition: all 0.3s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}

.list-move {
  transition: transform 0.3s ease;
}

@media (max-width: 1000px) {
  .workspace {
    grid-template-columns: minmax(240px, 0.8fr) minmax(0, 1.4fr);
  }
}

@media (max-width: 760px) {
  .workspace {
    grid-template-columns: minmax(0, 1fr);
  }
  .detail-body,
  .detail-header {
    padding: 17px;
  }
  .page-header h1 {
    font-size: 22px;
  }
  .filter-selects {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 480px) {
  .detail-grid {
    grid-template-columns: minmax(0, 1fr);
  }
  .decision-actions {
    flex-direction: column;
  }
}
</style>