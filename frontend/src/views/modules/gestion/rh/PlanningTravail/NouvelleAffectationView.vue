<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import Notification from "@/components/ui/Notification.vue";
import PageEntete from "@/components/gestion/PageEntete.vue";
import { planningTravailApi } from "@/services/planningTravail";
import "@/assets/gestion.css";

const router = useRouter();
const options = ref({ employes: [], horaires: [], regimes: [] });
const recherche = ref("");
const formulaire = ref({
  idEmploye: "",
  idHoraire: "",
  idRegime: "",
  dateDebut: new Date().toISOString().slice(0, 10),
  dateFin: "",
});
const dateFinActuelle = ref(null);
const chargement = ref(true);
const envoi = ref(false);
const notification = ref({ message: "", type: "erreur" });
let sequenceApercu = 0;

const employesFiltres = computed(() => {
  const terme = recherche.value.trim().toLocaleLowerCase("fr");
  return options.value.employes.filter((employe) =>
    !terme || `${employe.nom} ${employe.prenom} ${employe.matricule}`.toLocaleLowerCase("fr").includes(terme));
});

const erreurPeriode = computed(() =>
  formulaire.value.dateFin && formulaire.value.dateFin < formulaire.value.dateDebut
    ? "La date de fin doit être égale ou postérieure à la date de début."
    : "",
);

function notifier(message, type = "erreur") {
  notification.value = { message, type };
}

function dateFr(date) {
  if (!date) return "";
  return new Intl.DateTimeFormat("fr-FR", { dateStyle: "long", timeZone: "UTC" })
    .format(new Date(`${date}T00:00:00Z`));
}

watch(
  () => [formulaire.value.idEmploye, formulaire.value.dateDebut],
  async ([idEmploye, dateDebut]) => {
    const requete = ++sequenceApercu;
    dateFinActuelle.value = null;
    if (!idEmploye || !dateDebut) return;
    try {
      const apercu = await planningTravailApi.apercu(idEmploye, dateDebut);
      if (requete === sequenceApercu) dateFinActuelle.value = apercu.dateFinPlanningActuel;
    } catch (error) {
      if (requete === sequenceApercu) {
        notifier(error.message || "Impossible de vérifier le planning actuellement ouvert.");
      }
    }
  },
);

onMounted(async () => {
  try {
    options.value = await planningTravailApi.options();
    if (options.value.employes.length === 0) {
      notifier("Aucun salarié actif ou suspendu n'est disponible pour une nouvelle affectation.");
    }
  } catch (error) {
    notifier(error.message || "Impossible de charger les données du formulaire.");
  } finally {
    chargement.value = false;
  }
});

async function soumettre() {
  if (!formulaire.value.idEmploye || !formulaire.value.idHoraire || !formulaire.value.dateDebut) {
    notifier("Sélectionnez un salarié, un horaire et une date de début.");
    return;
  }
  if (erreurPeriode.value) {
    notifier(erreurPeriode.value);
    return;
  }

  envoi.value = true;
  try {
    await planningTravailApi.affecter({
      idEmploye: Number(formulaire.value.idEmploye),
      idHoraire: Number(formulaire.value.idHoraire),
      idRegime: formulaire.value.idRegime ? Number(formulaire.value.idRegime) : null,
      dateDebut: formulaire.value.dateDebut,
      dateFin: formulaire.value.dateFin || null,
    });
    await router.push({ path: "/si/app/rh/plannings", query: { succes: "cree" } });
  } catch (error) {
    notifier(error.message || "Impossible de créer cette affectation.");
  } finally {
    envoi.value = false;
  }
}
</script>

<template>
  <div class="g-page affectation-page">
    <Notification
      :message="notification.message"
      :type="notification.type"
      @fermer="notification.message = ''"
    />

    <PageEntete
      titre="Nouvelle affectation"
      sous-titre="Affectez un horaire de travail à un salarié pour une période donnée."
    >
      <RouterLink class="g-btn" to="/si/app/rh/plannings">← Retour aux plannings</RouterLink>
    </PageEntete>

    <p v-if="chargement" class="g-vide">Chargement des options…</p>
    <form v-else class="g-carte g-form" @submit.prevent="soumettre">
      <div class="g-champ g-champ--large">
        <label for="planning-recherche-salarie">Rechercher un salarié (nom ou matricule)</label>
        <input
          id="planning-recherche-salarie"
          v-model="recherche"
          class="g-input"
          type="search"
          placeholder="Commencez à saisir un nom ou un matricule…"
        />
      </div>

      <div class="g-champ g-champ--large">
        <label for="planning-salarie" class="g-obligatoire">Salarié</label>
        <select id="planning-salarie" v-model="formulaire.idEmploye" class="g-input" required>
          <option value="" disabled>Sélectionner un salarié…</option>
          <option v-for="employe in employesFiltres" :key="employe.id" :value="String(employe.id)">
            {{ employe.nom }} {{ employe.prenom }} — {{ employe.matricule }}
          </option>
        </select>
        <small>Les salariés au statut PARTI ne sont pas proposés.</small>
      </div>

      <div class="g-champ">
        <label for="planning-horaire" class="g-obligatoire">Horaire</label>
        <select id="planning-horaire" v-model="formulaire.idHoraire" class="g-input" required>
          <option value="" disabled>Sélectionner un horaire actif…</option>
          <option v-for="horaire in options.horaires" :key="horaire.id" :value="String(horaire.id)">
            {{ horaire.libelle }} ({{ horaire.code }})
          </option>
        </select>
      </div>

      <div class="g-champ">
        <label for="planning-regime">Régime (facultatif)</label>
        <select id="planning-regime" v-model="formulaire.idRegime" class="g-input">
          <option value="">Aucun régime</option>
          <option v-for="regime in options.regimes" :key="regime.id" :value="String(regime.id)">
            {{ regime.libelle }} ({{ regime.code }})
          </option>
        </select>
      </div>

      <div class="g-champ">
        <label for="planning-date-debut" class="g-obligatoire">Date de début</label>
        <input id="planning-date-debut" v-model="formulaire.dateDebut" class="g-input" type="date" required />
      </div>

      <div class="g-champ">
        <label for="planning-date-fin">Date de fin (facultative)</label>
        <input id="planning-date-fin" v-model="formulaire.dateFin" class="g-input" type="date" />
      </div>

      <p v-if="erreurPeriode" class="g-champ--large planning-erreur" role="alert">{{ erreurPeriode }}</p>
      <p v-else-if="dateFinActuelle" class="g-champ--large planning-apercu" role="status">
        Le planning actuel se terminera le <strong>{{ dateFr(dateFinActuelle) }}</strong>.
      </p>
      <p v-else-if="formulaire.idEmploye && formulaire.dateDebut" class="g-champ--large planning-note">
        Aucun planning ouvert antérieur ne sera clôturé.
      </p>

      <div class="g-form__actions">
        <RouterLink class="g-btn" to="/si/app/rh/plannings">Annuler</RouterLink>
        <button
          class="g-btn g-btn--primaire"
          type="submit"
          :disabled="envoi || Boolean(erreurPeriode) || employesFiltres.length === 0"
        >
          {{ envoi ? "Enregistrement…" : "Valider l'affectation" }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.affectation-page {
  max-width: 900px;
}

.planning-apercu,
.planning-note,
.planning-erreur {
  margin: 0;
  padding: 14px 16px;
  border-radius: var(--radius-sm);
  font-size: 14px;
}

.planning-apercu {
  color: #155724;
  background: #eaf6ed;
  border: 1px solid #acd4b4;
}

.planning-note {
  color: var(--color-muted);
  background: var(--color-subtle);
}

.planning-erreur {
  color: var(--color-danger);
  background: var(--color-danger-background);
  border: 1px solid var(--color-danger-line);
}
</style>
