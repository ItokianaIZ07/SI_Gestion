<script setup>
import ApercuPDF from './ApercuPDF.vue';
</script>
<template>
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
    <ApercuPDF/>
</template>