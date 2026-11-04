<script setup>
// CV d'un candidat : /si/app/rh/candidats/:candidatId/cv?cvId=…
// Permet de lire le CV puis de décider (si la candidature est EN_ATTENTE).
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import "@/assets/gestion.css";
import PageEntete from "@/components/gestion/PageEntete.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import CandidatureActions from "@/components/gestion/CandidatureActions.vue";
import { cvsApi } from "@/services/ressources";
import { candidaturesRhApi } from "@/services/candidaturesRh";
import { formatDate } from "@/config/libelles";

const route = useRoute();
const router = useRouter();

const ligne = ref(null);
const urlPdf = ref("");
const chargement = ref(true);
const erreur = ref("");

let candidatId = Number(route.params.candidatId);
let cvId = Number(route.query.cvId);

function libererUrl() {
  if (urlPdf.value) URL.revokeObjectURL(urlPdf.value);
  urlPdf.value = "";
}

async function charger() {
  chargement.value = true;
  erreur.value = "";
  libererUrl();
  try {
    if (!cvId) throw new Error("L'identifiant du CV est absent de l'adresse.");
    const toutes = (await candidaturesRhApi.liste()) ?? [];
    ligne.value = toutes.find((l) => l.cvId === cvId && l.candidatId === candidatId) ?? null;
    if (!ligne.value) throw new Error("CV introuvable pour ce candidat.");

    const blob = await cvsApi.fichier(cvId);
    if (!blob || blob.size === 0) throw new Error("Le fichier CV reçu est vide.");
    urlPdf.value = URL.createObjectURL(blob);

    if (ligne.value.statutLecture === "NON_LU") {
      candidaturesRhApi.marquerLu(cvId).catch(() => {}); // non bloquant
      ligne.value = { ...ligne.value, statutLecture: "LU" };
    }
  } catch (e) {
    erreur.value = e.message ?? "Impossible de charger le CV.";
  } finally {
    chargement.value = false;
  }
}

function majStatut({ statut, remarque }) {
  erreur.value = "";
  ligne.value = { ...ligne.value, statut, remarque };
}

async function surErreur({ message, status }) {
  if (status === 409 || status === 404) {
    try {
      const toutes = (await candidaturesRhApi.liste()) ?? [];
      ligne.value = toutes.find((l) => l.cvId === cvId && l.candidatId === candidatId) ?? ligne.value;
    } catch {
      /* on garde l'affichage actuel */
    }
  }
  erreur.value = message;
}

const complet = computed(() => (ligne.value ? `${ligne.value.prenom} ${ligne.value.nom}` : "Candidat"));

onMounted(charger);

// Même composant réutilisé pour un autre CV : on recharge.
watch(
  () => [route.params.candidatId, route.query.cvId],
  ([nouveauCandidat, nouveauCv]) => {
    if (nouveauCandidat === undefined) return;
    candidatId = Number(nouveauCandidat);
    cvId = Number(nouveauCv);
    charger();
  },
);
onBeforeUnmount(libererUrl);
</script>

<template>
  <div class="g-page">
    <PageEntete :titre="complet" :sous-titre="ligne?.candidatureId ? `${ligne.poste} · ${ligne.recrutementReference}` : 'Candidature spontanée'">
      <button class="g-btn" type="button" @click="router.back()">← Retour aux candidatures</button>
    </PageEntete>

    <MessageAlerte :message="erreur" />
    <p v-if="chargement" class="g-vide">Chargement du CV…</p>

    <div v-if="ligne" class="cv-page">
      <aside class="g-carte fiche">
        <h2>Candidat</h2>
        <dl>
          <div><dt>E-mail</dt><dd>{{ ligne.email }}</dd></div>
          <div><dt>Téléphone</dt><dd>{{ ligne.telephone || "—" }}</dd></div>
          <div><dt>Expérience</dt><dd>{{ ligne.experience ?? "—" }}<template v-if="ligne.experience !== null && ligne.experience !== undefined"> an(s)</template></dd></div>
          <div><dt>Reçu le</dt><dd>{{ formatDate(ligne.dateCandidature ?? ligne.dateDepot) }}</dd></div>
          <div><dt>Fichier</dt><dd>{{ ligne.nomFichier }}</dd></div>
        </dl>

        <div class="fiche__decision">
          <h3>Décision</h3>
          <CandidatureActions :ligne="ligne" @statut-change="majStatut" @erreur="surErreur" />
          <p v-if="ligne.candidatureId && ligne.statut === 'EN_ATTENTE'" class="aide">Cette candidature attend votre décision.</p>
        </div>

        <a v-if="urlPdf" class="g-btn" :href="urlPdf" target="_blank" rel="noopener noreferrer">Ouvrir le PDF dans un onglet</a>
      </aside>

      <section class="g-carte lecteur" aria-label="CV du candidat">
        <iframe v-if="urlPdf" :src="urlPdf" title="CV du candidat" class="pdf"></iframe>
      </section>
    </div>
  </div>
</template>

<style scoped>
.cv-page { display: grid; grid-template-columns: minmax(260px, 320px) minmax(0, 1fr); gap: 20px; align-items: start; }
.fiche { display: flex; flex-direction: column; gap: 16px; padding: 20px; }
.fiche h2 { margin: 0; font-size: 1.05rem; color: var(--color-ink); }
.fiche h3 { margin: 0 0 8px; font-size: .95rem; color: var(--color-ink); }
.fiche dl { display: flex; flex-direction: column; gap: 12px; margin: 0; }
.fiche dt { font-size: 11.5px; font-weight: 600; letter-spacing: .06em; text-transform: uppercase; color: var(--color-muted); }
.fiche dd { margin: 2px 0 0; color: var(--color-ink); overflow-wrap: anywhere; }
.fiche__decision { padding-top: 14px; border-top: 1px solid var(--color-border); }
.aide { margin: 8px 0 0; font-size: 12.5px; color: var(--color-muted); }
.lecteur { min-height: 70vh; background: var(--color-subtle); }
.pdf { display: block; width: 100%; height: 78vh; min-height: 500px; border: 0; }
@media (max-width: 900px) { .cv-page { grid-template-columns: 1fr; } }
</style>
