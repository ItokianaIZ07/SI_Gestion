<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useAuthStore } from "@/stores/authStore";
import Notification from "@/components/ui/Notification.vue";
import PageEntete from "@/components/gestion/PageEntete.vue";
import { jourFerieApi } from "@/services/jourFerie";
import "@/assets/gestion.css";

const anneeCourante = new Date().getFullYear();
const authStore = useAuthStore();
const estRh = authStore.user?.role?.toUpperCase() === "RH";
const annee = ref(anneeCourante);
const annees = ref([]);
const joursFeries = ref([]);
const chargement = ref(false);
const envoi = ref(false);
const dateEdition = ref(null);
const brouillonDate = ref("");
const brouillonLibelle = ref("");
const formulaire = reactive({ dateJour: "", libelle: "" });
const notification = ref({ message: "", type: "erreur" });

const anneesDisponibles = computed(() => {
  return [...new Set([anneeCourante, ...annees.value])].sort((a, b) => b - a);
});

function notifier(message, type = "erreur") {
  notification.value = { message, type };
}

function afficherDate(date) {
  return new Intl.DateTimeFormat("fr-FR", {
    day: "2-digit",
    month: "long",
    year: "numeric",
    timeZone: "UTC",
  }).format(new Date(`${date}T00:00:00Z`));
}

function nomJourISO(jourSemaine) {
  return ["", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"][jourSemaine]
    ?? "—";
}

async function chargerAnnees() {
  try {
    annees.value = await jourFerieApi.annees();
  } catch (error) {
    notifier(error.message || "Impossible de charger les années disponibles.");
  }
}

async function chargerJours() {
  chargement.value = true;
  try {
    joursFeries.value = (await jourFerieApi.liste(annee.value)) ?? [];
  } catch (error) {
    notifier(error.message || "Impossible de charger les jours fériés.");
  } finally {
    chargement.value = false;
  }
}

watch(annee, chargerJours);

async function ajouter() {
  if (!formulaire.dateJour || !formulaire.libelle.trim()) {
    notifier("Renseignez une date et un libellé.");
    return;
  }

  envoi.value = true;
  try {
    await jourFerieApi.ajouter({
      dateJour: formulaire.dateJour,
      libelle: formulaire.libelle.trim(),
    });
    formulaire.dateJour = "";
    formulaire.libelle = "";
    notifier("Le jour férié a été ajouté.", "succes");
    await Promise.all([chargerAnnees(), chargerJours()]);
  } catch (error) {
    notifier(error.message === "Ce jour est déjà férié"
      ? "Ce jour existe déjà dans la liste des jours fériés."
      : error.message || "Impossible d'ajouter ce jour férié.");
  } finally {
    envoi.value = false;
  }
}

function commencerModification(jourFerie) {
  dateEdition.value = jourFerie.dateJour;
  brouillonDate.value = jourFerie.dateJour;
  brouillonLibelle.value = jourFerie.libelle;
}

function annulerModification() {
  dateEdition.value = null;
  brouillonDate.value = "";
  brouillonLibelle.value = "";
}

async function enregistrerModification(jourFerie) {
  if (!brouillonDate.value || !brouillonLibelle.value.trim()) {
    notifier("La date et le libellé sont obligatoires.");
    return;
  }
  envoi.value = true;
  try {
    await jourFerieApi.modifier(jourFerie.dateJour, {
      dateJour: brouillonDate.value,
      libelle: brouillonLibelle.value.trim(),
    });
    annulerModification();
    notifier("Le jour férié a été modifié.", "succes");
    await Promise.all([chargerAnnees(), chargerJours()]);
  } catch (error) {
    notifier(error.message === "Ce jour est déjà férié"
      ? "Ce jour existe déjà dans la liste des jours fériés."
      : error.message || "Impossible de modifier ce jour férié.");
  } finally {
    envoi.value = false;
  }
}

async function supprimer(jourFerie) {
  if (!window.confirm(`Supprimer le jour férié du ${afficherDate(jourFerie.dateJour)} (« ${jourFerie.libelle} ») ?`)) {
    return;
  }
  envoi.value = true;
  try {
    await jourFerieApi.supprimer(jourFerie.dateJour);
    notifier("Le jour férié a été supprimé.", "succes");
    await Promise.all([chargerAnnees(), chargerJours()]);
  } catch (error) {
    notifier(error.message || "Impossible de supprimer ce jour férié.");
  } finally {
    envoi.value = false;
  }
}

onMounted(async () => {
  await Promise.all([chargerAnnees(), chargerJours()]);
});
</script>

<template>
  <div class="g-page jf-page">
    <Notification
      :message="notification.message"
      :type="notification.type"
      @fermer="notification.message = ''"
    />

    <PageEntete
      titre="Jours fériés"
      :sous-titre="estRh
        ? 'Gérez les jours fériés et repérez ceux qui tombent pendant le week-end.'
        : 'Consultez les jours fériés et repérez ceux qui tombent pendant le week-end.'"
    />

    <section v-if="estRh" class="g-carte jf-ajout">
      <h2>Ajouter un jour férié</h2>
      <form class="jf-formulaire" @submit.prevent="ajouter">
        <label class="g-champ">
          <span class="g-obligatoire">Date</span>
          <input v-model="formulaire.dateJour" class="g-input" type="date" required />
        </label>
        <label class="g-champ">
          <span class="g-obligatoire">Libellé</span>
          <input
            v-model="formulaire.libelle"
            class="g-input"
            type="text"
            maxlength="100"
            placeholder="Ex. Fête nationale"
            required
          />
        </label>
        <button class="g-btn g-btn--primaire" type="submit" :disabled="envoi">
          {{ envoi ? "Ajout…" : "Ajouter" }}
        </button>
      </form>
    </section>

    <section class="g-carte">
      <div class="g-filtres">
        <label for="jf-annee">Année</label>
        <select id="jf-annee" v-model.number="annee" class="g-input">
          <option v-for="valeur in anneesDisponibles" :key="valeur" :value="valeur">{{ valeur }}</option>
        </select>
        <span class="g-compteur">{{ joursFeries.length }} jour(s) férié(s)</span>
      </div>

      <p v-if="chargement" class="g-vide">Chargement des jours fériés…</p>
      <div v-else-if="joursFeries.length" class="g-table-wrap">
        <table class="g-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Jour de la semaine</th>
              <th>Libellé</th>
              <th v-if="estRh" class="g-droite">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="jourFerie in joursFeries"
              :key="jourFerie.dateJour"
              :class="{ 'jf-weekend': jourFerie.jourSemaine >= 6 }"
            >
              <td>
                <input
                  v-if="estRh && dateEdition === jourFerie.dateJour"
                  v-model="brouillonDate"
                  class="g-input jf-date-edition"
                  type="date"
                  aria-label="Modifier la date"
                  @keyup.enter="enregistrerModification(jourFerie)"
                  @keyup.esc="annulerModification"
                />
                <span v-else>{{ afficherDate(jourFerie.dateJour) }}</span>
              </td>
              <td>
                <span>{{ nomJourISO(jourFerie.jourSemaine) }}</span>
                <span v-if="jourFerie.jourSemaine >= 6" class="jf-badge-weekend">
                  Week-end
                </span>
              </td>
              <td>
                <input
                  v-if="estRh && dateEdition === jourFerie.dateJour"
                  v-model="brouillonLibelle"
                  class="g-input jf-libelle-edition"
                  type="text"
                  maxlength="100"
                  aria-label="Modifier le libellé"
                  @keyup.enter="enregistrerModification(jourFerie)"
                  @keyup.esc="annulerModification"
                />
                <span v-else class="g-principal">{{ jourFerie.libelle }}</span>
              </td>
              <td v-if="estRh">
                <div class="g-actions-cellule">
                  <template v-if="dateEdition === jourFerie.dateJour">
                    <button class="g-btn g-btn--petit g-btn--primaire" type="button" :disabled="envoi"
                      @click="enregistrerModification(jourFerie)">Enregistrer</button>
                    <button class="g-btn g-btn--petit" type="button" :disabled="envoi"
                      @click="annulerModification">Annuler</button>
                  </template>
                  <template v-else>
                    <button class="g-btn g-btn--petit" type="button" @click="commencerModification(jourFerie)">
                      Modifier
                    </button>
                    <button class="g-btn g-btn--petit g-btn--danger" type="button" :disabled="envoi"
                      @click="supprimer(jourFerie)">Supprimer</button>
                  </template>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="g-vide">
        <strong>Aucun jour férié pour {{ annee }}</strong>
        {{ estRh ? "Ajoutez un jour férié à cette année à l’aide du formulaire ci-dessus." : "Aucun jour férié n'est enregistré pour cette année." }}
      </p>
    </section>
  </div>
</template>

<style scoped>
.jf-page {
  padding-bottom: 24px;
}

.jf-ajout {
  padding: 20px;
}

.jf-ajout h2 {
  margin: 0 0 16px;
  color: var(--color-ink);
  font-size: 1.05rem;
}

.jf-formulaire {
  display: grid;
  grid-template-columns: minmax(180px, 0.8fr) minmax(240px, 1.5fr) auto;
  align-items: end;
  gap: 14px;
}

.jf-weekend {
  background: #fff8e7;
}

.jf-badge-weekend {
  display: inline-block;
  margin-left: 8px;
  padding: 3px 8px;
  color: #825600;
  background: #ffedbd;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}

.jf-libelle-edition {
  min-width: 220px;
}

.jf-date-edition {
  min-width: 160px;
}

@media (max-width: 700px) {
  .jf-formulaire {
    grid-template-columns: 1fr;
  }
}
</style>
