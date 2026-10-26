<!-- Brandon -->
<script setup>
import Pagination from "@/components/ui/Pagination.vue";
import { usePagination } from "@/composable/usePagination";
import { ref, computed, onMounted, nextTick } from "vue";
import { useAuthStore } from "@/stores/authStore";
import { getContrats, signerContrat, getSignatureUrl } from "@/services/contratService";
import SignaturePad from "@/components/SignaturePad.vue";
import ContratDocument from "@/components/contrat/ContratDocument.vue";
import html2pdf from "html2pdf.js";

const authStore = useAuthStore();
const role = computed(() => authStore.user?.role?.toUpperCase());
const estRh = computed(() => role.value === "RH");

const contrats = ref([]);
const chargement = ref(false);
const erreur = ref("");
const message = ref("");
const signatureEnCours = ref(null);
const feedbackAnim = ref("");
const afficherSignature = ref(false);

const rechercheContrat = ref("");
const filtreStatutContrat = ref("TOUS");
const contratSelectionneId = ref(null);

/* ---------- AJOUT : affichage contrat + PDF ---------- */
const afficherContrat = ref(false);
const contratDocRef = ref(null);
const signatureRhUrl = ref("");
const signatureCandidatUrl = ref("");

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

const contratSelectionne = computed(() =>
  contrats.value.find((c) => c.id === contratSelectionneId.value) ?? null
);

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
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "long" }).format(new Date(d));
}

function formatDateCourt(d) {
  if (!d) return "—";
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "short" }).format(new Date(d));
}

function formatMontant(v) {
  if (v == null) return "—";
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency: "MGA",
    maximumFractionDigits: 0,
  }).format(v);
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

function selectionner(ct) {
  contratSelectionneId.value = ct.id;
  // Réinitialise les signatures chargées quand on change de contrat
  signatureRhUrl.value = "";
  signatureCandidatUrl.value = "";
}

/* Ouvre la modale de signature manuscrite */
function ouvrirSignature() {
  afficherSignature.value = true;
}

/* Reçoit l'image base64 de la signature et lance la sauvegarde */
function onSignatureValide(base64) {
  if (contratSelectionne.value) {
    signer(contratSelectionne.value, "RH", base64);
  }
}

async function signer(contrat, partie, signatureBase64 = null) {
  if (!estRh.value) return;
  signatureEnCours.value = `${contrat.id}-${partie}`;
  erreur.value = "";
  message.value = "";

  try {
    const updated = await signerContrat(contrat.id, partie, signatureBase64);
    const index = contrats.value.findIndex((c) => c.id === contrat.id);
    if (index !== -1 && updated) contrats.value.splice(index, 1, updated);

    // Recharge les signatures (celle qui vient d'être ajoutée)
    signatureRhUrl.value = "";
    signatureCandidatUrl.value = "";

    if (updated && updated.statut === "ACTIF") {
      afficherFeedback(`✓ Contrat ${updated.reference} signé par les deux parties — il est maintenant ACTIF.`);
    } else {
      afficherFeedback(`Signature ${partie} enregistrée. En attente de la signature du candidat.`);
    }
  } catch (e) {
    erreur.value = e.response?.data?.message ?? "Échec de la signature.";
  } finally {
    signatureEnCours.value = null;
    afficherSignature.value = false;
  }
}

/* ---------- AJOUT : voir le contrat ---------- */
async function ouvrirContrat() {
  const c = contratSelectionne.value;
  if (!c) return;

  // Charge les signatures en parallèle (vide si absentes)
  const [rh, cand] = await Promise.all([
    signatureRhUrl.value || getSignatureUrl(c.id, "RH"),
    signatureCandidatUrl.value || getSignatureUrl(c.id, "CANDIDAT"),
  ]);
  signatureRhUrl.value = rh;
  signatureCandidatUrl.value = cand;

  afficherContrat.value = true;
}

/* ---------- AJOUT : générer le PDF ---------- */
async function telechargerPDF() {
  const c = contratSelectionne.value;
  if (!c) return;

  // 1. Charge les signatures si nécessaire
  if (!signatureRhUrl.value) {
    signatureRhUrl.value = await getSignatureUrl(c.id, "RH");
  }
  if (!signatureCandidatUrl.value) {
    signatureCandidatUrl.value = await getSignatureUrl(c.id, "CANDIDAT");
  }

  // 2. Ouvre la modale pour que le DOM soit rendu
  afficherContrat.value = true;
  await nextTick();

  const el = contratDocRef.value?.$el;
  if (!el) return;

  html2pdf().set({
    margin: 10,
    filename: `contrat-${c.reference}.pdf`,
    image: { type: "jpeg", quality: 0.98 },
    html2canvas: { scale: 2, useCORS: true },
    jsPDF: { unit: "mm", format: "a4", orientation: "portrait" },
  }).from(el).save();
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
      <!-- ===== Colonne gauche : liste des contrats ===== -->
      <div class="request-panel">
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

        <div class="request-list">
          <transition-group name="list" tag="div">
            <button
              v-for="ct in pg.items"
              :key="ct.id"
              type="button"
              class="request-item"
              :class="{
                active: contratSelectionneId === ct.id,
                actif: ct.statut === 'ACTIF',
              }"
              @click="selectionner(ct)"
            >
              <div class="request-item-top">
                <span class="request-id">{{ ct.reference }}</span>
                <span class="priority" :class="String(ct.statut).toLowerCase()">
                  {{ libelleStatut(ct.statut) }}
                </span>
              </div>

              <h3>{{ ct.employeNom }} {{ ct.employePrenom }}</h3>
              <p class="department">{{ ct.typeContrat }} — {{ formatDateCourt(ct.dateDebut) }}</p>

              <div class="signatures-info">
                <span class="signature-pill" :class="{ signee: ct.nbSignatures >= 1 }">
                  {{ ct.nbSignatures >= 1 ? "✓" : "○" }} RH
                </span>
                <span class="signature-pill" :class="{ signee: ct.nbSignatures >= 2 }">
                  {{ ct.nbSignatures >= 2 ? "✓" : "○" }} Candidat
                </span>
                <span class="signature-compteur">{{ ct.nbSignatures }}/2</span>
              </div>
            </button>
          </transition-group>

          <div v-if="!chargement && contratsFiltres.length === 0" class="empty-list">
            <h3>Aucun contrat</h3>
            <p>Modifiez vos filtres ou la recherche.</p>
          </div>
        </div>
        <Pagination v-bind="pg.barre" compact @update:page="pg.aller" />
      </div>

      <!-- ===== Colonne droite : détail contrat ===== -->
      <section class="detail-panel">
        <transition name="fade" mode="out-in">
          <div
            v-if="contratSelectionne"
            :key="'detail-' + contratSelectionne.id"
          >
            <header class="detail-header">
              <div>
                <span class="eyebrow">CONTRAT {{ contratSelectionne.reference }}</span>
                <h2>{{ contratSelectionne.employeNom }} {{ contratSelectionne.employePrenom }}</h2>
                <p>{{ contratSelectionne.typeContrat }} — {{ formatDate(contratSelectionne.dateDebut) }}</p>
              </div>
              <span class="priority" :class="String(contratSelectionne.statut).toLowerCase()">
                {{ libelleStatut(contratSelectionne.statut) }}
              </span>
            </header>

            <div class="detail-body">
              <!-- Bandeau statut -->
              <div class="status-banner">
                <span>Statut actuel</span>
                <strong>{{ libelleStatut(contratSelectionne.statut) }}</strong>
              </div>

              <!-- Signatures -->
              <h3 class="section-title">Signatures</h3>
              <div class="signatures-block">
                <div class="signature-step" :class="{ done: contratSelectionne.nbSignatures >= 1 }">
                  <span class="signature-step__icon">
                    {{ contratSelectionne.nbSignatures >= 1 ? "✓" : "○" }}
                  </span>
                  <div>
                    <strong>Signature RH</strong>
                    <p>{{ contratSelectionne.nbSignatures >= 1 ? "Signée" : "En attente" }}</p>
                  </div>
                </div>
                <div class="signature-step" :class="{ done: contratSelectionne.nbSignatures >= 2 }">
                  <span class="signature-step__icon">
                    {{ contratSelectionne.nbSignatures >= 2 ? "✓" : "○" }}
                  </span>
                  <div>
                    <strong>Signature candidat</strong>
                    <p>{{ contratSelectionne.nbSignatures >= 2 ? "Signée" : "En attente" }}</p>
                  </div>
                </div>
              </div>

              <!-- Informations générales -->
              <h3 class="section-title">Informations du contrat</h3>
              <div class="detail-grid">
                <div class="detail-field">
                  <span>Référence</span>
                  <strong>{{ contratSelectionne.reference }}</strong>
                </div>
                <div class="detail-field">
                  <span>Type de contrat</span>
                  <strong>{{ contratSelectionne.typeContrat }}</strong>
                </div>
                <div class="detail-field">
                  <span>Date de début</span>
                  <strong>{{ formatDate(contratSelectionne.dateDebut) }}</strong>
                </div>
                <div class="detail-field">
                  <span>Date de fin</span>
                  <strong>{{ contratSelectionne.dateFin ? formatDate(contratSelectionne.dateFin) : "Indéterminée" }}</strong>
                </div>
                <div class="detail-field">
                  <span>Salaire mensuel</span>
                  <strong class="montant">{{ formatMontant(contratSelectionne.salaire) }}</strong>
                </div>
                <div class="detail-field">
                  <span>Date de signature</span>
                  <strong>{{ contratSelectionne.dateSignature ? formatDate(contratSelectionne.dateSignature) : "Pas encore signé" }}</strong>
                </div>
              </div>

              <!-- Actions RH -->
              <template v-if="estRh">
                <h3 class="section-title">Actions</h3>

                <!-- AJOUT : Voir le contrat + Générer en PDF -->
                <div class="decision-actions" style="margin-bottom: 12px">
                  <button type="button" class="approve-button" @click="ouvrirContrat">
                    Voir le contrat
                  </button>
                  <button type="button" class="reject-button" @click="telechargerPDF">
                    Générer en PDF
                  </button>
                </div>

                <!-- Signer RH (si pas encore signé) -->
                <div
                  v-if="contratSelectionne.statut === 'EN_ATTENTE_SIGNATURE' && contratSelectionne.nbSignatures < 1"
                  class="decision-actions"
                >
                  <button
                    class="approve-button"
                    :disabled="signatureEnCours === contratSelectionne.id + '-RH'"
                    @click="ouvrirSignature"
                  >
                    {{ signatureEnCours === contratSelectionne.id + "-RH" ? "..." : "Signer RH" }}
                  </button>
                </div>

                <!-- En attente candidat -->
                <div
                  v-else-if="contratSelectionne.statut === 'EN_ATTENTE_SIGNATURE' && contratSelectionne.nbSignatures === 1"
                  class="attente-candidat"
                >
                  <span class="attente-candidat__icon">⏳</span>
                  <div>
                    <strong>En attente de la signature du candidat</strong>
                    <p>Le candidat a reçu un mail avec un lien sécurisé pour signer son contrat.</p>
                  </div>
                </div>

                <!-- Contrat actif -->
                <div v-else-if="contratSelectionne.statut === 'ACTIF'" class="contrat-actif-notice">
                  ✓ Contrat signé par les deux parties
                  <span v-if="contratSelectionne.dateSignature">le {{ formatDate(contratSelectionne.dateSignature) }}</span>
                </div>
              </template>

              <div class="read-only-notice">
                Cette page permet de consulter et de signer les contrats en tant que RH.
                La signature du candidat se fait via un lien sécurisé reçu par email.
              </div>
            </div>
          </div>

          <div v-else class="empty-detail" key="empty">
            <div class="empty-document">☷</div>
            <h2>Sélectionnez un contrat</h2>
            <p>Choisissez un contrat dans la liste pour voir ses détails et le faire signer.</p>
          </div>
        </transition>
      </section>
    </section>

    <!-- ===== Modale de signature manuscrite ===== -->
    <SignaturePad
      v-if="afficherSignature"
      @validate="onSignatureValide"
      @cancel="afficherSignature = false"
    />

    <!-- ===== AJOUT : Modale "Voir le contrat" ===== -->
    <div v-if="afficherContrat && contratSelectionne" class="modal-contrat" @click.self="afficherContrat = false">
      <div class="modal-contrat__inner">
        <div class="modal-contrat__barre">
          <span>Aperçu du contrat — {{ contratSelectionne.reference }}</span>
          <button type="button" @click="afficherContrat = false">✕ Fermer</button>
        </div>
        <div class="modal-contrat__scroll">
          <ContratDocument
            ref="contratDocRef"
            :contrat="contratSelectionne"
            :signature-rh="signatureRhUrl"
            :signature-candidat="signatureCandidatUrl"
          />
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
/* ====== Styles existants (inchangés) ====== */
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
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, transform 0.15s;
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

.request-item.actif {
  background: #f0f9ef;
}

.request-item.actif.active {
  background: #e8f4e7;
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

.detail-body { padding: 22px; }

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

.status-banner span { color: var(--muted); }
.status-banner strong { color: var(--color-primary); }

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
  font-size: 13px;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.detail-field strong.montant {
  color: var(--color-primary);
  font-size: 16px;
  font-weight: 700;
}

.signatures-block {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.signature-step {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px dashed #e4e7e2;
  border-radius: 9px;
  background: #fafbf9;
  transition: background 0.25s, border-color 0.25s;
}

.signature-step.done {
  border-style: solid;
  border-color: #b7dfb7;
  background: #f0f9ef;
}

.signature-step__icon {
  display: grid;
  place-items: center;
  flex: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #eef1ed;
  color: #888;
  font-size: 15px;
  font-weight: 700;
}

.signature-step.done .signature-step__icon {
  background: #d5ecd3;
  color: #24752b;
}

.signature-step strong {
  display: block;
  font-size: 13px;
  color: #292e2a;
}

.signature-step p {
  margin: 2px 0 0;
  font-size: 11px;
  color: #818781;
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

.approve-button {
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: white;
}

.approve-button:hover:not(:disabled) {
  background: #014b07;
}

.approve-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Le bouton "Générer en PDF" réutilise .reject-button (existant dans Embauche) */
.reject-button {
  border: 1px solid #d4d4d4;
  background: #fff;
  color: #292e2a;
}

.reject-button:hover:not(:disabled) {
  background: #f7f7f7;
}

.attente-candidat {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-top: 16px;
  padding: 14px 16px;
  border: 1px dashed #e5c98f;
  border-radius: 9px;
  background: #fffaf0;
}

.attente-candidat__icon {
  font-size: 20px;
  line-height: 1;
}

.attente-candidat strong {
  display: block;
  color: #98621b;
  font-size: 13px;
}

.attente-candidat p {
  margin: 3px 0 0;
  color: #98621b;
  font-size: 11px;
  line-height: 1.5;
}

.contrat-actif-notice {
  margin-top: 16px;
  padding: 12px 16px;
  border-radius: 9px;
  background: #eaf5e9;
  color: #24752b;
  font-size: 13px;
  font-weight: 600;
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
.empty-detail h2 { margin: 12px 0 7px; font-size: 15px; }

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

.fade-enter-active, .fade-leave-active { transition: opacity 0.25s ease, transform 0.25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(6px); }

.feedback-enter-active, .feedback-leave-active { transition: opacity 0.3s ease, transform 0.3s ease; }
.feedback-enter-from, .feedback-leave-to { opacity: 0; transform: translateY(-8px); }

.list-enter-active, .list-leave-active { transition: all 0.3s ease; }
.list-enter-from, .list-leave-to { opacity: 0; transform: translateX(-10px); }
.list-move { transition: transform 0.3s ease; }

@media (max-width: 1000px) {
  .workspace { grid-template-columns: minmax(240px, 0.8fr) minmax(0, 1.4fr); }
}

@media (max-width: 760px) {
  .workspace { grid-template-columns: minmax(0, 1fr); }
  .detail-body, .detail-header { padding: 17px; }
  .page-header h1 { font-size: 22px; }
}

@media (max-width: 480px) {
  .detail-grid, .signatures-block { grid-template-columns: minmax(0, 1fr); }
  .decision-actions { flex-direction: column; }
}

/* ====== AJOUT : Modale contrat ====== */
.modal-contrat {
  position: fixed;
  inset: 0;
  z-index: 999;
  display: grid;
  place-items: center;
  background: rgba(0, 0, 0, 0.5);
  padding: 24px;
}

.modal-contrat__inner {
  background: #fff;
  border-radius: 10px;
  max-width: 900px;
  width: 100%;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.modal-contrat__barre {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 18px;
  border-bottom: 1px solid #e5e5e5;
  background: #fafafa;
  font-weight: 600;
  font-size: 14px;
}

.modal-contrat__barre button {
  border: 1px solid #d4d4d4;
  background: #fff;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  font: inherit;
}

.modal-contrat__barre button:hover {
  background: #f0f0f0;
}

.modal-contrat__scroll {
  overflow-y: auto;
  padding: 24px;
  background: #f4f5f3;
}
</style>