<script setup>
import { useRouter } from 'vue-router';
import NavBar from '../components/NavBar.vue';
import FormulaireCV from '../components/FormulaireCV.vue';

const router = useRouter()

function retourner(){
    router.back()
}

</script>
<template>
  <NavBar />

   <button type="button" class="bouton-retour" @click="retourner">
    ← Retour
  </button>

  <main class="principal principal--formulaire">
    <div class="intro">
      <h1>Uploader un CV</h1>
      <p>Renseignez vos informations et joignez votre CV au format PDF. Il s'affichera en dessous dès que vous l'aurez choisi.</p>
    </div>

    <FormulaireCV/>

    
  </main>

</template>
