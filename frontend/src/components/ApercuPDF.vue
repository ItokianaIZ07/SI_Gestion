<template>
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
</template>