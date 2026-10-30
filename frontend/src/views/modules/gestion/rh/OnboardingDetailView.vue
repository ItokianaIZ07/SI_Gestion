<script setup>
// Suivi d'un onboarding : /si/app/rh/onboarding/:id
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import "@/assets/gestion.css";
import PageEntete from "@/components/gestion/PageEntete.vue";
import StatutBadge from "@/components/gestion/StatutBadge.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import { onboardingApi } from "@/services/onboardingService";
import { CATEGORIES_ONBOARDING, TYPES_DOCUMENT, formatDate, libelle } from "@/config/libelles";

const route = useRoute();
let id = Number(route.params.id);

const o = ref(null);
const chargement = ref(true);
const erreur = ref("");
const succes = ref("");
const occupe = ref(false); // une action est en cours : on bloque les autres boutons

async function recharger() {
  o.value = await onboardingApi.detail(id);
}

async function charger() {
  chargement.value = true;
  erreur.value = "";
  succes.value = "";
  o.value = null;
  try {
    await recharger();
  } catch (e) {
    erreur.value = e.message;
  } finally {
    chargement.value = false;
  }
}

onMounted(charger);

// Même composant réutilisé avec un autre identifiant dans l'adresse : on recharge.
watch(
  () => route.params.id,
  (nouveau) => {
    if (nouveau === undefined) return;
    id = Number(nouveau);
    charger();
  },
);

/** Exécute une action, affiche l'erreur éventuelle et rafraîchit l'écran (le statut global peut changer). */
async function executer(action, messageSucces = "") {
  if (occupe.value) return false;
  occupe.value = true;
  erreur.value = "";
  succes.value = "";
  try {
    await action();
    succes.value = messageSucces;
    return true;
  } catch (e) {
    erreur.value = e.message;
    return false;
  } finally {
    try {
      await recharger();
    } catch {
      /* on garde l'affichage actuel */
    }
    occupe.value = false;
  }
}

// ---------------- Progression ----------------
const tachesActives = computed(() => (o.value?.taches ?? []).filter((t) => t.statut !== "ANNULE"));
const nbFaites = computed(() => tachesActives.value.filter((t) => t.statut === "FAIT").length);
const pourcentage = computed(() =>
  tachesActives.value.length ? Math.round((nbFaites.value * 100) / tachesActives.value.length) : 0,
);
const nbEnRetard = computed(() => (o.value?.taches ?? []).filter((t) => t.enRetard).length);

// ---------------- Tâches ----------------
const transitions = (t) =>
  t.statut === "A_FAIRE"
    ? [{ a: "EN_COURS", label: "Démarrer" }, { a: "FAIT", label: "Terminer", principal: true }, { a: "ANNULE", label: "Annuler", danger: true }]
    : t.statut === "EN_COURS"
      ? [{ a: "FAIT", label: "Terminer", principal: true }, { a: "ANNULE", label: "Annuler", danger: true }]
      : [{ a: "A_FAIRE", label: "Rouvrir" }];

function changerTache(t, statut) {
  if (statut === "ANNULE" && !window.confirm(`Annuler la tâche « ${t.libelle} » ?`)) return;
  return executer(() => onboardingApi.changerStatutTache(t.id, statut));
}

const nouvelleTache = reactive({ libelle: "", categorie: "AUTRE", dateEcheance: "" });

async function ajouterTache() {
  if (!nouvelleTache.libelle.trim()) {
    erreur.value = "Le libellé de la tâche est obligatoire.";
    return;
  }
  const ok = await executer(
    () =>
      onboardingApi.ajouterTache(id, {
        libelle: nouvelleTache.libelle.trim(),
        categorie: nouvelleTache.categorie,
        dateEcheance: nouvelleTache.dateEcheance || null,
      }),
    "Tâche ajoutée.",
  );
  if (ok) Object.assign(nouvelleTache, { libelle: "", categorie: "AUTRE", dateEcheance: "" });
}

// ---------------- Documents ----------------
const TAILLE_MAX = 5 * 1024 * 1024;
const DOCUMENTS_ATTENDUS = ["CIN", "DIPLOME", "RIB", "CERTIFICAT_MEDICAL", "PHOTO"];
const nouveauDoc = reactive({ type: "CIN", fichier: null });
const inputFichier = ref(null);

const checklist = computed(() =>
  DOCUMENTS_ATTENDUS.map((type) => {
    const docs = (o.value?.documents ?? []).filter((d) => d.typeDocument === type);
    const statut = docs.some((d) => d.statut === "VALIDE")
      ? "VALIDE"
      : docs.some((d) => d.statut === "FOURNI")
        ? "FOURNI"
        : docs.length
          ? "REFUSE"
          : null;
    return { type, statut };
  }),
);

function choisirFichier(event) {
  const f = event.target.files[0];
  erreur.value = "";
  nouveauDoc.fichier = null;
  if (!f) return;
  if (!/\.(pdf|jpe?g|png)$/i.test(f.name)) {
    erreur.value = "Format non accepté : joignez un PDF, un JPEG ou un PNG.";
    event.target.value = "";
  } else if (f.size > TAILLE_MAX) {
    erreur.value = "Le fichier dépasse 5 Mo.";
    event.target.value = "";
  } else {
    nouveauDoc.fichier = f;
  }
}

async function ajouterDocument() {
  if (!nouveauDoc.fichier) {
    erreur.value = "Choisissez un fichier à ajouter.";
    return;
  }
  const ok = await executer(
    () => onboardingApi.ajouterDocument(id, nouveauDoc.type, nouveauDoc.fichier),
    "Document ajouté.",
  );
  if (ok) {
    nouveauDoc.fichier = null;
    if (inputFichier.value) inputFichier.value.value = "";
  }
}

const decider = (d, statut) => executer(() => onboardingApi.changerStatutDocument(d.id, statut));

function supprimerDocument(d) {
  if (!window.confirm(`Supprimer « ${d.nomFichier} » ?`)) return;
  return executer(() => onboardingApi.supprimerDocument(d.id), "Document supprimé.");
}

const urlsOuvertes = [];
async function voirDocument(d) {
  erreur.value = "";
  try {
    const blob = await onboardingApi.fichierDocument(d.id);
    const url = URL.createObjectURL(blob);
    urlsOuvertes.push(url);
    window.open(url, "_blank", "noopener");
  } catch (e) {
    erreur.value = e.message;
  }
}
onBeforeUnmount(() => urlsOuvertes.forEach((u) => URL.revokeObjectURL(u)));
</script>

<template>
  <div class="g-page">
    <PageEntete
      :titre="o ? `Onboarding — ${o.prenom} ${o.nom}` : 'Onboarding'"
      :sous-titre="o ? `${o.poste} · ${o.departement} · ${o.matricule}` : ''"
    >
      <RouterLink class="g-btn" to="/si/app/rh/onboarding">← Retour à la liste</RouterLink>
    </PageEntete>

    <MessageAlerte :message="erreur" />
    <MessageAlerte :message="succes" type="succes" />
    <p v-if="chargement" class="g-vide">Chargement…</p>

    <template v-if="o">
      <MessageAlerte v-if="o.statut === 'TERMINE'" type="succes" message="Onboarding terminé : toutes les tâches sont faites." />

      <!-- Résumé -->
      <section class="g-carte resume">
        <dl>
          <div><dt>E-mail</dt><dd>{{ o.email }}</dd></div>
          <div><dt>Téléphone</dt><dd>{{ o.telephone || "—" }}</dd></div>
          <div><dt>Embauché le</dt><dd>{{ formatDate(o.dateEmbauche) }}</dd></div>
          <div><dt>Période d'intégration</dt><dd>{{ formatDate(o.dateDebut) }} → {{ formatDate(o.dateFinPrevue) }}</dd></div>
          <div><dt>Responsable</dt><dd>{{ o.responsableNom || "—" }}</dd></div>
          <div><dt>Statut</dt><dd><StatutBadge :statut="o.statut" /></dd></div>
        </dl>
        <div class="progression">
          <div class="progression__texte">
            <strong>{{ pourcentage }} %</strong>
            <span>{{ nbFaites }}/{{ tachesActives.length }} tâches faites<template v-if="nbEnRetard"> · <b class="rouge">{{ nbEnRetard }} en retard</b></template></span>
          </div>
          <div class="barre" role="progressbar" :aria-valuenow="pourcentage" aria-valuemin="0" aria-valuemax="100">
            <span :style="{ width: pourcentage + '%' }"></span>
          </div>
        </div>
      </section>

      <!-- Contrats : tous les contrats successifs sont conservés (CDD, renouvellement, CDI…) -->
      <section class="g-carte" aria-labelledby="titre-contrats">
        <div class="bloc-entete"><h2 id="titre-contrats">Contrats</h2></div>
        <p v-if="!o.contrats.length" class="g-vide">Aucun contrat enregistré pour cet employé.</p>
        <div v-else class="g-table-wrap">
          <table class="g-table">
            <thead>
              <tr><th>Référence</th><th>Type</th><th>Début</th><th>Fin</th><th>Signé le</th><th>Statut</th></tr>
            </thead>
            <tbody>
              <tr v-for="c in o.contrats" :key="c.id">
                <td class="g-principal">{{ c.reference }}</td>
                <td>{{ c.typeContrat }}</td>
                <td>{{ formatDate(c.dateDebut) }}</td>
                <td>{{ c.dateFin ? formatDate(c.dateFin) : "Indéterminée" }}</td>
                <td>{{ formatDate(c.dateSignature) }}</td>
                <td><StatutBadge :statut="c.statut" /></td>
              </tr>
            </tbody>
          </table>
        </div>
        <p class="aide aide--haut">La tâche « Contrat signé » se coche automatiquement dès que le contrat est signé par les deux parties.</p>
      </section>

      <!-- Tâches -->
      <section class="g-carte" aria-labelledby="titre-taches">
        <div class="bloc-entete"><h2 id="titre-taches">Tâches d'intégration</h2></div>

        <div v-if="!o.taches.length" class="g-vide">
          <strong>Aucune tâche</strong>
          Aucun modèle actif n'existait à l'embauche. Ajoutez des tâches ci-dessous ou activez des
          <RouterLink to="/si/app/rh/onboarding/modeles">modèles de tâches</RouterLink>.
        </div>

        <div v-else class="g-table-wrap">
          <table class="g-table">
            <thead>
              <tr><th>Tâche</th><th>Échéance</th><th>Statut</th><th class="g-droite">Actions</th></tr>
            </thead>
            <tbody>
              <tr v-for="t in o.taches" :key="t.id" :class="{ 'ligne-fermee': t.statut === 'ANNULE' }">
                <td>
                  <span class="g-principal">{{ t.libelle }}</span>
                  <span class="g-secondaire">{{ libelle(t.categorie) }}<template v-if="t.responsableNom"> · {{ t.responsableNom }}</template></span>
                  <span v-if="t.remarque" class="g-secondaire">{{ t.remarque }}</span>
                </td>
                <td>
                  {{ formatDate(t.dateEcheance) }}
                  <span v-if="t.enRetard" class="g-badge g-badge--danger retard">En retard</span>
                  <span v-else-if="t.dateRealisation" class="g-secondaire">Fait le {{ formatDate(t.dateRealisation) }}</span>
                </td>
                <td><StatutBadge :statut="t.statut" /></td>
                <td>
                  <div class="g-actions-cellule">
                    <button
                      v-for="a in transitions(t)"
                      :key="a.a"
                      type="button"
                      class="g-btn g-btn--petit"
                      :class="{ 'g-btn--primaire': a.principal, 'g-btn--danger': a.danger }"
                      :disabled="occupe"
                      :aria-label="`${a.label} : ${t.libelle}`"
                      @click="changerTache(t, a.a)"
                    >{{ a.label }}</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <form class="ajout" @submit.prevent="ajouterTache">
          <input v-model="nouvelleTache.libelle" class="g-input" type="text" maxlength="150" placeholder="Ajouter une tâche (ex. Remettre le badge d'accès)" aria-label="Libellé de la nouvelle tâche" />
          <select v-model="nouvelleTache.categorie" class="g-input" aria-label="Catégorie">
            <option v-for="c in CATEGORIES_ONBOARDING" :key="c.value" :value="c.value">{{ c.label }}</option>
          </select>
          <input v-model="nouvelleTache.dateEcheance" class="g-input" type="date" aria-label="Échéance" />
          <button class="g-btn g-btn--primaire" type="submit" :disabled="occupe">Ajouter</button>
        </form>
      </section>

      <!-- Documents -->
      <section class="g-carte" aria-labelledby="titre-docs">
        <div class="bloc-entete"><h2 id="titre-docs">Documents administratifs</h2></div>

        <ul class="checklist" aria-label="Documents attendus">
          <li v-for="c in checklist" :key="c.type">
            <span>{{ libelle(c.type) }}</span>
            <StatutBadge v-if="c.statut" :statut="c.statut" />
            <span v-else class="g-badge g-badge--neutral">Manquant</span>
          </li>
        </ul>

        <div v-if="o.documents.length" class="g-table-wrap">
          <table class="g-table">
            <thead>
              <tr><th>Type</th><th>Fichier</th><th>Déposé le</th><th>Statut</th><th class="g-droite">Actions</th></tr>
            </thead>
            <tbody>
              <tr v-for="d in o.documents" :key="d.id">
                <td class="g-principal">{{ libelle(d.typeDocument) }}</td>
                <td>{{ d.nomFichier }}</td>
                <td>{{ formatDate(d.dateDepot) }}</td>
                <td><StatutBadge :statut="d.statut" /></td>
                <td>
                  <div class="g-actions-cellule">
                    <button type="button" class="g-btn g-btn--petit" @click="voirDocument(d)">Voir</button>
                    <template v-if="d.statut === 'FOURNI'">
                      <button type="button" class="g-btn g-btn--petit g-btn--primaire" :disabled="occupe" :aria-label="`Valider ${d.nomFichier}`" @click="decider(d, 'VALIDE')">Valider</button>
                      <button type="button" class="g-btn g-btn--petit g-btn--danger" :disabled="occupe" :aria-label="`Refuser ${d.nomFichier}`" @click="decider(d, 'REFUSE')">Refuser</button>
                    </template>
                    <button v-if="d.statut !== 'VALIDE'" type="button" class="g-btn g-btn--petit" :disabled="occupe" :aria-label="`Supprimer ${d.nomFichier}`" @click="supprimerDocument(d)">Supprimer</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else class="g-vide">Aucun document reçu pour le moment.</p>

        <form class="ajout" @submit.prevent="ajouterDocument">
          <select v-model="nouveauDoc.type" class="g-input" aria-label="Type de document">
            <option v-for="t in TYPES_DOCUMENT" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>
          <input ref="inputFichier" class="g-input" type="file" accept="application/pdf,image/jpeg,image/png,.pdf,.jpg,.jpeg,.png" aria-label="Fichier du document" @change="choisirFichier" />
          <button class="g-btn g-btn--primaire" type="submit" :disabled="occupe">Ajouter le document</button>
        </form>
        <p class="aide">PDF, JPEG ou PNG — 5 Mo maximum. Un document validé ne peut plus être supprimé.</p>
      </section>
    </template>
  </div>
</template>

<style scoped>
.resume { display: grid; grid-template-columns: minmax(0, 1.6fr) minmax(240px, 1fr); gap: 24px; padding: 22px 24px; align-items: center; }
.resume dl { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 14px 24px; margin: 0; }
.resume dt { font-size: 11.5px; font-weight: 600; letter-spacing: .06em; text-transform: uppercase; color: var(--color-muted); }
.resume dd { margin: 3px 0 0; color: var(--color-ink); font-weight: 500; overflow-wrap: anywhere; }
.progression__texte { display: flex; flex-direction: column; gap: 2px; margin-bottom: 8px; }
.progression__texte strong { font-size: 1.8rem; line-height: 1; color: var(--color-ink); }
.progression__texte span { font-size: 13px; color: var(--color-muted); }
.rouge { color: var(--color-danger); }
.barre { height: 10px; overflow: hidden; border-radius: 999px; background: var(--color-subtle); }
.barre span { display: block; height: 100%; border-radius: 999px; background: linear-gradient(90deg, var(--color-primary), var(--color-secondary)); transition: width .3s; }

.bloc-entete { padding: 16px 20px; border-bottom: 1px solid var(--color-border); }
.bloc-entete h2 { margin: 0; font-size: 1.05rem; color: var(--color-ink); }
.retard { margin-left: 8px; }
.ligne-fermee { opacity: .6; }

.ajout { display: flex; flex-wrap: wrap; gap: 10px; padding: 16px 20px; border-top: 1px solid var(--color-border); background: var(--color-subtle); }
.ajout .g-input { width: auto; flex: 1 1 160px; min-width: 0; }
.ajout input[type="text"] { flex: 3 1 260px; }
.aide { margin: 0; padding: 0 20px 14px; background: var(--color-subtle); font-size: 12.5px; color: var(--color-muted); }
.aide--haut { padding-top: 12px; border-top: 1px solid var(--color-border); }

.checklist { display: flex; flex-wrap: wrap; gap: 10px 22px; margin: 0; padding: 16px 20px; list-style: none; border-bottom: 1px solid var(--color-border); }
.checklist li { display: flex; align-items: center; gap: 8px; font-size: 14px; color: var(--color-text); }

@media (max-width: 900px) { .resume { grid-template-columns: 1fr; } }
</style>
