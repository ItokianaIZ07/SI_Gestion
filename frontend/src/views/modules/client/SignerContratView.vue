<script setup>
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { getContratParToken, signerContratParToken } from "@/services/contratService";
import SignaturePad from "@/components/SignaturePad.vue";           // ← AJOUT

const route = useRoute();
const token = route.params.token;

const contrat = ref(null);
const chargement = ref(true);
const erreur = ref("");
const signe = ref(false);
const enCours = ref(false);
const afficherSignature = ref(false);                             // ← AJOUT

function formatDate(d) {
  if (!d) return "—";
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "long" }).format(new Date(d));
}

function formatMontant(v) {
  if (v == null) return "—";
  return new Intl.NumberFormat("fr-FR", { style: "currency", currency: "MGA", maximumFractionDigits: 0 }).format(v);
}

async function charger() {
  chargement.value = true;
  erreur.value = "";
  try {
    contrat.value = await getContratParToken(token);
  } catch (e) {
    erreur.value = e.response?.data?.message ?? "Ce lien est invalide ou a expiré.";
  } finally {
    chargement.value = false;
  }
}

/* Ouvre la modale de signature manuscrite */
function ouvrirSignature() {                                       // ← AJOUT
  afficherSignature.value = true;
}

/* Reçoit la signature base64 du composant et envoie au backend */
async function onSignatureValide(base64) {                         // ← AJOUT
  enCours.value = true;
  erreur.value = "";
  try {
    await signerContratParToken(token, base64);
    signe.value = true;
  } catch (e) {
    erreur.value = e.response?.data?.message ?? "Impossible de signer ce contrat.";
  } finally {
    enCours.value = false;
    afficherSignature.value = false;
  }
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

      <div v-else-if="erreur && !signe" class="state error">
        <p>{{ erreur }}</p>
        <button type="button" @click="charger">Réessayer</button>
      </div>

      <div v-else-if="signe" class="state success">
        <div class="checkmark">✓</div>
        <h2>Contrat signé avec succès</h2>
        <p>Merci. Le service RH a été notifié et prendra contact avec vous.</p>
      </div>

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
</style>