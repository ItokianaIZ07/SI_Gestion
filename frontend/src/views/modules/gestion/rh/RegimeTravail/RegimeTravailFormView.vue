<script setup>
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import Notification from "@/components/ui/Notification.vue";
import PageEntete from "@/components/gestion/PageEntete.vue";
import RegimeTravailForm from "@/components/rh/RegimeTravail/RegimeTravailForm.vue";
import { regimeTravailApi } from "@/services/regimeTravail";
import "@/assets/gestion.css";

const route = useRoute();
const router = useRouter();
const regime = ref(null);
const chargement = ref(false);
const envoi = ref(false);
const notification = ref({ message: "", type: "erreur" });
const id = computed(() => (route.params.id ? Number(route.params.id) : null));
const edition = computed(() => id.value !== null);

function notifier(message, type = "erreur") {
  notification.value = { message, type };
}

watch(
  id,
  async (regimeId) => {
    regime.value = null;
    if (regimeId === null) return;

    chargement.value = true;
    try {
      regime.value = await regimeTravailApi.parId(regimeId);
    } catch (error) {
      notifier(error.message || "Impossible de charger ce régime.");
    } finally {
      chargement.value = false;
    }
  },
  { immediate: true },
);

async function enregistrer(valeurs) {
  envoi.value = true;
  try {
    await (edition.value
      ? regimeTravailApi.modifier(id.value, valeurs)
      : regimeTravailApi.creer(valeurs));
    await router.push({
      path: "/si/app/rh/regimes-travail",
      query: { succes: edition.value ? "modifie" : "cree" },
    });
  } catch (error) {
    notifier(error.message || "Impossible d'enregistrer ce régime.");
  } finally {
    envoi.value = false;
  }
}
</script>

<template>
  <div class="g-page rt-form-page">
    <Notification
      :message="notification.message"
      :type="notification.type"
      @fermer="notification.message = ''"
    />

    <PageEntete
      :titre="edition ? 'Modifier un régime de travail' : 'Nouveau régime de travail'"
      sous-titre="Les heures hebdomadaires doivent être supérieures à 0 et ne pas dépasser 60."
    >
      <RouterLink class="g-btn" to="/si/app/rh/regimes-travail">← Retour à la liste</RouterLink>
    </PageEntete>

    <p v-if="chargement" class="g-vide">Chargement du régime…</p>
    <RegimeTravailForm
      v-else-if="!edition || regime"
      :regime="regime"
      :envoi="envoi"
      @enregistrer="enregistrer"
      @annuler="router.push('/si/app/rh/regimes-travail')"
    />
  </div>
</template>

<style scoped>
.rt-form-page {
  max-width: 900px;
}
</style>
