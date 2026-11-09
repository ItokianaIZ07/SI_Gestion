<script setup>
import { computed, onMounted, ref } from "vue";
import PageEntete from "@/components/gestion/PageEntete.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import ValiditeBadge from "@/components/rh/ValiditeBadge.vue";
import Pagination from "@/components/ui/Pagination.vue";
import { usePagination } from "@/composable/usePagination";
import { alertesDocumentsApi } from "@/services/employeService";
import { formatDate, libelle } from "@/config/libelles";

const documents = ref([]);
const chargement = ref(true);
const occupe = ref(false);
const erreur = ref("");
const succes = ref("");

const nbExpires = computed(() => documents.value.filter((d) => d.etatValidite === "EXPIRE").length);
const nbBientot = computed(() => documents.value.filter((d) => d.etatValidite === "BIENTOT_EXPIRE").length);
const pg = usePagination(documents, 10);

async function charger() {
  chargement.value = true;
  try {
    documents.value = await alertesDocumentsApi.liste();
  } catch (e) {
    erreur.value = e.message;
  } finally {
    chargement.value = false;
  }
}

async function generer() {
  if (occupe.value) return;
  occupe.value = true;
  erreur.value = "";
  succes.value = "";
  try {
    const nb = await alertesDocumentsApi.generer();
    succes.value = nb === 0 ? "Aucune nouvelle alerte à envoyer (déjà notifiées cette semaine)." : `${nb} notification(s) envoyée(s).`;
  } catch (e) {
    erreur.value = e.message;
  } finally {
    occupe.value = false;
  }
}

onMounted(charger);
</script>

<template>
  <div class="g-page">
    <PageEntete titre="Documents à renouveler" sous-titre="Les alertes partent automatiquement chaque jour à 07h00 : au salarié s'il a un espace personnel, sinon aux RH.">
      <button type="button" class="g-btn g-btn--primaire" :disabled="occupe" @click="generer">
        {{ occupe ? "Envoi…" : "Générer les alertes maintenant" }}
      </button>
    </PageEntete>

    <MessageAlerte :message="erreur" />
    <MessageAlerte :message="succes" type="succes" />

    <section class="g-carte">
      <div class="g-filtres">
        <span class="g-compteur" style="margin-left: 0">{{ nbExpires }} expiré(s) · {{ nbBientot }} bientôt expiré(s)</span>
      </div>
      <p v-if="chargement" class="g-vide">Chargement…</p>
      <p v-else-if="!documents.length" class="g-vide">Aucun document à renouveler pour le moment.</p>
      <div v-else class="g-table-wrap">
        <table class="g-table">
          <thead>
            <tr><th>Salarié</th><th>Document</th><th>Expiration</th><th>Validité</th><th class="g-droite">Fiche</th></tr>
          </thead>
          <tbody>
            <tr v-for="d in pg.items" :key="d.idDocument">
              <td>
                <span class="g-principal">{{ d.employeNom }}</span>
                <span class="g-secondaire">{{ d.matricule }}</span>
              </td>
              <td>{{ libelle(d.typeDocument) }}</td>
              <td>{{ formatDate(d.dateExpiration) }}</td>
              <td><ValiditeBadge :etat="d.etatValidite" :jours-restants="d.joursRestants" /></td>
              <td>
                <div class="g-actions-cellule">
                  <RouterLink class="g-btn g-btn--petit" :to="`/si/app/rh/employes/${d.idEmploye}`" :aria-label="`Ouvrir la fiche de ${d.employeNom}`">Ouvrir</RouterLink>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <Pagination v-bind="pg.barre" @update:page="pg.aller" />
      </div>
    </section>
  </div>
</template>
