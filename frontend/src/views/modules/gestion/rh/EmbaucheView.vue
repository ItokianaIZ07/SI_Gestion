<!-- Brandon -->
<script setup>
import Pagination from "@/components/ui/Pagination.vue";
import { usePagination } from "@/composable/usePagination";
import { ref, computed, onMounted } from "vue";
import { useAuthStore } from "@/stores/authStore";
import {
  getCandidatsAEmbaucher,
  embaucherCandidat,
} from "@/services/contratService";
import { useRouter } from "vue-router";

const authStore = useAuthStore();
const role = computed(() => authStore.user?.role?.toUpperCase());
const estRh = computed(() => role.value === "RH");

const router = useRouter();

const aEmbaucher = ref([]);
const chargement = ref(false);
const erreur = ref("");
const message = ref("");
const selectionId = ref(null);
const actionEnCours = ref(null);
const feedbackAnim = ref("");

const formulaire = ref({
  idCandidature: null,
  salaire: 0,
  typeContrat: "CDI",
  dateDebut: new Date().toISOString().slice(0, 10),
  periodeEssaiJours: 90,
});

const candidatSelectionne = computed(() =>
  aEmbaucher.value.find((c) => c.id === selectionId.value) ?? null
);

function afficherFeedback(texte, type = "success") {
  feedbackAnim.value = type;
  message.value = texte;
  setTimeout(() => {
    feedbackAnim.value = "";
  }, 4000);
}

async function charger() {
  chargement.value = true;
  erreur.value = "";
  try {
    const c1 = await getCandidatsAEmbaucher();
    aEmbaucher.value = Array.isArray(c1) ? c1 : [];
  } catch (e) {
    erreur.value = e.response?.data?.message ?? "Impossible de charger les candidats.";
  } finally {
    chargement.value = false;
  }
}

function selectionner(c) {
  if (!estRh.value) return;
  selectionId.value = c.id;
  formulaire.value = {
    idCandidature: c.id,
    salaire: c.salaireEstime ?? 0,
    typeContrat: c.typeContratSouhaite ?? "CDI",
    dateDebut: new Date().toISOString().slice(0, 10),
    periodeEssaiJours: 90,
  };
}

async function embaucher() {
  if (!estRh.value || !formulaire.value.idCandidature) return;
  erreur.value = "";
  message.value = "";
  actionEnCours.value = "embauche";

  try {
    await embaucherCandidat(formulaire.value);
    afficherFeedback("Embauche effectuée : employé + contrat + onboarding créés ✓");
    selectionId.value = null;
    await charger();
  } catch (e) {
    erreur.value = e.response?.data?.message ?? "Échec de l'embauche.";
  } finally {
    actionEnCours.value = null;
  }
}

onMounted(charger);

const pg = usePagination(aEmbaucher, 8);
</script>

<template>
  <main class="validation-page">
    <header class="page-header">
      <div>
        <span class="eyebrow">MODULE RH — EMBAUCHE</span>
        <h1>Embaucher un candidat</h1>
        <p>Créez l'employé, le contrat et l'onboarding en une seule action.</p>
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
      <!-- Liste des candidats -->
      <div class="request-panel">
        <header class="panel-heading">
          <div>
            <h2>À embaucher</h2>
            <p>{{ aEmbaucher.length }} candidat(s) RETENU sans employé</p>
          </div>
        </header>

        <div class="request-list" style="max-height: 720px">
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
              </div>
              <h3>{{ c.candidatPrenom }} {{ c.candidatNom }}</h3>
              <p class="department">{{ c.poste }} — {{ c.departement }}</p>
            </button>
          </transition-group>

          <div v-if="!chargement && aEmbaucher.length === 0" class="empty-list">
            <h3>Aucun candidat à embaucher</h3>
            <p>Tous les candidats RETENU ont déjà un employé et un contrat.</p>
          </div>
        </div>
        <Pagination v-bind="pg.barre" compact @update:page="pg.aller" />
      </div>

      <!-- Formulaire -->
      <section class="detail-panel">
        <transition name="fade" mode="out-in">
          <div
            v-if="candidatSelectionne"
            :key="'detail-' + candidatSelectionne.id"
          >
            <header class="detail-header">
              <div>
                <span class="eyebrow">EMBAUCHE #{{ candidatSelectionne.id }}</span>
                <h2>{{ candidatSelectionne.candidatPrenom }} {{ candidatSelectionne.candidatNom }}</h2>
                <p>{{ candidatSelectionne.poste }} — {{ candidatSelectionne.departement }}</p>
              </div>
            </header>

            <div class="detail-body">
              <h3 class="section-title">Paramètres du contrat</h3>
              <div class="detail-grid">
                <div class="detail-field">
                  <span>Type de contrat</span>
                  <select v-model="formulaire.typeContrat">
                    <option value="CDI">CDI</option>
                    <option value="CDD">CDD</option>
                    <option value="STAGE">Stage</option>
                  </select>
                </div>
                <div class="detail-field">
                  <span>Date de début</span>
                  <input type="date" v-model="formulaire.dateDebut" />
                </div>
                <div class="detail-field">
                  <span>Salaire (MGA)</span>
                  <input type="number" v-model.number="formulaire.salaire" min="0" step="1000" />
                </div>
                <div class="detail-field">
                  <span>Période d'essai (jours)</span>
                  <input type="number" v-model.number="formulaire.periodeEssaiJours" min="0" />
                </div>
              </div>

              <div v-if="estRh" class="decision-actions">
                <button
                  class="approve-button"
                  :disabled="actionEnCours === 'embauche'"
                  @click="embaucher"
                >
                  {{ actionEnCours === "embauche" ? "..." : "Créer l'employé + le contrat" }}
                </button>
                <button
                  class="reject-button"
                  type="button"
                  @click="router.push('/si/app/rh/contrats')"
                >
                  Voir les contrats
                </button>
              </div>

              <div class="read-only-notice">
                Cette action crée en une transaction : l'employé, le contrat (statut
                EN_ATTENTE_SIGNATURE), l'historique, l'onboarding et ses tâches.
                Le contrat apparaîtra ensuite dans la page <strong>Contrats</strong>
                pour la signature des deux parties.
              </div>
            </div>
          </div>

          <div v-else class="empty-detail" key="empty">
            <div class="empty-document">☷</div>
            <h2>Sélectionnez un candidat</h2>
            <p>Choisissez un candidat RETENU pour créer son contrat.</p>
          </div>
        </transition>
      </section>
    </section>
  </main>
</template>

<style scoped>
/* ===== Reprend les mêmes styles que ContratView ===== */
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

.request-list {
  display: flex;
  flex-direction: column;
  gap: 7px;
  max-height: 420px;
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
  transition: background 0.2s, border-color 0.2s, transform 0.15s;
}

button.request-item {
  cursor: pointer;
}

button.request-item:disabled {
  opacity: 0.7;
  cursor: default;
}

.request-item:hover {
  background: #fafbf9;
  border-color: #e8ebe6;
  transform: translateX(2px);
}

.request-item.active {
  border-color: #bfd8bc;
  background: #f4f8f2;
  box-shadow: inset 3px 0 var(--color-primary);
}

.request-item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
}

.request-id,
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
  transition: transform 0.1s, background 0.2s, opacity 0.2s;
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
  .workspace { grid-template-columns: minmax(240px, 0.8fr) minmax(0, 1.4fr); }
}
@media (max-width: 760px) {
  .workspace { grid-template-columns: minmax(0, 1fr); }
  .detail-body, .detail-header { padding: 17px; }
  .page-header h1 { font-size: 22px; }
}
@media (max-width: 480px) {
  .detail-grid { grid-template-columns: minmax(0, 1fr); }
  .decision-actions { flex-direction: column; }
}
</style>