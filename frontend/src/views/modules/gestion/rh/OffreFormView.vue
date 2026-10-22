<script setup>

import { ref, reactive, computed, watch, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { offresApi, recrutementsApi, besoinsApi } from "@/services/ressources";
import { STATUTS_OFFRE, formatMontant } from "@/config/libelles";
import PageEntete from "@/components/gestion/PageEntete.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import "@/assets/gestion.css";

const route = useRoute();
const router = useRouter();

const id = route.params.id ? Number(route.params.id) : null;
const edition = id !== null;

const form = reactive({
  recrutementId: "",
  titre: "",
  descriptionPoste: "",
  missions: "",
  competencesRequises: "",
  niveauEtude: "",
  experienceMinAnnees: 0,
  salaireMin: "",
  salaireMax: "",
  statut: "BROUILLON",
});

const recrutements = ref([]);
const offres = ref([]);
const besoins = ref(null); // null = indisponible (le pré-remplissage depuis le besoin est alors ignoré)
const chargement = ref(true);
const envoi = ref(false);
const erreur = ref("");

const recrutementsDisponibles = computed(() => {
  const pris = new Set(offres.value.filter((o) => o.id !== id).map((o) => o.recrutementId));
  return recrutements.value.filter((r) => !pris.has(r.id));
});

const besoinLie = computed(() => {
  const r = recrutements.value.find((x) => x.id === Number(form.recrutementId));
  return r && besoins.value ? besoins.value.find((b) => b.id === r.besoinId) ?? null : null;
});

// Pré-remplit uniquement les champs encore vides (création)
function preremplir() {
  if (edition) return;
  const r = recrutements.value.find((x) => x.id === Number(form.recrutementId));
  if (!r) return;
  if (!form.titre) form.titre = r.titre;
  if (!form.descriptionPoste) form.descriptionPoste = r.description ?? "";
  const b = besoinLie.value;
  if (b) {
    if (!form.competencesRequises) form.competencesRequises = b.competencesRequises ?? "";
    if (!form.niveauEtude) form.niveauEtude = b.niveauEtude ?? "";
    if (!form.experienceMinAnnees) form.experienceMinAnnees = parseInt(b.experienceRequise, 10) || 0;
  }
}
watch(() => form.recrutementId, preremplir);

onMounted(async () => {
  try {
    const [r, o, b] = await Promise.all([
      recrutementsApi.liste(),
      offresApi.liste(),
      besoinsApi.liste().catch(() => null),
    ]);
    recrutements.value = r ?? [];
    offres.value = o ?? [];
    besoins.value = b;

    if (edition) {
      const offre = offres.value.find((x) => x.id === id) ?? (await offresApi.parId(id));
      Object.assign(form, {
        recrutementId: offre.recrutementId,
        titre: offre.titre,
        descriptionPoste: offre.descriptionPoste,
        missions: offre.missions,
        competencesRequises: offre.competencesRequises ?? "",
        niveauEtude: offre.niveauEtude ?? "",
        experienceMinAnnees: offre.experienceMinAnnees ?? 0,
        salaireMin: offre.salaireMin ?? "",
        salaireMax: offre.salaireMax ?? "",
        statut: offre.statut,
      });
    } else if (route.query.recrutementId) {
      form.recrutementId = Number(route.query.recrutementId);
      preremplir();
    }
  } catch (e) {
    erreur.value = e.message;
  } finally {
    chargement.value = false;
  }
});

const nombreOuNull = (v) => (v === "" || v === null ? null : Number(v));

async function soumettre(publierEnsuite = false) {
  erreur.value = "";
  if (!form.recrutementId) {
    erreur.value = "Sélectionnez le recrutement concerné.";
    return;
  }
  const min = nombreOuNull(form.salaireMin);
  const max = nombreOuNull(form.salaireMax);
  if (min !== null && max !== null && min > max) {
    erreur.value = "Le salaire minimum ne peut pas dépasser le salaire maximum.";
    return;
  }

  const charge = {
    recrutementId: Number(form.recrutementId),
    titre: form.titre.trim(),
    descriptionPoste: form.descriptionPoste.trim(),
    missions: form.missions.trim(),
    competencesRequises: form.competencesRequises.trim() || null,
    niveauEtude: form.niveauEtude.trim() || null,
    experienceMinAnnees: Number(form.experienceMinAnnees) || 0,
    salaireMin: min,
    salaireMax: max,
    statut: form.statut,
  };

  envoi.value = true;
  try {
    const o = edition ? await offresApi.modifier(id, charge) : await offresApi.creer(charge);
    if (publierEnsuite) {
      router.push({ path: "/si/app/rh/publications/creation", query: { offreId: o.id } });
    } else {
      router.push("/si/app/rh/offres");
    }
  } catch (e) {
    erreur.value = e.message;
  } finally {
    envoi.value = false;
  }
}
</script>

<template>
  <div class="g-page" style="max-width: 860px">
    <PageEntete :titre="edition ? 'Modifier l\'offre d\'emploi' : 'Nouvelle offre d\'emploi'"
      sous-titre="Seules les offres au statut « Validée » peuvent être publiées aux candidats.">
      <RouterLink class="g-btn" to="/si/app/rh/offres">← Retour à la liste</RouterLink>
    </PageEntete>

    <MessageAlerte :message="erreur" />

    <p v-if="chargement" class="g-vide">Chargement…</p>

    <form v-else class="g-carte g-form" @submit.prevent="soumettre(false)">
      <div class="g-champ g-champ--large">
        <label for="recrutement" class="g-obligatoire">Recrutement concerné</label>
        <select id="recrutement" v-model="form.recrutementId" class="g-input" :disabled="edition" required>
          <option value="" disabled>Sélectionner un recrutement…</option>
          <option v-for="r in recrutementsDisponibles" :key="r.id" :value="r.id">{{ r.reference }} — {{ r.titre }}
          </option>
        </select>
        <small v-if="!edition && !recrutementsDisponibles.length">
          Tous les recrutements ont déjà une offre. Une offre est limitée à un recrutement.
        </small>
      </div>

      <div class="g-champ g-champ--large">
        <label for="titre" class="g-obligatoire">Titre de l'offre</label>
        <input id="titre" v-model="form.titre" class="g-input" type="text" maxlength="150" required />
      </div>

      <div class="g-champ g-champ--large">
        <label for="descriptionPoste" class="g-obligatoire">Description du poste</label>
        <textarea id="descriptionPoste" v-model="form.descriptionPoste" class="g-input" rows="4" required></textarea>
      </div>

      <div class="g-champ g-champ--large">
        <label for="missions" class="g-obligatoire">Missions</label>
        <textarea id="missions" v-model="form.missions" class="g-input" rows="5" required
          placeholder="Une mission par ligne…"></textarea>
      </div>

      <div class="g-champ g-champ--large">
        <label for="competences">Compétences requises</label>
        <textarea id="competences" v-model="form.competencesRequises" class="g-input" rows="3"
          placeholder="Java, Spring Boot, SQL…"></textarea>
      </div>

      <div class="g-champ">
        <label for="niveau">Niveau d'étude</label>
        <input id="niveau" v-model="form.niveauEtude" class="g-input" type="text" maxlength="100"
          placeholder="Licence, Master…" />
      </div>

      <div class="g-champ">
        <label for="experience" class="g-obligatoire">Expérience minimale (années)</label>
        <input id="experience" v-model.number="form.experienceMinAnnees" class="g-input" type="number" min="0"
          required />
      </div>

      <div class="g-champ">
        <label for="salaireMin">Salaire minimum (Ar)</label>
        <input id="salaireMin" v-model="form.salaireMin" class="g-input" type="number" min="0" step="1000" />
      </div>

      <div class="g-champ">
        <label for="salaireMax">Salaire maximum (Ar)</label>
        <input id="salaireMax" v-model="form.salaireMax" class="g-input" type="number" min="0" step="1000" />
        <small v-if="besoinLie && besoinLie.salaireEstime">
          Salaire estimé dans le besoin : {{ formatMontant(besoinLie.salaireEstime) }}
        </small>
      </div>

      <div class="g-champ">
        <label for="statut">Statut</label>
        <select id="statut" v-model="form.statut" class="g-input">
          <option v-for="o in STATUTS_OFFRE" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
      </div>

      <div class="g-form__actions">
        <RouterLink class="g-btn" to="/si/app/rh/offres">Annuler</RouterLink>
        <button v-if="form.statut === 'VALIDEE'" class="g-btn" type="button" :disabled="envoi" @click="soumettre(true)">
          Enregistrer et publier
        </button>
        <button class="g-btn g-btn--primaire" type="submit" :disabled="envoi">{{ envoi ? "Enregistrement…" :
          "Enregistrer" }}</button>
      </div>
    </form>
  </div>
</template>
