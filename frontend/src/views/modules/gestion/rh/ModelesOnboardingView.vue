<script setup>
// Modèles de tâches copiés dans l'onboarding de chaque nouvel employé au moment de l'embauche.
import { computed, onMounted, reactive, ref } from "vue";
import "@/assets/gestion.css";
import Pagination from "@/components/ui/Pagination.vue";
import { usePagination } from "@/composable/usePagination";
import PageEntete from "@/components/gestion/PageEntete.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import { modelesOnboardingApi } from "@/services/onboardingService";
import { CATEGORIES_ONBOARDING, libelle } from "@/config/libelles";

const modeles = ref([]);
const chargement = ref(true);
const envoi = ref(false);
const erreur = ref("");
const succes = ref("");

const vide = () => ({ id: null, code: "", libelle: "", categorie: "ADMINISTRATIF", delaiJours: 0, actif: true });
const form = reactive(vide());
const formulaireOuvert = ref(false);

async function charger() {
  try {
    modeles.value = (await modelesOnboardingApi.liste()) ?? [];
  } catch (e) {
    erreur.value = e.message;
  } finally {
    chargement.value = false;
  }
}

const nbActifs = computed(() => modeles.value.filter((m) => m.actif).length);

// Recherche, filtres et pagination
const recherche = ref("");
const filtreCategorie = ref("");
const filtreEtat = ref(""); // '' | 'actif' | 'inactif'
const filtres = computed(() => {
  const q = recherche.value.trim().toLowerCase();
  return modeles.value.filter((m) => {
    if (filtreCategorie.value && m.categorie !== filtreCategorie.value) return false;
    if (filtreEtat.value === "actif" && !m.actif) return false;
    if (filtreEtat.value === "inactif" && m.actif) return false;
    return !q || `${m.libelle} ${m.code}`.toLowerCase().includes(q);
  });
});
const pg = usePagination(filtres, 10);

function nouveau() {
  Object.assign(form, vide());
  formulaireOuvert.value = true;
  erreur.value = "";
  succes.value = "";
}

function modifier(m) {
  Object.assign(form, { ...m });
  formulaireOuvert.value = true;
  erreur.value = "";
  succes.value = "";
}

function fermer() {
  formulaireOuvert.value = false;
}

async function enregistrer() {
  erreur.value = "";
  succes.value = "";
  envoi.value = true;
  try {
    const charge = {
      code: form.code.trim(),
      libelle: form.libelle.trim(),
      categorie: form.categorie,
      delaiJours: Number(form.delaiJours) || 0,
      actif: form.actif,
    };
    if (form.id) await modelesOnboardingApi.modifier(form.id, charge);
    else await modelesOnboardingApi.creer(charge);
    succes.value = form.id ? "Modèle modifié." : "Modèle créé.";
    formulaireOuvert.value = false;
    await charger();
  } catch (e) {
    erreur.value = e.message;
  } finally {
    envoi.value = false;
  }
}

async function basculer(m) {
  erreur.value = "";
  succes.value = "";
  try {
    await modelesOnboardingApi.modifier(m.id, {
      code: m.code, libelle: m.libelle, categorie: m.categorie, delaiJours: m.delaiJours, actif: !m.actif,
    });
    succes.value = m.actif ? "Modèle désactivé." : "Modèle activé.";
    await charger();
  } catch (e) {
    erreur.value = e.message;
  }
}

onMounted(charger);
</script>

<template>
  <div class="g-page">
    <PageEntete
      titre="Modèles de tâches d'onboarding"
      sous-titre="Ces tâches sont copiées dans l'onboarding de chaque nouvel employé lors de l'embauche. Les onboardings déjà créés ne sont pas modifiés."
    >
      <RouterLink class="g-btn" to="/si/app/rh/onboarding">← Onboarding</RouterLink>
      <button class="g-btn g-btn--primaire" type="button" @click="nouveau">Nouveau modèle</button>
    </PageEntete>

    <MessageAlerte :message="erreur" />
    <MessageAlerte :message="succes" type="succes" />
    <MessageAlerte
      v-if="!chargement && !nbActifs"
      message="Aucun modèle actif : les prochains employés embauchés n'auront aucune tâche d'onboarding."
    />

    <form v-if="formulaireOuvert" class="g-carte g-form" @submit.prevent="enregistrer">
      <div class="g-champ">
        <label for="code" class="g-obligatoire">Code</label>
        <input id="code" v-model="form.code" class="g-input" type="text" maxlength="50" required placeholder="BADGE_ACCES" />
        <small>Identifiant unique, mis en majuscules.</small>
      </div>
      <div class="g-champ">
        <label for="categorie">Catégorie</label>
        <select id="categorie" v-model="form.categorie" class="g-input">
          <option v-for="c in CATEGORIES_ONBOARDING" :key="c.value" :value="c.value">{{ c.label }}</option>
        </select>
      </div>
      <div class="g-champ g-champ--large">
        <label for="libelle" class="g-obligatoire">Libellé</label>
        <input id="libelle" v-model="form.libelle" class="g-input" type="text" maxlength="150" required placeholder="Remettre le badge d'accès" />
      </div>
      <div class="g-champ">
        <label for="delai" class="g-obligatoire">Délai (jours après le début)</label>
        <input id="delai" v-model.number="form.delaiJours" class="g-input" type="number" min="0" required />
      </div>
      <div class="g-champ">
        <label for="actif">Statut</label>
        <select id="actif" v-model="form.actif" class="g-input">
          <option :value="true">Actif (copié à l'embauche)</option>
          <option :value="false">Inactif</option>
        </select>
      </div>
      <div class="g-form__actions">
        <button class="g-btn" type="button" @click="fermer">Annuler</button>
        <button class="g-btn g-btn--primaire" type="submit" :disabled="envoi">{{ envoi ? "Enregistrement…" : "Enregistrer" }}</button>
      </div>
    </form>

    <section class="g-carte">
      <div class="g-filtres">
        <input v-model="recherche" class="g-input g-input--recherche" type="search" placeholder="Rechercher un modèle (libellé ou code)…" aria-label="Rechercher un modèle" />
        <select v-model="filtreCategorie" class="g-input" aria-label="Filtrer par catégorie">
          <option value="">Toutes les catégories</option>
          <option v-for="c in CATEGORIES_ONBOARDING" :key="c.value" :value="c.value">{{ c.label }}</option>
        </select>
        <select v-model="filtreEtat" class="g-input" aria-label="Filtrer par état">
          <option value="">Actifs et inactifs</option>
          <option value="actif">Actifs</option>
          <option value="inactif">Inactifs</option>
        </select>
        <span class="g-compteur">{{ filtres.length }} modèle(s)</span>
      </div>

      <p v-if="chargement" class="g-vide">Chargement…</p>
      <div v-else-if="!filtres.length" class="g-vide">
        <strong>{{ modeles.length ? "Aucun résultat" : "Aucun modèle" }}</strong>
        {{ modeles.length ? "Modifiez vos filtres." : "Créez les tâches types d'une intégration (badge, compte e-mail, matériel, formation…)." }}
      </div>
      <div v-else class="g-table-wrap">
        <table class="g-table">
          <thead>
            <tr><th>Tâche</th><th>Catégorie</th><th>Délai</th><th>État</th><th class="g-droite">Actions</th></tr>
          </thead>
          <tbody>
            <tr v-for="m in pg.items" :key="m.id">
              <td><span class="g-principal">{{ m.libelle }}</span><span class="g-secondaire">{{ m.code }}</span></td>
              <td>{{ libelle(m.categorie) }}</td>
              <td>J+{{ m.delaiJours }}</td>
              <td><span class="g-badge" :class="m.actif ? 'g-badge--success' : 'g-badge--neutral'">{{ m.actif ? "Actif" : "Inactif" }}</span></td>
              <td>
                <div class="g-actions-cellule">
                  <button type="button" class="g-btn g-btn--petit" :aria-label="`Modifier ${m.libelle}`" @click="modifier(m)">Modifier</button>
                  <button type="button" class="g-btn g-btn--petit" :aria-label="`${m.actif ? 'Désactiver' : 'Activer'} ${m.libelle}`" @click="basculer(m)">
                    {{ m.actif ? "Désactiver" : "Activer" }}
                  </button>
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
