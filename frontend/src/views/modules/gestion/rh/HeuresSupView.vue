<!-- BY : Brandon -->
<script setup>
import { ref, computed, onMounted } from "vue";
import api from "@/services/api";
import PageEntete from "@/components/gestion/PageEntete.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import "@/assets/gestion.css";

const lignes = ref([]);
const erreur = ref("");
const succes = ref("");
const recherche = ref("");

async function charger() {
  erreur.value = "";
  succes.value = "";
  try {
    const { data } = await api.get("/pointage/heures-sup/a-valider");
    lignes.value = data;
  } catch (e) {
    erreur.value = e.response?.data?.message || "Erreur de chargement.";
  }
}

const lignesFiltrees = computed(() => {
  const q = recherche.value.trim().toLowerCase();
  return lignes.value.filter(l =>
    !q || [l.employe.nom, l.employe.prenom, l.dateTravail].join(" ").toLowerCase().includes(q)
  );
});

async function decider(id, decision) {
  erreur.value = "";
  succes.value = "";
  try {
    await api.post(`/pointage/heures-sup/${id}/valider`, { decision });
    succes.value = `Heures supplémentaires ${decision === "VALIDEE" ? "validées" : "refusées"}.`;
    // ✅ Attente avant rechargement pour laisser la transaction se commiter
    await new Promise(r => setTimeout(r, 250));
    await charger();
  } catch (e) {
    erreur.value = e.response?.data?.message || "Erreur.";
  }
}

onMounted(charger);
</script>

<template>
  <div class="g-page">
    <PageEntete titre="Heures supplémentaires" sous-titre="Validez ou refusez les heures sup en attente." />
    <MessageAlerte :message="erreur" />
    <MessageAlerte :message="succes" type="succes" />

    <section class="g-carte">
      <div class="g-filtres">
        <input v-model="recherche" class="g-input g-input--recherche" type="search" placeholder="Rechercher un employé..." />
        <span class="g-compteur">{{ lignesFiltrees.length }} ligne(s)</span>
      </div>

      <div class="g-table-wrap">
        <table class="g-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Employé</th>
              <th class="g-droite">Temps réel (min)</th>
              <th class="g-droite">Heures Sup (min)</th>
              <th class="g-droite">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="l in lignesFiltrees" :key="l.id">
              <td>{{ l.dateTravail }}</td>
              <td class="g-principal">{{ l.employe.nom }} {{ l.employe.prenom }}</td>
              <td class="g-droite">{{ l.tempsReelMinutes }}</td>
              <td class="g-droite"><strong>{{ l.heuresSupplementairesMinutes }}</strong></td>
              <td>
                <div class="g-actions-cellule">
                  <button class="g-btn g-btn--primaire g-btn--petit" @click="decider(l.id, 'VALIDEE')">Valider</button>
                  <button class="g-btn g-btn--danger g-btn--petit" @click="decider(l.id, 'REFUSEE')">Refuser</button>
                </div>
              </td>
            </tr>
            <tr v-if="!lignesFiltrees.length"><td colspan="5" class="g-vide">Aucune heure sup à valider.</td></tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>