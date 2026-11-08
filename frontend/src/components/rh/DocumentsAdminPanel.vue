<script setup>
import { onBeforeUnmount, reactive, ref } from "vue";
import MessageAlerte from "@/components/gestion/MessageAlerte.vue";
import StatutBadge from "@/components/gestion/StatutBadge.vue";
import ValiditeBadge from "@/components/rh/ValiditeBadge.vue";
import { employeApi } from "@/services/employeService";
import { onboardingApi } from "@/services/onboardingService";
import { TYPES_DOCUMENT, formatDate, libelle } from "@/config/libelles";
import { aujourdhui } from "@/config/libelles";

const props = defineProps({
  employeId: { type: Number, required: true },
  documents: { type: Array, default: () => [] },
  lectureSeule: { type: Boolean, default: false },
});
const emit = defineEmits(["modifie"]);

const TAILLE_MAX = 5 * 1024 * 1024;
const erreur = ref("");
const succes = ref("");
const occupe = ref(false);

async function executer(action, messageSucces = "") {
  if (occupe.value) return false;
  occupe.value = true;
  erreur.value = "";
  succes.value = "";
  try {
    await action();
    succes.value = messageSucces;
    emit("modifie");
    return true;
  } catch (e) {
    erreur.value = e.message;
    return false;
  } finally {
    occupe.value = false;
  }
}

const nouveau = reactive({ type: "CIN", fichier: null, dateExpiration: "", delaiAlerteJours: 30 });
const inputFichier = ref(null);
const remplace = ref(null); // document que le nouveau fichier remplace (renouvellement)

function choisirFichier(event) {
  const f = event.target.files[0];
  erreur.value = "";
  nouveau.fichier = null;
  if (!f) return;
  if (!/\.(pdf|jpe?g|png)$/i.test(f.name)) {
    erreur.value = "Format non accepté : joignez un PDF, un JPEG ou un PNG.";
    event.target.value = "";
  } else if (f.size > TAILLE_MAX) {
    erreur.value = "Le fichier dépasse 5 Mo.";
    event.target.value = "";
  } else {
    nouveau.fichier = f;
  }
}

function demarrerRenouvellement(d) {
  remplace.value = d;
  nouveau.type = d.typeDocument;
  nouveau.delaiAlerteJours = d.delaiAlerteJours ?? 30;
  nouveau.dateExpiration = "";
  succes.value = "";
}

function annulerRenouvellement() {
  remplace.value = null;
}

async function deposer() {
  if (!nouveau.fichier) {
    erreur.value = "Choisissez un fichier à déposer.";
    return;
  }
  const ancien = remplace.value;
  const ok = await executer(async () => {
    await employeApi.deposerDocument(props.employeId, nouveau);
    // Renouvellement : l'ancien document est marqué renouvelé (il n'envoie plus d'alerte).
    if (ancien) await employeApi.marquerRenouvele(ancien.id);
  }, ancien ? "Document renouvelé : l'ancien exemplaire n'envoie plus d'alerte." : "Document déposé.");
  if (ok) {
    Object.assign(nouveau, { fichier: null, dateExpiration: "", delaiAlerteJours: 30 });
    remplace.value = null;
    if (inputFichier.value) inputFichier.value.value = "";
  }
}

const edition = ref(null);

function modifierValidite(d) {
  edition.value = {
    id: d.id,
    nomFichier: d.nomFichier,
    dateExpiration: d.dateExpiration ?? "",
    delaiAlerteJours: d.delaiAlerteJours ?? 30,
  };
}

async function enregistrerValidite() {
  const e = edition.value;
  const ok = await executer(
    () => employeApi.modifierExpiration(e.id, e.dateExpiration, e.delaiAlerteJours),
    "Validité du document mise à jour.",
  );
  if (ok) edition.value = null;
}

const marquerRenouvele = (d) => executer(() => employeApi.marquerRenouvele(d.id), "Document marqué comme renouvelé.");
const decider = (d, statut) => executer(() => onboardingApi.changerStatutDocument(d.id, statut));

function supprimer(d) {
  if (!window.confirm(`Supprimer « ${d.nomFichier} » ?`)) return;
  return executer(() => onboardingApi.supprimerDocument(d.id), "Document supprimé.");
}

const urlsOuvertes = [];
async function voir(d) {
  erreur.value = "";
  try {
    const blob = await employeApi.fichierDocument(d.id);
    const url = URL.createObjectURL(blob);
    urlsOuvertes.push(url);
    window.open(url, "_blank", "noopener");
  } catch (e) {
    erreur.value = e.message;
  }
}
onBeforeUnmount(() => urlsOuvertes.forEach((u) => URL.revokeObjectURL(u)));
</script>

<template>
  <div>
    <div class="messages">
      <MessageAlerte :message="erreur" />
      <MessageAlerte :message="succes" type="succes" />
    </div>

    <p v-if="!documents.length" class="g-vide">Aucun document pour ce salarié.</p>
    <div v-else class="g-table-wrap">
      <table class="g-table">
        <thead>
          <tr>
            <th>Document</th>
            <th>Déposé le</th>
            <th>Expiration</th>
            <th>Validité</th>
            <th>Statut</th>
            <th class="g-droite">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="d in documents" :key="d.id">
            <td>
              <span class="g-principal">{{ libelle(d.typeDocument) }}</span>
              <span class="g-secondaire">{{ d.nomFichier }}</span>
            </td>
            <td>{{ formatDate(d.dateDepot) }}</td>
            <td>
              {{ d.dateExpiration ? formatDate(d.dateExpiration) : "—" }}
              <span v-if="d.dateExpiration" class="g-secondaire">Alerte à J-{{ d.delaiAlerteJours }}</span>
              <span v-if="d.dateRenouvellement" class="g-secondaire">Renouvelé le {{ formatDate(d.dateRenouvellement)
                }}</span>
            </td>
            <td>
              <ValiditeBadge :etat="d.etatValidite" :jours-restants="d.joursRestants" />
            </td>
            <td>
              <StatutBadge :statut="d.statut" />
            </td>
            <td>
              <div class="g-actions-cellule">
                <button type="button" class="g-btn g-btn--petit" @click="voir(d)">Voir</button>
                <template v-if="!lectureSeule && d.statut === 'FOURNI'">
                  <button type="button" class="g-btn g-btn--petit g-btn--primaire" :disabled="occupe"
                    :aria-label="`Valider ${d.nomFichier}`" @click="decider(d, 'VALIDE')">Valider</button>
                  <button type="button" class="g-btn g-btn--petit g-btn--danger" :disabled="occupe"
                    :aria-label="`Refuser ${d.nomFichier}`" @click="decider(d, 'REFUSE')">Refuser</button>
                </template>
                <button v-if="!lectureSeule" type="button" class="g-btn g-btn--petit" :disabled="occupe"
                  :aria-label="`Modifier la validité de ${d.nomFichier}`" @click="modifierValidite(d)">Validité</button>
                <template v-if="!lectureSeule && d.dateExpiration && !d.dateRenouvellement">
                  <button type="button" class="g-btn g-btn--petit" :disabled="occupe"
                    :aria-label="`Renouveler ${d.nomFichier} avec un nouveau fichier`"
                    @click="demarrerRenouvellement(d)">Renouveler</button>
                  <button type="button" class="g-btn g-btn--petit" :disabled="occupe"
                    :aria-label="`Marquer ${d.nomFichier} comme renouvelé`" @click="marquerRenouvele(d)">Déjà
                    renouvelé</button>
                </template>
                <button v-if="!lectureSeule && d.statut !== 'VALIDE'" type="button" class="g-btn g-btn--petit" :disabled="occupe"
                  :aria-label="`Supprimer ${d.nomFichier}`" @click="supprimer(d)">Supprimer</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Modification de la validité -->
    <form v-if="!lectureSeule && edition" class="ajout ajout--edition" @submit.prevent="enregistrerValidite">
      <strong class="ajout__titre">Validité de « {{ edition.nomFichier }} »</strong>
      <label class="g-champ">
        <span>Date d'expiration (vide = n'expire pas)</span>
        <input v-model="edition.dateExpiration" class="g-input" type="date" />
      </label>
      <label class="g-champ">
        <span>Alerte (jours avant)</span>
        <input v-model.number="edition.delaiAlerteJours" class="g-input" type="number" min="0" max="365" />
      </label>
      <button class="g-btn" type="button" :disabled="occupe" @click="edition = null">Annuler</button>
      <button class="g-btn g-btn--primaire" type="submit" :disabled="occupe">Enregistrer</button>
    </form>

    <!-- Dépôt / renouvellement -->
    <form v-if="!lectureSeule" class="ajout" @submit.prevent="deposer">
      <p v-if="remplace" class="ajout__titre ajout__titre--renouvellement">
        Renouvellement de « {{ remplace.nomFichier }} » : déposez le nouveau fichier.
        <button type="button" class="g-btn g-btn--petit" @click="annulerRenouvellement">Annuler</button>
      </p>
      <select v-model="nouveau.type" class="g-input" aria-label="Type de document" :disabled="Boolean(remplace)">
        <option v-for="t in TYPES_DOCUMENT" :key="t.value" :value="t.value">{{ t.label }}</option>
      </select>
      <input ref="inputFichier" class="g-input" type="file"
        accept="application/pdf,image/jpeg,image/png,.pdf,.jpg,.jpeg,.png" aria-label="Fichier du document"
        @change="choisirFichier" />
      <label class="champ-court">
        <span>Expire le</span>
        <input v-model="nouveau.dateExpiration" class="g-input" type="date" :min="aujourdhui()" />
      </label>
      <label class="champ-court">
        <span>Alerte (jours avant)</span>
        <input v-model.number="nouveau.delaiAlerteJours" class="g-input" type="number" min="0" max="365" />
      </label>
      <button class="g-btn g-btn--primaire" type="submit" :disabled="occupe">{{ remplace ? "Déposer et renouveler" :
        "Déposer le document" }}</button>
    </form>
    <p v-if="!lectureSeule" class="aide">PDF, JPEG ou PNG — 5 Mo maximum. Sans date d'expiration, aucune alerte n'est envoyée. Un document
      validé
      ne peut pas être supprimé.</p>
  </div>
</template>

<style scoped>
.messages {
  padding: 0 20px;
}

.messages:has(p) {
  padding-top: 14px;
}

.ajout {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 10px;
  padding: 16px 20px;
  border-top: 1px solid var(--color-border);
  background: var(--color-subtle);
}

.ajout .g-input {
  width: auto;
  flex: 1 1 160px;
  min-width: 0;
}

.ajout--edition {
  align-items: flex-end;
}

.ajout__titre {
  flex: 1 1 100%;
  margin: 0;
  color: var(--color-ink);
}

.ajout__titre--renouvellement {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  font-weight: 600;
}

.champ-court {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1 1 150px;
  font-size: 12px;
  color: var(--color-muted);
}

.aide {
  margin: 0;
  padding: 0 20px 14px;
  background: var(--color-subtle);
  font-size: 12.5px;
  color: var(--color-muted);
}
</style>
