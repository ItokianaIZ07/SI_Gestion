<script setup>
// Création / modification d'une publication.
// Création : /si/app/rh/publications/creation[?offreId=5]
// Édition  : /si/app/rh/publications/:id/modifier
import { ref, reactive, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { publicationsApi, offresApi } from "@/services/ressources";
import { STATUTS_PUBLICATION, aujourdhui, libelle } from "@/config/libelles";
import PageEntete from "@/components/gestion/PageEntete.vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import "@/assets/gestion.css";

const route = useRoute();
const router = useRouter();

const id = route.params.id ? Number(route.params.id) : null;
const edition = id !== null;

const PLATEFORMES = ["Site carrière", "LinkedIn", "Facebook", "Emploi.mg", "Affichage interne"];

const form = reactive({
  offreId: "",
  plateforme: "Site carrière",
  urlPublication: "",
  datePublication: aujourdhui(),
  dateExpiration: "",
  statut: "PUBLIEE",
});

const offres = ref([]);
const chargement = ref(true);
const envoi = ref(false);
const erreur = ref("");

const offresProposees = computed(() => offres.value.filter((o) => o.statut !== "ARCHIVEE" || o.id === Number(form.offreId)));
const offreChoisie = computed(() => offres.value.find((o) => o.id === Number(form.offreId)));

onMounted(async () => {
  try {
    offres.value = (await offresApi.liste()) ?? [];

    if (edition) {
      const p = await publicationsApi.parId(id);
      Object.assign(form, {
        offreId: p.offreId,
        plateforme: p.plateforme,
        urlPublication: p.urlPublication ?? "",
        datePublication: p.datePublication ?? "",
        dateExpiration: p.dateExpiration ?? "",
        statut: p.statut,
      });
    } else if (route.query.offreId) {
      form.offreId = Number(route.query.offreId);
    }
  } catch (e) {
    erreur.value = e.message;
  } finally {
    chargement.value = false;
  }
});

async function soumettre() {
  erreur.value = "";
  if (!form.offreId) {
    erreur.value = "Sélectionnez l'offre à publier.";
    return;
  }
  if (form.dateExpiration && form.datePublication && form.dateExpiration < form.datePublication) {
    erreur.value = "La date d'expiration ne peut pas précéder la date de publication.";
    return;
  }

  const charge = {
    offreId: Number(form.offreId),
    plateforme: form.plateforme.trim(),
    urlPublication: form.urlPublication.trim() || null,
    datePublication: form.datePublication || null,
    dateExpiration: form.dateExpiration || null,
    statut: form.statut,
  };

  envoi.value = true;
  try {
    if (edition) await publicationsApi.modifier(id, charge);
    else await publicationsApi.creer(charge);
    router.push("/si/app/rh/publications");
  } catch (e) {
    erreur.value = e.message;
  } finally {
    envoi.value = false;
  }
}
</script>

<template>
  <div class="g-page" style="max-width: 860px">
    <PageEntete :titre="edition ? 'Modifier la publication' : 'Nouvelle publication'" sous-titre="Choisissez l'offre, la plateforme et la période de diffusion.">
      <RouterLink class="g-btn" to="/si/app/rh/publications">← Retour à la liste</RouterLink>
    </PageEntete>

    <MessageAlerte :message="erreur" />

    <p v-if="chargement" class="g-vide">Chargement…</p>

    <form v-else class="g-carte g-form" @submit.prevent="soumettre">
      <div class="g-champ g-champ--large">
        <label for="offre" class="g-obligatoire">Offre d'emploi</label>
        <select id="offre" v-model="form.offreId" class="g-input" :disabled="edition" required>
          <option value="" disabled>Sélectionner une offre…</option>
          <option v-for="o in offresProposees" :key="o.id" :value="o.id">{{ o.titre }} ({{ libelle(o.statut) }})</option>
        </select>
        <small v-if="offreChoisie && offreChoisie.statut !== 'VALIDEE'" style="color: #9a6400">
          Cette offre n'est pas « Validée » : validez-la avant de la diffuser aux candidats.
        </small>
      </div>

      <div class="g-champ">
        <label for="plateforme" class="g-obligatoire">Plateforme</label>
        <input id="plateforme" v-model="form.plateforme" class="g-input" type="text" list="plateformes" maxlength="100" required />
        <datalist id="plateformes">
          <option v-for="p in PLATEFORMES" :key="p" :value="p" />
        </datalist>
        <small>« Site carrière » correspond à la page publique des candidats.</small>
      </div>

      <div class="g-champ">
        <label for="statut">Statut</label>
        <select id="statut" v-model="form.statut" class="g-input">
          <option v-for="o in STATUTS_PUBLICATION" :key="o.value" :value="o.value">{{ o.label }}</option>
        </select>
      </div>

      <div class="g-champ g-champ--large">
        <label for="url">Lien de l'annonce</label>
        <input id="url" v-model="form.urlPublication" class="g-input" type="url" placeholder="https://…" />
      </div>

      <div class="g-champ">
        <label for="datePublication">Date de publication</label>
        <input id="datePublication" v-model="form.datePublication" class="g-input" type="date" />
      </div>

      <div class="g-champ">
        <label for="dateExpiration">Date d'expiration</label>
        <input id="dateExpiration" v-model="form.dateExpiration" class="g-input" type="date" :min="form.datePublication || undefined" />
        <small>Après cette date, l'offre disparaît de la page des candidats.</small>
      </div>

      <div class="g-form__actions">
        <RouterLink class="g-btn" to="/si/app/rh/publications">Annuler</RouterLink>
        <button class="g-btn g-btn--primaire" type="submit" :disabled="envoi">{{ envoi ? "Enregistrement…" : "Enregistrer" }}</button>
      </div>
    </form>
  </div>
</template>
