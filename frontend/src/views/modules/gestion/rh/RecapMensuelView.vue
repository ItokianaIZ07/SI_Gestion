<!-- BY : Brandon -->
<script setup>
import { ref, onMounted } from "vue";
import api from "@/services/api";
import PageEntete from "@/components/gestion/PageEntete.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import "@/assets/gestion.css";

const departements = ref([]);
const departementId = ref("");
const annee = ref(new Date().getFullYear());
const mois = ref(new Date().getMonth() + 1);
const lignes = ref([]);
const erreur = ref("");

async function chargerDepartements() {
  try {
    const { data } = await api.get("/manager/departements");
    departements.value = data;
  } catch (e) {
    erreur.value = "Erreur de chargement des départements.";
  }
}

async function chargerRecap() {
  if (!departementId.value) return;
  erreur.value = "";
  try {
    const { data } = await api.get(`/pointage/recap-mensuel?departementId=${departementId.value}&annee=${annee.value}&mois=${mois.value}`);
    lignes.value = data.lignes;
  } catch (e) {
    erreur.value = e.response?.data?.message || "Erreur.";
  }
}

onMounted(chargerDepartements);
</script>

<template>
  <div class="g-page">
    <PageEntete titre="Récapitulatif Mensuel" sous-titre="Totaux des temps de travail et heures sup." />
    <MessageAlerte :message="erreur" />

    <div class="g-carte g-form" style="grid-template-columns: repeat(3, 1fr); padding: 20px;">
      <div class="g-champ">
        <label>Département</label>
        <select v-model="departementId" class="g-input" @change="chargerRecap">
          <option value="">-- Choisir --</option>
          <option v-for="d in departements" :key="d.id" :value="d.id">{{ d.nom }}</option>
        </select>
      </div>
      <div class="g-champ">
        <label>Mois</label>
        <select v-model="mois" class="g-input" @change="chargerRecap">
          <option v-for="m in 12" :key="m" :value="m">{{ m }}</option>
        </select>
      </div>
      <div class="g-champ">
        <label>Année</label>
        <input type="number" v-model="annee" class="g-input" @change="chargerRecap" />
      </div>
    </div>

    <div v-if="departementId" class="g-carte">
      <table class="g-table">
        <thead>
          <tr>
            <th>Employé</th>
            <th class="g-droite">Temps prévu (min)</th>
            <th class="g-droite">Temps réel (min)</th>
            <th class="g-droite">Retard total (min)</th>
            <th class="g-droite">Nb retards</th>
            <th class="g-droite">HS validées (min)</th>
            <th class="g-droite">HS à valider (min)</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="l in lignes" :key="l.employeId">
            <td class="g-principal">{{ l.nom }} {{ l.prenom }} <span class="g-secondaire">{{ l.matricule }}</span></td>
            <td class="g-droite">{{ l.tempsPrevu }}</td>
            <td class="g-droite">{{ l.tempsReel }}</td>
            <td class="g-droite">{{ l.retardTotal }}</td>
            <td class="g-droite">{{ l.nbRetards }}</td>
            <td class="g-droite">{{ l.hsValidees }}</td>
            <td class="g-droite">{{ l.hsAValider }}</td>
          </tr>
          <tr v-if="!lignes.length"><td colspan="7" class="g-vide">Aucune donnée pour ce mois.</td></tr>
        </tbody>
      </table>
    </div>
  </div>
</template>