<script setup>
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import Notification from "@/components/ui/Notification.vue";
import PageEntete from "@/components/gestion/PageEntete.vue";
import { planningTravailApi } from "@/services/planningTravail";
import "@/assets/gestion.css";

const route = useRoute();
const fiche = ref(null);
const plannings = ref([]);
const chargement = ref(true);
const notification = ref({ message: "", type: "erreur" });

function dateFr(date) {
  if (!date) return "—";
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium", timeZone: "UTC" })
    .format(new Date(`${date}T00:00:00Z`));
}

onMounted(async () => {
  try {
    const id = Number(route.params.id);
    const [ficheSalarie, historique] = await Promise.all([
      planningTravailApi.ficheEmploye(id),
      planningTravailApi.historique(id),
    ]);
    fiche.value = ficheSalarie;
    plannings.value = historique ?? [];
  } catch (error) {
    notification.value.message = error.message || "Impossible de charger l'historique du salarié.";
  } finally {
    chargement.value = false;
  }
});
</script>

<template>
  <div class="g-page planning-historique">
    <Notification
      :message="notification.message"
      :type="notification.type"
      @fermer="notification.message = ''"
    />

    <PageEntete
      :titre="fiche ? `Plannings de ${fiche.prenom} ${fiche.nom}` : 'Historique des plannings'"
      :sous-titre="fiche ? `${fiche.matricule} · ${fiche.departement}` : ''"
    >
      <RouterLink class="g-btn" to="/si/app/rh/plannings">← Retour aux plannings</RouterLink>
    </PageEntete>

    <p v-if="chargement" class="g-vide">Chargement de l'historique…</p>
    <section v-else class="g-carte">
      <div v-if="plannings.length" class="g-table-wrap">
        <table class="g-table">
          <thead>
            <tr>
              <th>Horaire</th>
              <th>Régime</th>
              <th>Début</th>
              <th>Fin</th>
              <th>Statut</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="planning in plannings"
              :key="planning.id"
              :class="{ 'planning-courant': planning.enCours }"
            >
              <td class="g-principal">{{ planning.horaire }}</td>
              <td>{{ planning.regime || "—" }}</td>
              <td>{{ dateFr(planning.dateDebut) }}</td>
              <td>{{ dateFr(planning.dateFin) }}</td>
              <td>
                <span
                  class="g-badge"
                  :class="planning.enCours ? 'g-badge--success' : planning.dateDebut > new Date().toISOString().slice(0, 10) ? 'g-badge--warning' : 'g-badge--neutral'"
                >
                  {{ planning.enCours ? "En cours" : planning.dateDebut > new Date().toISOString().slice(0, 10) ? "À venir" : "Terminé" }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="g-vide">
        <strong>Aucun planning enregistré</strong>
        Ce salarié ne possède pas encore d’historique de planning.
      </p>
    </section>
  </div>
</template>

<style scoped>
.planning-historique {
  padding-bottom: 24px;
}

.planning-courant {
  background: #eaf6ed;
  box-shadow: inset 4px 0 var(--color-success);
}
</style>
