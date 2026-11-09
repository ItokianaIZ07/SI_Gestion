<script setup>
// Page 3 : nouvelle demande de congé saisie par le RH pour un employé.
// Le formulaire (types avec règles, solde, remplaçant) est dans le composant partagé.
import '@/assets/finance.css'
import FormulaireDemandeConge from '@/components/conge/FormulaireDemandeConge.vue'
</script>

<template>
  <section class="fin-page">
    <h1>Nouvelle demande de congé</h1>
    <p class="fin-sub">Saisissez une demande pour un employé. Le nombre de jours compte du lundi au vendredi ; le backend contrôle le décompte final.</p>
    <FormulaireDemandeConge lien-demandes="/si/app/rh/conges/demandes" libelle-lien="Voir les demandes" />
  </section>
</template>
