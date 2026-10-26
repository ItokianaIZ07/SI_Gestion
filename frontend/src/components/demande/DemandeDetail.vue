<script setup>
import {
  formatDate,
  prioriteLabel,
  statutLabel,
  formatSalaire,
} from "@/utils/demandeUtils";
import { ref, computed } from "vue";
import api from "@/services/api";
import ValidationSalaire from "@/components/demande/ValidationSalaire.vue";
import ModalRecrutement from "@/components/demande/ModalRecrutement.vue";
import { STATUTS_RECRUTEMENT } from "@/config/libelles";
import { besoinsApi, recrutementsApi } from "@/services/ressources";
import { useAuthStore } from "@/stores/authStore";

const authStore = useAuthStore();

const props = defineProps({
  demande: {
    type: Object,
    required: true,
  },
  reloadBesoin: {
    type: Function,
    required: true,
  },
  showNotification: {
    type: Function,
    required: true
  }
});

console.log(props.demande);

const modalOuvert = ref(false);
const remarque = ref("");
const envoiEnCours = ref(false);
const departementEstSature = props.demande.effectifTotal >= props.demande.effectifMax;
const afficherValidationSalaire = ref(false);

const accepterDemande = () => {
  const estEtapeRH =
    props.demande.peutDecider &&
    String(props.demande.decisionRh ?? "EN_ATTENTE").toUpperCase() ===
      "EN_ATTENTE";

  if (estEtapeRH) {
    ouvrirValidationSalaire();
  } else {
    sendDecision(props.demande.id, "ACCEPTE");
  }
};

const ouvrirModalRecrutement = () => {
  modalOuvert.value = true;
};

const fermerModalRecrutement = () => {
  modalOuvert.value = false;
};

const validerCreationRecrutement = async (donnees) => {
  if (donnees.dateCloture && donnees.dateOuverture && donnees.dateCloture < donnees.dateOuverture) {
    erreur.value = "La date de clôture ne peut pas précéder la date d'ouverture.";
    return;
  }

  if(authStore.user.role != "RH"){
    props.showNotification("Vous devez être un RH pour faire cette action", "erreur");
    return;
  }

  const user = await api.get(`/manager/utilisateurs/${authStore.user.role}`);
  const responsableId = user.id;

  const charge = {
    besoinId: Number(donnees.demandeId),
    reference: donnees.reference.trim(),
    titre: donnees.titre.trim(),
    description: donnees.description.trim() || null,
    dateOuverture: donnees.dateOuverture || null,
    dateCloture: donnees.dateCloture || null,
    statut: donnees.statut,
    responsableId
  };

  try {
    const r = await recrutementsApi.creer(charge);
    router.push({ path: "/si/app/rh/offres/creation", query: { recrutementId: r.id } });
  } catch (e) {
    console.error(e);
    props.showNotification(e.message, "erreur");
  }
};

const sendDecision = async (
  idBesoin,
  decision,
  informationsSalaire = null
) => {
  envoiEnCours.value = true;

  try {
    const payload = {
      idBesoin,
      decisionCode: decision,
      remarque: remarque.value,
      ...(informationsSalaire && decision === "ACCEPTE"
        ? {
            idGrilleSalaire: informationsSalaire.idGrilleSalaire,
            salaire: informationsSalaire.salaire,
          }
        : {}),
    };

    const response = await api.post(
      "/rh/besoins/decision",
      payload
    );

    if (response.status === 201) {
      afficherValidationSalaire.value = false;

      await props.reloadBesoin();

      props.showNotification(
        "Décision prise avec succès.",
        "succes"
      );
    }
  } catch (error) {
    const message =
      error.response?.data?.detail ??
      error.response?.data?.message ??
      (typeof error.response?.data === "string"
        ? error.response.data
        : null) ??
      error.message ??
      "La décision n'a pas pu être enregistrée.";

    props.showNotification(message, "erreur");
  } finally {
    envoiEnCours.value = false;
  }
};

function ouvrirValidationSalaire() {
  if (departementEstSature) {
    props.showNotification(
      "Le département a atteint son effectif maximal.",
      "erreur"
    );
    return;
  }

  afficherValidationSalaire.value = true;
}

const confirmerAcceptation = (informationsSalaire) => {
  sendDecision(
    props.demande.id,
    "ACCEPTE",
    informationsSalaire
  );
};
</script>

<template>
  <header class="detail-header">
    <div>
      <span class="eyebrow">DEMANDE #{{ demande.id }}</span>
      <h2>{{ demande.poste }}</h2>
      <p>{{ demande.departement }}</p>
    </div>
    <span class="priority" :class="String(demande.priorite).toLowerCase()">
      {{ prioriteLabel(demande.priorite) }}
    </span>
  </header>

  <div class="detail-body">
    <div class="status-banner">
      <span>Statut actuel</span>
      <strong>{{ statutLabel(demande.statut) }}</strong>
    </div>

    <h3 class="section-title">Décisions de validation</h3>
    <div class="validation-grid">
      <div>
        <span>Ressources humaines</span
        ><strong>{{ demande.decisionRh }}</strong>
      </div>
      <div>
        <span>Finance</span><strong>{{ demande.decisionFinance }}</strong>
      </div>
      <div>
        <span>Direction</span><strong>{{ demande.decisionDirection }}</strong>
      </div>
    </div>

    <h3 class="section-title">Informations générales</h3>
    <div class="detail-grid">
      <div class="detail-field">
        <span>Demandeur</span>
        <strong>{{ demande.demandeur }}</strong>
      </div>
      <div class="detail-field">
        <span>Type de besoin</span>
        <strong>{{ demande.type }}</strong>
      </div>
      <div class="detail-field">
        <span>Nombre de postes</span>
        <strong>{{ demande.effectif }}</strong>
      </div>
      <div class="detail-field">
        <span>Lieu de travail</span>
        <strong>{{ demande.lieu }}</strong>
      </div>
      <div class="detail-field">
        <span>Date de demande</span>
        <strong>{{ formatDate(demande.dateDemande) }}</strong>
      </div>
      <div class="detail-field">
        <span>Arrivée souhaitée</span>
        <strong>{{ formatDate(demande.dateArrivee) }}</strong>
      </div>
    </div>

    <h3 class="section-title">Département {{ demande.departement }}</h3>
    <div class="detail-grid">
      <div class="detail-field">
        <span>Effectif présent dans le département</span>
        <strong>{{ demande.effectifTotal }} <span style="color: var(--color-danger)">{{ departementEstSature ? "(Saturé)":"" }}</span></strong>
      </div>
      <div class="detail-field">
        <span>Effectif max du département</span>
        <strong>{{ demande.effectifMax }}</strong>
      </div>
    </div>

    <h3 class="section-title">Motif de la demande</h3>
    <p class="motif">{{ demande.motif }}</p>

    <h3 class="section-title">Profil recherché</h3>
    <div class="detail-grid">
      <div class="detail-field">
        <span>Expérience</span>
        <strong>{{ demande.experience }}</strong>
      </div>
      <div class="detail-field">
        <span>Niveau d'étude</span>
        <strong>{{ demande.niveauEtude }}</strong>
      </div>
    </div>

    <div class="detail-field full-field">
      <span>Compétences requises</span>
      <strong>{{ demande.competences }}</strong>
    </div>

    <div class="salary-box">
      <span>Salaire mensuel estimé</span>
      <strong>{{ formatSalaire(demande.salaire) }}</strong>
    </div>

    <div class="decision-actions">
      <div class="remarque-container" v-if="demande.statut === 'EN_ATTENTE'">
        <label>Remarque de décision</label>
        <input
          type="text"
          v-model="remarque"
          placeholder="remarque s'il y en a"
        />
      </div>

      <template v-if="demande.statut == 'ACCEPTE'">
        <button @click="ouvrirModalRecrutement(demande)" class="approve-button"> // mila vérifiena hoe ef associé @nah recrutement v 
          Envoyer recrutement
        </button>
  
        <ModalRecrutement
          v-if="modalOuvert && demande"
          :demande="demande"
          :statuts-recrutement="STATUTS_RECRUTEMENT"
          @fermer="fermerModalRecrutement"
          @valider="validerCreationRecrutement"
        />
      </template>

      <template v-else-if="demande.statut !== 'REFUSE'">
        <button
          class="reject-button"
          :disabled="!demande.peutDecider || envoiEnCours"
          @click="sendDecision(demande.id, 'REFUSE')"
        >
          Refuser
        </button>
        <button
          class="approve-button"
          @click="accepterDemande"
          :disabled="envoiEnCours || !demande.peutDecider"
        >
          Accepter
        </button>
      </template>
      <ValidationSalaire
          v-if="afficherValidationSalaire"
          :demande="demande"
          @fermer="afficherValidationSalaire = false"
          @confirmer="confirmerAcceptation"
        />
    </div>
  </div>
</template>

<style scoped>
.detail-header p {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}
.detail-header h2 {
  margin: 0 0 7px;
  font-size: 17px;
  font-weight: 700;
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

.priority.urgente {
  background: #fff0ed;
  color: #b74736;
}

.priority.haute {
  background: #fff5e7;
  color: #98621b;
}

.priority.normale,
.priority.basse {
  background: #f0f1ef;
  color: #697169;
}

/* En-tête de la demande */
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 14px;
  padding: 22px;
  border-bottom: 1px solid var(--line, #e9ece8);
}

.detail-header h2 {
  margin: 9px 0 7px;
  font-size: 21px;
  font-weight: 700;
  color: var(--ink, #292e2a);
}

.detail-header p {
  margin: 0;
  color: var(--muted, #818781);
  font-size: 13px;
}

.eyebrow {
  color: var(--color-primary, #176b24);
  font-size: 10px;
  font-weight: 750;
  letter-spacing: 1px;
}

/* Badge de priorité */
.priority {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  padding: 6px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.priority.urgente {
  background: #fde8e7;
  color: #b42318;
}

.priority.haute {
  background: #fff0df;
  color: #b45309;
}

.priority.normale {
  background: #e8f2ff;
  color: #1d4ed8;
}

.priority.basse {
  background: #edf0ed;
  color: #626b62;
}

/* Corps du détail */
.detail-body {
  padding: 22px;
  color: var(--ink, #292e2a);
}

/* Bannière du statut */
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

.status-banner span {
  color: var(--muted, #818781);
}

.status-banner strong {
  color: var(--color-primary, #176b24);
  font-weight: 700;
}

.validation-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.validation-grid > div {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border: 1px solid #eff0ed;
  border-radius: 8px;
}

.validation-grid span {
  color: var(--muted, #818781);
  font-size: 11px;
}

.validation-grid strong {
  font-size: 12px;
}

/* Titres des sections */
.section-title {
  margin: 25px 0 14px;
  font-size: 13px;
  font-weight: 750;
  color: var(--ink, #292e2a);
}

/* Grille des informations */
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 20px;
}

/* Champs d'information */
.detail-field {
  display: flex;
  flex-direction: column;
  gap: 7px;
  min-width: 0;
}

.detail-field span {
  color: var(--muted, #818781);
  font-size: 11px;
}

.detail-field strong {
  color: #363c36;
  font-size: 12px;
  font-weight: 600;
  overflow-wrap: anywhere;
}

/* Champ occupant toute la largeur */
.full-field {
  margin-top: 19px;
}

/* Motif de la demande */
.motif {
  margin: 0;
  padding: 14px;
  border: 1px solid #eff0ed;
  border-radius: 8px;
  background: #fafbf9;
  color: #656c65;
  font-size: 12px;
  line-height: 1.8;
  overflow-wrap: anywhere;
}

/* Salaire estimé */
.salary-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 22px;
  padding: 16px;
  border: 1px solid #e1ebdd;
  border-radius: 9px;
  background: #f6f9f4;
}

.salary-box span {
  color: #727a70;
  font-size: 12px;
}

.salary-box strong {
  color: var(--color-primary, #176b24);
  font-size: 19px;
  font-weight: 700;
}

/* Message informatif en lecture seule */
.read-only-notice {
  margin-top: 20px;
  padding: 13px;
  border: 1px solid var(--line, #e9ece8);
  border-radius: 8px;
  background: #fafbf9;
  color: #737a73;
  font-size: 12px;
  line-height: 1.7;
}

.decision-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.decision-actions button {
  flex: 1;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
}

.decision-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
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

.decision-hint {
  margin: 12px 0 0;
  color: #969c96;
  font-size: 11px;
  line-height: 1.6;
}

.remarque-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.remarque-container label {
  color: var(--color-text);
  font-style: italic;
}

.remarque-container input {
  outline: none;
  border-radius: 4px;
  border: 1px solid var(--color-text);
  height: 24px;
  padding: 0 4px;
}

/* Adaptation aux tablettes et petits écrans */
@media (max-width: 760px) {
  .detail-header,
  .detail-body {
    padding: 17px;
  }

  .detail-header h2 {
    font-size: 19px;
  }
}

/* Adaptation aux téléphones */
@media (max-width: 480px) {
  .detail-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 16px;
  }

  .validation-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .detail-header {
    gap: 12px;
  }

  .salary-box strong {
    font-size: 17px;
  }
}
</style>
