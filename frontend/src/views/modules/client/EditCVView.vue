<script setup>
import { ref, computed } from 'vue';

import BoutonRetour from '@/components/BoutonRetour.vue';
import NavBar from '@/components/NavBar.vue';
import { usePhoto } from '@/composable/usePhoto';
import { useExperiences } from '@/composable/useExperiences';
import { useFormations } from '@/composable/useFormations';
import { useLignes } from '@/composable/useLignes';
import html2pdf from 'html2pdf.js';

const { photo, photoUrl, choisirPhoto } = usePhoto()

//identite
const nom = ref('Brandon ANDRIANAVALONA')
const titre = ref('Consultant Business Intelligence')
const age = ref(30)
const adress = ref('Ankadifotsy')
const tel = ref('')
const telFormate = computed(() => {
  const numero = tel.value
    .replace(/\D/g, '')
    .replace(/^0/, '')
    .slice(0, 9)

  return numero.replace(/^(\d{2})(\d{2})(\d{0,3})(\d{0,2}).*/, (_, a, b, c, d) =>
    [a, b, c, d].filter(Boolean).join(' ')
  )
})
const email = ref('prenom@mail.com')
const accroche = ref('Consultant BI, communicatif et orienté travail d’équipe')

//experiences
const { experiences, ajouterExperience, supprimerExperience, dureeExperience } = useExperiences()

//formations
const { formations, ajouterFormation, supprimerFormation } = useFormations()

//utilitaire
const { lignes } = useLignes()

//competence
const langues = ref('Anglais : parlé couramment')
const competences = ref(
  'Power BI : Dax / SQL\nTableau Software\nBW / BO : création et diffusion requêtes'
)
const controleGestion = ref('SAP / COPA, analyse de la performance et reporting, analyse des coûts.')
const bureautique = ref('Suite Office : fonctions avancées, VBA')

//recommandation
const temoignage = ref(
  "Un collaborateur rigoureux, force de proposition et très à l'écoute des équipes."
)
const auteurTemoignage = ref('Prénom Nom, responsabilité')

//centre intere
const voyages = ref('Tour du monde (2015-2016)')
const sport = ref('Course à pied (semi-marathon), natation')

const downloadCV = () => {
  const cvPanel = document.querySelector(".ed-cv");
  const options = {
    margin: 10,
    filename: `${nom.value}_${email}.pdf`,
    image: { type: 'jpeg', quality: 0.98 },
    html2canvas: { scale: 2, useCORS: true },
    jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' }
  };
  html2pdf().set(options).from(cvPanel).save();
}

const uploadCV =  async () => {
  
}

</script>
<template>

  <NavBar />

  <BoutonRetour />

  <!-- ===== Barre d'actions ===== -->
  <div class="ed-barre">
    <h1 class="ed-barre__titre">Création CV</h1>
    <button class="ed-bouton" type="button">Envoyer</button>
  </div>

  <main class="ed-editeur">

    <!-- ================= GAUCHE : champs à remplir ================= -->
    <form class="ed-formulaire" aria-label="Champs du CV" onsubmit="return false">

      <details class="ed-bloc" open>
        <summary>Identité</summary>
        <div class="ed-bloc__contenu">
          <label class="ed-champ">Nom complet
            <input type="text" name="nom" v-model="nom" autocomplete="name">
          </label>
          <label class="ed-champ">Titre du poste recherché
            <input type="text" name="titre" v-model="titre">
          </label>

          <label class="ed-champ">Âge
            <input type="number" name="age" v-model.number="age" min="1">
          </label>
          <label class="ed-champ">Adresse
            <input type="text" name="adresse" v-model="adress" autocomplete="street-address">
          </label>
          <div class="ed-ligne">
            <label class="ed-champ">
              Téléphone

              <div class="ed-champ__telephone">
                <span class="ed-champ__indicatif">+261</span>

                <input type="tel" name="tel" placeholder="38 38 414 46" v-model="tel" autocomplete="tel-national"
                  maxlength="12" inputmode="numeric">
              </div>
            </label>

            <label class="ed-champ">
              E-mail
              <input type="email" name="email" v-model="email" autocomplete="email">
            </label>
          </div>

          <label class="ed-champ">
            Photo
            <input type="file" name="photo" accept="image/*" @change="choisirPhoto">
          </label>

        </div>
      </details>

      <details class="ed-bloc">
        <summary>Accroche</summary>
        <div class="ed-bloc__contenu">
          <label class="ed-champ">Phrase d'accroche
            <textarea name="accroche"
              rows="4" v-model="accroche"></textarea>
          </label>
        </div>
      </details>

      <details class="ed-bloc">
        <summary>Expériences professionnelles</summary>
        <div class="ed-bloc__contenu">

          <fieldset class="ed-groupe" v-for="(exp, index) in experiences" :key="index">
            <legend>Expérience {{ index + 1 }}</legend>

            <label class="ed-champ">Poste
              <input type="text" v-model="exp.poste">
            </label>
            <label class="ed-champ">Entreprise / Lieu
              <input type="text" v-model="exp.entreprise">
            </label>
            <div class="ed-ligne">
              <label class="ed-champ">Début
                <input type="number" v-model.number="exp.debut">
              </label>
              <label class="ed-champ">Fin
                <input type="number" v-model.number="exp.fin">
              </label>
            </div>
            <label class="ed-champ">Missions (une par ligne)
              <textarea rows="4" v-model="exp.mission"></textarea>
            </label>

            <button
              v-if="experiences.length > 1"
              class="ed-bouton ed-bouton--leger"
              type="button"
              @click="supprimerExperience(index)">
              − Supprimer
            </button>
          </fieldset>

          <button class="ed-bouton ed-bouton--leger" type="button" @click="ajouterExperience">
            + Ajouter une expérience
          </button>
        </div>
      </details>

      <details class="ed-bloc">
        <summary>Formation</summary>
        <div class="ed-bloc__contenu">

          <fieldset class="ed-groupe" v-for="(formation, index) in formations" :key="index">
            <legend>Formation {{ index + 1 }}</legend>

            <label class="ed-champ">École
              <input type="text" v-model="formation.ecole">
            </label>
            <label class="ed-champ">Diplôme
              <input type="text" v-model="formation.diplome">
            </label>
            <div class="ed-ligne">
              <label class="ed-champ">Début
                <input type="number" v-model.number="formation.debut">
              </label>
              <label class="ed-champ">Fin
                <input type="number" v-model.number="formation.fin">
              </label>
            </div>

            <button
              v-if="formations.length > 1"
              class="ed-bouton ed-bouton--leger"
              type="button"
              @click="supprimerFormation(index)">
              − Supprimer
            </button>
          </fieldset>

          <button class="ed-bouton ed-bouton--leger" type="button" @click="ajouterFormation">
            + Ajouter une formation
          </button>
        </div>
      </details>

      <details class="ed-bloc">
        <summary>Compétences</summary>
        <div class="ed-bloc__contenu">
          <label class="ed-champ">Langues
            <input type="text" v-model="langues">
          </label>
          <label class="ed-champ">Compétences (une par ligne)
            <textarea rows="5" v-model="competences"></textarea>
          </label>
          <label class="ed-champ">Contrôle de gestion
            <textarea rows="3" v-model="controleGestion"></textarea>
          </label>
          <label class="ed-champ">Bureautique
            <input type="text" v-model="bureautique">
          </label>
        </div>
      </details>

      <details class="ed-bloc">
        <summary>Recommandations</summary>
        <div class="ed-bloc__contenu">
          <label class="ed-champ">Témoignage
            <textarea rows="3" v-model="temoignage"></textarea>
          </label>
          <label class="ed-champ">Auteur
            <input type="text" v-model="auteurTemoignage">
          </label>
        </div>
      </details>

      <details class="ed-bloc">
        <summary>Centres d'intérêt</summary>
        <div class="ed-bloc__contenu">
          <label class="ed-champ">Voyages
            <input type="text" v-model="voyages">
          </label>
          <label class="ed-champ">Sport
            <input type="text" v-model="sport">
          </label>
        </div>
      </details>

    </form>

    <!-- ================= DROITE : aperçu du CV ================= -->
    <section class="ed-apercu" aria-label="Aperçu du CV">

      <div class="ed-apercu__outils">
        <span class="ed-apercu__label">Aperçu du CV</span>
        <button class="ed-bouton ed-bouton--leger" type="button" @click="downloadCV()">Télécharger</button>
      </div>

      <!-- .feuille = conteneur : tout le CV s'adapte à sa largeur -->
      <div class="ed-feuille">
        <article class="ed-cv">

          <header class="ed-cv__entete">
            <div class="ed-cv__photo">
              <img v-if="photoUrl" :src="photoUrl" alt="Photo de profil">

              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"
                stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <circle cx="12" cy="8" r="4" />
                <path d="M4 21c0-4.4 3.6-8 8-8s8 3.6 8 8" />
              </svg>
            </div>


            <div class="ed-cv__identite">
              <h2>{{ nom }}</h2>
              <p>{{ age }} {{ age == 1 ? "an" : "ans" }}</p>
              <p>{{ adress }}</p>
              <p>{{ telFormate ? '+261 ' + telFormate : '' }}</p>
              <p>{{ email }}</p>
            </div>

            <div class="ed-cv__poste">
              <h3>{{ titre }}</h3>
              <p>« {{ accroche }} »</p>
            </div>
          </header>

          <div class="ed-cv__corps">

            <aside class="ed-cv__lateral">

              <section class="ed-cv__section">
                <h4 class="ed-cv__titre">Compétences</h4>

                <div class="ed-cv__groupe" v-if="langues">
                  <strong>Langues :</strong>
                  <p>{{ langues }}</p>
                </div>

                <div class="ed-cv__groupe" v-if="competences">
                  <strong>Business Intelligence :</strong>
                  <ul>
                    <li v-for="(comp, i) in lignes(competences)" :key="i">{{ comp }}</li>
                  </ul>
                </div>

                <div class="ed-cv__groupe" v-if="controleGestion">
                  <strong>Contrôle de gestion :</strong>
                  <p>{{ controleGestion }}</p>
                </div>

                <div class="ed-cv__groupe" v-if="bureautique">
                  <strong>Bureautique :</strong>
                  <p>{{ bureautique }}</p>
                </div>
              </section>

              <section class="ed-cv__section" v-if="temoignage || auteurTemoignage">
                <h4 class="ed-cv__titre">Recommandations</h4>
                <blockquote class="ed-cv__citation">
                  <p v-if="temoignage">« {{ temoignage }} »</p>
                  <cite v-if="auteurTemoignage">{{ auteurTemoignage }}</cite>
                </blockquote>
              </section>

              <section class="ed-cv__section" v-if="voyages || sport">
                <h4 class="ed-cv__titre">Centres d'intérêt</h4>
                <div class="ed-cv__groupe" v-if="voyages">
                  <strong>Voyages :</strong>
                  <p>{{ voyages }}</p>
                </div>
                <div class="ed-cv__groupe" v-if="sport">
                  <strong>Sport :</strong>
                  <p>{{ sport }}</p>
                </div>
              </section>

            </aside>

            <div class="ed-cv__principal">

              <section class="ed-cv__section" v-if="experiences.length">
                <h4 class="ed-cv__titre">Expériences professionnelles</h4>

                <div class="ed-cv__item" v-for="(exp, index) in experiences" :key="index">
                  <div class="ed-cv__ligne-titre">
                    <p><strong>{{ exp.poste }}</strong>, {{ exp.entreprise }}</p>
                    <span class="ed-cv__dates">
                      {{ exp.debut }} - {{ exp.fin }}
                      <small v-if="dureeExperience(exp)">{{ dureeExperience(exp) }}</small>
                    </span>
                  </div>
                  <ul v-if="lignes(exp.mission).length">
                    <li v-for="(m, i) in lignes(exp.mission)" :key="i">{{ m }}</li>
                  </ul>
                </div>
              </section>

              <section class="ed-cv__section" v-if="formations.length">
                <h4 class="ed-cv__titre">Formation</h4>

                <div class="ed-cv__item" v-for="(formation, index) in formations" :key="index">
                  <div class="ed-cv__ligne-titre">
                    <p><strong>{{ formation.ecole }}</strong></p>
                    <span class="ed-cv__dates">{{ formation.debut }} - {{ formation.fin }}</span>
                  </div>
                  <p class="ed-cv__detail" v-if="formation.diplome">{{ formation.diplome }}</p>
                </div>
              </section>

            </div>
          </div>

        </article>
      </div>

    </section>
  </main>

</template>