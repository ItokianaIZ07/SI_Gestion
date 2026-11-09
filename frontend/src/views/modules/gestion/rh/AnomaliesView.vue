<!-- BY : Brandon -->
<script setup>
import { ref, computed, onMounted } from "vue";
import { useAuthStore } from "@/stores/authStore";
import api from "@/services/api";
import Notification from "@/components/ui/Notification.vue";

const authStore = useAuthStore();
const estRh = computed(() => authStore.user.role?.toUpperCase() === "RH");

const anomalies = ref([]);
const recherche = ref("");
const filtreStatut = ref("");
const message = ref("");
const typeMessage = ref("succes");

const modaleOuverte = ref(false);
const anomalieSelectionnee = ref(null);
const justification = ref("");
const enCours = ref(false);

function afficherNotification(texte, type = "succes") {
  message.value = texte;
  typeMessage.value = type;
  setTimeout(() => { message.value = ""; }, 4000);
}

async function charger() {
  try {
    const { data } = await api.get("/pointage/anomalies");
    anomalies.value = data;
  } catch (e) {
    afficherNotification(e.response?.data?.message || "Erreur.", "erreur");
  }
}

const anomaliesFiltrees = computed(() => {
  const q = recherche.value.trim().toLowerCase();
  return anomalies.value.filter(a => {
    if (filtreStatut.value && a.statut !== filtreStatut.value) return false;
    return !q || [a.employe.nom, a.employe.prenom, a.typeAnomalie, a.description]
      .join(" ").toLowerCase().includes(q);
  });
});

function ouvrirModale(anomalie) {
  anomalieSelectionnee.value = anomalie;
  justification.value = "";
  modaleOuverte.value = true;
}

function fermerModale() {
  modaleOuverte.value = false;
  anomalieSelectionnee.value = null;
  justification.value = "";
}

async function confirmerResolution() {
  if (!justification.value.trim()) {
    afficherNotification("La justification est obligatoire.", "erreur");
    return;
  }
  enCours.value = true;
  try {
    await api.patch(`/pointage/anomalies/${anomalieSelectionnee.value.id}/resoudre`, {
      justification: justification.value.trim()
    });
    afficherNotification("Anomalie résolue.", "succes");
    fermerModale();
    // ✅ Attente avant rechargement
    await new Promise(r => setTimeout(r, 250));
    await charger();
  } catch (e) {
    afficherNotification(e.response?.data?.message || "Erreur.", "erreur");
  } finally {
    enCours.value = false;
  }
}

function libelleType(code) {
  return {
    RETARD: "Retard",
    JOUR_NON_POINTE: "Jour non pointé",
    SORTIE_MANQUANTE: "Sortie manquante",
    SEQUENCE_INVALIDE: "Séquence invalide"
  }[code] ?? code;
}

function libelleStatut(code) {
  return {
    A_TRAITER: "À traiter",
    EN_COURS: "En cours",
    RESOLUE: "Résolue",
    IGNOREE: "Ignorée"
  }[code] ?? code;
}

onMounted(charger);
</script>

<template>
  <div class="g-page">
    <Notification :message="message" :type="typeMessage" @fermer="message = ''" />

    <header class="g-entete">
      <div>
        <h1>Anomalies de pointage</h1>
        <p>Consultez et justifiez la résolution des anomalies détectées.</p>
      </div>
    </header>

    <section class="g-carte">
      <div class="g-filtres">
        <input v-model="recherche" class="g-input g-input--recherche" type="search" placeholder="Rechercher..." />
        <select v-model="filtreStatut" class="g-input">
          <option value="">Tous les statuts</option>
          <option value="A_TRAITER">À traiter</option>
          <option value="EN_COURS">En cours</option>
          <option value="RESOLUE">Résolue</option>
          <option value="IGNOREE">Ignorée</option>
        </select>
        <span class="g-compteur">{{ anomaliesFiltrees.length }} anomalie(s)</span>
      </div>

      <div class="g-table-wrap">
        <table class="g-table">
          <thead>
            <tr>
              <th>Employé</th>
              <th>Date</th>
              <th>Type</th>
              <th>Description</th>
              <th>Statut</th>
              <th v-if="estRh" class="g-droite">Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="a in anomaliesFiltrees" :key="a.id">
              <td class="g-principal">{{ a.employe.nom }} {{ a.employe.prenom }}</td>
              <td>{{ a.dateAnomalie }}</td>
              <td><span class="g-badge g-badge--warning">{{ libelleType(a.typeAnomalie) }}</span></td>
              <td>{{ a.description }}</td>
              <td>
                <span class="g-badge" :class="{
                  'g-badge--danger': a.statut === 'A_TRAITER',
                  'g-badge--warning': a.statut === 'EN_COURS',
                  'g-badge--success': a.statut === 'RESOLUE',
                  'g-badge--neutral': a.statut === 'IGNOREE'
                }">
                  {{ libelleStatut(a.statut) }}
                </span>
                <p v-if="a.statut === 'RESOLUE' && a.justification" style="margin: 4px 0 0; font-size: 11px; color: var(--muted);">
                  « {{ a.justification }} »
                </p>
              </td>
              <td v-if="estRh">
                <div class="g-actions-cellule">
                  <button
                    v-if="a.statut !== 'RESOLUE' && a.statut !== 'IGNOREE'"
                    class="g-btn g-btn--petit"
                    @click="ouvrirModale(a)"
                  >
                    Examiner
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!anomaliesFiltrees.length">
              <td :colspan="estRh ? 6 : 5" class="g-vide">Aucune anomalie.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div v-if="modaleOuverte" class="modal-fond" @click.self="fermerModale">
      <div class="modal" role="dialog" aria-modal="true">
        <header class="modal-entete">
          <div>
            <span class="surtitre">RÉSOLUTION D'ANOMALIE</span>
            <h2>{{ libelleType(anomalieSelectionnee.typeAnomalie) }}</h2>
            <p>{{ anomalieSelectionnee.employe.prenom }} {{ anomalieSelectionnee.employe.nom }} — {{ anomalieSelectionnee.dateAnomalie }}</p>
          </div>
          <button class="bouton-fermer" @click="fermerModale" aria-label="Fermer">&times;</button>
        </header>

        <div class="modal-corps">
          <section class="info-source">
            <h3>Source de l'anomalie</h3>
            <p class="description">{{ anomalieSelectionnee.description }}</p>
            <p class="statut">
              Statut actuel :
              <span class="g-badge g-badge--danger">{{ libelleStatut(anomalieSelectionnee.statut) }}</span>
            </p>
          </section>

          <section class="saisie-justification">
            <label for="justif" class="obligatoire">Justification de la résolution</label>
            <textarea
              id="justif"
              v-model="justification"
              rows="4"
              placeholder="Expliquez pourquoi cette anomalie peut être considérée comme résolue (ex. pointage oublié corrigé manuellement, justificatif fourni, erreur du système...)"
            ></textarea>
            <p class="aide">
              Cette justification sera enregistrée dans l'historique de l'anomalie avec votre nom et la date de résolution.
            </p>
          </section>
        </div>

        <footer class="modal-pied">
          <button class="g-btn" @click="fermerModale" :disabled="enCours">Annuler</button>
          <button class="g-btn g-btn--primaire" @click="confirmerResolution" :disabled="enCours || !justification.trim()">
            {{ enCours ? "Enregistrement..." : "Confirmer la résolution" }}
          </button>
        </footer>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-fond { position: fixed; inset: 0; z-index: 9999; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgb(0 0 0 / 45%); backdrop-filter: blur(3px); }
.modal { display: flex; flex-direction: column; width: min(100%, 640px); max-height: calc(100vh - 40px); overflow: hidden; background: var(--color-card, #fff); border-radius: 16px; box-shadow: 0 20px 60px rgb(0 0 0 / 20%); }
.modal-entete { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; padding: 24px 28px; border-bottom: 1px solid #e8e8e8; }
.surtitre { display: block; margin-bottom: 8px; color: var(--color-primary, #015f09); font-size: 11px; font-weight: 800; letter-spacing: 1.4px; }
.modal-entete h2 { margin: 0; color: #222; font-size: 22px; font-weight: 750; }
.modal-entete p { margin: 6px 0 0; color: #777; font-size: 13px; }
.bouton-fermer { display: grid; flex-shrink: 0; width: 36px; height: 36px; place-items: center; color: #555; font-size: 26px; background: #f3f3f3; border: none; border-radius: 9px; cursor: pointer; }
.bouton-fermer:hover { background: #e8e8e8; }
.modal-corps { padding: 24px 28px; overflow-y: auto; }
.info-source { padding: 16px; background: #f7f9f7; border: 1px solid #e1e9e1; border-radius: 12px; margin-bottom: 20px; }
.info-source h3 { margin: 0 0 8px; font-size: 13px; font-weight: 700; color: #444; }
.info-source .description { margin: 0 0 12px; color: #333; font-size: 14px; line-height: 1.6; }
.info-source .statut { margin: 0; font-size: 12px; color: #666; }
.saisie-justification { display: flex; flex-direction: column; gap: 8px; }
.saisie-justification label { font-size: 13px; font-weight: 650; color: #333; }
.obligatoire::after { content: " *"; color: var(--color-danger, #cd3d3d); }
.saisie-justification textarea { width: 100%; box-sizing: border-box; padding: 10px 12px; color: #252525; font: inherit; font-size: 13px; background: #fff; border: 1px solid #dcdcdc; border-radius: 8px; outline: none; resize: vertical; transition: border-color 0.2s, box-shadow 0.2s; }
.saisie-justification textarea:focus { border-color: var(--color-primary, #015f09); box-shadow: 0 0 0 3px rgb(1 95 9 / 9%); }
.aide { margin: 4px 0 0; color: #777; font-size: 12px; line-height: 1.5; }
.modal-pied { display: flex; align-items: center; justify-content: flex-end; gap: 12px; padding: 18px 28px; background: #fff; border-top: 1px solid #e8e8e8; }
@media (max-width: 600px) { .modal-entete, .modal-corps { padding: 20px; } .modal-pied { padding: 16px 20px; flex-wrap: wrap; } }
</style>