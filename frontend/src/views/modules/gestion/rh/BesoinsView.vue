<script setup>
// Liste des besoins (demandes) exprimés par les managers.
// Point de départ du circuit : un besoin ACCEPTE peut être transformé en recrutement.
//
// Contrat attendu de GET /api/besoins (liste de BesoinDTO, enveloppée dans ApiResponse) :
//   id, posteNom, departementNom, typeBesoinLibelle, effectif, priorite, lieuTravail,
//   dateArriveeSouhaitee, motif, niveauEtude, experienceRequise, competencesRequises,
//   salaireEstime, statutGlobal
// (les formes imbriquées poste.nom / poste.departement.nom / typeBesoin.libelle sont aussi lues)
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { besoinsApi, recrutementsApi } from "@/services/ressources";
import {
  STATUTS_BESOIN, PRIORITES, posteDe, departementDe, typeDe, formatDate,
} from "@/config/libelles";
import PageEntete from "@/components/gestion/PageEntete.vue";
import StatutBadge from "@/components/gestion/StatutBadge.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import "@/assets/gestion.css";

const router = useRouter();

const besoins = ref([]);
const recrutements = ref([]);
const chargement = ref(true);
const erreur = ref("");

const recherche = ref("");
const filtreStatut = ref("");
const filtrePriorite = ref("");

async function charger() {
  chargement.value = true;
  erreur.value = "";
  try {
    const [b, r] = await Promise.all([besoinsApi.liste(), recrutementsApi.liste()]);
    besoins.value = b ?? [];
    recrutements.value = r ?? [];
  } catch (e) {
    erreur.value = e.message;
  } finally {
    chargement.value = false;
  }
}

const recrutementParBesoin = computed(
  () => new Map(recrutements.value.map((r) => [r.besoinId, r])),
);

const lignes = computed(() => {
  const q = recherche.value.trim().toLowerCase();
  return besoins.value.filter((b) => {
    if (filtreStatut.value && b.statutGlobal !== filtreStatut.value) return false;
    if (filtrePriorite.value && b.priorite !== filtrePriorite.value) return false;
    if (!q) return true;
    return [posteDe(b), departementDe(b), typeDe(b), b.motif, b.competencesRequises]
      .join(" ").toLowerCase().includes(q);
  });
});

const aTraiter = computed(
  () => besoins.value.filter((b) => b.statutGlobal === "ACCEPTE" && !recrutementParBesoin.value.has(b.id)).length,
);

function lancer(b) {
  router.push({ path: "/si/app/rh/recrutements/creation", query: { besoinId: b.id } });
}

onMounted(charger);
</script>

<template>
  <div class="g-page">
    <PageEntete
      titre="Liste des besoins"
      sous-titre="Demandes de recrutement exprimées par les départements. Un besoin accepté peut être transformé en recrutement."
    >
      <button class="g-btn" type="button" @click="charger">Actualiser</button>
      <RouterLink class="g-btn g-btn--primaire" to="/si/app/rh/recrutements/creation">Nouveau recrutement</RouterLink>
    </PageEntete>

    <MessageAlerte :message="erreur" />

    <section class="g-carte">
      <div class="g-filtres">
        <input v-model="recherche" class="g-input g-input--recherche" type="search" placeholder="Rechercher un poste, un département, un motif…" />
        <select v-model="filtreStatut" class="g-input" aria-label="Filtrer par statut">
          <option value="">Tous les statuts</option>
          <option v-for="o in STATUTS_BESOIN" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
        <select v-model="filtrePriorite" class="g-input" aria-label="Filtrer par priorité">
          <option value="">Toutes les priorités</option>
          <option v-for="o in PRIORITES" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
        <span class="g-compteur">
          {{ lignes.length }} besoin(s)
          <template v-if="aTraiter"> · {{ aTraiter }} à transformer en recrutement</template>
        </span>
      </div>

      <p v-if="chargement" class="g-vide">Chargement des besoins…</p>
      <div v-else-if="!lignes.length" class="g-vide">
        <strong>Aucun besoin à afficher</strong>
        Modifiez vos filtres ou attendez de nouvelles demandes.
      </div>

      <div v-else class="g-table-wrap">
        <table class="g-table">
          <thead>
            <tr>
              <th>Poste</th>
              <th>Type</th>
              <th>Effectif</th>
              <th>Priorité</th>
              <th>Arrivée souhaitée</th>
              <th>Statut</th>
              <th class="g-droite">Recrutement</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="b in lignes" :key="b.id">
              <td>
                <span class="g-principal">{{ posteDe(b) }}</span>
                <span class="g-secondaire">{{ departementDe(b) }}<template v-if="b.lieuTravail"> · {{ b.lieuTravail }}</template></span>
              </td>
              <td>{{ typeDe(b) }}</td>
              <td>{{ b.effectif ?? 1 }}</td>
              <td><StatutBadge :statut="b.priorite" /></td>
              <td>{{ formatDate(b.dateArriveeSouhaitee) }}</td>
              <td><StatutBadge :statut="b.statutGlobal" /></td>
              <td>
                <div class="g-actions-cellule">
                  <template v-if="recrutementParBesoin.get(b.id)">
                    <RouterLink class="g-btn g-btn--petit" to="/si/app/rh/recrutements">
                      {{ recrutementParBesoin.get(b.id).reference }}
                    </RouterLink>
                  </template>
                  <button v-else-if="b.statutGlobal === 'ACCEPTE'" class="g-btn g-btn--primaire g-btn--petit" type="button" @click="lancer(b)">
                    Lancer le recrutement
                  </button>
                  <span v-else class="g-secondaire">En attente de validation</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
