<!-- Brandon -->
<script setup>
import { ref, computed, onMounted } from "vue";
import { useEntretiens } from "@/composable/useEntretiens";


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
const selectionId = ref(null);
const entretienEdite = ref(null);

const candidaturesFiltrees = computed(() => {
  const terme = recherche.value.trim().toLowerCase();
  if (!terme) return candidatures.value;
  return candidatures.value.filter((c) => {
    const contenu = [
      c.candidatNom,
      c.candidatPrenom,
      c.poste,
      c.departement,
      c.statut,
    ]
      .filter(Boolean)
      .join(" ")
      .toLowerCase();
    return contenu.includes(terme);
  });
});

const candidatureSelectionnee = computed(() =>
  candidatures.value.find((c) => c.id === selectionId.value) ?? null
);

function selectionner(c) {
  selectionId.value = c.id;

  if (c.entretien) {
    // Tronque les microsecondes : 2026-10-30T16:08:16.734499 → 2026-10-30T16:08:16
    let date = c.entretien.dateEntretien || '';
    if (date.length > 19) date = date.slice(0, 19);  // YYYY-MM-DDTHH:mm:ss

    entretienEdite.value = {
      ...c.entretien,
      dateEntretien: date,
    };
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

async function enregistrer() {
  if (!entretienEdite.value?.dateEntretien) {
    alert("Veuillez saisir une date d'entretien.");
    return;
  }

  // Normalise la date : "YYYY-MM-DDTHH:mm" → "YYYY-MM-DDTHH:mm:00"
  let date = entretienEdite.value.dateEntretien;
  if (date.length === 16) date += ":00";  // ajoute :00 si les secondes manquent

  const payload = { ...entretienEdite.value, dateEntretien: date };
  const resultat = await sauvegarder(payload);
  if (!resultat.success) alert(resultat.message);
}

async function convoquerEntretien() {
  if (!entretienEdite.value?.id) return alert("Enregistrez d'abord l'entretien.");
  const resultat = await convoquer(entretienEdite.value.id);
  alert(resultat.success ? "Convocation envoyée." : resultat.message);
}

async function envoyerResultat(decision) {
  if (!candidatureSelectionnee.value) return;
  const resultat = await notifierResultat(candidatureSelectionnee.value.id, decision);
  alert(resultat.success ? "Résultat envoyé." : resultat.message);
}

function formatDate(d) {
  if (!d) return "—";
  return new Intl.DateTimeFormat("fr-FR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(d));
}

onMounted(charger);
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

    <div v-if="erreur" class="feedback error-feedback">
      <span>{{ erreur }}</span>
      <button @click="charger">Réessayer</button>
    </div>
    <div v-if="message" class="feedback success-feedback">
      <span>{{ message }}</span>
    </div>

    <section class="workspace">
      <!-- Liste gauche -->
      <div class="request-panel">
        <header class="panel-heading">
          <div>
            <h2>Candidatures retenues / présélectionnées</h2>
            <p>{{ candidaturesFiltrees.length }} candidat(s)</p>
          </div>
        </header>

        <div class="search-box">
          <span aria-hidden="true">⌕</span>
          <input v-model="recherche" type="search" placeholder="Nom, poste, département..." />
        </div>

        <div class="request-list">
          <button
            v-for="c in candidaturesFiltrees"
            :key="c.id"
            type="button"
            class="request-item"
            :class="{ active: selectionId === c.id }"
            @click="selectionner(c)"
          >
            <div class="request-item-top">
              <span class="request-id">#{{ c.id }}</span>
              <span class="priority" :class="String(c.statut).toLowerCase()">
                {{ c.statut === "RETENU" ? "Retenu" : "Présélectionné" }}
              </span>
            </div>
            <h3>{{ c.candidatPrenom }} {{ c.candidatNom }}</h3>
            <p class="department">{{ c.poste }} — {{ c.departement }}</p>
            <div class="request-item-bottom">
              <span>{{ c.entretien ? "Entretien planifié" : "Aucun entretien" }}</span>
              <span v-if="c.entretien">{{ formatDate(c.entretien.dateEntretien) }}</span>
            </div>
          </button>

          <div v-if="!chargement && candidaturesFiltrees.length === 0" class="empty-list">
            <h3>Aucune candidature</h3>
            <p>Les candidatures apparaîtront ici dès qu'elles seront retenues.</p>
          </div>
        </div>
      </div>

      <!-- Détail droite -->
      <section class="detail-panel">
        <template v-if="candidatureSelectionnee && entretienEdite">
          <header class="detail-header">
            <div>
              <span class="eyebrow">CANDIDAT #{{ candidatureSelectionnee.id }}</span>
              <h2>{{ candidatureSelectionnee.candidatPrenom }} {{ candidatureSelectionnee.candidatNom }}</h2>
              <p>{{ candidatureSelectionnee.poste }} — {{ candidatureSelectionnee.departement }}</p>
            </div>
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
                <input type="datetime-local" v-model="entretienEdite.dateEntretien" step="1"/>
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

            <div class="decision-actions">
              <button class="approve-button" @click="enregistrer">
                {{ entretienEdite.id ? "Mettre à jour" : "Enregistrer" }}
              </button>
              <button class="reject-button" @click="convoquerEntretien">
                Envoyer la convocation
              </button>
            </div>

            <h3 class="section-title">Résultat</h3>
            <div class="decision-actions">
              <button class="approve-button" @click="envoyerResultat('RETENU')">
                Notifier « Retenu »
              </button>
              <button class="reject-button" @click="envoyerResultat('REJETE')">
                Notifier « Rejeté »
              </button>
            </div>

            <div v-if="candidatureSelectionnee.entretien" class="read-only-notice">
              Entretien actuel : {{ formatDate(candidatureSelectionnee.entretien.dateEntretien) }}
              — {{ candidatureSelectionnee.entretien.typeEntretien }}
            </div>
          </div>
        </template>

        <div v-else class="empty-detail">
          <div class="empty-document">☷</div>
          <h2>Sélectionnez un candidat</h2>
          <p>Choisissez une candidature pour planifier ou modifier son entretien.</p>
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

/* Liste */
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
  transition: background 0.2s, border-color 0.2s;
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

/* Badges */
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

.approve-button:disabled {
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