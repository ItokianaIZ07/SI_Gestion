<script setup>
// Formulaire de dépôt de CV (PDF).
// - Avec recrutementId / publicationId : le dépôt vaut candidature à l'offre.
// - Sans : dépôt spontané (candidat + CV uniquement).
import { reactive, ref, onBeforeUnmount } from "vue";
import ApercuPDF from "./ApercuPDF.vue";
import { clientApi } from "@/services/ressources";

const props = defineProps({
  recrutementId: { type: Number, default: null },
  publicationId: { type: Number, default: null },
});
const emit = defineEmits(["depose"]);

const TAILLE_MAX = 5 * 1024 * 1024; // 5 Mo

const form = reactive({ nom: "", prenom: "", cin: "", telephone: "", email: "", annees: 0, experience: "" });
const fichier = ref(null);
const filePDF = ref("");
const inputCv = ref(null);
const erreurFichier = ref("");
const erreur = ref("");
const succes = ref("");
const envoi = ref(false);

function viderApercu() {
  if (filePDF.value) URL.revokeObjectURL(filePDF.value);
  filePDF.value = "";
}

function refuserFichier(message) {
  erreurFichier.value = message;
  fichier.value = null;
  if (inputCv.value) inputCv.value.value = "";
}

const handleUpload = (event) => {
  const file = event.target.files[0];
  erreurFichier.value = "";
  viderApercu();
  fichier.value = null;
  if (!file) return;

  if (file.type !== "application/pdf" && !file.name.toLowerCase().endsWith(".pdf")) {
    return refuserFichier("Le CV doit être au format PDF.");
  }
  if (file.size > TAILLE_MAX) {
    return refuserFichier("Le fichier dépasse 5 Mo.");
  }
  fichier.value = file;
  filePDF.value = URL.createObjectURL(file);
};

async function envoyer() {
  erreur.value = "";
  succes.value = "";
  if (!fichier.value) {
    erreurFichier.value = "Joignez votre CV au format PDF.";
    return;
  }

  envoi.value = true;
  try {
    await clientApi.deposerCv(
      {
        nom: form.nom.trim(),
        prenom: form.prenom.trim(),
        email: form.email.trim(),
        telephone: form.telephone.trim(),
        numeroCin: form.cin.trim(),
        experience: Number(form.annees) || 0,
        details: form.experience.trim(),
        nomFichier: fichier.value.name,
        mimeType: "application/pdf",
        recrutementId: props.recrutementId,
        publicationId: props.publicationId,
      },
      fichier.value,
    );
    succes.value = props.recrutementId
      ? "Votre candidature a bien été envoyée. Merci !"
      : "Votre CV a bien été déposé. Merci !";
    Object.assign(form, { nom: "", prenom: "", cin: "", telephone: "", email: "", annees: 0, experience: "" });
    fichier.value = null;
    if (inputCv.value) inputCv.value.value = "";
    viderApercu();
    emit("depose");
  } catch (e) {
    erreur.value = e.message;
  } finally {
    envoi.value = false;
  }
}

onBeforeUnmount(viderApercu);
</script>

<template>
  <form class="formulaire" @submit.prevent="envoyer">
    <p v-if="succes" class="champ--large depot__succes" role="status">{{ succes }}</p>
    <p v-if="erreur" class="champ__erreur champ--large" role="alert">{{ erreur }}</p>

    <div class="champ">
      <label for="nom">Nom</label>
      <input v-model="form.nom" type="text" id="nom" name="nom" autocomplete="family-name" maxlength="100" required>
    </div>

    <div class="champ">
      <label for="prenom">Prénom</label>
      <input v-model="form.prenom" type="text" id="prenom" name="prenom" autocomplete="given-name" maxlength="100" required>
    </div>

    <div class="champ">
      <label for="cin">CIN</label>
      <input v-model="form.cin" type="text" id="cin" name="cin" inputmode="numeric" maxlength="12" pattern="[0-9]{12}"
             title="Le CIN contient 12 chiffres" placeholder="12 chiffres" required>
    </div>

    <div class="champ">
      <label for="telephone">Numéro de téléphone</label>
      <input v-model="form.telephone" type="tel" id="telephone" name="telephone" autocomplete="tel" maxlength="30" placeholder="034 00 000 00" required>
    </div>

    <div class="champ">
      <label for="email">Email</label>
      <input v-model="form.email" type="email" id="email" name="email" autocomplete="email" maxlength="150" placeholder="nom@exemple.com" required>
    </div>

    <div class="champ">
      <label for="annees">Années d'expérience</label>
      <input v-model.number="form.annees" type="number" id="annees" name="annees" min="0" max="60" required>
    </div>

    <div class="champ champ--large">
      <label for="cv">CV (PDF, 5 Mo maximum)</label>
      <input ref="inputCv" @change="handleUpload($event)" type="file" id="cv" name="cv" accept="application/pdf,.pdf" required>
      <p v-if="erreurFichier" class="champ__erreur" id="cv-erreur" role="alert">{{ erreurFichier }}</p>
    </div>

    <div class="champ champ--large">
      <label for="experience">Expérience</label>
      <textarea v-model="form.experience" id="experience" name="experience" rows="6"
                placeholder="Postes occupés, entreprises, durées…" required></textarea>
    </div>

    <div class="formulaire__actions champ--large">
      <button type="submit" class="bouton" :disabled="envoi">{{ envoi ? "Envoi en cours…" : "Envoyer" }}</button>
    </div>
  </form>
  <ApercuPDF :file="filePDF" />
</template>

<style scoped>
.depot__succes {
  margin: 0;
  padding: 10px 14px;
  border-radius: 6px;
  color: var(--texte-success);
  background: var(--fond-texte-success);
  font-weight: 600;
}
.bouton:disabled { opacity: .6; cursor: not-allowed; }
</style>
