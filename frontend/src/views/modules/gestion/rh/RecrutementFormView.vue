<script setup>
// Création / modification d'un recrutement.
// Création : /si/app/rh/recrutements/creation[?besoinId=12]
// Édition  : /si/app/rh/recrutements/:id/modifier
import { ref, reactive, computed, watch, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { recrutementsApi, besoinsApi } from "@/services/ressources";
import { STATUTS_RECRUTEMENT, aujourdhui, posteDe, departementDe } from "@/config/libelles";
import PageEntete from "@/components/gestion/PageEntete.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import "@/assets/gestion.css";

const route = useRoute();
const router = useRouter();

const id = route.params.id ? Number(route.params.id) : null;
const edition = id !== null;

const form = reactive({
  besoinId: "",
  reference: "",
  titre: "",
  description: "",
  dateOuverture: aujourdhui(),
  dateCloture: "",
  statut: "BROUILLON",
});
let responsableId = null; // conservé tel quel à la modification

const besoins = ref(null); // null = endpoint /api/besoins indisponible
const recrutements = ref([]);
const chargement = ref(true);
const envoi = ref(false);
const erreur = ref("");

const besoinsDisponibles = computed(() => {
  if (besoins.value === null) return null;
  const pris = new Set(recrutements.value.filter((r) => r.id !== id).map((r) => r.besoinId));
  return besoins.value.filter((b) => b.statutGlobal === "ACCEPTE" && !pris.has(b.id));
});

function libelleBesoin(b) {
  return `#${b.id} — ${posteDe(b)} · ${departementDe(b)} (${b.effectif ?? 1} poste(s))`;
}

function preremplir() {
  if (edition || !besoins.value) return;
  const b = besoins.value.find((x) => x.id === Number(form.besoinId));
  if (!b) return;
  if (!form.titre) form.titre = `Recrutement — ${posteDe(b)}`;
  if (!form.reference) form.reference = `REC-${new Date().getFullYear()}-${String(b.id).padStart(3, "0")}`;
  if (!form.description) form.description = b.motif ?? "";
}
watch(() => form.besoinId, preremplir);

onMounted(async () => {
  try {
    const [liste, listeBesoins] = await Promise.all([
      recrutementsApi.liste(),
      besoinsApi.liste().catch(() => null),
    ]);
    recrutements.value = liste ?? [];
    besoins.value = listeBesoins;

    if (edition) {
      const r = recrutements.value.find((x) => x.id === id) ?? (await recrutementsApi.parId(id));
      Object.assign(form, {
        besoinId: r.besoinId,
        reference: r.reference,
        titre: r.titre,
        description: r.description ?? "",
        dateOuverture: r.dateOuverture ?? "",
        dateCloture: r.dateCloture ?? "",
        statut: r.statut,
      });
      responsableId = r.responsableId ?? null;
    } else if (route.query.besoinId) {
      form.besoinId = Number(route.query.besoinId);
      preremplir();
    }
  } catch (e) {
    erreur.value = e.message;
  } finally {
    chargement.value = false;
  }
});

async function soumettre(creerOffreEnsuite = false) {
  erreur.value = "";
  if (!form.besoinId) {
    erreur.value = "Sélectionnez le besoin à l'origine de ce recrutement.";
    return;
  }
  if (form.dateCloture && form.dateOuverture && form.dateCloture < form.dateOuverture) {
    erreur.value = "La date de clôture ne peut pas précéder la date d'ouverture.";
    return;
  }

  const charge = {
    besoinId: Number(form.besoinId),
    reference: form.reference.trim(),
    titre: form.titre.trim(),
    description: form.description.trim() || null,
    dateOuverture: form.dateOuverture || null,
    dateCloture: form.dateCloture || null,
    statut: form.statut,
    responsableId,
  };

  envoi.value = true;
  try {
    const r = edition ? await recrutementsApi.modifier(id, charge) : await recrutementsApi.creer(charge);
    if (creerOffreEnsuite) {
      router.push({ path: "/si/app/rh/offres/creation", query: { recrutementId: r.id } });
    } else {
      router.push("/si/app/rh/recrutements");
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
    <PageEntete
      :titre="edition ? 'Modifier le recrutement' : 'Nouveau recrutement'"
      sous-titre="Un recrutement est rattaché à un besoin accepté. L'offre d'emploi se rédige ensuite."
    >
      <RouterLink class="g-btn" to="/si/app/rh/recrutements">← Retour à la liste</RouterLink>
    </PageEntete>

    <MessageAlerte :message="erreur" />

    <p v-if="chargement" class="g-vide">Chargement…</p>

    <form v-else class="g-carte g-form" @submit.prevent="soumettre(false)">
      <div class="g-champ g-champ--large">
        <label for="besoin" class="g-obligatoire">Besoin d'origine</label>
        <select v-if="besoinsDisponibles" id="besoin" v-model="form.besoinId" class="g-input" :disabled="edition" required>
          <option value="" disabled>Sélectionner un besoin accepté…</option>
          <option v-for="b in besoinsDisponibles" :key="b.id" :value="b.id">{{ libelleBesoin(b) }}</option>
        </select>
        <input v-else id="besoin" v-model.number="form.besoinId" class="g-input" type="number" min="1" :disabled="edition" required placeholder="Identifiant du besoin" />
        <small v-if="besoinsDisponibles && !besoinsDisponibles.length && !edition">
          Aucun besoin accepté sans recrutement pour le moment.
        </small>
        <small v-if="!besoinsDisponibles">La liste des besoins n'est pas disponible : saisissez son identifiant.</small>
      </div>

      <div class="g-champ">
        <label for="reference" class="g-obligatoire">Référence</label>
        <input id="reference" v-model="form.reference" class="g-input" type="text" maxlength="50" required placeholder="REC-2026-001" />
      </div>

      <div class="g-champ">
        <label for="statut">Statut</label>
        <select id="statut" v-model="form.statut" class="g-input">
          <option v-for="o in STATUTS_RECRUTEMENT" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
      </div>

      <div class="g-champ g-champ--large">
        <label for="titre" class="g-obligatoire">Intitulé</label>
        <input id="titre" v-model="form.titre" class="g-input" type="text" maxlength="150" required />
      </div>

      <div class="g-champ g-champ--large">
        <label for="description">Description</label>
        <textarea id="description" v-model="form.description" class="g-input" rows="4" placeholder="Contexte et objectifs du recrutement…"></textarea>
      </div>

      <div class="g-champ">
        <label for="ouverture">Date d'ouverture</label>
        <input id="ouverture" v-model="form.dateOuverture" class="g-input" type="date" />
      </div>

      <div class="g-champ">
        <label for="cloture">Date de clôture</label>
        <input id="cloture" v-model="form.dateCloture" class="g-input" type="date" :min="form.dateOuverture || undefined" />
      </div>

      <div class="g-form__actions">
        <RouterLink class="g-btn" to="/si/app/rh/recrutements">Annuler</RouterLink>
        <button v-if="!edition" class="g-btn" type="button" :disabled="envoi" @click="soumettre(true)">Enregistrer et créer l'offre</button>
        <button class="g-btn g-btn--primaire" type="submit" :disabled="envoi">{{ envoi ? "Enregistrement…" : "Enregistrer" }}</button>
      </div>
    </form>
  </div>
</template>
