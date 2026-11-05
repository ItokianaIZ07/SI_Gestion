<script setup>
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import Notification from "@/components/ui/Notification.vue";
import ConfirmationSuppressionRegime from "@/components/rh/RegimeTravail/ConfirmationSuppressionRegime.vue";
import PageEntete from "@/components/gestion/PageEntete.vue";
import { regimeTravailApi } from "@/services/regimeTravail";
import "@/assets/gestion.css";

const route = useRoute();
const router = useRouter();

const regimes = ref([]);
const chargement = ref(true);
const envoi = ref(false);
const regimeASupprimer = ref(null);
const notification = ref({ message: "", type: "erreur" });

function notifier(message, type = "erreur") {
  notification.value = { message, type };
}

async function charger() {
  chargement.value = true;
  try {
    regimes.value = (await regimeTravailApi.liste()) ?? [];
  } catch (error) {
    notifier(error.message || "Impossible de charger les régimes de travail.");
  } finally {
    chargement.value = false;
  }
}

async function confirmerSuppression() {
  const regime = regimeASupprimer.value;
  if (!regime) return;

  envoi.value = true;
  try {
    await regimeTravailApi.supprimer(regime.id);
    regimeASupprimer.value = null;
    notifier("Le régime de travail a été supprimé.", "succes");
    await charger();
  } catch (error) {
    regimeASupprimer.value = null;
    notifier(error.message || "Impossible de supprimer ce régime.");
  } finally {
    envoi.value = false;
  }
}

function modifier(regime) {
  router.push(`/si/app/rh/regimes-travail/${regime.id}/modifier`);
}

onMounted(async () => {
  if (route.query.succes === "cree") {
    notifier("Le régime de travail a été créé.", "succes");
  } else if (route.query.succes === "modifie") {
    notifier("Les modifications ont été enregistrées.", "succes");
  }
  if (route.query.succes) {
    await router.replace({ path: route.path });
  }
  await charger();
});
</script>

<template>
  <div class="g-page rt-page">
    <Notification
      :message="notification.message"
      :type="notification.type"
      @fermer="notification.message = ''"
    />

    <PageEntete
      titre="Régimes de travail"
      sous-titre="Consultez et gérez les régimes ainsi que leur utilisation dans les plannings."
    >
      <RouterLink class="g-btn g-btn--primaire" to="/si/app/rh/regimes-travail/formulaire">
        + Nouveau régime
      </RouterLink>
    </PageEntete>

    <section class="g-carte">
      <p v-if="chargement" class="g-vide rt-etat">Chargement des régimes…</p>
      <div v-else-if="regimes.length" class="g-table-wrap">
        <table class="g-table">
          <thead>
            <tr>
              <th>Code</th>
              <th>Libellé</th>
              <th>Heures hebdomadaires</th>
              <th>Nombre de plannings</th>
              <th class="g-droite">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="regime in regimes" :key="regime.id">
              <td class="g-principal">{{ regime.code }}</td>
              <td>{{ regime.libelle }}</td>
              <td>
                {{ regime.heuresHebdomadaires == null ? "—" : `${regime.heuresHebdomadaires} h` }}
              </td>
              <td>{{ regime.nbPlannings ?? 0 }}</td>
              <td>
                <div class="g-actions-cellule">
                  <button class="g-btn g-btn--petit" type="button" @click="modifier(regime)">
                    Modifier
                  </button>
                  <span
                    class="rt-info-suppression"
                    :title="
                      regime.code === 'TEMPS_PLEIN'
                        ? 'Le régime TEMPS_PLEIN ne peut pas être supprimé.'
                        : regime.nbPlannings > 0
                          ? `Ce régime est affecté à ${regime.nbPlannings} plannings.`
                          : ''
                    "
                  >
                    <button
                      class="g-btn g-btn--petit g-btn--danger"
                      type="button"
                      :disabled="regime.code === 'TEMPS_PLEIN' || regime.nbPlannings > 0"
                      @click="regimeASupprimer = regime"
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
      <p v-else class="g-vide rt-etat">
        <strong>Aucun régime de travail</strong>
        Créez un régime pour pouvoir l'affecter aux plannings.
      </p>
    </section>

    <ConfirmationSuppressionRegime
      :regime="regimeASupprimer"
      :envoi="envoi"
      @confirmer="confirmerSuppression"
      @annuler="regimeASupprimer = null"
    />
  </div>
</template>

<style scoped>
.rt-page {
  padding-bottom: 24px;
}

.rt-etat {
  margin: 0;
}

.rt-info-suppression {
  display: inline-flex;
}
</style>
