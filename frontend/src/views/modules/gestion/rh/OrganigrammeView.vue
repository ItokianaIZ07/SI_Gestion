<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { api } from "@/services/api";
import OrganigrammeBranche from "@/components/rh/organigramme/OrganigrammeBranche.vue";
import Pagination from "@/components/ui/Pagination.vue";

const TAILLE_PAGE = 10;
const noeuds = ref([]);
const departements = ref([]);
const departementSelectionne = ref("");
const recherche = ref("");
const page = ref(1);
const expandKey = ref(0);
const chargement = ref(true);
const erreur = ref("");

const noeudsFiltres = computed(() => {
  const terme = recherche.value.trim().toLocaleLowerCase();
  const departement = departementSelectionne.value;

  function filtrer(noeud) {
    const subordonnes = noeud.subordonnes.map(filtrer).filter(Boolean);
    const departementCorrespond = !departement || noeud.departement === departement;
    const texte = `${noeud.nom} ${noeud.prenom} ${noeud.matricule}`.toLocaleLowerCase();
    const rechercheCorrespond = !terme || texte.includes(terme);
    if ((departementCorrespond && rechercheCorrespond) || subordonnes.length > 0) {
      return { ...noeud, subordonnes };
    }
    return null;
  }

  return noeuds.value.map(filtrer).filter(Boolean);
});
const nombrePages = computed(() => Math.ceil(noeudsFiltres.value.length / TAILLE_PAGE));
const noeudsPage = computed(() => {
  const debut = (page.value - 1) * TAILLE_PAGE;
  return noeudsFiltres.value.slice(debut, debut + TAILLE_PAGE);
});
const debutPage = computed(() => (page.value - 1) * TAILLE_PAGE + 1);
const finPage = computed(() => Math.min(page.value * TAILLE_PAGE, noeudsFiltres.value.length));

watch(departementSelectionne, () => {
  page.value = 1;
});

watch(recherche, (terme) => {
  page.value = 1;
  if (!terme.trim()) return;

  const indexRacine = noeudsFiltres.value.findIndex((noeud) =>
    correspondDansBranche(noeud, terme),
  );
  if (indexRacine >= 0) {
    page.value = Math.floor(indexRacine / TAILLE_PAGE) + 1;
  }
});

watch(nombrePages, (total) => {
  if (page.value > total) page.value = Math.max(total, 1);
});

onMounted(charger);

async function charger() {
  chargement.value = true;
  erreur.value = "";
  try {
    const [arbre, listeDepartements] = await Promise.all([
      api.get("/rh/organigramme"),
      api.get("/rh/organigramme/departements"),
    ]);
    noeuds.value = arbre;
    departements.value = listeDepartements;
  } catch (error) {
    erreur.value = error.message || "Impossible de charger l'organigramme.";
  } finally {
    chargement.value = false;
  }
}

function toutDeplier() {
  expandKey.value += 1;
}

function correspondDansBranche(noeud, terme) {
  const rechercheNormalisee = terme.trim().toLocaleLowerCase();
  const texte = `${noeud.nom} ${noeud.prenom} ${noeud.matricule}`.toLocaleLowerCase();
  return texte.includes(rechercheNormalisee)
    || noeud.subordonnes.some((enfant) => correspondDansBranche(enfant, terme));
}
</script>

<template>
  <main class="organigramme-page">
    <header class="page-entete">
      <div>
        <p class="surtitre">Ressources humaines</p>
        <h1>Organigramme</h1>
        <p class="intro">Structure hiérarchique des salariés actuellement en poste.</p>
      </div>
      <button class="bouton-primaire" type="button" @click="toutDeplier">
        Tout déplier
      </button>
    </header>

    <section class="filtres" aria-label="Filtres de l'organigramme">
      <label>
        Rechercher un salarié
        <input
          v-model="recherche"
          type="search"
          placeholder="Nom, prénom ou matricule"
        />
      </label>
      <label>
        Département
        <select v-model="departementSelectionne">
          <option value="">Tous les départements</option>
          <option v-for="departement in departements" :key="departement.id" :value="departement.nom">
            {{ departement.nom }}
          </option>
        </select>
      </label>
    </section>

    <p v-if="chargement" class="etat">Chargement de l'organigramme…</p>
    <p v-else-if="erreur" class="etat etat--erreur" role="alert">{{ erreur }}</p>
    <p v-else-if="noeuds.length === 0" class="etat">Aucun salarié.</p>
    <p v-else-if="noeudsFiltres.length === 0" class="etat">
      Aucun salarié ne correspond à ces filtres.
    </p>

    <section v-else class="arbre" aria-label="Arbre hiérarchique">
      <ul class="arbre__racines">
        <OrganigrammeBranche
          v-for="noeud in noeudsPage"
          :key="noeud.idEmploye"
          :noeud="noeud"
          :recherche="recherche"
          :expand-key="expandKey"
        />
      </ul>
      <Pagination
        v-if="noeudsFiltres.length > 0"
        v-model:page="page"
        :page-count="nombrePages"
        :total="noeudsFiltres.length"
        :debut="debutPage"
        :fin="finPage"
      />
    </section>
  </main>
</template>

<style scoped>
.organigramme-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 34px clamp(16px, 3vw, 36px);
  color: #202820;
}

.page-entete {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 25px;
}

.surtitre {
  margin: 0 0 6px;
  color: #087018;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  font-size: clamp(26px, 4vw, 36px);
}

.intro {
  margin: 8px 0 0;
  color: #697369;
}

.bouton-primaire {
  padding: 10px 16px;
  color: #fff;
  background: #075d0e;
  border: 0;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}

.filtres {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) minmax(220px, 0.6fr);
  gap: 16px;
  margin-bottom: 18px;
  padding: 18px;
  background: #fff;
  border: 1px solid #e3e9e3;
  border-radius: 12px;
}

.filtres label {
  display: grid;
  gap: 7px;
  color: #465146;
  font-size: 13px;
  font-weight: 650;
}

.filtres input,
.filtres select {
  min-height: 42px;
  padding: 8px 11px;
  color: inherit;
  background: #fff;
  border: 1px solid #ccd5cc;
  border-radius: 7px;
  font: inherit;
  font-weight: 400;
}

.arbre {
  overflow: auto;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #e3e9e3;
  border-radius: 12px;
}

.arbre__racines {
  min-width: 680px;
  margin: 0;
  padding: 0;
}

.etat {
  padding: 28px;
  color: #596559;
  text-align: center;
  background: #fff;
  border: 1px solid #e3e9e3;
  border-radius: 12px;
}

.etat--erreur {
  color: #a32525;
}

@media (max-width: 650px) {
  .page-entete {
    align-items: flex-start;
    flex-direction: column;
  }

  .filtres {
    grid-template-columns: 1fr;
  }
}
</style>
