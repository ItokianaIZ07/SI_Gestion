<script setup>
import NavBar from '../components/NavBar.vue';
</script>
<template>
  <NavBar />

   

  <main class="principal principal--formulaire">
    <div class="intro">
      <h1>Uploader un CV</h1>
      <p>Renseignez vos informations et joignez votre CV au format PDF. Il s'affichera en dessous dès que vous l'aurez choisi.</p>
    </div>

    <form class="formulaire" action="#" method="post" enctype="multipart/form-data">
      <div class="champ">
        <label for="nom">Nom</label>
        <input type="text" id="nom" name="nom" autocomplete="family-name" required>
      </div>

      <div class="champ">
        <label for="prenom">Prénom</label>
        <input type="text" id="prenom" name="prenom" autocomplete="given-name" required>
      </div>

      <div class="champ">
        <label for="cin">CIN</label>
        <input type="text" id="cin" name="cin" inputmode="numeric" maxlength="12" pattern="[0-9]{12}"
               title="Le CIN contient 12 chiffres" placeholder="12 chiffres" required>
      </div>

      <div class="champ">
        <label for="telephone">Numéro de téléphone</label>
        <input type="tel" id="telephone" name="telephone" autocomplete="tel" placeholder="034 00 000 00" required>
      </div>

      <div class="champ champ--large">
        <label for="email">Email</label>
        <input type="email" id="email" name="email" autocomplete="email" placeholder="nom@exemple.com" required>
      </div>

      <div class="champ champ--large">
        <label for="cv">CV (PDF)</label>
        <input type="file" id="cv" name="cv" accept="application/pdf,.pdf" required>
        <p class="champ__erreur" id="cv-erreur" role="alert" hidden></p>
      </div>

      <div class="champ champ--large">
        <label for="experience">Expérience</label>
        <textarea id="experience" name="experience" rows="6"
                  placeholder="Postes occupés, entreprises, durées…" required></textarea>
      </div>

      <div class="formulaire__actions champ--large">
        <button type="submit" class="bouton">Envoyer</button>
      </div>
    </form>

    <!-- Aperçu du PDF : affiché après le choix du fichier -->
    <section class="apercu" id="apercu" aria-labelledby="apercu-titre" hidden>
      <div class="apercu__entete">
        <h2 id="apercu-titre">Aperçu du CV</h2>
        <span class="apercu__fichier" id="apercu-nom"></span>
      </div>
      <iframe class="apercu__cadre" id="apercu-cadre" title="Aperçu du CV au format PDF"></iframe>
      <p class="apercu__secours">
        L'aperçu ne s'affiche pas ? <a id="apercu-lien" href="#" target="_blank" rel="noopener">Ouvrir le PDF dans un nouvel onglet</a>.
      </p>
    </section>
  </main>

</template>
