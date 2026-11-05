<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import Notification from "@/components/ui/Notification.vue";
import PageEntete from "@/components/gestion/PageEntete.vue";
import { horaireTravailApi } from "@/services/horaireTravail";
import "@/assets/gestion.css";

const route = useRoute();
const router = useRouter();
const horaires = ref([]);
const filtreStatut = ref("tous");
const chargement = ref(true);
const envoiId = ref(null);
const notification = ref({ message: "", type: "erreur" });
const jours = ["L", "M", "M", "J", "V", "S", "D"];

const horairesFiltres = computed(() => horaires.value.filter((horaire) => {
  if (filtreStatut.value === "actif") return horaire.actif;
  if (filtreStatut.value === "inactif") return !horaire.actif;
  return true;
}));

function notifier(message, type = "erreur") {
  notification.value = { message, type };
}

function joursFormates(valeurs) {
  const selection = new Set((valeurs ?? []).map(Number));
  return jours.map((jour, index) => selection.has(index + 1) ? jour : "·").join(" ");
}

function dureeFormatee(minutes) {
  if (minutes == null) return "—";
  const valeur = Number(minutes);
  if (!Number.isFinite(valeur)) return "—";
  return `${Math.floor(valeur / 60)} h ${String(Math.round(valeur % 60)).padStart(2, "0")} min`;
}

async function charger() {
  chargement.value = true;
  try {
    horaires.value = (await horaireTravailApi.liste()) ?? [];
  } catch (error) {
    notifier(error.message || "Impossible de charger les horaires.");
  } finally {
    chargement.value = false;
  }
}

function modifier(horaire) {
  router.push(`/si/app/rh/horaires-travail/${horaire.id}/modifier`);
}

async function changerStatut(horaire) {
  envoiId.value = horaire.id;
  try {
    if (horaire.actif) await horaireTravailApi.desactiver(horaire.id);
    else await horaireTravailApi.reactiver(horaire.id);
    notifier(`L'horaire a été ${horaire.actif ? "désactivé" : "réactivé"}.`, "succes");
    await charger();
  } catch (error) {
    notifier(error.message || "Impossible de modifier le statut de l'horaire.");
  } finally {
    envoiId.value = null;
  }
}

async function supprimer(horaire) {
  if (Number(horaire.nbPlannings) > 0 || horaire.code === "STANDARD") return;
  if (!window.confirm(`Supprimer l'horaire « ${horaire.libelle} » ?`)) return;

  envoiId.value = horaire.id;
  try {
    await horaireTravailApi.supprimer(horaire.id);
    notifier("L'horaire a été supprimé.", "succes");
    await charger();
  } catch (error) {
    notifier(error.message || "Impossible de supprimer cet horaire.");
  } finally {
    envoiId.value = null;
  }
}

onMounted(async () => {
  if (route.query.succes === "cree") notifier("L'horaire a été créé.", "succes");
  if (route.query.succes === "modifie") notifier("L'horaire a été modifié.", "succes");
  if (route.query.succes) await router.replace({ path: route.path });
  await charger();
});
</script>

<template>
  <div class="g-page ht-page">
    <Notification
      :message="notification.message"
      :type="notification.type"
      @fermer="notification.message = ''"
    />

    <PageEntete
      titre="Horaires de travail"
      sous-titre="Gérez les horaires journaliers, leur durée prévue et leur statut."
    >
      <RouterLink class="g-btn g-btn--primaire" to="/si/app/rh/horaires-travail/formulaire">
        + Nouvel horaire
      </RouterLink>
    </PageEntete>

    <section class="g-carte">
      <div class="g-filtres">
        <label for="ht-filtre">Statut</label>
        <select id="ht-filtre" v-model="filtreStatut" class="g-input">
          <option value="tous">Tous</option>
          <option value="actif">Actifs</option>
          <option value="inactif">Inactifs</option>
        </select>
        <span class="g-compteur">{{ horairesFiltres.length }} horaire(s)</span>
      </div>

      <p v-if="chargement" class="g-vide">Chargement des horaires…</p>
      <div v-else-if="horairesFiltres.length" class="g-table-wrap">
        <table class="g-table ht-table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Libellé</th>
              <th>Début</th>
              <th>Fin</th>
              <th>Pause</th>
              <th>Tolérance</th>
              <th>Jours (L M M J V S D)</th>
              <th>Durée prévue</th>
              <th>Statut</th>
              <th class="g-droite">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="horaire in horairesFiltres" :key="horaire.id">
              <td class="g-principal">{{ horaire.code }}</td>
              <td>{{ horaire.libelle }}</td>
              <td>{{ String(horaire.heureDebut ?? "").slice(0, 5) }}</td>
              <td>{{ String(horaire.heureFin ?? "").slice(0, 5) }}</td>
              <td>
                {{ horaire.debutPause
                  ? `${String(horaire.debutPause).slice(0, 5)}–${String(horaire.finPause).slice(0, 5)}`
                  : "Aucune" }}
              </td>
              <td>{{ horaire.toleranceRetardMinutes }} min</td>
              <td class="ht-jours">{{ joursFormates(horaire.joursTravail) }}</td>
              <td>{{ dureeFormatee(horaire.minutesPrevues) }}</td>
              <td>
                <span class="g-badge" :class="horaire.actif ? 'g-badge--success' : 'g-badge--neutral'">
                  {{ horaire.actif ? "Actif" : "Inactif" }}
                </span>
              </td>
              <td>
                <div class="g-actions-cellule">
                  <button class="g-btn g-btn--petit" type="button" @click="modifier(horaire)">
                    Modifier
                  </button>
                  <button
                    class="g-btn g-btn--petit"
                    type="button"
                    :disabled="envoiId === horaire.id"
                    @click="changerStatut(horaire)"
                  >
                    {{ horaire.actif ? "Désactiver" : "Réactiver" }}
                  </button>
                  <span
                    class="ht-info-suppression"
                    :title="
                      horaire.code === 'STANDARD'
                        ? 'L’horaire STANDARD ne peut pas être supprimé.'
                        : Number(horaire.nbPlannings) > 0
                          ? `Utilisé dans ${horaire.nbPlannings} planning(s).`
                          : ''
                    "
                  >
                    <button
                      class="g-btn g-btn--petit g-btn--danger"
                      type="button"
                      :disabled="Number(horaire.nbPlannings) > 0 || horaire.code === 'STANDARD' || envoiId === horaire.id"
                      @click="supprimer(horaire)"
                    >
                      Supprimer
                    </button>
                  </span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="g-vide">
        <strong>Aucun horaire à afficher</strong>
        Aucun horaire ne correspond au filtre sélectionné.
      </p>
    </section>
  </div>
</template>

<style scoped>
.ht-page {
  padding-bottom: 24px;
}

.ht-table {
  min-width: 1180px;
}

.ht-jours {
  white-space: pre;
  font-variant-numeric: tabular-nums;
}

.ht-info-suppression {
  display: inline-flex;
}
</style>
