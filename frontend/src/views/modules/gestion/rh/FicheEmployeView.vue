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
import { formatDate, formatMontant } from "@/config/libelles";

const route = useRoute();
const employeId = computed(() => Number(route.params.id));

const fiche = ref(null);
const chargement = ref(true);
const erreur = ref("");
const succes = ref("");
const mutationOuverte = ref(false);

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

onMounted(charger);
watch(employeId, () => {
  mutationOuverte.value = false;
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
        <DocumentsAdminPanel :employe-id="fiche.id" :documents="fiche.documents" @modifie="recharger" />
      </section>

      <!-- Carrière -->
      <section class="g-carte" aria-labelledby="titre-carriere">
        <div class="bloc-entete">
          <h2 id="titre-carriere">Historique de carrière</h2>
        </div>
        <FriseCarriere :carriere="fiche.carriere" />
      </section>
    </template>
  </div>
</template>

<style scoped>
.resume {
  padding: 22px 24px;
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
</style>
