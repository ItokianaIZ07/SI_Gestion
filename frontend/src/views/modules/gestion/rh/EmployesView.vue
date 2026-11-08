<script setup>
import { computed, onMounted, ref } from "vue";
import PageEntete from "@/components/gestion/PageEntete.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import StatutBadge from "@/components/gestion/StatutBadge.vue";
import Pagination from "@/components/ui/Pagination.vue";
import { usePagination } from "@/composable/usePagination";
import { employeApi } from "@/services/employeService";
import { STATUTS_EMPLOYE } from "@/config/libellesBloc5";
import { formatDate } from "@/config/libelles";

const employes = ref([]);
const chargement = ref(true);
const erreur = ref("");
const recherche = ref("");
const statut = ref("");

const lignes = computed(() => {
  const q = recherche.value.trim().toLowerCase();
  return employes.value.filter((e) => {
    if (statut.value && e.statut !== statut.value) return false;
    if (!q) return true;
    return [e.nom, e.prenom, e.matricule, e.email, e.poste, e.departement]
      .some((v) => (v ?? "").toLowerCase().includes(q));
  });
});
const pg = usePagination(lignes, 10);

onMounted(async () => {
  try {
    employes.value = await employeApi.liste();
  } catch (e) {
    erreur.value = e.message;
  } finally {
    chargement.value = false;
  }
});
</script>

<template>
  <div class="g-page">
    <PageEntete titre="Employés" sous-titre="Ouvrez la fiche d'un salarié : identité, affectation, contrat, documents et carrière." />
    <MessageAlerte :message="erreur" />

    <section class="g-carte">
      <div class="g-filtres">
        <input v-model="recherche" class="g-input g-input--recherche" type="search" placeholder="Rechercher (nom, matricule, poste, département…)" aria-label="Rechercher un employé" />
        <select v-model="statut" class="g-input" aria-label="Filtrer par statut">
          <option value="">Tous les statuts</option>
          <option v-for="s in STATUTS_EMPLOYE" :key="s.value" :value="s.value">{{ s.label }}</option>
        </select>
        <span class="g-compteur">{{ lignes.length }} employé(s)</span>
      </div>

      <p v-if="chargement" class="g-vide">Chargement…</p>
      <p v-else-if="!lignes.length" class="g-vide">Aucun employé ne correspond à la recherche.</p>
      <div v-else class="g-table-wrap">
        <table class="g-table">
          <thead>
            <tr>
              <th>Employé</th>
              <th>Poste</th>
              <th>Département</th>
              <th>Embauché le</th>
              <th>Statut</th>
              <th class="g-droite">Fiche</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="e in pg.items" :key="e.id">
              <td>
                <span class="g-principal">{{ e.prenom }} {{ e.nom }}</span>
                <span class="g-secondaire">{{ e.matricule }} · {{ e.email }}</span>
              </td>
              <td>{{ e.poste }}</td>
              <td>{{ e.departement }}</td>
              <td>{{ formatDate(e.dateEmbauche) }}</td>
              <td><StatutBadge :statut="e.statut" /></td>
              <td>
                <div class="g-actions-cellule">
                  <RouterLink class="g-btn g-btn--petit g-btn--primaire" :to="`/si/app/rh/employes/${e.id}`" :aria-label="`Ouvrir la fiche de ${e.prenom} ${e.nom}`">Ouvrir</RouterLink>
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
