<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import PageEntete from "@/components/gestion/PageEntete.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import StatutBadge from "@/components/gestion/StatutBadge.vue";
import Pagination from "@/components/ui/Pagination.vue";
import { usePagination } from "@/composable/usePagination";
import { onboardingApi } from "@/services/onboardingService";
import { CATEGORIES_ONBOARDING, formatDate, libelle } from "@/config/libelles";

const taches = ref([]);
const chargement = ref(true);
const erreur = ref("");
const succes = ref("");
const occupe = ref(false);
const rechercheSalarie = ref("");
const filtre = reactive({ statut: "", categorie: "" });
const modification = ref(null);
const formulaire = reactive({ statut: "", dateEcheance: "", remarque: "" });
const historique = ref(null);
const historiqueCharge = ref(false);
const historiqueLignes = ref([]);

const tachesFiltrees = computed(() => {
  const recherche = rechercheSalarie.value.trim().toLocaleLowerCase("fr");
  return taches.value.filter((t) => {
    const correspondAuSalarie = !recherche
      || `${t.employeNom ?? ""} ${t.matricule ?? ""}`.toLocaleLowerCase("fr").includes(recherche);
    return correspondAuSalarie
      && (!filtre.statut || t.statut === filtre.statut)
      && (!filtre.categorie || t.categorie === filtre.categorie);
  });
});
const pagination = usePagination(tachesFiltrees, 10);

async function charger() {
  chargement.value = true;
  erreur.value = "";
  try {
    taches.value = (await onboardingApi.mesTaches()) ?? [];
  } catch (e) {
    erreur.value = e.message || "Impossible de charger vos tâches d'onboarding.";
  } finally {
    chargement.value = false;
  }
}

onMounted(charger);

async function executer(action, message) {
  if (occupe.value) return false;
  occupe.value = true;
  erreur.value = "";
  succes.value = "";
  try {
    await action();
    succes.value = message;
    await charger();
    return true;
  } catch (e) {
    erreur.value = e.message || "Impossible de modifier la tâche.";
    return false;
  } finally {
    occupe.value = false;
  }
}

function ouvrirModification(tache) {
  modification.value = tache;
  Object.assign(formulaire, {
    statut: tache.statut,
    dateEcheance: tache.dateEcheance ?? "",
    remarque: tache.remarque ?? "",
  });
}

async function enregistrer() {
  if (!modification.value) return;
  const ok = await executer(
    () => onboardingApi.modifierTache(modification.value.id, {
      statut: formulaire.statut,
      dateEcheance: formulaire.dateEcheance || null,
      remarque: formulaire.remarque,
    }),
    "Tâche mise à jour.",
  );
  if (ok) modification.value = null;
}

async function ouvrirHistorique(tache) {
  historique.value = tache;
  historiqueCharge.value = true;
  historiqueLignes.value = [];
  erreur.value = "";
  try {
    historiqueLignes.value = await onboardingApi.historiqueTache(tache.id);
  } catch (e) {
    erreur.value = e.message || "Impossible de charger l'historique de la tâche.";
    historique.value = null;
  } finally {
    historiqueCharge.value = false;
  }
}

const champsDifferents = (ancien, nouveau) => ancien !== nouveau;
function valeurHistorique(type, valeur) {
  if (valeur == null || valeur === "") return "—";
  if (type === "statut") return libelle(valeur);
  if (type === "date") return formatDate(valeur);
  return valeur;
}
const dateHeure = (date) => date ? new Date(date).toLocaleString("fr-FR") : "—";
</script>

<template>
  <div class="g-page">
    <PageEntete titre="Mes tâches d'onboarding" sous-titre="Suivez les tâches qui vous sont attribuées et consultez leur historique." />
    <MessageAlerte :message="erreur" />
    <MessageAlerte :message="succes" type="succes" />

    <section class="g-carte">
      <div class="g-filtres">
        <input
          v-model="rechercheSalarie"
          class="g-input g-input--recherche"
          type="search"
          placeholder="Rechercher un salarié (nom, prénom ou matricule)"
          aria-label="Rechercher un salarié par nom, prénom ou matricule"
        />
        <select v-model="filtre.statut" class="g-input" aria-label="Filtrer par statut">
          <option value="">Tous les statuts</option>
          <option v-for="s in ['A_FAIRE', 'EN_COURS', 'FAIT', 'ANNULE']" :key="s" :value="s">{{ libelle(s) }}</option>
        </select>
        <select v-model="filtre.categorie" class="g-input" aria-label="Filtrer par catégorie">
          <option value="">Toutes les catégories</option>
          <option v-for="c in CATEGORIES_ONBOARDING" :key="c.value" :value="c.value">{{ c.label }}</option>
        </select>
        <span class="g-compteur">{{ tachesFiltrees.length }} tâche(s)</span>
      </div>

      <p v-if="chargement" class="g-vide">Chargement…</p>
      <p v-else-if="!tachesFiltrees.length" class="g-vide">
        {{ taches.length ? "Aucune tâche ne correspond aux filtres." : "Aucune tâche ne vous est attribuée." }}
      </p>
      <div v-else class="g-table-wrap">
        <table class="g-table">
          <thead>
            <tr><th>Salarié</th><th>Tâche</th><th>Échéance</th><th>Statut</th><th>Réalisée le</th><th class="g-droite">Actions</th></tr>
          </thead>
          <tbody>
            <tr v-for="t in pagination.items" :key="t.id" :class="{ 'ligne-retard': t.enRetard, 'ligne-fermee': t.statut === 'ANNULE' }">
              <td>
                <span class="g-principal">{{ t.employeNom }}</span>
                <span class="g-secondaire">{{ t.matricule }}</span>
              </td>
              <td>
                <span class="g-principal">{{ t.libelle }}</span>
                <span class="g-secondaire">{{ libelle(t.categorie) }}</span>
                <span v-if="t.remarque" class="g-secondaire">{{ t.remarque }}</span>
              </td>
              <td>
                {{ formatDate(t.dateEcheance) }}
                <span v-if="t.enRetard" class="g-badge g-badge--danger retard">En retard</span>
              </td>
              <td><StatutBadge :statut="t.statut" /></td>
              <td>{{ formatDate(t.dateRealisation) }}</td>
              <td>
                <div class="g-actions-cellule">
                  <button type="button" class="g-btn g-btn--petit" :disabled="occupe" @click="ouvrirModification(t)">Modifier</button>
                  <button type="button" class="g-btn g-btn--petit" @click="ouvrirHistorique(t)">Historique</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <Pagination v-bind="pagination.barre" @update:page="pagination.aller" />
      </div>
    </section>

    <div v-if="modification" class="dialogue-fond" @click.self="modification = null">
      <form class="dialogue-tache" role="dialog" aria-modal="true" @submit.prevent="enregistrer">
        <header class="dialogue-tache__entete">
          <div><h2>Modifier la tâche</h2><p>{{ modification.libelle }} · {{ modification.employeNom }}</p></div>
          <button type="button" class="g-btn g-btn--petit" :disabled="occupe" @click="modification = null">Fermer</button>
        </header>
        <div class="dialogue-tache__champs">
          <label class="g-champ">
            <span>Statut</span>
            <select v-model="formulaire.statut" class="g-input" required>
              <option v-for="s in ['A_FAIRE', 'EN_COURS', 'FAIT', 'ANNULE']" :key="s" :value="s">{{ libelle(s) }}</option>
            </select>
          </label>
          <label class="g-champ">
            <span>Échéance</span>
            <input v-model="formulaire.dateEcheance" class="g-input" type="date" />
          </label>
          <label class="g-champ dialogue-tache__remarque">
            <span>Remarque</span>
            <textarea v-model="formulaire.remarque" class="g-input" maxlength="2000" rows="4"></textarea>
          </label>
        </div>
        <footer class="dialogue-tache__actions">
          <button type="button" class="g-btn" :disabled="occupe" @click="modification = null">Annuler</button>
          <button type="submit" class="g-btn g-btn--primaire" :disabled="occupe">Enregistrer</button>
        </footer>
      </form>
    </div>

    <div v-if="historique" class="dialogue-fond" @click.self="historique = null">
      <section class="dialogue-tache dialogue-tache--historique" role="dialog" aria-modal="true" aria-labelledby="titre-historique">
        <header class="dialogue-tache__entete">
          <div><h2 id="titre-historique">Historique de la tâche</h2><p>{{ historique.libelle }} · {{ historique.employeNom }}</p></div>
          <button type="button" class="g-btn g-btn--petit" @click="historique = null">Fermer</button>
        </header>
        <p v-if="historiqueCharge" class="g-vide">Chargement de l'historique…</p>
        <p v-else-if="!historiqueLignes.length" class="g-vide">Aucune modification enregistrée.</p>
        <div v-else class="g-table-wrap">
          <table class="g-table">
            <thead><tr><th>Date et heure</th><th>Auteur</th><th>Champs modifiés</th></tr></thead>
            <tbody>
              <tr v-for="h in historiqueLignes" :key="h.id">
                <td>{{ dateHeure(h.dateAction) }}</td>
                <td>{{ h.auteur || "Inconnu" }}</td>
                <td>
                  <ul class="historique-champs">
                    <li v-if="champsDifferents(h.ancienStatut, h.nouveauStatut)"><strong>Statut :</strong> {{ valeurHistorique("statut", h.ancienStatut) }} → {{ valeurHistorique("statut", h.nouveauStatut) }}</li>
                    <li v-if="champsDifferents(h.ancienneEcheance, h.nouvelleEcheance)"><strong>Échéance :</strong> {{ valeurHistorique("date", h.ancienneEcheance) }} → {{ valeurHistorique("date", h.nouvelleEcheance) }}</li>
                    <li v-if="champsDifferents(h.ancienneRemarque, h.nouvelleRemarque)"><strong>Remarque :</strong> {{ valeurHistorique("texte", h.ancienneRemarque) }} → {{ valeurHistorique("texte", h.nouvelleRemarque) }}</li>
                  </ul>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.ligne-fermee { opacity: .6; }
.ligne-retard { background: color-mix(in srgb, var(--color-danger) 7%, transparent); }
.retard { display: block; width: fit-content; margin-top: 5px; }
.dialogue-fond { position: fixed; inset: 0; z-index: 1000; display: grid; place-items: center; padding: 20px; background: rgb(15 23 42 / 55%); }
.dialogue-tache { width: min(620px, 100%); max-height: min(85vh, 760px); overflow: auto; border-radius: 12px; background: var(--color-surface, #fff); box-shadow: var(--shadow-lg); }
.dialogue-tache--historique { width: min(900px, 100%); }
.dialogue-tache__entete { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; padding: 18px 20px; border-bottom: 1px solid var(--color-border); }
.dialogue-tache__entete h2 { margin: 0; font-size: 1.15rem; }
.dialogue-tache__entete p { margin: 4px 0 0; color: var(--color-muted); }
.dialogue-tache__champs { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; padding: 20px; }
.dialogue-tache__remarque { grid-column: 1 / -1; }
.dialogue-tache__remarque textarea { width: 100%; resize: vertical; }
.dialogue-tache__actions { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 20px; border-top: 1px solid var(--color-border); }
.historique-champs { margin: 0; padding-left: 18px; }
.historique-champs li + li { margin-top: 5px; }
</style>
