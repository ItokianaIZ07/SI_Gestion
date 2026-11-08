<script setup>
import { onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { api } from "@/services/api";

const route = useRoute();
const hierarchie = ref(null);
const chargement = ref(true);
const erreur = ref("");

onMounted(charger);
watch(() => route.params.idEmploye, charger);

async function charger() {
  chargement.value = true;
  erreur.value = "";
  try {
    hierarchie.value = await api.get(
      `/rh/organigramme/employes/${route.params.idEmploye}/hierarchie`,
    );
  } catch (error) {
    erreur.value = error.message || "Impossible de charger la hiérarchie du salarié.";
    hierarchie.value = null;
  } finally {
    chargement.value = false;
  }
}
</script>

<template>
  <main class="fiche-page">
    <RouterLink class="retour" to="/si/app/rh/organigramme">
      ← Retour à l'organigramme
    </RouterLink>

    <p v-if="chargement" class="etat">Chargement de la fiche…</p>
    <p v-else-if="erreur" class="etat etat--erreur" role="alert">{{ erreur }}</p>
    <template v-else-if="hierarchie">
      <header class="fiche-entete">
        <p class="surtitre">Fiche salarié · Hiérarchie</p>
        <h1>{{ hierarchie.employe.prenom }} {{ hierarchie.employe.nom }}</h1>
        <p>{{ hierarchie.employe.poste }} · {{ hierarchie.employe.departement }}</p>
        <small>Matricule : {{ hierarchie.employe.matricule }}</small>
      </header>

      <section class="bloc">
        <h2>Supérieur direct</h2>
        <p v-if="!hierarchie.superieurDirect">Aucun supérieur.</p>
        <p v-else-if="hierarchie.superieurParti" class="avertissement">
          {{ hierarchie.superieurDirect.prenom }} {{ hierarchie.superieurDirect.nom }}
          <span>(supérieur parti)</span>
        </p>
        <RouterLink
          v-else
          class="personne-lien"
          :to="`/si/app/rh/organigramme/employes/${hierarchie.superieurDirect.idEmploye}`"
        >
          {{ hierarchie.superieurDirect.prenom }} {{ hierarchie.superieurDirect.nom }}
          <small>{{ hierarchie.superieurDirect.poste }} · {{ hierarchie.superieurDirect.departement }}</small>
        </RouterLink>
      </section>

      <section class="bloc">
        <h2>Chaîne de supérieurs</h2>
        <p v-if="hierarchie.chaineSuperieurs.length === 0">Aucun supérieur.</p>
        <ul v-else class="liste">
          <li v-for="personne in hierarchie.chaineSuperieurs" :key="personne.idEmploye">
            <RouterLink :to="`/si/app/rh/organigramme/employes/${personne.idEmploye}`">
              {{ personne.prenom }} {{ personne.nom }}
            </RouterLink>
            <span>Niveau {{ personne.niveau }}</span>
          </li>
        </ul>
      </section>

      <section class="bloc">
        <h2>Subordonnés directs</h2>
        <p v-if="hierarchie.subordonnesDirects.length === 0">Aucun subordonné.</p>
        <ul v-else class="liste">
          <li v-for="personne in hierarchie.subordonnesDirects" :key="personne.idEmploye">
            <RouterLink :to="`/si/app/rh/organigramme/employes/${personne.idEmploye}`">
              {{ personne.prenom }} {{ personne.nom }}
            </RouterLink>
            <small>{{ personne.poste }} · {{ personne.departement }}</small>
          </li>
        </ul>
      </section>

      <section class="bloc">
        <h2>Subordonnés indirects</h2>
        <p v-if="hierarchie.subordonnesIndirects.length === 0">Aucun subordonné indirect.</p>
        <ul v-else class="liste">
          <li v-for="personne in hierarchie.subordonnesIndirects" :key="personne.idEmploye">
            <RouterLink :to="`/si/app/rh/organigramme/employes/${personne.idEmploye}`">
              {{ personne.prenom }} {{ personne.nom }}
            </RouterLink>
            <span>Niveau {{ personne.niveau }}</span>
          </li>
        </ul>
      </section>
    </template>
  </main>
</template>

<style scoped>
.fiche-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 34px clamp(16px, 3vw, 36px);
  color: #202820;
}

.retour {
  display: inline-block;
  margin-bottom: 22px;
  color: #075d0e;
  font-weight: 650;
  text-decoration: none;
}

.fiche-entete,
.bloc {
  margin-bottom: 16px;
  padding: 22px;
  background: #fff;
  border: 1px solid #e3e9e3;
  border-radius: 12px;
}

.fiche-entete h1 {
  margin: 4px 0 8px;
  font-size: clamp(25px, 4vw, 34px);
}

.fiche-entete p:not(.surtitre) {
  margin: 0 0 8px;
  color: #536053;
}

.fiche-entete small,
.liste small,
.personne-lien small {
  color: #687368;
}

.surtitre {
  margin: 0;
  color: #087018;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.bloc h2 {
  margin: 0 0 14px;
  font-size: 18px;
}

.bloc > p {
  margin: 0;
  color: #687368;
}

.personne-lien,
.liste li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.personne-lien {
  justify-content: flex-start;
  flex-direction: column;
  align-items: flex-start;
  color: #075d0e;
}

.liste {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.liste a {
  color: #075d0e;
}

.liste span,
.avertissement span {
  color: #8a4b00;
  font-size: 13px;
}

.avertissement {
  color: #8a4b00 !important;
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
</style>
