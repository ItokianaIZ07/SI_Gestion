<script setup>
// Liste de candidatures/CV (à gauche) + aperçu du PDF (à droite).
// Utilisé par « CV reçus » (tous les CV) et par « Candidatures d'une offre ».
import { computed, onBeforeUnmount, ref } from "vue";
import ApercuPDF from "@/components/ApercuPDF.vue";
import Pagination from "@/components/ui/Pagination.vue";
import { usePagination } from "@/composable/usePagination";
import CandidatureItem from "./CandidatureItem.vue";
import { cvsApi } from "@/services/ressources";
import { candidaturesRhApi } from "@/services/candidaturesRh";
import { STATUTS_CANDIDATURE } from "@/config/libelles";

const props = defineProps({
  lignes: { type: Array, default: () => [] },
  chargement: { type: Boolean, default: false },
  titre: { type: String, default: "CV reçus" },
  sousTitre: { type: String, default: "Consultez les candidatures reçues." },
  afficherOffre: { type: Boolean, default: true },
});
const emit = defineEmits(["statut-change", "erreur", "cv-lu"]);

const recherche = ref("");
const filtreStatut = ref("");
const ordre = ref("desc");

const selection = ref(null);
const pdf = ref("");
const erreurApercu = ref("");
const chargementApercu = ref(false);
let jeton = 0; // ignore la réponse d'un aperçu devenu obsolète

const cle = (l) => `${l.cvId}-${l.candidatureId ?? 0}`;
const horodatage = (l) => new Date(l.dateCandidature ?? l.dateDepot ?? 0).getTime();

const filtrees = computed(() => {
  const q = recherche.value.trim().toLowerCase();
  return props.lignes
    .filter((l) => {
      if (filtreStatut.value === "SPONTANE") {
        if (l.candidatureId) return false;
      } else if (filtreStatut.value && l.statut !== filtreStatut.value) {
        return false;
      }
      if (!q) return true;
      return [l.prenom, l.nom, l.email, l.poste, l.recrutementReference].join(" ").toLowerCase().includes(q);
    })
    .sort((a, b) => (ordre.value === "asc" ? horodatage(a) - horodatage(b) : horodatage(b) - horodatage(a)));
});

const pg = usePagination(filtrees, 8);

function viderApercu() {
  if (pdf.value) URL.revokeObjectURL(pdf.value);
  pdf.value = "";
}

async function selectionner(ligne) {
  const mien = ++jeton;
  selection.value = ligne;
  erreurApercu.value = "";
  viderApercu();
  chargementApercu.value = true;
  try {
    const blob = await cvsApi.fichier(ligne.cvId);
    if (mien !== jeton) return;
    pdf.value = URL.createObjectURL(blob);
    if (ligne.statutLecture === "NON_LU") {
      candidaturesRhApi
        .marquerLu(ligne.cvId)
        .then(() => emit("cv-lu", ligne.cvId))
        .catch(() => {}); // non bloquant : le CV reste simplement « Nouveau »
    }
  } catch (e) {
    if (mien === jeton) erreurApercu.value = e.message ?? "Impossible de charger le CV.";
  } finally {
    if (mien === jeton) chargementApercu.value = false;
  }
}

onBeforeUnmount(viderApercu);
</script>

<template>
  <div class="cvp">
    <!-- Panneau gauche : liste -->
    <section class="cvp__panneau cvp__liste">
      <header class="cvp__entete">
        <div>
          <h2>{{ titre }}</h2>
          <p>{{ sousTitre }}</p>
        </div>
        <span class="cvp__compteur" :aria-label="`${lignes.length} CV`">{{ lignes.length }}</span>
      </header>

      <div class="cvp__filtres">
        <input v-model="recherche" type="search" placeholder="Rechercher par nom, e-mail, poste…" aria-label="Rechercher un candidat" />
        <select v-model="filtreStatut" aria-label="Filtrer par statut">
          <option value="">Tous les statuts</option>
          <option v-for="o in STATUTS_CANDIDATURE" :key="o.value" :value="o.value">{{ o.label }}</option>
          <option v-if="afficherOffre" value="SPONTANE">Dépôts spontanés</option>
        </select>
        <button type="button" class="cvp__tri" @click="ordre = ordre === 'asc' ? 'desc' : 'asc'">
          {{ ordre === "asc" ? "↑ Plus ancien" : "↓ Plus récent" }}
        </button>
      </div>

      <div class="cvp__contenu">
        <p v-if="chargement" class="cvp__vide">Chargement…</p>
        <p v-else-if="!filtrees.length" class="cvp__vide">
          {{ lignes.length ? "Aucun résultat pour ces filtres." : "Aucun CV reçu pour le moment." }}
        </p>
        <CandidatureItem
          v-for="l in pg.items"
          :key="cle(l)"
          :ligne="l"
          :actif="!!selection && cle(selection) === cle(l)"
          :afficher-offre="afficherOffre"
          @select="selectionner"
          @statut-change="emit('statut-change', $event)"
          @erreur="emit('erreur', $event)"
        />
      </div>

      <Pagination v-bind="pg.barre" compact @update:page="pg.aller" />
    </section>

    <!-- Panneau droit : aperçu -->
    <section class="cvp__panneau cvp__apercu">
      <header class="cvp__entete">
        <div>
          <h2>Aperçu du CV</h2>
          <p>{{ selection ? `${selection.prenom} ${selection.nom} — ${selection.nomFichier}` : "Sélectionnez un candidat pour consulter son CV." }}</p>
        </div>
        <span v-if="selection" class="cvp__type">PDF</span>
      </header>

      <div class="cvp__pdf">
        <p v-if="erreurApercu" class="g-alerte g-alerte--erreur" role="alert">{{ erreurApercu }}</p>
        <p v-else-if="chargementApercu" class="cvp__vide">Chargement du CV…</p>
        <ApercuPDF v-else-if="pdf" :file="pdf" />
        <div v-else class="cvp__placeholder">
          <h3>Aucun CV sélectionné</h3>
          <p>Choisissez un candidat dans la liste pour afficher son document ici.</p>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.cvp { display: grid; grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1.7fr); gap: 20px; align-items: stretch; width: 100%; min-height: 650px; }
.cvp__panneau { min-width: 0; display: flex; flex-direction: column; overflow: hidden; background: #fff; border: 1px solid var(--color-border); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); }
.cvp__liste { max-height: 820px; }
.cvp__apercu { min-height: 650px; }

.cvp__entete { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 18px 20px; border-bottom: 1px solid #f0f1ef; }
.cvp__entete h2 { margin: 0; color: #252a26; font-size: 18px; font-weight: 700; }
.cvp__entete p { margin: 6px 0 0; color: #858b86; font-size: 13px; line-height: 1.5; overflow-wrap: anywhere; }
.cvp__compteur { display: flex; align-items: center; justify-content: center; min-width: 32px; height: 32px; padding: 0 8px; border: 1px solid #dcebdc; border-radius: 8px; background: #f0f7f0; color: var(--color-primary); font-size: 13px; font-weight: 700; }
.cvp__type { padding: 5px 9px; border: 1px solid #e5e7e3; border-radius: 5px; background: #f7f8f6; color: #697169; font-size: 11px; font-weight: 700; }

.cvp__filtres { display: flex; flex-wrap: wrap; gap: 8px; padding: 12px 16px; border-bottom: 1px solid #f0f1ef; }
.cvp__filtres input, .cvp__filtres select, .cvp__tri { padding: 9px 11px; border: 1px solid var(--color-border); border-radius: 8px; background: #fff; color: #343a35; font: inherit; font-size: 13px; }
.cvp__filtres input { flex: 1 1 180px; min-width: 0; }
.cvp__filtres input:focus, .cvp__filtres select:focus { outline: none; border-color: var(--color-primary); box-shadow: var(--shadow-focus); }
.cvp__tri { font-weight: 600; cursor: pointer; }
.cvp__tri:hover { background: #f3f6f1; }

.cvp__contenu { display: flex; flex-direction: column; gap: 8px; padding: 12px; overflow-y: auto; background: #fdfdfc; flex: 1; }
.cvp__vide { margin: 0; padding: 24px 12px; color: #858b86; font-size: 13px; text-align: center; }

.cvp__pdf { flex: 1; min-height: 0; padding: 16px; background: #f4f5f3; }
.cvp__pdf > :deep(*) { width: 100%; }
.cvp__placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 480px; padding: 24px; text-align: center; border: 1px dashed #dfe3dc; border-radius: 10px; background: #fafbf9; }
.cvp__placeholder h3 { margin: 0; color: #343a35; font-size: 17px; font-weight: 650; }
.cvp__placeholder p { max-width: 300px; margin: 10px 0 0; color: #858b85; font-size: 13px; line-height: 1.7; }

@media (max-width: 850px) {
  .cvp { grid-template-columns: 1fr; gap: 16px; }
  .cvp__liste { max-height: 560px; }
  .cvp__apercu { min-height: 500px; }
}
</style>
