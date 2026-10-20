<script setup>
  import NavBar from '../components/NavBar.vue';
  import FormulaireManifestation from '@/components/Manager/FormulaireManifestation.vue';
</script>


<template>

  <NavBar />

  <main class="principal">
    <div class="intro">
      <h1>Votre CV, à votre façon</h1>
      <p>Importez un CV que vous avez déjà, ou créez-le directement en ligne avec nos modèles. Dans les deux cas, vous
        pouvez le télécharger en PDF quand il est prêt.</p>
    </div>

    <FormulaireManifestation/>
  </main>

</template> create mode 100644 guide-dependances-vuejs.md
harena@harena-dynabook:~/Documents/ITU/S5/Gestion Entreprise/SI_Gestion/frontend$ git checkout Module-Manager
M       frontend/src/router/index.js
D       frontend/src/views/ChoiceUploadView.vue
Déjà sur 'Module-Manager'
Votre branche et 'origin/Module-Manager' ont divergé,
et ont 12 et 15 commits différents chacune respectivement.
  (use "git pull" if you want to integrate the remote branch with yours)
harena@harena-dynabook:~/Documents/ITU/S5/Gestion Entreprise/SI_Gestion/frontend$ git fetch origin
remote: Enumerating objects: 27, done.
remote: Counting objects: 100% (27/27), done.
remote: Compressing objects: 100% (18/18), done.
remote: Total 27 (delta 7), reused 21 (delta 7), pack-reused 0 (from 0)
Dépaquetage des objets: 100% (27/27), 8.13 Kio | 833.00 Kio/s, fait.
Depuis https://github.com/ItokianaIZ07/SI_Gestion
   c59099d..8877a08  dev                   -> origin/dev
 * [nouvelle branche] features/login-system -> origin/features/login-system
harena@harena-dynabook:~/Documents/ITU/S5/Gestion Entreprise/SI_Gestion/frontend$ git rebase origin/dev  
error: impossible de rebase : vous avez des modifications non indexées.
error: Veuillez les valider ou les remiser.
harena@harena-dynabook:~/Documents/ITU/S5/Gestion Entreprise/SI_Gestion/frontend$ 