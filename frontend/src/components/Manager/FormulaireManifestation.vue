<script setup>

  defineProps({
    departements: {type: Object, required: true},
    postes: {type: Object, required: true}
  })

  (function () {
    var dateManifestation = document.getElementById('date-manifestation');
    var dateArrivee = document.getElementById('date-arrivee');

    // Date du jour au format AAAA-MM-JJ (heure locale)
    var aujourdhui = new Date().toLocaleDateString('en-CA');

    dateManifestation.value = aujourdhui;
    dateArrivee.min = aujourdhui;

    // La date d'arrivée ne peut pas précéder la date de la manifestation
    dateManifestation.addEventListener('change', function () {
      if (dateManifestation.value) {
        dateArrivee.min = dateManifestation.value;
        if (dateArrivee.value && dateArrivee.value < dateManifestation.value) {
          dateArrivee.value = '';
        }
      }
    });
  })();

</script>
<template>
  
  <form class="formulaire" action="#" method="post">
    <div class="champ">
      <label for="departement">Département <span class="requis" aria-hidden="true">*</span></label>
      <select id="departement" name="departement" required>
        <option value="" selected disabled>Choisir un département</option>
        <option v-for="departement in departements" :key="departement">{{ departement }}</option>
        <!-- <option>Informatique</option>
        <option>Ressources humaines</option>
        <option>Finance</option>
        <option>Commercial</option>
        <option>Production</option>
        <option>Logistique</option> -->
      </select>
    </div>

    <div class="champ">
      <label for="poste">Poste <span class="requis" aria-hidden="true">*</span></label>
      <input type="text" id="poste" name="poste" placeholder="Ex. Développeur backend" required>
    </div>

    <div class="champ">
      <label for="nombre">Nombre de personnes <span class="requis" aria-hidden="true">*</span></label>
      <input type="number" id="nombre" name="nombre" min="1" step="1" value="1" inputmode="numeric" required>
    </div>

    <div class="champ">
      <label for="type">Type de besoin <span class="requis" aria-hidden="true">*</span></label>
      <select id="type" name="type" required>
        <option value="" selected disabled>Choisir un type</option>
        <option v-for="poste in postes" :key="poste">{{ poste }}</option>
        <!-- <option>Nouveau poste</option>
        <option>Remplacement</option>
        <option>Renfort temporaire</option>
        <option>Stage</option> -->
      </select>
    </div>

    <div class="champ">
      <label for="date-manifestation">Date de la manifestation <span class="requis" aria-hidden="true">*</span></label>
      <input type="date" id="date-manifestation" name="date_manifestation" required>
    </div>

    <div class="champ">
      <label for="date-arrivee">Date souhaitée d'arrivée <span class="requis" aria-hidden="true">*</span></label>
      <input type="date" id="date-arrivee" name="date_arrivee" required>
    </div>

    <div class="champ champ--large">
      <label for="lieu">Lieu de travail <span class="requis" aria-hidden="true">*</span></label>
      <input type="text" id="lieu" name="lieu" placeholder="Ex. Siège, Antananarivo" required>
    </div>

    <div class="champ champ--large">
      <label for="motif">Motif détaillé <span class="requis" aria-hidden="true">*</span></label>
      <textarea id="motif" name="motif" rows="5"
                placeholder="Pourquoi ce besoin ? Contexte, charge de travail, projet concerné…" required></textarea>
    </div>

    <div class="champ champ--large">
      <label for="experience">Expérience requise <span class="requis" aria-hidden="true">*</span></label>
      <textarea id="experience" name="experience" rows="4"
                placeholder="Ex. 2 ans minimum en développement Java, maîtrise de Spring Boot…" required></textarea>
    </div>

    <p class="formulaire__note"><span class="requis" aria-hidden="true">*</span> Champs obligatoires</p>

    <div class="formulaire__actions champ--large">
      <button type="submit" class="bouton">Envoyer la demande</button>
    </div>
  </form>
</template>