<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import api from '@/services/api.js'

const departements = ref([])
const chargement = ref(true)
const erreur = ref('')
const recherche = ref('')
const filtreEffectif = ref('tous')
const filtrePostes = ref('tous')
const filtreDemandes = ref('tous')

const departementsFiltres = computed(() => {
  const terme = recherche.value.trim().toLocaleLowerCase('fr')

  return departements.value.filter((departement) => {
    const nomCorrespond = departement.nom.toLocaleLowerCase('fr').includes(terme)
    const effectif = Number(departement.effectifActif ?? 0)
    const effectifMax = Number(departement.effectifMax ?? 0)
    const correspondEffectif =
      filtreEffectif.value === 'tous' ||
      (filtreEffectif.value === 'avec-place' && effectifMax > effectif) ||
      (filtreEffectif.value === 'capacite-atteinte' && effectifMax > 0 && effectif >= effectifMax)
    const nombrePostes = Number(departement.postes ?? 0)
    const correspondPostes =
      filtrePostes.value === 'tous' ||
      (filtrePostes.value === 'avec' && nombrePostes > 0) ||
      (filtrePostes.value === 'sans' && nombrePostes === 0)
    const nombreDemandes = Number(departement.demandes ?? 0)
    const correspondDemandes =
      filtreDemandes.value === 'tous' ||
      (filtreDemandes.value === 'avec' && nombreDemandes > 0) ||
      (filtreDemandes.value === 'sans' && nombreDemandes === 0)

    return nomCorrespond && correspondEffectif && correspondPostes && correspondDemandes
  })
})

const filtresActifs = computed(
  () =>
    Boolean(recherche.value) ||
    filtreEffectif.value !== 'tous' ||
    filtrePostes.value !== 'tous' ||
    filtreDemandes.value !== 'tous',
)

function reinitialiserFiltres() {
  recherche.value = ''
  filtreEffectif.value = 'tous'
  filtrePostes.value = 'tous'
  filtreDemandes.value = 'tous'
}

async function getVisionDepartements() {
  const { data } = await api.get('/manager/departements/vision-globale')
  return data
}

async function chargerDepartements() {
  chargement.value = true
  erreur.value = ''
  try {
    departements.value = await getVisionDepartements()
  } catch (error) {
    erreur.value =
      error.response?.data?.message ??
      `Impossible de charger les départements (${error.response?.status ?? error.message}).`
  } finally {
    chargement.value = false
  }
}

onMounted(chargerDepartements)
</script>

<template>
  <div class="page-departements">
    <header class="entete">
      <div>
        <p class="sur-titre">Espace manager</p>
        <h1>Liste des départements</h1>
        <p class="description">Sélectionnez un département pour consulter sa vision globale.</p>
      </div>
      <div class="actions-entete">
        <RouterLink class="creer" to="/si/app/manager/departements/creation">
          Créer un département
        </RouterLink>
        <button class="actualiser" type="button" :disabled="chargement" @click="chargerDepartements">
          {{ chargement ? 'Chargement…' : 'Actualiser' }}
        </button>
      </div>
    </header>

    <p v-if="erreur" class="message message--erreur" role="alert">{{ erreur }}</p>
    <p v-else-if="chargement" class="message" role="status">Chargement des départements…</p>
    <p v-else-if="!departements.length" class="message">Aucun département n’est enregistré.</p>

    <section v-else class="tableau">
      <div class="tableau__entete">
        <div>
          <h2>Départements de l’entreprise</h2>
          <p>{{ departementsFiltres.length }} résultat(s) sur {{ departements.length }} département(s)</p>
        </div>
      </div>

      <div class="filtres" aria-label="Filtres des départements">
        <label class="filtre filtre--recherche">
          <span>Rechercher</span>
          <input
            v-model="recherche"
            type="search"
            placeholder="Nom du département"
            aria-label="Rechercher un département par son nom"
          >
        </label>

        <label class="filtre">
          <span>Effectif</span>
          <select v-model="filtreEffectif" aria-label="Filtrer par effectif">
            <option value="tous">Tous les effectifs</option>
            <option value="avec-place">Capacité disponible</option>
            <option value="capacite-atteinte">Capacité atteinte</option>
          </select>
        </label>

        <label class="filtre">
          <span>Postes</span>
          <select v-model="filtrePostes" aria-label="Filtrer par nombre de postes">
            <option value="tous">Tous</option>
            <option value="avec">Avec postes</option>
            <option value="sans">Sans poste</option>
          </select>
        </label>

        <label class="filtre">
          <span>Demandes</span>
          <select v-model="filtreDemandes" aria-label="Filtrer par nombre de demandes">
            <option value="tous">Toutes</option>
            <option value="avec">Avec demandes</option>
            <option value="sans">Sans demande</option>
          </select>
        </label>

        <button
          v-if="filtresActifs"
          class="reinitialiser"
          type="button"
          @click="reinitialiserFiltres"
        >
          Effacer les filtres
        </button>
      </div>

      <div class="table-conteneur">
        <table>
          <thead>
            <tr>
              <th>Département</th>
              <th>Effectif</th>
              <th>Nombre de postes</th>
              <th>Nombre de demandes</th>
              <th><span class="visuellement-cache">Action</span></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="departement in departementsFiltres" :key="departement.id">
              <td>
                <RouterLink
                  class="lien-departement"
                  :to="`/si/app/manager/departements/${departement.id}`"
                >
                  {{ departement.nom }}
                </RouterLink>
              </td>
              <td>
                {{ departement.effectifActif }}
                <span v-if="departement.effectifMax > 0" class="detail-effectif">
                  / {{ departement.effectifMax }}
                </span>
              </td>
              <td>{{ departement.postes }}</td>
              <td>{{ departement.demandes }}</td>
              <td>
                <RouterLink
                  class="action"
                  :to="`/si/app/manager/departements/${departement.id}`"
                  :aria-label="`Voir la vision globale de ${departement.nom}`"
                >
                  Voir le détail <span aria-hidden="true">→</span>
                </RouterLink>
              </td>
            </tr>
            <tr v-if="!departementsFiltres.length">
              <td colspan="5" class="aucun-resultat">
                Aucun département ne correspond aux filtres sélectionnés.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page-departements {
  display: grid;
  gap: 24px;
  max-width: 1440px;
  margin: 0 auto;
  color: #2b2b2b;
}

.entete {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.actions-entete {
  display: flex;
  align-items: center;
  gap: 10px;
}

.creer {
  padding: 10px 16px;
  border: 1px solid var(--principale, #015f09);
  border-radius: 9px;
  background: var(--principale, #015f09);
  color: white;
  font-weight: 600;
  text-decoration: none;
}

.creer:hover {
  filter: brightness(0.92);
}

.sur-titre {
  margin: 0 0 8px;
  color: var(--principale, #015f09);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #17211a;
  font-size: clamp(1.6rem, 3vw, 2.15rem);
}

.description,
.tableau__entete p {
  margin: 8px 0 0;
  color: var(--texte-doux, #6b6b6b);
}

.actualiser {
  flex: 0 0 auto;
  padding: 10px 16px;
  border: 1px solid var(--principale, #015f09);
  border-radius: 9px;
  background: var(--principale, #015f09);
  color: white;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.actualiser:disabled {
  cursor: wait;
  opacity: 0.7;
}

.message,
.tableau {
  border: 1px solid var(--couleur-texte, #bfbfbf);
  border-radius: 14px;
  background: var(--fond-card, #fff);
}

.message {
  margin: 0;
  padding: 18px 20px;
  color: #475569;
}

.message--erreur {
  border-color: #f3b9b9;
  background: #fff2f2;
  color: #a12828;
}

.tableau {
  overflow: hidden;
  box-shadow: 0 3px 12px rgba(15, 23, 42, 0.04);
}

.tableau__entete {
  padding: 22px 22px 18px;
  border-bottom: 1px solid #e5e7eb;
}

.filtres {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  gap: 12px;
  padding: 18px 22px;
  border-bottom: 1px solid #e5e7eb;
  background: #fcfdfc;
}

.filtre {
  display: grid;
  min-width: 160px;
  gap: 6px;
}

.filtre span {
  color: #64748b;
  font-size: 0.8rem;
  font-weight: 600;
}

.filtre input,
.filtre select {
  min-height: 40px;
  padding: 8px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: white;
  color: #334155;
  font: inherit;
}

.filtre--recherche {
  flex: 1 1 220px;
}

.reinitialiser {
  min-height: 40px;
  padding: 8px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: white;
  color: var(--principale, #015f09);
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

.reinitialiser:hover {
  background: #f0f7f1;
}

h2 {
  margin: 0;
  font-size: 1.15rem;
}

.table-conteneur {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

th,
td {
  padding: 15px 18px;
  border-bottom: 1px solid #e5e7eb;
  white-space: nowrap;
}

th {
  background: #f8fafc;
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 600;
  text-transform: uppercase;
}

td {
  color: #334155;
}

.aucun-resultat {
  padding: 28px 18px;
  color: #64748b;
  text-align: center;
}

tbody tr:last-child td {
  border-bottom: 0;
}

tbody tr:hover {
  background: #f8fbf8;
}

.lien-departement {
  color: var(--principale, #015f09);
  font-weight: 700;
}

.lien-departement:hover,
.action:hover {
  text-decoration: underline;
}

.detail-effectif {
  color: #64748b;
}

.action {
  color: var(--principale, #015f09);
  font-size: 0.9rem;
  font-weight: 600;
}

.visuellement-cache {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 620px) {
  .entete,
  .actions-entete {
    flex-direction: column;
  }

  .tableau__entete {
    padding: 16px;
  }

  .filtres {
    align-items: stretch;
    flex-direction: column;
    padding: 16px;
  }

  .filtre {
    min-width: 0;
  }
}
</style>
