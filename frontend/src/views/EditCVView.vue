<script setup>
import BoutonRetour from '../components/BoutonRetour.vue';
import NavBar from '../components/NavBar.vue';
</script>
<template>

  <NavBar/>

  <BoutonRetour/>

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
            <input type="text" name="nom" value="Sébastien Leroy" autocomplete="name">
          </label>
          <label class="ed-champ">Titre du poste recherché
            <input type="text" name="titre" value="Consultant Business Intelligence">
          </label>
          <label class="ed-champ">Âge / Permis
            <input type="text" name="age" value="30 ans, Permis B">
          </label>
          <label class="ed-champ">Adresse
            <input type="text" name="adresse" value="11 rue du petit chemin 75010 Paris" autocomplete="street-address">
          </label>
          <div class="ed-ligne">
            <label class="ed-champ">Téléphone
              <input type="tel" name="tel" value="07 78 78 79 65" autocomplete="tel">
            </label>
            <label class="ed-champ">E-mail
              <input type="email" name="email" value="prenom@mail.com" autocomplete="email">
            </label>
          </div>
          <label class="ed-champ">Photo
            <input type="file" name="photo" accept="image/*">
          </label>
        </div>
      </details>

      <details class="ed-bloc">
        <summary>Accroche</summary>
        <div class="ed-bloc__contenu">
          <label class="ed-champ">Phrase d'accroche
            <textarea name="accroche" rows="4">Je suis un consultant BI avec 7 ans d'expérience. Très à l'aise à l'oral, j'aime travailler en équipe tant au niveau opérationnel que stratégique.</textarea>
          </label>
        </div>
      </details>

      <details class="ed-bloc">
        <summary>Expériences professionnelles</summary>
        <div class="ed-bloc__contenu">
          <fieldset class="ed-groupe">
            <legend>Expérience 1</legend>
            <label class="ed-champ">Poste
              <input type="text" value="Data &amp; Business Analyst">
            </label>
            <label class="ed-champ">Entreprise / Lieu
              <input type="text" value="FREELANCE - Paris">
            </label>
            <div class="ed-ligne">
              <label class="ed-champ">Début
                <input type="text" value="2015">
              </label>
              <label class="ed-champ">Fin
                <input type="text" value="2017">
              </label>
            </div>
            <label class="ed-champ">Missions (une par ligne)
              <textarea rows="4">Exploration des données et visualisation
Conception de reporting opérationnel et décisionnel
Optimisation des processus de collecte des données</textarea>
            </label>
          </fieldset>
          <button class="ed-bouton ed-bouton--leger" type="button">+ Ajouter une expérience</button>
        </div>
      </details>

      <details class="ed-bloc">
        <summary>Formation</summary>
        <div class="ed-bloc__contenu">
          <fieldset class="ed-groupe">
            <legend>Formation 1</legend>
            <label class="ed-champ">École
              <input type="text" value="ESSEC Business School - Paris">
            </label>
            <label class="ed-champ">Diplôme
              <input type="text" value="Master In Business Intelligence">
            </label>
            <div class="ed-ligne">
              <label class="ed-champ">Début
                <input type="text" value="2008">
              </label>
              <label class="ed-champ">Fin
                <input type="text" value="2011">
              </label>
            </div>
          </fieldset>
          <button class="ed-bouton ed-bouton--leger" type="button">+ Ajouter une formation</button>
        </div>
      </details>

      <details class="ed-bloc">
        <summary>Compétences</summary>
        <div class="ed-bloc__contenu">
          <label class="ed-champ">Langues
            <input type="text" value="Anglais : parlé couramment">
          </label>
          <label class="ed-champ">Compétences (une par ligne)
            <textarea rows="5">Power BI : Dax / SQL
Tableau Software
SAP / COPA, analyse de la performance
Suite Office : fonctions avancées, VBA</textarea>
          </label>
        </div>
      </details>

      <details class="ed-bloc">
        <summary>Recommandations</summary>
        <div class="ed-bloc__contenu">
          <label class="ed-champ">Témoignage
            <textarea rows="3">Un collaborateur rigoureux, force de proposition et très à l'écoute des équipes.</textarea>
          </label>
          <label class="ed-champ">Auteur
            <input type="text" value="Prénom Nom, responsabilité">
          </label>
        </div>
      </details>

      <details class="ed-bloc">
        <summary>Centres d'intérêt</summary>
        <div class="ed-bloc__contenu">
          <label class="ed-champ">Voyages
            <input type="text" value="Tour du monde (2015-2016)">
          </label>
          <label class="ed-champ">Sport
            <input type="text" value="Course à pied (semi-marathon), natation">
          </label>
        </div>
      </details>

    </form>

    <!-- ================= DROITE : aperçu du CV ================= -->
    <section class="ed-apercu" aria-label="Aperçu du CV">

      <div class="ed-apercu__outils">
        <span class="ed-apercu__label">Aperçu du CV</span>
        <button class="ed-bouton ed-bouton--leger" type="button">Télécharger</button>
      </div>

      <!-- .feuille = conteneur : tout le CV s'adapte à sa largeur -->
      <div class="ed-feuille">
        <article class="ed-cv">

          <header class="ed-cv__entete">
            <div class="ed-cv__photo" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="8" r="4"/>
                <path d="M4 21c0-4.4 3.6-8 8-8s8 3.6 8 8"/>
              </svg>
            </div>

            <div class="ed-cv__identite">
              <h2>Sébastien Leroy</h2>
              <p>30 ans, Permis B</p>
              <p>11 rue du petit chemin 75010 Paris</p>
              <p>07 78 78 79 65</p>
              <p>prenom@mail.com</p>
            </div>

            <div class="ed-cv__poste">
              <h3>Consultant Business Intelligence</h3>
              <p>« Je suis un consultant BI avec 7 ans d'expérience. Très à l'aise à l'oral, j'aime travailler en équipe tant au niveau opérationnel que stratégique. »</p>
            </div>
          </header>

          <div class="ed-cv__corps">

            <aside class="ed-cv__lateral">

              <section class="ed-cv__section">
                <h4 class="ed-cv__titre">Compétences</h4>
                <div class="ed-cv__groupe">
                  <strong>Anglais :</strong>
                  <p>parlé couramment</p>
                </div>
                <div class="ed-cv__groupe">
                  <strong>Business Intelligence :</strong>
                  <ul>
                    <li>Power BI : Dax / SQL</li>
                    <li>Tableau Software</li>
                    <li>BW / BO : création et diffusion requêtes</li>
                  </ul>
                </div>
                <div class="ed-cv__groupe">
                  <strong>Contrôle de gestion :</strong>
                  <p>SAP / COPA, analyse de la performance et reporting, analyse des coûts.</p>
                </div>
                <div class="ed-cv__groupe">
                  <strong>Bureautique :</strong>
                  <p>Suite Office : fonctions avancées, VBA</p>
                </div>
              </section>

              <section class="ed-cv__section">
                <h4 class="ed-cv__titre">Recommandations</h4>
                <blockquote class="ed-cv__citation">
                  <p>« Un collaborateur rigoureux, force de proposition et très à l'écoute des équipes. »</p>
                  <cite>Prénom Nom, responsabilité</cite>
                </blockquote>
              </section>

              <section class="ed-cv__section">
                <h4 class="ed-cv__titre">Centres d'intérêt</h4>
                <div class="ed-cv__groupe">
                  <strong>Voyages :</strong>
                  <p>Tour du monde (2015-2016)</p>
                </div>
                <div class="ed-cv__groupe">
                  <strong>Sport :</strong>
                  <p>Course à pied (semi-marathon), natation</p>
                </div>
              </section>

            </aside>

            <div class="ed-cv__principal">

              <section class="ed-cv__section">
                <h4 class="ed-cv__titre">Expériences professionnelles</h4>

                <div class="ed-cv__item">
                  <div class="ed-cv__ligne-titre">
                    <p><strong>Data &amp; Business Analyst</strong>, FREELANCE - Paris</p>
                    <span class="ed-cv__dates">2015 - 2017<small>2 ans</small></span>
                  </div>
                  <ul>
                    <li>Exploration des données et visualisation</li>
                    <li>Conception de reporting opérationnel et décisionnel</li>
                    <li>Optimisation des processus de collecte des données</li>
                  </ul>
                </div>

                <div class="ed-cv__item">
                  <div class="ed-cv__ligne-titre">
                    <p><strong>Formateur Business Intelligence</strong>, FREELANCE - Paris</p>
                    <span class="ed-cv__dates">2016 - 2017<small>1 an</small></span>
                  </div>
                  <ul>
                    <li>Import et transformation des données ETL Power Query</li>
                    <li>Architecture et modélisation des tables</li>
                    <li>Fonctionnalités des outils de visualisation</li>
                  </ul>
                </div>

                <div class="ed-cv__item">
                  <div class="ed-cv__ligne-titre">
                    <p><strong>Contrôleur de Gestion</strong>, ENTREPRISE - Paris</p>
                    <span class="ed-cv__dates">2009 - 2015<small>6 ans</small></span>
                  </div>
                  <ul>
                    <li>Analyse de la performance commerciale et logistique</li>
                    <li>Suivi de la rentabilité de chaque Business Unit</li>
                    <li>Création et mise à jour de tableaux de bord</li>
                  </ul>
                </div>
              </section>

              <section class="ed-cv__section">
                <h4 class="ed-cv__titre">Formation</h4>

                <div class="ed-cv__item">
                  <div class="ed-cv__ligne-titre">
                    <p><strong>ESSEC Business School</strong> - Paris</p>
                    <span class="ed-cv__dates">2008 - 2011</span>
                  </div>
                  <p class="ed-cv__detail">Master In Business Intelligence</p>
                </div>

                <div class="ed-cv__item">
                  <div class="ed-cv__ligne-titre">
                    <p><strong>Université de Strasbourg</strong> - Strasbourg</p>
                    <span class="ed-cv__dates">2005 - 2007</span>
                  </div>
                  <p class="ed-cv__detail">Licence 2 de Science juridique</p>
                </div>
              </section>

            </div>
          </div>

        </article>
      </div>

    </section>
  </main>



</template>