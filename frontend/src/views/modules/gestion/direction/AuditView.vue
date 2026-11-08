<script setup>
import { onMounted, reactive, ref } from "vue";
import PageEntete from "@/components/gestion/PageEntete.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import Pagination from "@/components/ui/Pagination.vue";
import { auditApi } from "@/services/auditService";
import { ACTIONS_AUDIT, libelleAction } from "@/config/libellesBloc5";

const TAILLE = 25;
const filtres = reactive({ utilisateur: "", table: "", enregistrement: "", action: "", du: "", au: "" });
const utilisateurs = ref([]);
const tables = ref([]);
const resultat = ref({ contenu: [], page: 1, taille: TAILLE, total: 0, totalPages: 1 });
const ouvert = ref(null); // id de l'entrée dont le détail est affiché
const chargement = ref(true);
const erreur = ref("");

async function rechercher(page = 1) {
  chargement.value = true;
  erreur.value = "";
  ouvert.value = null;
  try {
    resultat.value = await auditApi.rechercher({ ...filtres, page, taille: TAILLE });
  } catch (e) {
    erreur.value = e.message;
  } finally {
    chargement.value = false;
  }
}

function reinitialiser() {
  Object.assign(filtres, { utilisateur: "", table: "", enregistrement: "", action: "", du: "", au: "" });
  rechercher(1);
}

onMounted(async () => {
  try {
    [utilisateurs.value, tables.value] = await Promise.all([auditApi.utilisateurs(), auditApi.tables()]);
  } catch (e) {
    erreur.value = e.message;
  }
  await rechercher(1);
});

function formatHeure(v) {
  return v ? new Date(v).toLocaleString("fr-FR", { timeZone: "Indian/Antananarivo" }) : "—";
}

function differences(a) {
  const avant = a.anciennesValeurs ?? {};
  const apres = a.nouvellesValeurs ?? {};
  return [...new Set([...Object.keys(avant), ...Object.keys(apres)])]
    .filter((cle) => JSON.stringify(avant[cle]) !== JSON.stringify(apres[cle]))
    .map((cle) => ({ cle, avant: avant[cle], apres: apres[cle] }));
}

const afficher = (v) => (v === null || v === undefined ? "—" : typeof v === "object" ? JSON.stringify(v) : String(v));
</script>

<template>
  <div class="g-page">
    <PageEntete titre="Journal d'audit"
      sous-titre="Historique de toutes les créations, modifications et suppressions. Lecture seule : aucune entrée ne peut être modifiée ni supprimée." />
    <MessageAlerte :message="erreur" />

    <section class="g-carte">
      <form class="g-filtres" @submit.prevent="rechercher(1)">
        <select v-model="filtres.utilisateur" class="g-input" aria-label="Utilisateur">
          <option value="">Tous les utilisateurs</option>
          <option v-for="u in utilisateurs" :key="u.id" :value="u.id">{{ u.nomComplet }}</option>
        </select>
        <select v-model="filtres.table" class="g-input" aria-label="Table">
          <option value="">Toutes les tables</option>
          <option v-for="t in tables" :key="t" :value="t">{{ t }}</option>
        </select>
        <input v-model="filtres.enregistrement" class="g-input" type="text" inputmode="numeric"
          placeholder="N° d'enregistrement" aria-label="Identifiant de l'enregistrement" />
        <select v-model="filtres.action" class="g-input" aria-label="Action">
          <option value="">Toutes les actions</option>
          <option v-for="a in ACTIONS_AUDIT" :key="a.value" :value="a.value">{{ a.label }}</option>
        </select>
        <label class="date">Du <input v-model="filtres.du" class="g-input" type="date" /></label>
        <label class="date">Au <input v-model="filtres.au" class="g-input" type="date" /></label>
        <button class="g-btn g-btn--primaire" type="submit" :disabled="chargement">Filtrer</button>
        <button class="g-btn" type="button" :disabled="chargement" @click="reinitialiser">Réinitialiser</button>
        <span class="g-compteur">{{ resultat.total }} entrée(s)</span>
      </form>

      <p v-if="chargement" class="g-vide">Chargement…</p>
      <p v-else-if="!resultat.contenu.length" class="g-vide">Aucune entrée ne correspond aux filtres.</p>
      <div v-else class="g-table-wrap">
        <table class="g-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Utilisateur</th>
              <th>Action</th>
              <th>Table</th>
              <th>N°</th>
              <th>IP</th>
              <th class="g-droite">Détail</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="a in resultat.contenu" :key="a.id">
              <tr>
                <td>{{ formatHeure(a.dateAction) }}</td>
                <td>{{ a.utilisateurNom || "Système" }}</td>
                <td>{{ libelleAction(a.action) }}</td>
                <td>{{ a.table }}</td>
                <td>{{ a.identifiant || "—" }}</td>
                <td>{{ a.adresseIp || "—" }}</td>
                <td>
                  <div class="g-actions-cellule">
                    <button type="button" class="g-btn g-btn--petit" :aria-expanded="ouvert === a.id"
                      @click="ouvert = ouvert === a.id ? null : a.id">
                      {{ ouvert === a.id ? "Masquer" : "Voir" }}
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="ouvert === a.id" class="detail">
                <td colspan="7">
                  <table v-if="a.action === 'UPDATE'" class="g-table">
                    <thead>
                      <tr>
                        <th>Champ</th>
                        <th>Avant</th>
                        <th>Après</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="d in differences(a)" :key="d.cle">
                        <td>{{ d.cle }}</td>
                        <td>{{ afficher(d.avant) }}</td>
                        <td>{{ afficher(d.apres) }}</td>
                      </tr>
                    </tbody>
                  </table>
                  <pre
                    v-else>{{ JSON.stringify(a.action === 'DELETE' ? a.anciennesValeurs : a.nouvellesValeurs, null, 2) }}</pre>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
        <Pagination :page="resultat.page" :page-count="resultat.totalPages" :total="resultat.total"
          :debut="(resultat.page - 1) * resultat.taille + 1"
          :fin="Math.min(resultat.page * resultat.taille, resultat.total)" @update:page="rechercher" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.date {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-muted);
}

.detail td {
  background: var(--color-subtle);
}

.detail pre {
  margin: 0;
  max-height: 260px;
  overflow: auto;
  font-size: 12.5px;
  white-space: pre-wrap;
}
</style>
