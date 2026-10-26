<script setup>
import { computed, nextTick, reactive, ref } from 'vue'
import api from '@/services/api.js'
import Notification from '@/components/ui/Notification.vue'

// Chaque liste est un tableau d'objets { id, libelle }
const props = defineProps({
  departements: { type: Array, default: () => [] },
  departementSelectionne: { type: [String, Number], default: '' },
  typesBesoin: { type: Array, required: true },
  postes: { type: Array, required: true },
  utilisateurs: { type: Array, required: true },
  employes: { type: Array, default: () => [] },
  typesContrat: { type: Array, default: () => [] },
  // À adapter aux valeurs réelles de l'enum Priorite
  priorites: { type: Array, default: () => ['BASSE', 'NORMALE', 'HAUTE', 'URGENTE'] }
})

const emit = defineEmits(['submit', 'update:departementSelectionne'])

const formatDate = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const aujourdhui = computed(() => formatDate(new Date()))

const etape = ref(1)
const racine = ref(null)
const section1 = ref(null)
const envoiEnCours = ref(false)
const erreurEnvoi = ref('')
const envoiReussi = ref(false)
const erreurs = reactive({})

// Les clés suivent les champs de l'entité
const formulaire = reactive({
  demandeurId: '',
  departementId: '',
  typeBesoinId: '',
  posteId: '',
  employeRemplaceId: '',
  typeContratSouhaiteId: '',
  effectif: 1,
  priorite: 'NORMALE',
  dateArriveeSouhaitee: '',
  lieuTravail: '',
  motif: '',
  experienceRequise: '',
  niveauEtude: '',
  competencesRequises: ''
})

function libellePriorite(valeur) {
  const texte = valeur.toLowerCase().replace(/_/g, ' ')
  return texte.charAt(0).toUpperCase() + texte.slice(1)
}

function remonter() {
  racine.value?.scrollIntoView({ block: 'start' })
}

// Vérifie les champs de la section 1 avant de passer à la section 2
const estTypeRemplacement = computed(() => {
  const typeSelectionne = props.typesBesoin.find(
    (type) => Number(type.id) === Number(formulaire.typeBesoinId),
  )

  if (!typeSelectionne) return false

  const code = String(typeSelectionne.code ?? '').trim().toUpperCase()
  const libelle = String(typeSelectionne.libelle ?? '').trim().toUpperCase()

  return code === 'REMPLACEMENT' || libelle.includes('REMPLACEMENT')
})

const champsEtape1 = [
  // ['besoin-departement', () => Boolean(formulaire.departementId), 'Choisissez un département.'],
  ['besoin-demandeur', () => Boolean(formulaire.demandeurId), 'Choisissez un demandeur.'],
  ['besoin-type', () => Boolean(formulaire.typeBesoinId), 'Choisissez un type de besoin.'],
  ['besoin-poste', () => Boolean(formulaire.posteId), 'Choisissez un poste.'],
  ['besoin-effectif', () => Number.isInteger(formulaire.effectif) && formulaire.effectif >= 1, 'L’effectif doit être un nombre entier supérieur ou egale à 1.'],
  ['besoin-employe', () => !estTypeRemplacement.value || Boolean(formulaire.employeRemplaceId), 'Choisissez un employé remplacé.'],
  ['besoin-contrat', () => Boolean(formulaire.typeContratSouhaiteId), 'Choisissez un type de contrat.'],
  ['besoin-priorite', () => Boolean(formulaire.priorite), 'Choisissez une priorité.'],
  ['besoin-arrivee', () => Boolean(formulaire.dateArriveeSouhaitee) && formulaire.dateArriveeSouhaitee >= aujourdhui.value, 'Choisissez une date égale ou postérieure à aujourd’hui.'],
  ['besoin-lieu', () => Boolean(formulaire.lieuTravail.trim()), 'Renseignez le lieu de travail.'],
  ['besoin-motif', () => Boolean(formulaire.motif.trim()), 'Renseignez le motif de la demande.'],
]

function nettoyerEmployeRemplaceSiNonRemplacement() {
  if (!estTypeRemplacement.value && formulaire.employeRemplaceId) {
    formulaire.employeRemplaceId = ''
  }
}

const champsEtape2 = [
  ['besoin-etude', () => Boolean(formulaire.niveauEtude.trim()), 'Renseignez le niveau d’étude.'],
  ['besoin-experience', () => Boolean(formulaire.experienceRequise.trim()), 'Renseignez l’expérience requise.'],
  ['besoin-competences', () => Boolean(formulaire.competencesRequises.trim()), 'Renseignez les compétences requises.'],
]

function validerChamps(champs) {
  let valide = true
  for (const [id, condition, message] of champs) {
    if (!condition()) {
      erreurs[id] = message
      valide = false
    } else {
      delete erreurs[id]
    }
  }
  return valide
}

function effacerErreur(event) {
  const champ = [...champsEtape1, ...champsEtape2].find(([id]) => id === event.target.id)
  if (champ?.[1]()) delete erreurs[event.target.id]
}

async function allerAuPremierChampEnErreur(champs = [...champsEtape1, ...champsEtape2]) {
  await nextTick()
  const id = champs.find(([fieldId]) => erreurs[fieldId])?.[0]
  document.getElementById(id)?.focus()
}

async function suivant() {
  if (!validerChamps(champsEtape1)) {
    await allerAuPremierChampEnErreur(champsEtape1)
    return
  }
  etape.value = 2
  remonter()
}

function retour() {
  etape.value = 1
  remonter()
}

function changerDepartement(event) {
  const departementId = event.target.value
  formulaire.departementId = departementId
  formulaire.demandeurId = ''
  formulaire.posteId = ''
  formulaire.employeRemplaceId = ''
  emit('update:departementSelectionne', departementId)
}

function onTypeBesoinChange() {
  if (!estTypeRemplacement.value) {
    formulaire.employeRemplaceId = ''
    delete erreurs['besoin-employe']
  }
}

async function envoyer() {
  // Touche Entrée dans la section 1 : on passe à la suite au lieu d'envoyer
  if (etape.value === 1) {
    await suivant()
    return
  }

  if (!validerChamps([...champsEtape1, ...champsEtape2])) {
    const premiereEtapeInvalide = champsEtape1.some(([id]) => erreurs[id])
    if (premiereEtapeInvalide) {
      etape.value = 1
      await nextTick()
      remonter()
    }
    await allerAuPremierChampEnErreur(
      premiereEtapeInvalide ? champsEtape1 : champsEtape2,
    )
    return
  }

  const payload = {
    ...formulaire,
    demandeurId: Number(formulaire.demandeurId),
    departementId: Number(formulaire.departementId),
    typeBesoinId: Number(formulaire.typeBesoinId),
    posteId: Number(formulaire.posteId),
    employeRemplaceId: Number(formulaire.employeRemplaceId),
    typeContratSouhaiteId: Number(formulaire.typeContratSouhaiteId),
    lieuTravail: formulaire.lieuTravail.trim(),
    motif: formulaire.motif.trim(),
    experienceRequise: formulaire.experienceRequise.trim(),
    niveauEtude: formulaire.niveauEtude.trim(),
    competencesRequises: formulaire.competencesRequises.trim()
  }

  envoiEnCours.value = true
  erreurEnvoi.value = ''
  envoiReussi.value = false
  try {
    const reponse = await api.post('/rh/besoins', payload)
    envoiReussi.value = true
    emit('submit', reponse.data)
  } catch (error) {
    erreurEnvoi.value = `Impossible d'envoyer la demande : ${error.message}`
  } finally {
    envoiEnCours.value = false
  }
}
</script>

<template>
  <form ref="racine" class="formulaire" novalidate @submit.prevent="envoyer" @input="effacerErreur" @change="effacerErreur">

    <!-- Indicateur d'étapes -->
    <ol class="etapes" aria-label="Progression du formulaire">
      <li :class="{ 'etapes__item--actif': etape === 1 }" :aria-current="etape === 1 ? 'step' : undefined">
        <span class="etapes__numero">1</span> Le besoin
      </li>
      <li :class="{ 'etapes__item--actif': etape === 2 }" :aria-current="etape === 2 ? 'step' : undefined">
        <span class="etapes__numero">2</span> Profil et suivi
      </li>
    </ol>

    <!-- ========== Section 1 : le besoin ========== -->
    <section v-show="etape === 1" ref="section1" class="formulaire__section" aria-labelledby="titre-section-1">
      <h2 id="titre-section-1" class="formulaire__titre">Le besoin</h2>

      <div class="champ">
        <label for="besoin-departement">Departement <span class="requis" aria-hidden="true">*</span></label>
        <select
          id="besoin-departement"
          :value="departementSelectionne"
          @change="changerDepartement"
          required
        >
          <option value="" disabled>Choisir un departement</option>
          <option v-for="departement in departements" :key="departement.id" :value="departement.id">
            {{ departement.nom }}
          </option>
        </select>
        <p v-if="erreurs['besoin-departement']" class="champ__erreur" role="alert">{{ erreurs['besoin-departement'] }}</p>
      </div>

      <div class="champ">
        <label for="besoin-demandeur">Demandeur <span class="requis" aria-hidden="true">*</span></label>
        <select id="besoin-demandeur" v-model="formulaire.demandeurId" required>
          <option value="" disabled>Choisir un demandeur</option>
          <option v-for="utilisateur in utilisateurs" :key="utilisateur.id" :value="utilisateur.id">
            {{ utilisateur.nom }} {{ utilisateur.prenom }}
          </option>
        </select>
        <p v-if="erreurs['besoin-demandeur']" class="champ__erreur" role="alert">{{ erreurs['besoin-demandeur'] }}</p>
      </div>

      <div class="champ">
        <label for="besoin-type">Type de besoin <span class="requis" aria-hidden="true">*</span></label>
        <select id="besoin-type" v-model="formulaire.typeBesoinId" required @change="onTypeBesoinChange">
          <option value="" disabled>Choisir un type</option>
          <option v-for="type in typesBesoin" :key="type.id" :value="type.id">
            {{ type.libelle }}
          </option>
        </select>
        <p v-if="erreurs['besoin-type']" class="champ__erreur" role="alert">{{ erreurs['besoin-type'] }}</p>
      </div>

      <div class="champ">
        <label for="besoin-poste">Poste <span class="requis" aria-hidden="true">*</span></label>
        <select id="besoin-poste" v-model="formulaire.posteId" required>
          <option value="" disabled>Choisir un poste</option>
          <option v-for="poste in postes" :key="poste.id" :value="poste.id">
            {{ poste.nom }}
          </option>
        </select>
        <p v-if="erreurs['besoin-poste']" class="champ__erreur" role="alert">{{ erreurs['besoin-poste'] }}</p>
      </div>

      <div class="champ">
        <label for="besoin-effectif">Effectif <span class="requis" aria-hidden="true">*</span></label>
        <input id="besoin-effectif" v-model.number="formulaire.effectif" type="number" min="1" step="1"
               inputmode="numeric" required>
        <p v-if="erreurs['besoin-effectif']" class="champ__erreur" role="alert">{{ erreurs['besoin-effectif'] }}</p>
      </div>

      <div class="champ">
        <label for="besoin-employe">Employé remplacé <span class="requis" aria-hidden="true">*</span></label>
        <select
          id="besoin-employe"
          v-model="formulaire.employeRemplaceId"
          :disabled="!estTypeRemplacement"
          :required="estTypeRemplacement"
        >
          <option value="" disabled>{{ estTypeRemplacement ? 'Choisir un employé' : 'Non requis pour ce type de besoin' }}</option>
          <option v-for="employe in employes" :key="employe.id" :value="employe.id">
            {{ employe.nom }} {{ employe.prenom }}
          </option>
        </select>
        <p v-if="erreurs['besoin-employe']" class="champ__erreur" role="alert">{{ erreurs['besoin-employe'] }}</p>
        <p class="champ__aide">
          {{ estTypeRemplacement ? "Sélectionnez l'employé concerné par ce remplacement." : "Ce champ est désactivé car le type de besoin ne correspond pas à un remplacement." }}
        </p>
      </div>

      <div class="champ">
        <label for="besoin-contrat">Type de contrat souhaité <span class="requis" aria-hidden="true">*</span></label>
        <select id="besoin-contrat" v-model="formulaire.typeContratSouhaiteId" required>
          <option value="" disabled>Choisir un type de contrat</option>
          <option v-for="contrat in typesContrat" :key="contrat.id" :value="contrat.id">
            {{ contrat.libelle }}
          </option>
        </select>
        <p v-if="erreurs['besoin-contrat']" class="champ__erreur" role="alert">{{ erreurs['besoin-contrat'] }}</p>
      </div>

      <div class="champ">
        <label for="besoin-priorite">Priorité <span class="requis" aria-hidden="true">*</span></label>
        <select id="besoin-priorite" v-model="formulaire.priorite" required>
          <option v-for="priorite in priorites" :key="priorite" :value="priorite">
            {{ libellePriorite(priorite) }}
          </option>
        </select>
        <p v-if="erreurs['besoin-priorite']" class="champ__erreur" role="alert">{{ erreurs['besoin-priorite'] }}</p>
      </div>

      <div class="champ">
        <label for="besoin-arrivee">Date d'arrivée souhaitée <span class="requis" aria-hidden="true">*</span></label>
        <input id="besoin-arrivee" v-model="formulaire.dateArriveeSouhaitee" type="date" :min="aujourdhui" required>
        <p v-if="erreurs['besoin-arrivee']" class="champ__erreur" role="alert">{{ erreurs['besoin-arrivee'] }}</p>
      </div>

      <div class="champ">
        <label for="besoin-lieu">Lieu de travail <span class="requis" aria-hidden="true">*</span></label>
        <input id="besoin-lieu" v-model="formulaire.lieuTravail" type="text" maxlength="150"
               placeholder="Ex. Siège, Antananarivo" required>
        <p v-if="erreurs['besoin-lieu']" class="champ__erreur" role="alert">{{ erreurs['besoin-lieu'] }}</p>
      </div>

      <div class="champ champ--large">
        <label for="besoin-motif">Motif <span class="requis" aria-hidden="true">*</span></label>
        <textarea id="besoin-motif" v-model="formulaire.motif" rows="5"
                  placeholder="Pourquoi ce besoin ? Contexte, charge de travail, projet concerné…" required></textarea>
        <p v-if="erreurs['besoin-motif']" class="champ__erreur" role="alert">{{ erreurs['besoin-motif'] }}</p>
      </div>

      <p class="formulaire__note"><span class="requis" aria-hidden="true">*</span> Champs obligatoires</p>

      <div class="formulaire__actions champ--large">
        <button type="button" class="bouton" @click="suivant">Suivant</button>
      </div>
    </section>

    <!-- ========== Section 2 : profil recherché et suivi ========== -->
    <section v-show="etape === 2" class="formulaire__section" aria-labelledby="titre-section-2">
      <h2 id="titre-section-2" class="formulaire__titre">Profil recherché et suivi</h2>

      <div class="champ">
        <label for="besoin-etude">Niveau d'étude <span class="requis" aria-hidden="true">*</span></label>
        <input id="besoin-etude" v-model="formulaire.niveauEtude" type="text" maxlength="100"
               placeholder="Ex. Licence, Master" required>
        <p v-if="erreurs['besoin-etude']" class="champ__erreur" role="alert">{{ erreurs['besoin-etude'] }}</p>
      </div>

      <div class="champ champ--large">
        <label for="besoin-experience">Expérience requise <span class="requis" aria-hidden="true">*</span></label>
        <textarea id="besoin-experience" v-model="formulaire.experienceRequise" rows="3" maxlength="255"
                  placeholder="Ex. 2 ans minimum en développement Java" required></textarea>
        <p v-if="erreurs['besoin-experience']" class="champ__erreur" role="alert">{{ erreurs['besoin-experience'] }}</p>
      </div>

      <div class="champ champ--large">
        <label for="besoin-competences">Compétences requises <span class="requis" aria-hidden="true">*</span></label>
        <textarea id="besoin-competences" v-model="formulaire.competencesRequises" rows="4"
                  placeholder="Ex. Spring Boot, PostgreSQL, Vue.js…" required></textarea>
        <p v-if="erreurs['besoin-competences']" class="champ__erreur" role="alert">{{ erreurs['besoin-competences'] }}</p>
      </div>

      <!-- Champs gérés automatiquement par le serveur : affichés en lecture seule -->
      <div class="champ">
        <label for="besoin-date-demande">Date de la demande</label>
        <input id="besoin-date-demande" type="date" :value="aujourdhui" disabled>
      </div>

      <div class="champ">
        <label for="besoin-date-modification">Date de modification</label>
        <input id="besoin-date-modification" type="text" placeholder="Mise à jour automatique" disabled>
      </div>

      <div class="champ champ--large">
        <label for="besoin-statut">Statut global</label>
        <input id="besoin-statut" type="text" value="En attente RH" disabled>
        <p class="champ__aide">La date de la demande, la date de modification et le statut sont gérés automatiquement.</p>
      </div>

      <div class="formulaire__actions formulaire__actions--double champ--large">
        <button type="button" class="bouton" @click="retour">Retour</button>
        <button type="submit" class="bouton" :disabled="envoiEnCours">
          {{ envoiEnCours ? 'Envoi en cours…' : 'Envoyer la demande' }}
        </button>
      </div>
      <Notification 
        v-if="envoiReussi"
        :message="'La demande a bien été envoyée.'"
        :type="'succes'"
      />
      <Notification
        v-else-if="erreurEnvoi"
        :message="erreurEnvoi"
        :type="'erreur'"
      />
      <!-- <p v-if="erreurEnvoi" class="champ--large" role="alert">{{ erreurEnvoi }}</p> -->
      <!-- <p v-if="envoiReussi" class="champ--large" role="status">La demande a bien été envoyée.</p> -->
    </section>

  </form>
</template>

<style scoped>
  .etapes__item--actif {
    color: var(--color-primary);
    background: var(--color-primary-soft);
  }
</style>