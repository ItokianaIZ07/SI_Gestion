<!-- Brandon -->
<script setup>

import { ref, computed, onMounted } from "vue";
import {
  getCandidatsAEmbaucher,
  getContrats,
  embaucherCandidat,
  signerContrat,
} from "@/services/contratService";

const aEmbaucher = ref([]);
const contrats = ref([]);
const chargement = ref(false);
const erreur = ref("");
const message = ref("");
const selectionId = ref(null);
const signatureEnCours = ref(null);

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

async function charger() {
  chargement.value = true;
  erreur.value = "";
  try {
    const [c1, c2] = await Promise.all([getCandidatsAEmbaucher(), getContrats()]);
    aEmbaucher.value = Array.isArray(c1) ? c1 : [];
    contrats.value = Array.isArray(c2) ? c2 : [];
  } catch (e) {
    erreur.value = e.response?.data?.message ?? "Impossible de charger les contrats.";
  } finally {
    chargement.value = false;
  }
}

function selectionner(c) {
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
  if (!formulaire.value.idCandidature) return;
  erreur.value = "";
  message.value = "";
  try {
    await embaucherCandidat(formulaire.value);
    message.value = "Embauche effectuée : employé + contrat + onboarding créés.";
    selectionId.value = null;
    await charger();
  } catch (e) {
    erreur.value = e.response?.data?.message ?? "Échec de l'embauche.";
  }
}

async function signer(contrat, partie) {
  signatureEnCours.value = `${contrat.id}-${partie}`;
  erreur.value = "";
  message.value = "";

  try {
    const updated = await signerContrat(contrat.id, partie);

    // Mise à jour locale du contrat concerné (pas de rechargement global)
    const index = contrats.value.findIndex((c) => c.id === contrat.id);
    if (index !== -1 && updated) {
      contrats.value.splice(index, 1, updated);
    }

    if (updated && updated.statut === "ACTIF") {
      message.value = `✓ Contrat ${updated.reference} signé par les deux parties — il est maintenant ACTIF.`;
    } else {
      message.value = `Signature ${partie} enregistrée. En attente de l'autre partie.`;
    }
  } catch (e) {
    erreur.value = e.response?.data?.message ?? "Échec de la signature.";
  } finally {
    signatureEnCours.value = null;
  }
}

function formatDate(d) {
  if (!d) return "—";
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "short" }).format(new Date(d));
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

onMounted(charger);
</script>

<template>
  <main class="validation-page">
    <header class="page-header">
      <div>
        <span class="eyebrow">MODULE RH — CONTRATS</span>
        <h1>Embauche et contrats</h1>
        <p>Transition post-recrutement vers employé + contrat + onboarding.</p>
      </div>
      <button class="approve-button refresh-button" :disabled="chargement" @click="charger">
        {{ chargement ? "Chargement..." : "Actualiser" }}
      </button>
    </header>

    <div v-if="erreur" class="feedback error-feedback">
      <span>{{ erreur }}</span>
      <button @click="charger">Réessayer</button>
    </div>
    <div v-if="message" class="feedback success-feedback">
      <span>{{ message }}</span>
    </div>

    <section class="workspace">
      <div class="request-panel">
        <!-- Liste À EMBAUCHER -->
        <header class="panel-heading">
          <div>
            <h2>À embaucher</h2>
            <p>{{ aEmbaucher.length }} candidat(s) RETENU sans employé</p>
          </div>
        </header>

        <div class="request-list">
          <button
            v-for="c in aEmbaucher"
            :key="c.id"
            type="button"
            class="request-item"
            :class="{ active: selectionId === c.id }"
            @click="selectionner(c)"
          >
            <div class="request-item-top">
              <span class="request-id">#{{ c.id }}</span>
            </div>
            <h3>{{ c.candidatPrenom }} {{ c.candidatNom }}</h3>
            <p class="department">{{ c.poste }} — {{ c.departement }}</p>
          </button>

          <div v-if="!chargement && aEmbaucher.length === 0" class="empty-list">
            <h3>Aucun candidat à embaucher</h3>
          </div>
        </div>

        <!-- Liste CONTRATS -->
        <header class="panel-heading" style="margin-top: 16px">
          <div>
            <h2>Contrats</h2>
            <p>{{ contrats.length }} contrat(s)</p>
          </div>
        </header>

        <div class="request-list">
          <div
            v-for="ct in contrats"
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

            <!-- Indicateur de signatures -->
            <div class="signatures-info">
              <span class="signature-pill" :class="{ signee: ct.nbSignatures >= 1 }">
                {{ ct.nbSignatures >= 1 ? "✓" : "○" }} RH
              </span>
              <span class="signature-pill" :class="{ signee: ct.nbSignatures >= 2 }">
                {{ ct.nbSignatures >= 2 ? "✓" : "○" }} Candidat
              </span>
              <span class="signature-compteur">{{ ct.nbSignatures }}/2</span>
            </div>

            <!-- Boutons de signature -->
            <div
              v-if="ct.statut === 'EN_ATTENTE_SIGNATURE'"
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

            <!-- Message quand contrat ACTIF -->
            <div v-if="ct.statut === 'ACTIF'" class="contrat-actif-notice">
              ✓ Contrat signé par les deux parties
              <span v-if="ct.dateSignature">le {{ formatDate(ct.dateSignature) }}</span>
            </div>
          </div>

          <div v-if="!chargement && contrats.length === 0" class="empty-list">
            <h3>Aucun contrat en cours</h3>
          </div>
        </div>
      </div>

      <!-- Panneau de détail embauche -->
      <section class="detail-panel">
        <template v-if="candidatSelectionne">
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

            <div class="decision-actions">
              <button class="approve-button" @click="embaucher">
                Créer l'employé + le contrat
              </button>
            </div>

            <div class="read-only-notice">
              Cette action crée en une transaction : l'employé, le contrat (statut
              EN_ATTENTE_SIGNATURE), l'historique, l'onboarding et ses tâches.
            </div>
          </div>
        </template>

        <div v-else class="empty-detail">
          <div class="empty-document">☷</div>
          <h2>Sélectionnez un candidat</h2>
          <p>Choisissez un candidat RETENU pour créer son contrat.</p>
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

/* Zone principale */
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

/* Liste */
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
  transition: background 0.2s, border-color 0.2s;
}

button.request-item {
  cursor: pointer;
}

.request-item:hover {
  background: #fafbf9;
  border-color: #e8ebe6;
}

.request-item.active {
  border-color: #bfd8bc;
  background: #f4f8f2;
  box-shadow: inset 3px 0 var(--color-primary);
}

.request-item.actif {
  background: #f0f9ef;
  border-color: #b7dfb7;
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

/* Badges statut */
.priority {
  display: inline-block;
  padding: 5px 8px;
  border-radius: 5px;
  background: #f0f1ef;
  color: #6d746d;
  font-size: 10px;
  font-weight: 700;
}

.priority.en_attente_signature {
  background: #fff5e7;
  color: #98621b;
}

.priority.actif {
  background: #eaf5e9;
  color: #24752b;
}

.priority.brouillon {
  background: #f0f1ef;
  color: #697169;
}

.priority.termine,
.priority.resilie {
  background: #f0f1ef;
  color: #697169;
}

/* Indicateur de signatures */
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
}

/* Détail */
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

.detail-field strong {
  color: #363c36;
  font-size: 12px;
  font-weight: 600;
  overflow-wrap: anywhere;
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
}

.detail-field input:focus,
.detail-field select:focus,
.detail-field textarea:focus {
  outline: 2px solid #d4e6d0;
  border-color: #9bc296;
  background: #fff;
}

/* Boutons */
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

/* Feedback */
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

/* États vides */
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

  .detail-body,
  .detail-header {
    padding: 17px;
  }

  .page-header h1 {
    font-size: 22px;
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