<!-- BY : Brandon -->
<script setup>
import { ref, computed, onMounted } from "vue";
import { useAuthStore } from "@/stores/authStore";
import api from "@/services/api";
import Notification from "@/components/ui/Notification.vue";

const authStore = useAuthStore();
const estRh = computed(() => authStore.user.role?.toUpperCase() === "RH");

const employes = ref([]);
const recherche = ref("");
const employeSelectionneId = ref(null);
const pointagesDuJour = ref([]);
const heureManuelle = ref("");
const dateFiltre = ref(new Date().toISOString().slice(0, 10));
const message = ref("");
const typeMessage = ref("succes");

const employesFiltres = computed(() => {
  const q = recherche.value.trim().toLowerCase();
  return employes.value.filter(e =>
    !q || [e.nom, e.prenom, e.matricule].join(" ").toLowerCase().includes(q)
  );
});

const employeSelectionne = computed(() =>
  employes.value.find(e => e.id === employeSelectionneId.value) ?? null
);

function afficherNotification(texte, type = "succes") {
  message.value = texte;
  typeMessage.value = type;
  setTimeout(() => { message.value = ""; }, 4000);
}

async function chargerEmployes() {
  try {
    const { data } = await api.get("/manager/employes");
    employes.value = data;
  } catch (e) {
    afficherNotification("Impossible de charger les employés.", "erreur");
  }
}

async function chargerPointages() {
  if (!employeSelectionneId.value) return;
  try {
    const { data } = await api.get(`/pointage/jour/${employeSelectionneId.value}?date=${dateFiltre.value}`);
    pointagesDuJour.value = data;
  } catch (e) {
    afficherNotification(e.response?.data?.message || "Erreur de chargement.", "erreur");
  }
}

async function pointer(type) {
  if (!employeSelectionneId.value) return afficherNotification("Sélectionnez un employé.", "erreur");

  const payload = { idEmploye: employeSelectionneId.value, typePointage: type };
  if (heureManuelle.value) {
    payload.dateHeure = heureManuelle.value + ":00+03:00";
  }

  try {
    const { data } = await api.post("/pointage", payload);
    // ✅ Mise à jour locale immédiate
    pointagesDuJour.value = [...pointagesDuJour.value, data].sort(
      (a, b) => new Date(a.dateHeure) - new Date(b.dateHeure)
    );
    afficherNotification(`Pointage ${type} enregistré.`, "succes");
    heureManuelle.value = "";
  } catch (e) {
    afficherNotification(e.response?.data?.message || "Erreur de pointage.", "erreur");
  }
}

async function recalculerJour() {
  if (!employeSelectionneId.value) return afficherNotification("Sélectionnez un employé.", "erreur");
  try {
    const { data } = await api.post(
      `/pointage/calculer-jour/${employeSelectionneId.value}?date=${dateFiltre.value}`
    );
    afficherNotification(
      `Calcul effectué — Temps réel : ${data.tempsReelMinutes} min · Retard : ${data.retardMinutes} min · Heures sup : ${data.heuresSupplementairesMinutes} min.`,
      "succes"
    );
    // ✅ Attente + rechargement des pointages (pour cohérence)
    await new Promise(r => setTimeout(r, 250));
    await chargerPointages();
  } catch (e) {
    afficherNotification(e.response?.data?.message || "Erreur lors du calcul.", "erreur");
  }
}

function changerDateFiltre() {
  if (employeSelectionneId.value) chargerPointages();
}

onMounted(chargerEmployes);
</script>

<template>
  <main class="validation-page">
    <Notification :message="message" :type="typeMessage" @fermer="message = ''" />

    <header class="page-header">
      <div>
        <span class="eyebrow">MODULE RH — POINTAGE</span>
        <h1>Pointage</h1>
        <p>Enregistrez les entrées et sorties, consultez l'historique et calculez la journée.</p>
      </div>
    </header>

    <section class="workspace">
      <div class="request-panel">
        <header class="panel-heading">
          <div>
            <h2>Employés</h2>
            <p>{{ employesFiltres.length }} employé(s)</p>
          </div>
        </header>
        <div class="search-box">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
          <input v-model="recherche" type="search" placeholder="Nom, prénom, matricule..." />
        </div>
        <div class="request-list">
          <button
            v-for="e in employesFiltres"
            :key="e.id"
            type="button"
            class="request-item"
            :class="{ active: employeSelectionneId === e.id }"
            @click="employeSelectionneId = e.id; chargerPointages()"
          >
            <h3>{{ e.prenom }} {{ e.nom }}</h3>
            <p class="department">{{ e.matricule }}</p>
          </button>
          <div v-if="!employesFiltres.length" class="empty-list">Aucun employé trouvé.</div>
        </div>
      </div>

      <section class="detail-panel">
        <div v-if="employeSelectionne">
          <header class="detail-header">
            <div>
              <span class="eyebrow">POINTAGE</span>
              <h2>{{ employeSelectionne.prenom }} {{ employeSelectionne.nom }}</h2>
              <p>{{ employeSelectionne.matricule }}</p>
            </div>
            <div class="date-filtre">
              <label for="dateFiltre">Date :</label>
              <input
                id="dateFiltre"
                v-model="dateFiltre"
                type="date"
                @change="changerDateFiltre"
              />
            </div>
          </header>

          <div class="detail-body">
            <h3 class="section-title">Saisie manuelle (facultatif)</h3>
            <div class="g-champ" style="margin-bottom: 20px;">
              <label>Date et heure (laisser vide pour l'instant actuel)</label>
              <input type="datetime-local" v-model="heureManuelle" class="g-input" />
            </div>

            <h3 class="section-title">Actions de pointage</h3>
            <div class="decision-actions" style="margin-bottom: 24px;">
              <button class="approve-button" @click="pointer('ENTREE')">Entrée</button>
              <button class="reject-button" @click="pointer('DEBUT_PAUSE')">Début pause</button>
              <button class="reject-button" @click="pointer('FIN_PAUSE')">Fin pause</button>
              <button class="reject-button" @click="pointer('SORTIE')">Sortie</button>
            </div>

            <h3 class="section-title">Pointages du {{ dateFiltre }}</h3>
            <table class="g-table" style="margin-bottom: 24px;">
              <thead><tr><th>Heure</th><th>Type</th></tr></thead>
              <tbody>
                <tr v-for="p in pointagesDuJour" :key="p.id">
                  <td>{{ new Date(p.dateHeure).toLocaleTimeString('fr-FR') }}</td>
                  <td><span class="g-badge g-badge--neutral">{{ p.typePointage }}</span></td>
                </tr>
                <tr v-if="!pointagesDuJour.length">
                  <td colspan="2" class="g-vide">Aucun pointage pour cette date.</td>
                </tr>
              </tbody>
            </table>

            <div class="read-only-notice">
              <button class="g-btn g-btn--primaire" @click="recalculerJour">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 8px;">
                  <path d="M23 4v6h-6"></path><path d="M1 20v-6h6"></path><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
                </svg>
                Calculer la journée du {{ dateFiltre }}
              </button>
            </div>
          </div>
        </div>
        <div v-else class="empty-detail">
          <div class="empty-document">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
          </div>
          <h2>Sélectionnez un employé</h2>
          <p>Choisissez un employé dans la liste pour pointer ou consulter son historique.</p>
        </div>
      </section>
    </section>
  </main>
</template>

<style scoped>
.validation-page { --ink: #292e2a; --muted: #818781; --line: #e9ece8; --surface: #ffffff; width: 100%; min-height: calc(100vh - 64px); padding: 0; margin: 0; box-sizing: border-box; background: var(--color-background); color: var(--ink); }
.page-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 20px; margin-bottom: 24px; }
.eyebrow { color: var(--color-primary); font-size: 10px; font-weight: 750; letter-spacing: 1px; }
.page-header h1 { margin: 8px 0; font-size: 26px; font-weight: 700; letter-spacing: -0.5px; }
.page-header p { margin: 0; color: var(--muted); font-size: 13px; }
.workspace { display: grid; grid-template-columns: minmax(280px, 0.82fr) minmax(0, 1.8fr); align-items: stretch; gap: 18px; width: 100%; min-width: 0; }
.request-panel, .detail-panel { min-width: 0; overflow: hidden; border: 1px solid var(--line); border-radius: 12px; background: var(--surface); }
.panel-heading { display: flex; justify-content: space-between; padding: 20px; }
.panel-heading h2, .detail-header h2 { margin: 0 0 7px; font-size: 17px; font-weight: 700; }
.panel-heading p, .detail-header p { margin: 0; color: var(--muted); font-size: 13px; }
.search-box { display: flex; align-items: center; gap: 10px; margin: 0 14px 14px; padding: 0 12px; border: 1px solid var(--line); border-radius: 8px; background: #fafbf9; color: #818781; }
.search-box input { width: 100%; min-width: 0; padding: 12px 0; border: 0; outline: 0; background: transparent; color: var(--ink); font: inherit; font-size: 12px; }
.request-list { display: flex; flex-direction: column; gap: 7px; max-height: 500px; overflow-y: auto; padding: 10px; scrollbar-width: thin; scrollbar-color: var(--line) transparent; }
.request-list::-webkit-scrollbar { width: 6px; }
.request-list::-webkit-scrollbar-thumb { background: var(--line); border-radius: 3px; }
.request-item { width: 100%; padding: 14px; border: 1px solid transparent; border-radius: 9px; background: white; text-align: left; cursor: pointer; transition: background 0.2s, border-color 0.2s; }
.request-item:hover { background: #fafbf9; border-color: #e8ebe6; }
.request-item.active { border-color: #bfd8bc; background: #f4f8f2; box-shadow: inset 3px 0 var(--color-primary); }
.request-item h3 { margin: 0 0 5px; color: var(--ink); font-size: 14px; }
.department { margin: 0; color: var(--muted); font-size: 11px; }
.detail-header { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 14px; padding: 22px; border-bottom: 1px solid var(--line); }
.detail-body { padding: 22px; }
.section-title { margin: 0 0 14px; font-size: 13px; font-weight: 750; }
.date-filtre { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--muted); }
.date-filtre input { padding: 6px 10px; border: 1px solid var(--line); border-radius: 8px; font: inherit; color: var(--ink); background: var(--surface); }
.decision-actions { display: flex; gap: 12px; flex-wrap: wrap; }
.decision-actions button { flex: 1; min-width: 120px; padding: 12px 16px; border-radius: 8px; cursor: pointer; font: inherit; font-size: 12px; font-weight: 700; }
.reject-button { border: 1px solid #f0d7d3; background: #fff7f6; color: #b74736; }
.reject-button:hover { background: #ffebe8; }
.approve-button { border: 1px solid var(--color-primary); background: var(--color-primary); color: white; }
.approve-button:hover { background: #014b07; }
.read-only-notice { margin-top: 20px; padding: 13px; border: 1px solid var(--line); border-radius: 8px; background: #fafbf9; }
.empty-list, .empty-detail { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 45px 22px; text-align: center; }
.empty-list h3, .empty-detail h2 { margin: 12px 0 7px; font-size: 15px; }
.empty-list p, .empty-detail p { color: var(--muted); font-size: 12px; }
.empty-document { display: grid; place-items: center; width: 50px; height: 50px; border-radius: 13px; background: #eef6ed; color: var(--color-primary); }
@media (max-width: 1000px) { .workspace { grid-template-columns: minmax(240px, 0.8fr) minmax(0, 1.4fr); } }
@media (max-width: 760px) { .workspace { grid-template-columns: minmax(0, 1fr); } }
</style>