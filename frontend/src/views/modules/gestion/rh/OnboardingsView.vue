<script setup>
// Suivi de l'intégration des nouveaux employés. Un onboarding est créé automatiquement
// à l'embauche (page « Embaucher ») avec les tâches issues des modèles actifs.
import { computed, onMounted, ref } from "vue";
import "@/assets/gestion.css";
import Pagination from "@/components/ui/Pagination.vue";
import { usePagination } from "@/composable/usePagination";
import PageEntete from "@/components/gestion/PageEntete.vue";
import StatutBadge from "@/components/gestion/StatutBadge.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import { onboardingApi } from "@/services/onboardingService";
import { STATUTS_ONBOARDING, formatDate } from "@/config/libelles";

const onboardings = ref([]);
const chargement = ref(true);
const erreur = ref("");
const recherche = ref("");
const filtreStatut = ref("");

async function charger() {
  chargement.value = true;
  erreur.value = "";
  try {
    onboardings.value = (await onboardingApi.liste()) ?? [];
  } catch (e) {
    erreur.value = e.message;
  } finally {
    chargement.value = false;
  }
}

const lignes = computed(() => {
  const q = recherche.value.trim().toLowerCase();
  return onboardings.value.filter((o) => {
    if (filtreStatut.value && o.statut !== filtreStatut.value) return false;
    return !q || [o.nom, o.prenom, o.matricule, o.poste, o.departement].join(" ").toLowerCase().includes(q);
  });
});

const compte = (statut) => onboardings.value.filter((o) => o.statut === statut).length;
const totalRetards = computed(() => onboardings.value.reduce((s, o) => s + (o.nbEnRetard || 0), 0));
const pourcentage = (o) => (o.nbTaches ? Math.round((o.nbFaites * 100) / o.nbTaches) : 0);

const pg = usePagination(lignes, 10);

onMounted(charger);
</script>

<template>
  <div class="g-page">
    <PageEntete
      titre="Onboarding des nouveaux employés"
      sous-titre="Suivez les tâches d'intégration et les documents administratifs de chaque nouvel embauché."
    >
      <RouterLink class="g-btn" to="/si/app/rh/onboarding/modeles">Modèles de tâches</RouterLink>
      <RouterLink class="g-btn g-btn--primaire" to="/si/app/rh/embauche">Embaucher</RouterLink>
    </PageEntete>

    <MessageAlerte :message="erreur" />

    <ul class="chiffres" aria-label="Synthèse">
      <li><strong>{{ compte("A_FAIRE") }}</strong><span>À démarrer</span></li>
      <li><strong>{{ compte("EN_COURS") }}</strong><span>En cours</span></li>
      <li><strong>{{ compte("TERMINE") }}</strong><span>Terminés</span></li>
      <li :class="{ 'chiffres--alerte': totalRetards }"><strong>{{ totalRetards }}</strong><span>Tâches en retard</span></li>
    </ul>

    <section class="g-carte">
      <div class="g-filtres">
        <input v-model="recherche" class="g-input g-input--recherche" type="search" placeholder="Rechercher un employé, un matricule, un poste…" />
        <select v-model="filtreStatut" class="g-input" aria-label="Filtrer par statut">
          <option value="">Tous les statuts</option>
          <option v-for="o in STATUTS_ONBOARDING" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
        <span class="g-compteur">{{ lignes.length }} onboarding(s)</span>
      </div>

      <p v-if="chargement" class="g-vide">Chargement…</p>
      <div v-else-if="!lignes.length" class="g-vide">
        <strong>Aucun onboarding</strong>
        Il est créé automatiquement lorsque vous embauchez un candidat retenu.
      </div>

      <div v-else class="g-table-wrap">
        <table class="g-table">
          <thead>
            <tr>
              <th>Employé</th>
              <th>Poste</th>
              <th>Début</th>
              <th>Fin prévue</th>
              <th>Avancement</th>
              <th>Statut</th>
              <th class="g-droite">Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="o in pg.items" :key="o.id">
              <td>
                <span class="g-principal">{{ o.prenom }} {{ o.nom }}</span>
                <span class="g-secondaire">{{ o.matricule }}</span>
              </td>
              <td>{{ o.poste }}<span class="g-secondaire">{{ o.departement }}</span></td>
              <td>{{ formatDate(o.dateDebut) }}</td>
              <td>{{ formatDate(o.dateFinPrevue) }}</td>
              <td class="avancement">
                <div class="barre" role="progressbar" :aria-valuenow="pourcentage(o)" aria-valuemin="0" aria-valuemax="100">
                  <span :style="{ width: pourcentage(o) + '%' }"></span>
                </div>
                <span class="g-secondaire">{{ o.nbFaites }}/{{ o.nbTaches }} tâches</span>
                <span v-if="o.nbEnRetard" class="g-badge g-badge--danger retard">{{ o.nbEnRetard }} en retard</span>
              </td>
              <td><StatutBadge :statut="o.statut" /></td>
              <td>
                <div class="g-actions-cellule">
                  <RouterLink class="g-btn g-btn--petit g-btn--primaire" :to="`/si/app/rh/onboarding/${o.id}`">Suivre</RouterLink>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <Pagination v-bind="pg.barre" @update:page="pg.aller" />
    </section>
  </div>
</template>

<style scoped>
.chiffres { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 14px; margin: 0; padding: 0; list-style: none; }
.chiffres li { display: flex; flex-direction: column; gap: 2px; padding: 16px 18px; background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); }
.chiffres strong { font-size: 1.6rem; line-height: 1.1; color: var(--color-ink); }
.chiffres span { font-size: 12.5px; color: var(--color-muted); }
.chiffres--alerte { border-color: var(--color-danger-line) !important; background: var(--color-danger-background) !important; }
.chiffres--alerte strong { color: var(--color-danger); }

.avancement { min-width: 170px; }
.barre { height: 8px; margin-bottom: 4px; overflow: hidden; border-radius: 999px; background: var(--color-subtle); }
.barre span { display: block; height: 100%; border-radius: 999px; background: linear-gradient(90deg, var(--color-primary), var(--color-secondary)); transition: width .3s; }
.retard { margin-top: 4px; }
</style>
