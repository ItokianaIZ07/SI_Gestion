<!-- Brandon -->
<script setup>
import Pagination from "@/components/ui/Pagination.vue";
import { usePagination } from "@/composable/usePagination";
import { ref, computed, onMounted } from "vue";
import { useAuthStore } from "@/stores/authStore";
import { getContrats, signerContrat } from "@/services/contratService";

const authStore = useAuthStore();
const role = computed(() => authStore.user?.role?.toUpperCase());
const estRh = computed(() => role.value === "RH");

const contrats = ref([]);
const chargement = ref(false);
const erreur = ref("");
const message = ref("");
const signatureEnCours = ref(null);
const feedbackAnim = ref("");

const rechercheContrat = ref("");
const filtreStatutContrat = ref("TOUS");

const contratsFiltres = computed(() => {
  const terme = rechercheContrat.value.trim().toLowerCase();
  return contrats.value.filter((c) => {
    const contenu = [c.reference, c.employeNom, c.employePrenom, c.typeContrat]
      .filter(Boolean).join(" ").toLowerCase();
    const matchRech = !terme || contenu.includes(terme);
    const matchStatut = filtreStatutContrat.value === "TOUS"
      || c.statut === filtreStatutContrat.value;
    return matchRech && matchStatut;
  });
});

function afficherFeedback(texte, type = "success") {
  feedbackAnim.value = type;
  message.value = texte;
  setTimeout(() => { feedbackAnim.value = ""; }, 4000);
}

function libelleStatut(code) {
  const labels = {
    BROUILLON: "Brouillon",
    EN_ATTENTE_SIGNATURE: "En attente signature",
    ACTIF: "Actif",
    TERMINE: "Terminé",
    RESILIE: "Résilié",
  };
  return labels[code] ?? code;
}

function formatDate(d) {
  if (!d) return "—";
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "short" }).format(new Date(d));
}

async function charger() {
  chargement.value = true;
  erreur.value = "";
  try {
    const c2 = await getContrats();
    contrats.value = Array.isArray(c2) ? c2 : [];
  } catch (e) {
    erreur.value = e.response?.data?.message ?? "Impossible de charger les contrats.";
  } finally {
    chargement.value = false;
  }
}

async function signer(contrat, partie) {
  if (!estRh.value) return;
  signatureEnCours.value = `${contrat.id}-${partie}`;
  erreur.value = "";
  message.value = "";

  try {
    const updated = await signerContrat(contrat.id, partie);
    const index = contrats.value.findIndex((c) => c.id === contrat.id);
    if (index !== -1 && updated) contrats.value.splice(index, 1, updated);

    if (updated && updated.statut === "ACTIF") {
      afficherFeedback(`✓ Contrat ${updated.reference} signé par les deux parties — il est maintenant ACTIF.`);
    } else {
      afficherFeedback(`Signature ${partie} enregistrée. En attente de l'autre partie.`);
    }
  } catch (e) {
    erreur.value = e.response?.data?.message ?? "Échec de la signature.";
  } finally {
    signatureEnCours.value = null;
  }
}

onMounted(charger);

const pg = usePagination(contratsFiltres, 8);
</script>

<template>
  <main class="validation-page">
    <header class="page-header">
      <div>
        <span class="eyebrow">MODULE RH — CONTRATS</span>
        <h1>Contrats</h1>
        <p>Consultez et faites signer les contrats créés lors des embauches.</p>
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
      <div class="request-panel" style="grid-column: 1 / -1">
        <header class="panel-heading">
          <div>
            <h2>Liste des contrats</h2>
            <p>{{ contratsFiltres.length }} / {{ contrats.length }} contrat(s)</p>
          </div>
        </header>

        <div class="search-box">
          <span aria-hidden="true">⌕</span>
          <input v-model="rechercheContrat" type="search" placeholder="Référence, employé, type..." />
        </div>

        <div class="filter-selects" style="grid-template-columns: 1fr">
          <label>
            <span>Statut</span>
            <select v-model="filtreStatutContrat">
              <option value="TOUS">Tous les statuts</option>
              <option value="BROUILLON">Brouillon</option>
              <option value="EN_ATTENTE_SIGNATURE">En attente signature</option>
              <option value="ACTIF">Actif</option>
              <option value="TERMINE">Terminé</option>
              <option value="RESILIE">Résilié</option>
            </select>
          </label>
        </div>

        <div class="request-list" style="max-height: 780px">
          <transition-group name="list" tag="div">
            <div
              v-for="ct in pg.items"
              :key="ct.id"
              class="request-item"
              :class="{ actif: ct.statut === 'ACTIF' }"
            >
              <div class="request-item-top">
                <span class="request-id">{{ ct.reference }}</span>
                <span class="priority" :class="String(ct.statut).toLowerCase()">
                  {{ libelleStatut(ct.statut) }}
                </span>
              </div>

              <h3>{{ ct.employeNom }} {{ ct.employePrenom }}</h3>
              <p class="department">{{ ct.typeContrat }} — {{ formatDate(ct.dateDebut) }}</p>

              <div class="signatures-info">
                <span class="signature-pill" :class="{ signee: ct.nbSignatures >= 1 }">
                  {{ ct.nbSignatures >= 1 ? "✓" : "○" }} RH
                </span>
                <span class="signature-pill" :class="{ signee: ct.nbSignatures >= 2 }">
                  {{ ct.nbSignatures >= 2 ? "✓" : "○" }} Candidat
                </span>
                <span class="signature-compteur">{{ ct.nbSignatures }}/2</span>
              </div>

              <div
                v-if="ct.statut === 'EN_ATTENTE_SIGNATURE' && estRh"
                class="decision-actions"
                style="margin-top: 8px"
              >
                <button
                  v-if="ct.nbSignatures < 1"
                  class="approve-button"
                  :disabled="signatureEnCours === ct.id + '-RH'"
                  @click.stop="signer(ct, 'RH')"
                >
                  {{ signatureEnCours === ct.id + "-RH" ? "..." : "Signer RH" }}
                </button>
                <button
                  v-if="ct.nbSignatures >= 1 && ct.nbSignatures < 2"
                  class="reject-button"
                  :disabled="signatureEnCours === ct.id + '-CANDIDAT'"
                  @click.stop="signer(ct, 'CANDIDAT')"
                >
                  {{ signatureEnCours === ct.id + "-CANDIDAT" ? "..." : "Signer candidat" }}
                </button>
              </div>

              <div v-if="ct.statut === 'ACTIF'" class="contrat-actif-notice">
                ✓ Contrat signé par les deux parties
                <span v-if="ct.dateSignature">le {{ formatDate(ct.dateSignature) }}</span>
              </div>
            </div>
          </transition-group>

          <div v-if="!chargement && contratsFiltres.length === 0" class="empty-list">
            <h3>Aucun contrat</h3>
            <p>Modifiez vos filtres ou la recherche.</p>
          </div>
        </div>
        <Pagination v-bind="pg.barre" @update:page="pg.aller" />
      </div>
    </section>
  </main>
</template>

<style scoped>
/* ===== Reprend les mêmes styles que ContratView actuel ===== */
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
.panel-heading p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}

.workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 18px;
  width: 100%;
  min-width: 0;
}

.request-panel {
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

.panel-heading h2 {
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

.search-box > span { color: #818781; font-size: 22px; }

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
  transition: background 0.2s, border-color 0.2s;
}

.request-item:hover {
  background: #fafbf9;
  border-color: #e8ebe6;
}

.request-item.actif {
  background: #f0f9ef;
  border-color: #b7dfb7;
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

.department { margin: 0 0 13px; }

.priority {
  display: inline-block;
  padding: 5px 8px;
  border-radius: 5px;
  background: #f0f1ef;
  color: #6d746d;
  font-size: 10px;
  font-weight: 700;
}

.priority.en_attente_signature { background: #fff5e7; color: #98621b; }
.priority.actif { background: #eaf5e9; color: #24752b; }
.priority.brouillon { background: #f0f1ef; color: #697169; }
.priority.termine, .priority.resilie { background: #f0f1ef; color: #697169; }

.signatures-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.signature-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 6px;
  border: 1px solid #e5e7e3;
  background: #f7f8f6;
  color: #888;
  font-size: 11px;
  font-weight: 600;
  transition: background 0.25s, border-color 0.25s, color 0.25s;
}

.signature-pill.signee {
  background: #eaf5e9;
  border-color: #b7dfb7;
  color: #24752b;
}

.signature-compteur {
  margin-left: auto;
  font-size: 11px;
  color: #666;
  font-weight: 600;
}

.contrat-actif-notice {
  margin-top: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #eaf5e9;
  color: #24752b;
  font-size: 12px;
  font-weight: 600;
  animation: slideIn 0.35s ease;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
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

.decision-actions button:active:not(:disabled) { transform: scale(0.97); }

.reject-button { border: 1px solid #f0d7d3; background: #fff7f6; color: #b74736; }
.reject-button:hover:not(:disabled) { background: #ffebe8; }
.approve-button { border: 1px solid var(--color-primary); background: var(--color-primary); color: white; }
.approve-button:hover:not(:disabled) { background: #014b07; }
.approve-button:disabled, .reject-button:disabled { opacity: 0.6; cursor: not-allowed; }

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

.success-feedback { border: 1px solid #d6ead4; background: #f0f8ee; color: #246d2a; }
.error-feedback { border: 1px solid #f0d7d3; background: #fff7f6; color: #a43b2e; }
.feedback button { border: 0; background: transparent; color: inherit; cursor: pointer; text-decoration: underline; }

.empty-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 45px 22px;
  text-align: center;
}

.empty-list h3 { margin: 12px 0 7px; font-size: 15px; }
.empty-list p { max-width: 300px; color: var(--muted); font-size: 12px; line-height: 1.8; }

.feedback-enter-active, .feedback-leave-active { transition: opacity 0.3s ease, transform 0.3s ease; }
.feedback-enter-from, .feedback-leave-to { opacity: 0; transform: translateY(-8px); }
.list-enter-active, .list-leave-active { transition: all 0.3s ease; }
.list-enter-from, .list-leave-to { opacity: 0; transform: translateX(-10px); }
.list-move { transition: transform 0.3s ease; }

@media (max-width: 760px) {
  .page-header h1 { font-size: 22px; }
}
</style>