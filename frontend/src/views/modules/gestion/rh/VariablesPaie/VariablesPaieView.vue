<script setup>
import { computed, onMounted, ref, watch } from "vue";
import Notification from "@/components/ui/Notification.vue";
import PageEntete from "@/components/gestion/PageEntete.vue";
import { variablesPaieApi } from "@/services/variablesPaie.js";
import "@/assets/gestion.css";

const maintenant = new Date();
const moisPrecedent = new Date(maintenant.getFullYear(), maintenant.getMonth() - 1, 1);
const annee = ref(moisPrecedent.getFullYear());
const mois = ref(moisPrecedent.getMonth() + 1);
const idDepartement = ref("");
const departements = ref([]);
const recapitulatif = ref(null);
const chargement = ref(true);
const exportEnCours = ref(false);
const notification = ref({ message: "", type: "erreur" });
let sequenceChargement = 0;

const moisOptions = [
  "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
  "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre",
];
const annees = computed(() => {
  const anneeCourante = maintenant.getFullYear();
  const premiereAnnee = Math.min(2020, moisPrecedent.getFullYear());
  return Array.from(
    { length: anneeCourante - premiereAnnee + 1 },
    (_, index) => anneeCourante - index,
  );
});
const filtres = computed(() => ({
  annee: annee.value,
  mois: mois.value,
  idDepartement: idDepartement.value || null,
}));
const lignes = computed(() => recapitulatif.value?.lignes ?? []);
const totaux = computed(() => recapitulatif.value?.totaux);
const periodeLibelle = computed(() => `${moisOptions[mois.value - 1]} ${annee.value}`);

function notifier(message, type = "erreur") {
  notification.value = { message, type };
}

async function chargerDepartements() {
  try {
    departements.value = (await variablesPaieApi.departements()) ?? [];
  } catch (error) {
    notifier(error.message || "Impossible de charger la liste des départements.");
  }
}

async function chargerRecapitulatif() {
  const sequence = ++sequenceChargement;
  chargement.value = true;
  try {
    const resultat = await variablesPaieApi.recapitulatif(filtres.value);
    if (sequence === sequenceChargement) recapitulatif.value = resultat;
  } catch (error) {
    if (sequence === sequenceChargement) {
      recapitulatif.value = null;
      notifier(error.message || "Impossible de charger les variables de paie.");
    }
  } finally {
    if (sequence === sequenceChargement) chargement.value = false;
  }
}

async function exporterCsv() {
  exportEnCours.value = true;
  try {
    const fichier = await variablesPaieApi.exporterCsv(filtres.value);
    const url = URL.createObjectURL(fichier);
    const lien = document.createElement("a");
    lien.href = url;
    lien.download = `variables_paie_${annee.value}-${String(mois.value).padStart(2, "0")}.csv`;
    document.body.appendChild(lien);
    lien.click();
    lien.remove();
    URL.revokeObjectURL(url);
  } catch (error) {
    notifier(error.message || "Impossible d'exporter les variables de paie.");
  } finally {
    exportEnCours.value = false;
  }
}

watch([annee, mois, idDepartement], chargerRecapitulatif);
onMounted(chargerDepartements);
</script>

<template>
  <div class="g-page variables-paie-page">
    <Notification
      :message="notification.message"
      :type="notification.type"
      @fermer="notification.message = ''"
    />

    <PageEntete
      titre="Variables de paie"
      :sous-titre="`Récapitulatif mensuel en lecture seule — ${periodeLibelle}.`"
    >
      <button
        class="g-btn g-btn--primaire"
        type="button"
        :disabled="chargement || exportEnCours"
        @click="exporterCsv"
      >
        {{ exportEnCours ? "Préparation…" : "Exporter en CSV" }}
      </button>
    </PageEntete>

    <section class="g-carte">
      <div class="g-filtres variables-paie-filtres">
        <label class="variables-paie-filtre">
          Mois
          <select v-model.number="mois" class="g-input" aria-label="Mois du récapitulatif">
            <option v-for="(nom, index) in moisOptions" :key="nom" :value="index + 1">
              {{ nom }}
            </option>
          </select>
        </label>
        <label class="variables-paie-filtre">
          Année
          <select v-model.number="annee" class="g-input" aria-label="Année du récapitulatif">
            <option v-for="optionAnnee in annees" :key="optionAnnee" :value="optionAnnee">
              {{ optionAnnee }}
            </option>
          </select>
        </label>
        <label class="variables-paie-filtre variables-paie-filtre--departement">
          Département
          <select v-model="idDepartement" class="g-input" aria-label="Filtrer par département">
            <option value="">Tous les départements</option>
            <option
              v-for="departement in departements"
              :key="departement.id"
              :value="String(departement.id)"
            >
              {{ departement.nom }}
            </option>
          </select>
        </label>
        <span class="g-compteur">{{ lignes.length }} salarié(s)</span>
      </div>

      <p v-if="chargement" class="g-vide">Chargement des variables de paie…</p>
      <p v-else-if="!lignes.length" class="g-vide">
        <strong>Aucune donnée pour ce mois</strong>
        La vue ne contient aucune journée calculée pour {{ periodeLibelle }}.
      </p>
      <div v-else class="g-table-wrap">
        <table class="g-table variables-paie-table">
          <thead>
            <tr>
              <th>Matricule</th>
              <th>Nom</th>
              <th>Prénom</th>
              <th>Prévu</th>
              <th>Travaillé</th>
              <th>Retard</th>
              <th>Nb retards</th>
              <th>Heures sup validées</th>
              <th>
                <span
                  class="variables-paie-aide"
                  title="à valider par le responsable avant la paie"
                  aria-label="Heures supplémentaires à valider par le responsable avant la paie"
                >
                  Heures sup à valider
                  <span aria-hidden="true">ⓘ</span>
                </span>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="ligne in lignes" :key="ligne.idEmploye">
              <td>{{ ligne.matricule }}</td>
              <td>{{ ligne.nom }}</td>
              <td>{{ ligne.prenom }}</td>
              <td>{{ ligne.tempsPrevu }}</td>
              <td>{{ ligne.tempsTravaille }}</td>
              <td>{{ ligne.retard }}</td>
              <td>{{ ligne.nbRetards }}</td>
              <td>{{ ligne.heuresSupValidees }}</td>
              <td class="variables-paie-a-valider">{{ ligne.heuresSupAValider }}</td>
            </tr>
          </tbody>
          <tfoot>
            <tr>
              <th colspan="3">Total</th>
              <th>{{ totaux.tempsPrevu }}</th>
              <th>{{ totaux.tempsTravaille }}</th>
              <th>{{ totaux.retard }}</th>
              <th>{{ totaux.nbRetards }}</th>
              <th>{{ totaux.heuresSupValidees }}</th>
              <th class="variables-paie-a-valider">{{ totaux.heuresSupAValider }}</th>
            </tr>
          </tfoot>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.variables-paie-page {
  padding-bottom: 24px;
}

.variables-paie-filtres {
  align-items: end;
}

.variables-paie-filtre {
  display: grid;
  gap: 6px;
  min-width: 150px;
  color: var(--color-ink);
  font-size: 13px;
  font-weight: 600;
}

.variables-paie-filtre--departement {
  min-width: 220px;
}

.variables-paie-table {
  min-width: 1040px;
}

.variables-paie-table tfoot th {
  padding: 14px 16px;
  text-align: left;
  color: var(--color-ink);
  background: var(--color-subtle);
  border-top: 2px solid var(--color-border-strong);
}

.variables-paie-aide {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  cursor: help;
}

.variables-paie-aide span {
  color: var(--color-muted);
  font-size: 12px;
}

.variables-paie-a-valider {
  color: #8a4b08 !important;
  background: #fff3df;
  font-weight: 700;
}

.variables-paie-table tfoot .variables-paie-a-valider {
  background: #ffe8c2;
}
</style>
