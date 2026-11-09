<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import { employeApi } from "@/services/employeService";
import { aujourdhui } from "@/config/libelles";

const props = defineProps({
  employeId: { type: Number, required: true },
  affectation: { type: Object, default: null }, // affectation actuelle (valeurs de départ)
});
const emit = defineEmits(["modifie", "annule"]);

const referentiels = ref({ departements: [], postes: [], employes: [] });
const chargement = ref(true);
const occupe = ref(false);
const erreur = ref("");

const form = reactive({
  idDepartement: props.affectation?.idDepartement ?? "",
  idPoste: props.affectation?.idPoste ?? "",
  idSuperieur: "",
  dateEffet: aujourdhui(),
  motif: "",
});

const postesDuDepartement = computed(() =>
  referentiels.value.postes.filter((p) => p.idDepartement === Number(form.idDepartement)),
);
const superieursPossibles = computed(() =>
  referentiels.value.employes.filter((e) => e.id !== props.employeId),
);
const departementChange = computed(
  () => props.affectation && Number(form.idDepartement) !== props.affectation.idDepartement,
);

// Un poste d'un autre département n'est plus valable quand le département change.
watch(
  () => form.idDepartement,
  () => {
    if (!postesDuDepartement.value.some((p) => p.id === Number(form.idPoste))) form.idPoste = "";
  },
);

onMounted(async () => {
  try {
    referentiels.value = await employeApi.referentiels();
  } catch (e) {
    erreur.value = e.message;
  } finally {
    chargement.value = false;
  }
});

async function valider() {
  erreur.value = "";
  if (!form.idDepartement || !form.idPoste) {
    erreur.value = "Choisissez le département et le poste.";
    return;
  }
  occupe.value = true;
  try {
    const carriere = await employeApi.changerAffectation(props.employeId, {
      idDepartement: Number(form.idDepartement),
      idPoste: Number(form.idPoste),
      idSuperieur: form.idSuperieur ? Number(form.idSuperieur) : null,
      dateEffet: form.dateEffet || null,
      motif: form.motif.trim() || null,
    });
    emit("modifie", carriere);
  } catch (e) {
    erreur.value = e.message;
  } finally {
    occupe.value = false;
  }
}
</script>

<template>
  <form class="g-form mutation" @submit.prevent="valider">
    <MessageAlerte :message="erreur" />
    <p v-if="chargement" class="g-vide">Chargement…</p>

    <template v-else>
      <div class="grille">
        <label class="g-champ">
          <span>Département <b class="g-obligatoire">*</b></span>
          <select v-model="form.idDepartement" class="g-input" required>
            <option value="" disabled>Choisir…</option>
            <option v-for="d in referentiels.departements" :key="d.id" :value="d.id">{{ d.nom }}</option>
          </select>
        </label>

        <label class="g-champ">
          <span>Poste <b class="g-obligatoire">*</b></span>
          <select v-model="form.idPoste" class="g-input" required :disabled="!form.idDepartement">
            <option value="" disabled>{{ form.idDepartement ? "Choisir…" : "Choisissez d'abord un département" }}
            </option>
            <option v-for="p in postesDuDepartement" :key="p.id" :value="p.id">{{ p.nom }}</option>
          </select>
        </label>

        <label class="g-champ">
          <span>Supérieur hiérarchique</span>
          <select v-model="form.idSuperieur" class="g-input">
            <option value="">
              {{ departementChange ? "Par défaut : manager du nouveau département" : "Conserver le supérieur actuel" }}
            </option>
            <option v-for="e in superieursPossibles" :key="e.id" :value="e.id">{{ e.nomComplet }} ({{ e.matricule }})
            </option>
          </select>
        </label>

        <label class="g-champ">
          <span>Date d'effet</span>
          <input v-model="form.dateEffet" class="g-input" type="date" :min="affectation?.dateDebut" />
        </label>

        <label class="g-champ g-champ--large">
          <span>Motif</span>
          <input v-model="form.motif" class="g-input" type="text" maxlength="500"
            placeholder="Ex. Promotion, réorganisation du service…" />
        </label>
      </div>

      <div class="g-form__actions">
        <button class="g-btn" type="button" :disabled="occupe" @click="emit('annule')">Annuler</button>
        <button class="g-btn g-btn--primaire" type="submit" :disabled="occupe">
          {{ occupe ? "Enregistrement…" : "Valider la mutation" }}
        </button>
      </div>
    </template>
  </form>
</template>

<style scoped>
.mutation {
  padding: 18px 20px;
  border-top: 1px solid var(--color-border);
  background: var(--color-subtle);
}

.grille {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 14px 18px;
}

.grille .g-champ--large {
  grid-column: 1 / -1;
}
</style>
