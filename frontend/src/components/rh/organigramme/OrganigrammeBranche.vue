<script setup>
import { computed, ref, watch } from "vue";
import { RouterLink } from "vue-router";
import Pagination from "@/components/ui/Pagination.vue";

defineOptions({ name: "OrganigrammeBranche" });

const TAILLE_PAGE = 10;
const props = defineProps({
  noeud: { type: Object, required: true },
  niveau: { type: Number, default: 0 },
  recherche: { type: String, default: "" },
  expandKey: { type: Number, default: 0 },
});

const aDesEnfants = computed(() => props.noeud.subordonnes?.length > 0);
const page = ref(1);
const nombrePages = computed(() => Math.ceil((props.noeud.subordonnes?.length ?? 0) / TAILLE_PAGE));
const subordonnesPage = computed(() => {
  const debut = (page.value - 1) * TAILLE_PAGE;
  return props.noeud.subordonnes?.slice(debut, debut + TAILLE_PAGE) ?? [];
});
const debutPage = computed(() => (page.value - 1) * TAILLE_PAGE + 1);
const finPage = computed(() => Math.min(page.value * TAILLE_PAGE, props.noeud.subordonnes?.length ?? 0));
const correspond = computed(() => {
  const terme = props.recherche.trim().toLocaleLowerCase();
  return terme && `${props.noeud.nom} ${props.noeud.prenom} ${props.noeud.matricule}`
    .toLocaleLowerCase()
    .includes(terme);
});
const contientCorrespondance = computed(() =>
  props.noeud.subordonnes?.some(
    (enfant) => correspondSousArbre(enfant, props.recherche),
  ),
);
const estOuvert = ref(
  props.niveau === 0
    || props.expandKey > 0
    || Boolean(props.recherche.trim() && contientCorrespondance.value),
);

watch(nombrePages, (total) => {
  if (page.value > total) page.value = Math.max(total, 1);
});

watch(
  () => props.recherche,
  (terme) => {
    page.value = 1;
    if (!terme.trim()) return;

    const indexEnfant = props.noeud.subordonnes.findIndex((enfant) =>
      correspondSousArbre(enfant, terme),
    );
    if (indexEnfant >= 0) {
      page.value = Math.floor(indexEnfant / TAILLE_PAGE) + 1;
    }
  },
);

watch(
  () => props.expandKey,
  (key) => {
    if (key > 0) estOuvert.value = true;
  },
);

watch(
  () => [props.recherche, contientCorrespondance.value],
  ([recherche, contientMatch]) => {
    if (recherche.trim() && contientMatch) estOuvert.value = true;
  },
);

function correspondSousArbre(noeud, recherche) {
  const terme = recherche.trim().toLocaleLowerCase();
  if (!terme) return false;
  const texte = `${noeud.nom} ${noeud.prenom} ${noeud.matricule}`.toLocaleLowerCase();
  return texte.includes(terme)
    || noeud.subordonnes?.some((enfant) => correspondSousArbre(enfant, recherche));
}
</script>

<template>
  <li class="branche" :class="{ 'branche--trouvee': correspond }">
    <div class="branche__ligne" :class="{ 'branche__ligne--racine': niveau === 0 }">
      <button
        v-if="aDesEnfants"
        class="branche__toggle"
        type="button"
        :aria-expanded="estOuvert"
        :aria-label="`${estOuvert ? 'Replier' : 'Déplier'} ${noeud.prenom} ${noeud.nom}`"
        @click="estOuvert = !estOuvert"
      >
        {{ estOuvert ? "−" : "+" }}
      </button>
      <span v-else class="branche__espace" aria-hidden="true"></span>

      <RouterLink
        class="branche__personne"
        :to="`/si/app/rh/organigramme/employes/${noeud.idEmploye}`"
      >
        <strong>{{ noeud.prenom }} {{ noeud.nom }}</strong>
        <small>{{ noeud.poste }} · {{ noeud.departement }}</small>
      </RouterLink>
      <span class="branche__matricule">{{ noeud.matricule }}</span>
      <span class="branche__effectif">
        {{ noeud.nbSubordonnesDirects ?? 0 }} direct(s)
      </span>
      <span v-if="noeud.superieurAbsent" class="branche__alerte">
        Supérieur parti
      </span>
    </div>

    <ul v-if="estOuvert && aDesEnfants" class="branche__enfants">
      <OrganigrammeBranche
        v-for="enfant in subordonnesPage"
        :key="enfant.idEmploye"
        :noeud="enfant"
        :niveau="niveau + 1"
        :recherche="recherche"
        :expand-key="expandKey"
      />
    </ul>
    <Pagination
      v-if="estOuvert && nombrePages > 1"
      v-model:page="page"
      :page-count="nombrePages"
      :total="noeud.subordonnes.length"
      :debut="debutPage"
      :fin="finPage"
    />
  </li>
</template>

<style scoped>
.branche {
  position: relative;
  list-style: none;
}

.branche__ligne {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  width: fit-content;
  min-width: min(680px, calc(100% - 2px));
  box-sizing: border-box;
  padding: 12px 14px;
  background: #fff;
  border: 1px solid #dfe7df;
  border-radius: 10px;
  box-shadow: 0 2px 6px rgb(22 57 25 / 4%);
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background-color 0.15s ease;
}

.branche__ligne:not(.branche__ligne--racine)::before {
  position: absolute;
  top: 50%;
  left: -25px;
  width: 24px;
  border-top: 2px solid #bdd0bd;
  content: "";
}

.branche__ligne:hover {
  border-color: #9cbe9e;
  box-shadow: 0 4px 12px rgb(22 57 25 / 8%);
}

.branche--trouvee > .branche__ligne {
  background: #eff8ef;
  border-color: #63a769;
  box-shadow: 0 0 0 2px rgb(65 139 72 / 12%);
}

.branche__toggle,
.branche__espace {
  display: grid;
  flex: 0 0 28px;
  width: 28px;
  height: 28px;
  place-items: center;
}

.branche__toggle {
  color: #075d0e;
  font-size: 19px;
  font-weight: 700;
  background: #eff7ef;
  border: 1px solid #cfe2d0;
  border-radius: 6px;
  cursor: pointer;
}

.branche__personne {
  display: flex;
  flex: 1;
  min-width: 200px;
  flex-direction: column;
  gap: 4px;
  color: inherit;
  text-decoration: none;
}

.branche__personne strong {
  font-size: 14px;
}

.branche__personne:hover strong {
  color: #075d0e;
  text-decoration: underline;
}

.branche__personne small,
.branche__matricule {
  color: #637064;
  font-size: 12px;
}

.branche__effectif {
  white-space: nowrap;
  color: #425443;
  font-size: 12px;
}

.branche__alerte {
  padding: 4px 8px;
  color: #8a4b00;
  background: #fff3d8;
  border-radius: 999px;
  font-size: 11px;
  white-space: nowrap;
}

.branche__enfants {
  position: relative;
  display: grid;
  gap: 8px;
  margin: 0 0 0 28px;
  padding: 10px 0 4px 24px;
}

.branche__enfants::before {
  position: absolute;
  top: 0;
  bottom: 28px;
  left: 0;
  border-left: 2px solid #bdd0bd;
  content: "";
}

@media (max-width: 700px) {
  .branche__ligne {
    gap: 8px;
    min-width: 600px;
    padding: 10px;
  }

  .branche__ligne:not(.branche__ligne--racine)::before {
    left: -17px;
    width: 16px;
  }

  .branche__enfants {
    margin-left: 18px;
    padding-left: 16px;
  }

  .branche__matricule {
    display: none;
  }
}
</style>
