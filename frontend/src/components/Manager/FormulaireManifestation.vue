<script setup>
import { reactive, ref } from 'vue'
import api from '@/services/api.js'

// Chaque liste est un tableau d'objets { id, libelle }
defineProps({
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

const aujourdhui = formatDate(new Date())

const etape = ref(1)
const racine = ref(null)
const section1 = ref(null)
const envoiEnCours = ref(false)
const erreurEnvoi = ref('')
const envoiReussi = ref(false)

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
  competencesRequises: '',
  salaireEstime: ''
})

function libellePriorite(valeur) {
  const texte = valeur.toLowerCase().replace(/_/g, ' ')
  return texte.charAt(0).toUpperCase() + texte.slice(1)
}

const vide = (valeur) => (valeur === '' || valeur === null ? null : valeur)

function remonter() {
  racine.value?.scrollIntoView({ block: 'start' })
}

// Vérifie les champs de la section 1 avant de passer à la section 2
function suivant() {
  const champs = section1.value.querySelectorAll('input, select, textarea')
  for (const champ of champs) {
    if (!champ.reportValidity()) return
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

async function envoyer() {
  // Touche Entrée dans la section 1 : on passe à la suite au lieu d'envoyer
  if (etape.value === 1) {
    suivant()
    return
  }

  const salaire = vide(formulaire.salaireEstime)
  const payload = {
    ...formulaire,
    demandeurId: Number(formulaire.demandeurId),
    departementId: Number(formulaire.departementId),
    typeBesoinId: Number(formulaire.typeBesoinId),
    posteId: Number(formulaire.posteId),
    employeRemplaceId: vide(formulaire.employeRemplaceId) === null
      ? null
      : Number(formulaire.employeRemplaceId),
    typeContratSouhaiteId: vide(formulaire.typeContratSouhaiteId) === null
      ? null
      : Number(formulaire.typeContratSouhaiteId),
    dateArriveeSouhaitee: vide(formulaire.dateArriveeSouhaitee),
    lieuTravail: vide(formulaire.lieuTravail),
    experienceRequise: vide(formulaire.experienceRequise),
    niveauEtude: vide(formulaire.niveauEtude),
    competencesRequises: vide(formulaire.competencesRequises),
    salaireEstime: salaire === null ? null : Number(salaire)
  }

  envoiEnCours.value = true
  erreurEnvoi.value = ''
  envoiReussi.value = false
  try {
    const reponse = await api.post('/manager/besoins', payload )
    if (!reponse.ok) {
      const message = reponse.data?.detail || reponse.data?.message
      throw new Error(message || `Erreur HTTP ${reponse.status}`)
    }
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
  <form ref="racine" class="formulaire" @submit.prevent="envoyer">

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
      </div>

      <div class="champ">
        <label for="besoin-demandeur">Demandeur <span class="requis" aria-hidden="true">*</span></label>
        <select id="besoin-demandeur" v-model="formulaire.demandeurId" required>
          <option value="" disabled>Choisir un demandeur</option>
          <option v-for="utilisateur in utilisateurs" :key="utilisateur.id" :value="utilisateur.id">
            {{ utilisateur.nom }} {{ utilisateur.prenom }}
          </option>
        </select>
      </div>

      <div class="champ">
        <label for="besoin-type">Type de besoin <span class="requis" aria-hidden="true">*</span></label>
        <select id="besoin-type" v-model="formulaire.typeBesoinId" required>
          <option value="" disabled>Choisir un type</option>
          <option v-for="type in typesBesoin" :key="type.id" :value="type.id">
            {{ type.libelle }}
          </option>
        </select>
      </div>

      <div class="champ">
        <label for="besoin-poste">Poste <span class="requis" aria-hidden="true">*</span></label>
        <select id="besoin-poste" v-model="formulaire.posteId" required>
          <option value="" disabled>Choisir un poste</option>
          <option v-for="poste in postes" :key="poste.id" :value="poste.id">
            {{ poste.nom }}
          </option>
        </select>
      </div>

      <div class="champ">
        <label for="besoin-effectif">Effectif <span class="requis" aria-hidden="true">*</span></label>
        <input id="besoin-effectif" v-model.number="formulaire.effectif" type="number" min="1" step="1"
               inputmode="numeric" required>
      </div>

      <div class="champ">
        <label for="besoin-employe">Employé remplacé <span class="requis" aria-hidden="true">*</span></label>
        <select id="besoin-employe" v-model="formulaire.employeRemplaceId">
          <option value="">Aucun</option>
          <option v-for="employe in employes" :key="employe.id" :value="employe.id">
            {{ employe.nom }} {{ employe.prenom }}
          </option>
        </select>
        <p class="champ__aide">À renseigner uniquement en cas de remplacement.</p>
      </div>

      <div class="champ">
        <label for="besoin-contrat">Type de contrat souhaité <span class="requis" aria-hidden="true">*</span></label>
        <select id="besoin-contrat" v-model="formulaire.typeContratSouhaiteId">
          <option value="">Non précisé</option>
          <option v-for="contrat in typesContrat" :key="contrat.id" :value="contrat.id">
            {{ contrat.libelle }}
          </option>
        </select>
      </div>

      <div class="champ">
        <label for="besoin-priorite">Priorité <span class="requis" aria-hidden="true">*</span></label>
        <select id="besoin-priorite" v-model="formulaire.priorite" required>
          <option v-for="priorite in priorites" :key="priorite" :value="priorite">
            {{ libellePriorite(priorite) }}
          </option>
        </select>
      </div>

      <div class="champ">
        <label for="besoin-arrivee">Date d'arrivée souhaitée <span class="requis" aria-hidden="true">*</span></label>
        <input id="besoin-arrivee" v-model="formulaire.dateArriveeSouhaitee" type="date" :min="aujourdhui">
      </div>

      <div class="champ">
        <label for="besoin-lieu">Lieu de travail</label>
        <input id="besoin-lieu" v-model="formulaire.lieuTravail" type="text" maxlength="150"
               placeholder="Ex. Siège, Antananarivo">
      </div>

      <div class="champ champ--large">
        <label for="besoin-motif">Motif <span class="requis" aria-hidden="true">*</span></label>
        <textarea id="besoin-motif" v-model="formulaire.motif" rows="5"
                  placeholder="Pourquoi ce besoin ? Contexte, charge de travail, projet concerné…" required></textarea>
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
               placeholder="Ex. Licence, Master">
      </div>

      <div class="champ">
        <label for="besoin-salaire">Salaire estimé <span class="requis" aria-hidden="true">*</span></label>
        <input id="besoin-salaire" v-model="formulaire.salaireEstime" type="number" min="0" step="0.01"
               inputmode="decimal" placeholder="Ex. 1500000">
      </div>

      <div class="champ champ--large">
        <label for="besoin-experience">Expérience requise <span class="requis" aria-hidden="true">*</span></label>
        <textarea id="besoin-experience" v-model="formulaire.experienceRequise" rows="3" maxlength="255"
                  placeholder="Ex. 2 ans minimum en développement Java"></textarea>
      </div>

      <div class="champ champ--large">
        <label for="besoin-competences">Compétences requises <span class="requis" aria-hidden="true">*</span></label>
        <textarea id="besoin-competences" v-model="formulaire.competencesRequises" rows="4"
                  placeholder="Ex. Spring Boot, PostgreSQL, Vue.js…"></textarea>
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
      <p v-if="erreurEnvoi" class="champ--large" role="alert">{{ erreurEnvoi }}</p>
      <p v-if="envoiReussi" class="champ--large" role="status">La demande a bien été envoyée.</p>
    </section>

  </form>
</template>