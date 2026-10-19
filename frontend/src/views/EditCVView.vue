<script setup>
import BoutonRetour from '../components/BoutonRetour.vue';
import NavBar from '../components/NavBar.vue';
</script>
<template>

  <NavBar/>

  <BoutonRetour/>

  <!-- ===== Barre d'actions ===== -->
  <div class="barre">
    <h1 class="barre__titre">Création CV</h1>
    <button class="bouton" type="button">Envoyer</button>
  </div>

  <main class="editeur">

    <!-- ================= GAUCHE : champs à remplir ================= -->
    <form class="formulaire" aria-label="Champs du CV" onsubmit="return false">

      <details class="bloc" open>
        <summary>Identité</summary>
        <div class="bloc__contenu">
          <label class="champ">Nom complet
            <input type="text" name="nom" value="Sébastien Leroy" autocomplete="name">
          </label>
          <label class="champ">Titre du poste recherché
            <input type="text" name="titre" value="Consultant Business Intelligence">
          </label>
          <label class="champ">Âge / Permis
            <input type="text" name="age" value="30 ans, Permis B">
          </label>
          <label class="champ">Adresse
            <input type="text" name="adresse" value="11 rue du petit chemin 75010 Paris" autocomplete="street-address">
          </label>
          <div class="ligne">
            <label class="champ">Téléphone
              <input type="tel" name="tel" value="07 78 78 79 65" autocomplete="tel">
            </label>
            <label class="champ">E-mail
              <input type="email" name="email" value="prenom@mail.com" autocomplete="email">
            </label>
          </div>
          <label class="champ">Photo
            <input type="file" name="photo" accept="image/*">
          </label>
        </div>
      </details>

      <details class="bloc">
        <summary>Accroche</summary>
        <div class="bloc__contenu">
          <label class="champ">Phrase d'accroche
            <textarea name="accroche" rows="4">Je suis un consultant BI avec 7 ans d'expérience. Très à l'aise à l'oral, j'aime travailler en équipe tant au niveau opérationnel que stratégique.</textarea>
          </label>
        </div>
      </details>

      <details class="bloc">
        <summary>Expériences professionnelles</summary>
        <div class="bloc__contenu">
          <fieldset class="groupe">
            <legend>Expérience 1</legend>
            <label class="champ">Poste
              <input type="text" value="Data &amp; Business Analyst">
            </label>
            <label class="champ">Entreprise / Lieu
              <input type="text" value="FREELANCE - Paris">
            </label>
            <div class="ligne">
              <label class="champ">Début
                <input type="text" value="2015">
              </label>
              <label class="champ">Fin
                <input type="text" value="2017">
              </label>
            </div>
            <label class="champ">Missions (une par ligne)
              <textarea rows="4">Exploration des données et visualisation
Conception de reporting opérationnel et décisionnel
Optimisation des processus de collecte des données</textarea>
            </label>
          </fieldset>
          <button class="bouton bouton--leger" type="button">+ Ajouter une expérience</button>
        </div>
      </details>

      <details class="bloc">
        <summary>Formation</summary>
        <div class="bloc__contenu">
          <fieldset class="groupe">
            <legend>Formation 1</legend>
            <label class="champ">École
              <input type="text" value="ESSEC Business School - Paris">
            </label>
            <label class="champ">Diplôme
              <input type="text" value="Master In Business Intelligence">
            </label>
            <div class="ligne">
              <label class="champ">Début
                <input type="text" value="2008">
              </label>
              <label class="champ">Fin
                <input type="text" value="2011">
              </label>
            </div>
          </fieldset>
          <button class="bouton bouton--leger" type="button">+ Ajouter une formation</button>
        </div>
      </details>

      <details class="bloc">
        <summary>Compétences</summary>
        <div class="bloc__contenu">
          <label class="champ">Langues
            <input type="text" value="Anglais : parlé couramment">
          </label>
          <label class="champ">Compétences (une par ligne)
            <textarea rows="5">Power BI : Dax / SQL
Tableau Software
SAP / COPA, analyse de la performance
Suite Office : fonctions avancées, VBA</textarea>
          </label>
        </div>
      </details>

      <details class="bloc">
        <summary>Recommandations</summary>
        <div class="bloc__contenu">
          <label class="champ">Témoignage
            <textarea rows="3">Un collaborateur rigoureux, force de proposition et très à l'écoute des équipes.</textarea>
          </label>
          <label class="champ">Auteur
            <input type="text" value="Prénom Nom, responsabilité">
          </label>
        </div>
      </details>

      <details class="bloc">
        <summary>Centres d'intérêt</summary>
        <div class="bloc__contenu">
          <label class="champ">Voyages
            <input type="text" value="Tour du monde (2015-2016)">
          </label>
          <label class="champ">Sport
            <input type="text" value="Course à pied (semi-marathon), natation">
          </label>
        </div>
      </details>

    </form>

    <!-- ================= DROITE : aperçu du CV ================= -->
    <section class="apercu" aria-label="Aperçu du CV">

      <div class="apercu__outils">
        <span class="apercu__label">Aperçu du CV</span>
        <button class="bouton bouton--leger" type="button">Télécharger</button>
      </div>

      <!-- .feuille = conteneur : tout le CV s'adapte à sa largeur -->
      <div class="feuille">
        <article class="cv">

          <header class="cv__entete">
            <div class="cv__photo" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="8" r="4"/>
                <path d="M4 21c0-4.4 3.6-8 8-8s8 3.6 8 8"/>
              </svg>
            </div>

            <div class="cv__identite">
              <h2>Sébastien Leroy</h2>
              <p>30 ans, Permis B</p>
              <p>11 rue du petit chemin 75010 Paris</p>
              <p>07 78 78 79 65</p>
              <p>prenom@mail.com</p>
            </div>

            <div class="cv__poste">
              <h3>Consultant Business Intelligence</h3>
              <p>« Je suis un consultant BI avec 7 ans d'expérience. Très à l'aise à l'oral, j'aime travailler en équipe tant au niveau opérationnel que stratégique. »</p>
            </div>
          </header>

          <div class="cv__corps">

            <aside class="cv__lateral">

              <section class="cv__section">
                <h4 class="cv__titre">Compétences</h4>
                <div class="cv__groupe">
                  <strong>Anglais :</strong>
                  <p>parlé couramment</p>
                </div>
                <div class="cv__groupe">
                  <strong>Business Intelligence :</strong>
                  <ul>
                    <li>Power BI : Dax / SQL</li>
                    <li>Tableau Software</li>
                    <li>BW / BO : création et diffusion requêtes</li>
                  </ul>
                </div>
                <div class="cv__groupe">
                  <strong>Contrôle de gestion :</strong>
                  <p>SAP / COPA, analyse de la performance et reporting, analyse des coûts.</p>
                </div>
                <div class="cv__groupe">
                  <strong>Bureautique :</strong>
                  <p>Suite Office : fonctions avancées, VBA</p>
                </div>
              </section>

              <section class="cv__section">
                <h4 class="cv__titre">Recommandations</h4>
                <blockquote class="cv__citation">
                  <p>« Un collaborateur rigoureux, force de proposition et très à l'écoute des équipes. »</p>
                  <cite>Prénom Nom, responsabilité</cite>
                </blockquote>
              </section>

              <section class="cv__section">
                <h4 class="cv__titre">Centres d'intérêt</h4>
                <div class="cv__groupe">
                  <strong>Voyages :</strong>
                  <p>Tour du monde (2015-2016)</p>
                </div>
                <div class="cv__groupe">
                  <strong>Sport :</strong>
                  <p>Course à pied (semi-marathon), natation</p>
                </div>
              </section>

            </aside>

            <div class="cv__principal">

              <section class="cv__section">
                <h4 class="cv__titre">Expériences professionnelles</h4>

                <div class="cv__item">
                  <div class="cv__ligne-titre">
                    <p><strong>Data &amp; Business Analyst</strong>, FREELANCE - Paris</p>
                    <span class="cv__dates">2015 - 2017<small>2 ans</small></span>
                  </div>
                  <ul>
                    <li>Exploration des données et visualisation</li>
                    <li>Conception de reporting opérationnel et décisionnel</li>
                    <li>Optimisation des processus de collecte des données</li>
                  </ul>
                </div>

                <div class="cv__item">
                  <div class="cv__ligne-titre">
                    <p><strong>Formateur Business Intelligence</strong>, FREELANCE - Paris</p>
                    <span class="cv__dates">2016 - 2017<small>1 an</small></span>
                  </div>
                  <ul>
                    <li>Import et transformation des données ETL Power Query</li>
                    <li>Architecture et modélisation des tables</li>
                    <li>Fonctionnalités des outils de visualisation</li>
                  </ul>
                </div>

                <div class="cv__item">
                  <div class="cv__ligne-titre">
                    <p><strong>Contrôleur de Gestion</strong>, ENTREPRISE - Paris</p>
                    <span class="cv__dates">2009 - 2015<small>6 ans</small></span>
                  </div>
                  <ul>
                    <li>Analyse de la performance commerciale et logistique</li>
                    <li>Suivi de la rentabilité de chaque Business Unit</li>
                    <li>Création et mise à jour de tableaux de bord</li>
                  </ul>
                </div>
              </section>

              <section class="cv__section">
                <h4 class="cv__titre">Formation</h4>

                <div class="cv__item">
                  <div class="cv__ligne-titre">
                    <p><strong>ESSEC Business School</strong> - Paris</p>
                    <span class="cv__dates">2008 - 2011</span>
                  </div>
                  <p class="cv__detail">Master In Business Intelligence</p>
                </div>

                <div class="cv__item">
                  <div class="cv__ligne-titre">
                    <p><strong>Université de Strasbourg</strong> - Strasbourg</p>
                    <span class="cv__dates">2005 - 2007</span>
                  </div>
                  <p class="cv__detail">Licence 2 de Science juridique</p>
                </div>
              </section>

            </div>
          </div>

        </article>
      </div>

    </section>
  </main>

</template>