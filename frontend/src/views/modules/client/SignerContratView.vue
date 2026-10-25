<!-- BY : Brandon -->
<script setup>
import { ref, onMounted, nextTick } from "vue";
import { useRoute } from "vue-router";
import {
  getContratParToken,
  signerContratParToken,
  getSignatureUrlPublique,
} from "@/services/contratService";
import SignaturePad from "@/components/SignaturePad.vue";
import ContratDocument from "@/components/contrat/ContratDocument.vue";
import html2pdf from "html2pdf.js";

const route = useRoute();
const token = route.params.token;

const contrat = ref(null);
const chargement = ref(true);
const erreur = ref("");
const signe = ref(false);
const enCours = ref(false);
const afficherSignature = ref(false);

/* ---------- Affichage contrat + PDF ---------- */
const afficherContrat = ref(false);
const contratDocRef = ref(null);
const signatureRhUrl = ref("");
const signatureCandidatUrl = ref("");

function formatDate(d) {
  if (!d) return "—";
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "long" }).format(new Date(d));
}

function formatMontant(v) {
  if (v == null) return "—";
  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency: "MGA",
    maximumFractionDigits: 0,
  }).format(v);
}

/* Charge les deux signatures (RH + candidat) en parallèle, tolérant aux absences */
async function chargerSignatures() {
  const [rh, cand] = await Promise.all([
    getSignatureUrlPublique(token, "RH"),
    getSignatureUrlPublique(token, "CANDIDAT"),
  ]);
  signatureRhUrl.value = rh || "";
  signatureCandidatUrl.value = cand || "";
}

async function charger() {
  chargement.value = true;
  erreur.value = "";
  try {
    contrat.value = await getContratParToken(token);
    await chargerSignatures();
  } catch (e) {
    erreur.value = e.response?.data?.message ?? "Ce lien est invalide ou a expiré.";
  } finally {
    chargement.value = false;
  }
}

function ouvrirSignature() {
  afficherSignature.value = true;
}

async function onSignatureValide(base64) {
  enCours.value = true;
  erreur.value = "";
  try {
    const updated = await signerContratParToken(token, base64);

    // ← FIX : met à jour le contrat avec la version retournée par le backend
    //          (contient la dateSignature fraîchement remplie)
    if (updated) {
      contrat.value = { ...contrat.value, ...updated };
    }

    signe.value = true;

    // Recharge les signatures pour que le PDF contienne la nouvelle signature
    await chargerSignatures();
  } catch (e) {
    erreur.value = e.response?.data?.message ?? "Impossible de signer ce contrat.";
  } finally {
    enCours.value = false;
    afficherSignature.value = false;
  }
}

function ouvrirContrat() {
  afficherContrat.value = true;
}

async function telechargerPDF() {
  if (!contrat.value) return;

  // S'assure que les deux signatures sont chargées
  if (!signatureRhUrl.value || !signatureCandidatUrl.value) {
    await chargerSignatures();
  }

  afficherContrat.value = true;
  await nextTick();

  const el = contratDocRef.value?.$el;
  if (!el) return;

  html2pdf()
    .set({
      margin: 10,
      filename: `contrat-${contrat.value.reference}.pdf`,
      image: { type: "jpeg", quality: 0.98 },
      html2canvas: { scale: 2, useCORS: true },
      jsPDF: { unit: "mm", format: "a4", orientation: "portrait" },
    })
    .from(el)
    .save();
}

onMounted(charger);
</script>

<template>
  <main class="signature-page">
    <div class="signature-card">
      <header class="signature-header">
        <span class="eyebrow">SIGNATURE ÉLECTRONIQUE</span>
        <h1>Contrat de travail</h1>
      </header>

      <p v-if="chargement" class="state loading">Chargement du contrat…</p>

      <!-- Erreur : uniquement avant signature -->
      <div v-else-if="erreur && !signe" class="state error">
        <p>{{ erreur }}</p>
        <button type="button" @click="charger">Réessayer</button>
      </div>

      <!-- Contrat signé : message + boutons conservés -->
      <div v-else-if="signe" class="state success">
        <div class="checkmark">✓</div>
        <h2>Contrat signé avec succès</h2>
        <p>Merci. Le service RH a été notifié et prendra contact avec vous.</p>

        <div class="actions-secondaires" style="margin-top: 20px">
          <button type="button" class="btn-secondaire" @click="ouvrirContrat">
            Voir le contrat
          </button>
          <button type="button" class="btn-secondaire" @click="telechargerPDF">
            Télécharger en PDF
          </button>
        </div>
      </div>

      <!-- Contrat à signer -->
      <template v-else-if="contrat">
        <div class="contrat-resume">
          <h2>Contrat {{ contrat.reference }}</h2>
          <dl>
            <div><dt>Type de contrat</dt><dd>{{ contrat.typeContrat }}</dd></div>
            <div><dt>Date de début</dt><dd>{{ formatDate(contrat.dateDebut) }}</dd></div>
            <div v-if="contrat.dateFin"><dt>Date de fin</dt><dd>{{ formatDate(contrat.dateFin) }}</dd></div>
            <div><dt>Salaire mensuel</dt><dd>{{ formatMontant(contrat.salaire) }}</dd></div>
          </dl>
        </div>

        <p class="hint">
          En cliquant sur « Signer », vous acceptez les termes du contrat ci-dessus.
          Cette action est définitive.
        </p>

        <div class="actions-secondaires">
          <button type="button" class="btn-secondaire" @click="ouvrirContrat">
            Voir le contrat
          </button>
          <button type="button" class="btn-secondaire" @click="telechargerPDF">
            Télécharger en PDF
          </button>
        </div>

        <button class="btn-signer" type="button" :disabled="enCours" @click="ouvrirSignature">
          {{ enCours ? "Signature en cours…" : "Signer le contrat" }}
        </button>
      </template>
    </div>

    <!-- Modale de signature manuscrite -->
    <SignaturePad
      v-if="afficherSignature"
      @validate="onSignatureValide"
      @cancel="afficherSignature = false"
    />

    <!-- Modale "Voir le contrat" -->
    <div v-if="afficherContrat && contrat" class="modal-contrat" @click.self="afficherContrat = false">
      <div class="modal-contrat__inner">
        <div class="modal-contrat__barre">
          <span>Contrat {{ contrat.reference }}</span>
          <button type="button" @click="afficherContrat = false">✕ Fermer</button>
        </div>
        <div class="modal-contrat__scroll">
          <ContratDocument
            ref="contratDocRef"
            :contrat="contrat"
            :signature-rh="signatureRhUrl"
            :signature-candidat="signatureCandidatUrl"
          />
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.signature-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--color-background, #f6f5f5);
}

.signature-card {
  max-width: 560px;
  width: 100%;
  padding: 32px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #e9ece8;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.06);
}

.signature-header { margin-bottom: 24px; text-align: center; }

.eyebrow {
  color: #015f09;
  font-size: 10px;
  font-weight: 750;
  letter-spacing: 1px;
}

.signature-header h1 {
  margin: 8px 0 0;
  font-size: 24px;
  font-weight: 700;
  color: #292e2a;
}

.state { text-align: center; padding: 24px 0; }
.state.loading { color: #818781; font-size: 14px; }
.state.error { color: #b74736; font-size: 14px; }
.state.error button {
  margin-top: 12px;
  padding: 8px 16px;
  border: 1px solid #f0d7d3;
  border-radius: 8px;
  background: #fff7f6;
  color: #b74736;
  cursor: pointer;
}

.state.success { color: #24752b; }
.checkmark {
  display: inline-grid;
  place-items: center;
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: #eaf5e9;
  color: #24752b;
  font-size: 32px;
  font-weight: 700;
}
.state.success h2 { margin: 0 0 8px; font-size: 18px; }
.state.success p { margin: 0; font-size: 13px; color: #666; }

.contrat-resume {
  padding: 20px;
  border-radius: 10px;
  background: #fafbf9;
  border: 1px solid #eef0ec;
  margin-bottom: 20px;
}
.contrat-resume h2 { margin: 0 0 14px; font-size: 16px; color: #292e2a; }
.contrat-resume dl { margin: 0; display: grid; gap: 10px; }
.contrat-resume dl > div { display: flex; justify-content: space-between; gap: 12px; }
.contrat-resume dt { color: #818781; font-size: 12px; }
.contrat-resume dd { margin: 0; color: #292e2a; font-size: 13px; font-weight: 600; text-align: right; }

.hint {
  margin: 0 0 20px;
  color: #818781;
  font-size: 12px;
  line-height: 1.6;
  text-align: center;
}

.btn-signer {
  width: 100%;
  padding: 14px;
  border: 0;
  border-radius: 10px;
  background: #015f09;
  color: #fff;
  font: inherit;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-signer:hover:not(:disabled) { background: #014b07; }
.btn-signer:disabled { opacity: 0.6; cursor: not-allowed; }

.actions-secondaires {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.btn-secondaire {
  flex: 1;
  padding: 12px;
  border: 1px solid #d4d4d4;
  border-radius: 10px;
  background: #fff;
  color: #292e2a;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-secondaire:hover { background: #f7f7f7; }

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
.modal-contrat__barre button:hover { background: #f0f0f0; }

.modal-contrat__scroll {
  overflow-y: auto;
  padding: 24px;
  background: #f4f5f3;
}
</style>