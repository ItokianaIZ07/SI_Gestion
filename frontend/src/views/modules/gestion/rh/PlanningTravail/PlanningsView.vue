<script setup>
import { computed, onMounted, ref } from "vue";
import { useAuthStore } from "@/stores/authStore";
import { useRoute, useRouter } from "vue-router";
import Notification from "@/components/ui/Notification.vue";
import Pagination from "@/components/ui/Pagination.vue";
import PageEntete from "@/components/gestion/PageEntete.vue";
import { planningTravailApi } from "@/services/planningTravail";
import { usePagination } from "@/composable/usePagination";
import "@/assets/gestion.css";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const estRh = computed(() => authStore.user?.role?.toUpperCase() === "RH");
const plannings = ref([]);
const options = ref({ departements: [] });
const rechercheSalarie = ref("");
const idDepartement = ref("");
const periode = ref("tous");
const chargement = ref(true);
const notification = ref({ message: "", type: "erreur" });
const aujourdHui = new Date().toISOString().slice(0, 10);

const planningsFiltres = computed(() => {
  const recherche = rechercheSalarie.value.trim().toLocaleLowerCase("fr");
  return plannings.value.filter((planning) => {
    if (recherche
        && !`${planning.nom} ${planning.prenom} ${planning.matricule}`.toLocaleLowerCase("fr").includes(recherche)) {
      return false;
    }
    if (idDepartement.value && String(planning.idDepartement) !== idDepartement.value) return false;
    if (periode.value === "en-cours" && !planning.enCours) return false;
    if (periode.value === "passes" && !(planning.dateFin && planning.dateFin < aujourdHui)) return false;
    if (periode.value === "futurs" && !(planning.dateDebut > aujourdHui)) return false;
    return true;
  });
});
const pagination = usePagination(planningsFiltres, 10);

function notifier(message, type = "erreur") {
  notification.value = { message, type };
}

function dateFr(date) {
  if (!date) return "—";
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "medium", timeZone: "UTC" })
    .format(new Date(`${date}T00:00:00Z`));
}

async function charger() {
  chargement.value = true;
  try {
    const [liste, donneesOptions] = await Promise.all([
      planningTravailApi.liste(),
      planningTravailApi.options(),
    ]);
    plannings.value = liste ?? [];
    options.value = donneesOptions ?? { departements: [] };
  } catch (error) {
    notifier(error.message || "Impossible de charger les plannings.");
  } finally {
    chargement.value = false;
  }
}

onMounted(async () => {
  if (route.query.succes === "cree") notifier("La nouvelle affectation a été enregistrée.", "succes");
  if (route.query.succes) await router.replace({ path: route.path });
  await charger();
});
</script>

<template>
  <div class="g-page planning-page">
    <Notification
      :message="notification.message"
      :type="notification.type"
      @fermer="notification.message = ''"
    />

    <PageEntete
      titre="Plannings de travail"
      sous-titre="Consultez les affectations et l’historique des horaires des salariés."
    >
      <RouterLink
        v-if="estRh"
        class="g-btn g-btn--primaire"
        to="/si/app/rh/plannings/nouvelle-affectation"
      >
        + Nouvelle affectation
      </RouterLink>
    </PageEntete>

    <section class="g-carte">
      <div class="g-filtres planning-filtres">
        <label class="planning-filtre">
          Salarié
          <input
            v-model="rechercheSalarie"
            class="g-input"
            type="search"
            placeholder="Nom, prénom ou matricule"
          />
        </label>
        <label class="planning-filtre">
          Département
          <select v-model="idDepartement" class="g-input">
            <option value="">Tous les départements</option>
            <option v-for="departement in options.departements" :key="departement.id" :value="String(departement.id)">
              {{ departement.nom }}
            </option>
          </select>
        </label>
        <label class="planning-filtre">
          Période
          <select v-model="periode" class="g-input">
            <option value="tous">Toutes</option>
            <option value="en-cours">En cours</option>
            <option value="passes">Passés</option>
            <option value="futurs">Futurs</option>
          </select>
        </label>
        <span class="g-compteur">{{ planningsFiltres.length }} planning(s)</span>
      </div>

      <p v-if="chargement" class="g-vide">Chargement des plannings…</p>
      <template v-else-if="planningsFiltres.length">
        <div class="g-table-wrap">
          <table class="g-table planning-table">
            <thead>
              <tr>
                <th>Salarié</th>
                <th>Département</th>
                <th>Horaire</th>
                <th>Régime</th>
                <th>Début</th>
                <th>Fin</th>
                <th>Statut</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="planning in pagination.items" :key="planning.id">
                <td>
                  <RouterLink
                    class="planning-salarie"
                    :to="`/si/app/rh/plannings/salarie/${planning.idEmploye}`"
                  >
                    {{ planning.prenom }} {{ planning.nom }}
                  </RouterLink>
                  <small class="g-secondaire">{{ planning.matricule }}</small>
                </td>
                <td>{{ planning.departement }}</td>
                <td>{{ planning.horaire }}</td>
                <td>{{ planning.regime || "—" }}</td>
                <td>{{ dateFr(planning.dateDebut) }}</td>
                <td>{{ dateFr(planning.dateFin) }}</td>
                <td>
                  <span
                    class="g-badge"
                    :class="planning.enCours ? 'g-badge--success' : planning.dateDebut > aujourdHui ? 'g-badge--warning' : 'g-badge--neutral'"
                  >
                    {{ planning.enCours ? "En cours" : planning.dateDebut > aujourdHui ? "À venir" : "Terminé" }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <Pagination
          v-bind="pagination.barre"
          @update:page="pagination.aller"
        />
      </template>
      <p v-else class="g-vide">
        <strong>Aucun planning à afficher</strong>
        Aucun résultat ne correspond aux filtres sélectionnés.
      </p>
    </section>
  </div>
</template>

<style scoped>
.planning-page {
  padding-bottom: 24px;
}

.planning-filtres {
  align-items: end;
}

.planning-filtre {
  display: grid;
  gap: 6px;
  min-width: 190px;
  color: var(--color-ink);
  font-size: 13px;
  font-weight: 600;
}

.planning-table {
  min-width: 900px;
}

.planning-salarie {
  color: var(--color-primary);
  font-weight: 650;
  text-decoration: none;
}

.planning-salarie:hover {
  text-decoration: underline;
}
</style>
