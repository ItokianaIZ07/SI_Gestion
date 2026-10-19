<script setup>
</script>


<template>

  <header class="navbar">
    <a class="navbar__nom" href="#">Nom de la plateforme</a>

    <nav class="navbar__liens" aria-label="Navigation principale">
      <a href="#" aria-current="page">Accueil</a>
      <a href="#uploader">Uploader un CV</a>
      <a href="#editer">Editer un CV</a>
      <a href="#apropos">À propos</a>
    </nav>

    <a class="navbar__bouton" href="#">Connexion</a>
  </header>

  <main class="principal">
    <div class="intro">
      <h1>Votre CV, à votre façon</h1>
      <p>Importez un CV que vous avez déjà, ou créez-le directement en ligne avec nos modèles. Dans les deux cas, vous pouvez le télécharger en PDF quand il est prêt.</p>
    </div>

    <div class="choix">
      <a class="carte" id="uploader" href="#">
        <span class="carte__icone" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 16V4"/>
            <path d="M7 9l5-5 5 5"/>
            <path d="M4 16v3a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-3"/>
          </svg>
        </span>
        <span class="carte__titre">Uploader un CV</span>
        <span class="carte__texte">Importez votre CV existant au format PDF ou Word.</span>
        <span class="carte__action">Choisir un fichier</span>
      </a>

      <a class="carte" id="editer" href="#">
        <span class="carte__icone" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 20h9"/>
            <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"/>
          </svg>
        </span>
        <span class="carte__titre">Editer un CV en ligne</span>
        <span class="carte__texte">Créez ou modifiez votre CV directement dans l'éditeur.</span>
        <span class="carte__action">Ouvrir l'éditeur</span>
      </a>
    </div>
  </main>



</template>