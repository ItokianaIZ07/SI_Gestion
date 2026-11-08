<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import PageEntete from "@/components/gestion/PageEntete.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import StatutBadge from "@/components/gestion/StatutBadge.vue";
import ChangementAffectationForm from "@/components/rh/ChangementAffectationForm.vue";
import DocumentsAdminPanel from "@/components/rh/DocumentsAdminPanel.vue";
import FriseCarriere from "@/components/rh/FriseCarriere.vue";
import { employeApi } from "@/services/employeService";
import { aujourdhui, formatDate, formatMontant } from "@/config/libelles";
import { useAuthStore } from "@/stores/authStore";

const route = useRoute();
const authStore = useAuthStore();
const employeId = computed(() => Number(route.params.id));

const fiche = ref(null);
const chargement = ref(true);
const erreur = ref("");
const succes = ref("");
const mutationOuverte = ref(false);
const dialogueDepart = ref(false);
const dateDepart = ref(aujourdhui());
const controlesDepart = ref(null);
const departEnCours = ref(false);
const estRh = computed(() => authStore.user?.role?.toUpperCase() === "RH");
const peutConfirmerDepart = computed(() =>
  Boolean(dateDepart.value)
  && !departEnCours.value
  && (!fiche.value?.dateEmbauche || dateDepart.value >= fiche.value.dateEmbauche)
  && dateDepart.value <= aujourdhui(),
);

async function charger() {
  chargement.value = true;
  erreur.value = "";
  try {
    fiche.value = await employeApi.fiche(employeId.value);
  } catch (e) {
    fiche.value = null;
    erreur.value = e.message;
  } finally {
    chargement.value = false;
  }
}

async function recharger() {
  try {
    fiche.value = await employeApi.fiche(employeId.value);
  } catch (e) {
    erreur.value = e.message;
  }
}

async function apresMutation() {
  mutationOuverte.value = false;
  succes.value = "Affectation modifiée : l'ancienne affectation reste visible dans l'historique de carrière.";
  await recharger();
}

async function ouvrirDialogueDepart() {
  erreur.value = "";
  succes.value = "";
  dateDepart.value = aujourdhui();
  try {
    controlesDepart.value = await employeApi.controlesDepart(employeId.value);
    dialogueDepart.value = true;
  } catch (e) {
    erreur.value = e.message || "Impossible de charger les contrôles avant départ.";
  }
}

async function confirmerDepart() {
  if (!peutConfirmerDepart.value) return;
  erreur.value = "";
  departEnCours.value = true;
  try {
    fiche.value = await employeApi.marquerParti(employeId.value, dateDepart.value);
    dialogueDepart.value = false;
    mutationOuverte.value = false;
    succes.value = "Le salarié a été marqué comme parti.";
  } catch (e) {
    erreur.value = e.message || "Impossible d'enregistrer le départ du salarié.";
  } finally {
    departEnCours.value = false;
  }
}

onMounted(charger);
watch(employeId, () => {
  mutationOuverte.value = false;
  dialogueDepart.value = false;
  succes.value = "";
  charger();
});

const parti = computed(() => fiche.value?.statut === "PARTI");
const actuelle = computed(() => fiche.value?.affectationActuelle ?? null);
</script>

<template>
  <div class="g-page">
    <PageEntete :titre="fiche ? `${fiche.prenom} ${fiche.nom}` : 'Fiche salarié'"
      :sous-titre="actuelle ? `${actuelle.poste} · ${actuelle.departement} · ${fiche.matricule}` : (fiche ? fiche.matricule : '')">
      <RouterLink class="g-btn" to="/si/app/rh/employes">← Retour à la liste</RouterLink>
    </PageEntete>

    <MessageAlerte :message="erreur" />
    <MessageAlerte :message="succes" type="succes" />
    <p v-if="chargement" class="g-vide">Chargement…</p>

    <template v-if="fiche">
      <!-- Identité -->
      <section class="g-carte resume" aria-labelledby="titre-identite">
        <h2 id="titre-identite" class="sr-only">Identité et coordonnées</h2>
        <dl>
          <div>
            <dt>E-mail</dt>
            <dd>{{ fiche.email }}</dd>
          </div>
          <div>
            <dt>Téléphone</dt>
            <dd>{{ fiche.telephone || "—" }}</dd>
          </div>
          <div>
            <dt>N° CIN</dt>
            <dd>{{ fiche.numeroCin || "—" }}</dd>
          </div>
          <div>
            <dt>Date de naissance</dt>
            <dd>{{ formatDate(fiche.dateNaissance) }}</dd>
          </div>
          <div>
            <dt>Adresse</dt>
            <dd>{{ fiche.adresse || "—" }}</dd>
          </div>
          <div>
            <dt>Embauché le</dt>
            <dd>{{ formatDate(fiche.dateEmbauche) }}</dd>
          </div>
          <div v-if="fiche.dateDepart">
            <dt>Parti le</dt>
            <dd>{{ formatDate(fiche.dateDepart) }}</dd>
          </div>
          <div>
            <dt>Statut</dt>
            <dd>
              <StatutBadge :statut="fiche.statut" />
            </dd>
          </div>
          <div>
            <dt>Espace personnel</dt>
            <dd>{{ fiche.aCompteUtilisateur ? "Oui (alertes envoyées au salarié)" : "Non (alertes envoyées aux RH)" }}
            </dd>
          </div>
        </dl>
      </section>

      <section v-if="estRh && !parti" class="g-carte depart-actions">
        <div>
          <h2>Départ du salarié</h2>
          <p>Enregistrez son départ et clôturez ses affectations, droits de congé et planning.</p>
        </div>
        <button type="button" class="g-btn g-btn--danger" @click="ouvrirDialogueDepart">
          Marquer comme parti
        </button>
      </section>

      <!-- Affectation actuelle + mutation -->
      <section class="g-carte" aria-labelledby="titre-affectation">
        <div class="bloc-entete bloc-entete--actions">
          <h2 id="titre-affectation">Affectation actuelle</h2>
          <button v-if="!parti && !mutationOuverte" type="button" class="g-btn g-btn--primaire"
            @click="mutationOuverte = true">Changer d'affectation</button>
        </div>
        <p v-if="!actuelle" class="g-vide">{{ parti ? "Ce salarié a quitté l'entreprise : plus d'affectation en cours."
          : "Aucune affectation enregistrée." }}</p>
        <dl v-else class="bloc-dl">
          <div>
            <dt>Département</dt>
            <dd>{{ actuelle.departement }}</dd>
          </div>
          <div>
            <dt>Poste</dt>
            <dd>{{ actuelle.poste }}</dd>
          </div>
          <div>
            <dt>Supérieur hiérarchique</dt>
            <dd>{{ actuelle.superieurNom || "—" }}</dd>
          </div>
          <div>
            <dt>Depuis le</dt>
            <dd>{{ formatDate(actuelle.dateDebut) }}</dd>
          </div>
        </dl>
        <ChangementAffectationForm v-if="mutationOuverte" :employe-id="fiche.id" :affectation="actuelle"
          @modifie="apresMutation" @annule="mutationOuverte = false" />
      </section>

      <!-- Contrat actif et salaire -->
      <section class="g-carte" aria-labelledby="titre-contrat">
        <div class="bloc-entete">
          <h2 id="titre-contrat">Contrat actif et salaire</h2>
        </div>
        <p v-if="!fiche.contratActif" class="g-vide">Aucun contrat actif pour ce salarié.</p>
        <dl v-else class="bloc-dl">
          <div>
            <dt>Référence</dt>
            <dd>{{ fiche.contratActif.reference }}</dd>
          </div>
          <div>
            <dt>Type</dt>
            <dd>{{ fiche.contratActif.typeContrat }}</dd>
          </div>
          <div>
            <dt>Salaire</dt>
            <dd>{{ formatMontant(fiche.contratActif.salaire) ?? "Non renseigné" }}</dd>
          </div>
          <div>
            <dt>Début</dt>
            <dd>{{ formatDate(fiche.contratActif.dateDebut) }}</dd>
          </div>
          <div>
            <dt>Fin</dt>
            <dd>{{ fiche.contratActif.dateFin ? formatDate(fiche.contratActif.dateFin) : "Durée indéterminée" }}</dd>
          </div>
          <div>
            <dt>Signé le</dt>
            <dd>{{ formatDate(fiche.contratActif.dateSignature) }}</dd>
          </div>
          <div>
            <dt>Période d'essai</dt>
            <dd>{{ fiche.contratActif.periodeEssaiJours != null ? `${fiche.contratActif.periodeEssaiJours} jours` : "—"
              }}</dd>
          </div>
        </dl>

        <template v-if="fiche.contrats.length">
          <div class="g-table-wrap">
            <table class="g-table">
              <caption class="sr-only">Tous les contrats du salarié</caption>
              <thead>
                <tr>
                  <th>Référence</th>
                  <th>Type</th>
                  <th>Début</th>
                  <th>Fin</th>
                  <th>Signé le</th>
                  <th>Statut</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in fiche.contrats" :key="c.id">
                  <td>{{ c.reference }}</td>
                  <td>{{ c.typeContrat }}</td>
                  <td>{{ formatDate(c.dateDebut) }}</td>
                  <td>{{ c.dateFin ? formatDate(c.dateFin) : "—" }}</td>
                  <td>{{ formatDate(c.dateSignature) }}</td>
                  <td>
                    <StatutBadge :statut="c.statut" />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
        <p class="aide aide--haut">Un salarié ne peut avoir qu'un seul contrat actif à la fois (contrôlé par la base de
          données).</p>
      </section>

      <!-- Documents administratifs -->
      <section class="g-carte" aria-labelledby="titre-documents">
        <div class="bloc-entete">
          <h2 id="titre-documents">Documents administratifs</h2>
        </div>
        <DocumentsAdminPanel
          :employe-id="fiche.id"
          :documents="fiche.documents"
          :lecture-seule="parti"
          @modifie="recharger"
        />
      </section>

      <!-- Carrière -->
      <section class="g-carte" aria-labelledby="titre-carriere">
        <div class="bloc-entete">
          <h2 id="titre-carriere">Historique de carrière</h2>
        </div>
        <FriseCarriere :carriere="fiche.carriere" />
      </section>
    </template>

    <div v-if="dialogueDepart" class="dialogue-fond" @click.self="dialogueDepart = false">
      <section
        class="dialogue-depart"
        role="dialog"
        aria-modal="true"
        aria-labelledby="titre-dialogue-depart"
        aria-describedby="description-dialogue-depart"
      >
        <header class="dialogue-depart__entete">
          <h2 id="titre-dialogue-depart">Confirmer le départ</h2>
          <button type="button" class="g-btn g-btn--petit" :disabled="departEnCours" @click="dialogueDepart = false">
            Fermer
          </button>
        </header>

        <p id="description-dialogue-depart">
          Le statut passera immédiatement à <strong>PARTI</strong>. Le départ ne peut pas être annulé depuis cette fiche.
        </p>

        <label class="g-champ dialogue-depart__date">
          <span>Date de départ <b class="g-obligatoire">*</b></span>
          <input
            v-model="dateDepart"
            class="g-input"
            type="date"
            required
            :min="fiche.dateEmbauche"
            :max="aujourdhui()"
          />
        </label>

        <div class="dialogue-depart__effets">
          <strong>Effets du départ</strong>
          <ul>
            <li>L’affectation en cours sera fermée à la date choisie.</li>
            <li>Les droits de congé seront clôturés et désactivés.</li>
            <li>Le planning de travail sera clôturé.</li>
            <li>Le compte utilisateur, le contrat et les subordonnés ne seront pas modifiés.</li>
          </ul>
        </div>

        <div v-if="controlesDepart" class="dialogue-depart__controles">
          <strong>Points à vérifier</strong>
          <ul>
            <li v-if="controlesDepart.nbSubordonnesDirects">
              Ce salarié a {{ controlesDepart.nbSubordonnesDirects }} subordonné(s) direct(s). Ils resteront rattachés à lui.
            </li>
            <li v-if="controlesDepart.contratActif">Il a un contrat actif, qui ne sera pas clôturé.</li>
            <li v-if="controlesDepart.demandesCongeEnAttente">
              {{ controlesDepart.demandesCongeEnAttente }} demande(s) de congé en attente ne seront pas modifiées.
            </li>
            <li v-if="controlesDepart.missionsEnCours">
              {{ controlesDepart.missionsEnCours }} mission(s) en cours ne seront pas modifiées.
            </li>
            <li v-if="!controlesDepart.nbSubordonnesDirects && !controlesDepart.contratActif
              && !controlesDepart.demandesCongeEnAttente && !controlesDepart.missionsEnCours">
              Aucun point particulier détecté.
            </li>
          </ul>
        </div>

        <p v-if="dateDepart && fiche.dateEmbauche && dateDepart < fiche.dateEmbauche" class="dialogue-depart__erreur">
          La date de départ ne peut pas précéder la date d'embauche.
        </p>
        <p v-else-if="dateDepart > aujourdhui()" class="dialogue-depart__erreur">
          La date de départ ne peut pas être dans le futur.
        </p>

        <footer class="dialogue-depart__actions">
          <button type="button" class="g-btn" :disabled="departEnCours" @click="dialogueDepart = false">
            Annuler
          </button>
          <button
            type="button"
            class="g-btn g-btn--danger"
            :disabled="!peutConfirmerDepart"
            @click="confirmerDepart"
          >
            {{ departEnCours ? "Enregistrement…" : "Confirmer le départ" }}
          </button>
        </footer>
      </section>
    </div>
  </div>
</template>

<style scoped>
.resume {
  padding: 22px 24px;
}

.depart-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 22px;
}

.depart-actions h2 {
  margin: 0 0 5px;
  font-size: 1.05rem;
}

.depart-actions p {
  margin: 0;
  color: var(--color-muted);
  font-size: 13px;
}

.dialogue-fond {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  padding: 18px;
  background: rgb(18 28 18 / 48%);
}

.dialogue-depart {
  width: min(620px, 100%);
  max-height: min(90vh, 780px);
  overflow: auto;
  padding: 22px;
  background: var(--color-card, #fff);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  box-shadow: 0 18px 60px rgb(0 0 0 / 22%);
}

.dialogue-depart__entete,
.dialogue-depart__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.dialogue-depart__entete h2 {
  margin: 0;
}

.dialogue-depart__date {
  display: grid;
  gap: 7px;
  margin: 18px 0;
}

.dialogue-depart__effets,
.dialogue-depart__controles {
  margin-top: 14px;
  padding: 14px 16px;
  background: var(--color-subtle, #f5f8f5);
  border-radius: 9px;
}

.dialogue-depart__effets ul,
.dialogue-depart__controles ul {
  display: grid;
  gap: 6px;
  margin: 8px 0 0;
  padding-left: 20px;
  color: var(--color-muted);
  font-size: 13px;
}

.dialogue-depart__erreur {
  color: #a32525;
  font-size: 13px;
}

.dialogue-depart__actions {
  justify-content: flex-end;
  margin-top: 20px;
}

.resume dl,
.bloc-dl {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 14px 24px;
  margin: 0;
}

.bloc-dl {
  padding: 18px 20px;
}

.resume dt,
.bloc-dl dt {
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: .06em;
  text-transform: uppercase;
  color: var(--color-muted);
}

.resume dd,
.bloc-dl dd {
  margin: 3px 0 0;
  color: var(--color-ink);
  font-weight: 500;
  overflow-wrap: anywhere;
}

.bloc-entete {
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
}

.bloc-entete h2 {
  margin: 0;
  font-size: 1.05rem;
  color: var(--color-ink);
}

.bloc-entete--actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.aide {
  margin: 0;
  padding: 0 20px 14px;
  font-size: 12.5px;
  color: var(--color-muted);
}

.aide--haut {
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
}

.sr-only {
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

@media (max-width: 600px) {
  .depart-actions {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
