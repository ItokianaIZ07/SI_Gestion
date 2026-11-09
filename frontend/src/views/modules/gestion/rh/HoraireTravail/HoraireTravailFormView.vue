<script setup>
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import Notification from "@/components/ui/Notification.vue";
import PageEntete from "@/components/gestion/PageEntete.vue";
import HoraireTravailForm from "@/components/rh/HoraireTravail/HoraireTravailForm.vue";
import { horaireTravailApi } from "@/services/horaireTravail";
import "@/assets/gestion.css";

const route = useRoute();
const router = useRouter();
const horaire = ref(null);
const nbPlannings = ref(0);
const chargement = ref(false);
const envoi = ref(false);
const notification = ref({ message: "", type: "erreur" });
const id = computed(() => route.params.id ? Number(route.params.id) : null);
const edition = computed(() => id.value !== null);

function notifier(message, type = "erreur") {
  notification.value = { message, type };
}

watch(
  id,
  async (horaireId) => {
    horaire.value = null;
    nbPlannings.value = 0;
    if (horaireId === null) return;
    chargement.value = true;
    try {
      const [detail, liste] = await Promise.all([
        horaireTravailApi.parId(horaireId),
        horaireTravailApi.liste(),
      ]);
      horaire.value = detail;
      nbPlannings.value = Number(liste.find((item) => item.id === horaireId)?.nbPlannings ?? 0);
    } catch (error) {
      notifier(error.message || "Impossible de charger cet horaire.");
    } finally {
      chargement.value = false;
    }
  },
  { immediate: true },
);

async function enregistrer(valeurs) {
  if (edition.value && nbPlannings.value > 0
      && !window.confirm(`Cet horaire est utilisé dans ${nbPlannings.value} planning(s). Le modifier peut changer les horaires prévus. Confirmer la modification ?`)) {
    return;
  }

  envoi.value = true;
  try {
    if (edition.value) await horaireTravailApi.modifier(id.value, valeurs);
    else await horaireTravailApi.creer(valeurs);
    await router.push({
      path: "/si/app/rh/horaires-travail",
      query: { succes: edition.value ? "modifie" : "cree" },
    });
  } catch (error) {
    notifier(error.message || "Impossible d'enregistrer l'horaire.");
  } finally {
    envoi.value = false;
  }
}
</script>

<template>
  <div class="g-page ht-form-page">
    <Notification
      :message="notification.message"
      :type="notification.type"
      @fermer="notification.message = ''"
    />

    <PageEntete
      :titre="edition ? 'Modifier un horaire de travail' : 'Nouvel horaire de travail'"
      sous-titre="Définissez les heures de la journée, la pause et les jours travaillés."
    >
      <RouterLink class="g-btn" to="/si/app/rh/horaires-travail">← Retour à la liste</RouterLink>
    </PageEntete>

    <aside v-if="edition && nbPlannings > 0" class="ht-avertissement" role="status">
      Cet horaire est actuellement utilisé dans <strong>{{ nbPlannings }} planning(s)</strong>.
      Une confirmation sera demandée avant d’enregistrer une modification.
    </aside>

    <p v-if="chargement" class="g-vide">Chargement de l'horaire…</p>
    <HoraireTravailForm
      v-else-if="!edition || horaire"
      :horaire="horaire"
      :envoi="envoi"
      @enregistrer="enregistrer"
      @annuler="router.push('/si/app/rh/horaires-travail')"
    />
  </div>
</template>

<style scoped>
.ht-form-page {
  max-width: 900px;
}

.ht-avertissement {
  padding: 14px 18px;
  color: #704b00;
  background: #fff7df;
  border: 1px solid #e8c86b;
  border-radius: var(--radius-sm);
  font-size: 14px;
}
</style>
