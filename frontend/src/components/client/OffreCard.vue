<script setup>
import { defineProps } from 'vue';
import { formatDate, formatSalaire } from "@/config/libelles";


const proprs = defineProps({
    offre: { type: Object, required: true },
    publication: { type: Object, required: true }
});

const extrait = (t, n = 160) => (t && t.length > n ? `${t.slice(0, n).trimEnd()}…` : t ?? "");

</script>

<template>
    <article class="offre">
        <div class="offre__tete">
            <span class="offre__avatar" aria-hidden="true">{{ (offre.titre ?? "?").trim()[0]?.toUpperCase() }}</span>
            <h2>{{ offre.titre }}</h2>
        </div>
        <p class="offre__meta">
            <span v-if="offre.niveauEtude">{{ offre.niveauEtude }}</span>
            <span>{{ offre.experienceMinAnnees }} an(s) d'expérience min.</span>
        </p>
        <p class="offre__texte">{{ extrait(offre.descriptionPoste) }}</p>
        <p class="offre__salaire">{{ formatSalaire(offre.salaireMin, offre.salaireMax) }}</p>
        <p v-if="publication.dateExpiration" class="offre__limite">Candidatures jusqu'au {{
            formatDate(publication.dateExpiration) }}</p>
        <div class="offre__actions">
            <RouterLink class="bouton" :to="`/si/client/offres/${publication.id}`">Voir l'offre</RouterLink>
            <RouterLink class="bouton bouton--accent" :to="`/si/client/offres/${publication.id}/postuler`">Postuler
            </RouterLink>
        </div>
    </article>
</template>

<style scoped>
.offre {
    height: 100%;
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 24px;
    background: var(--fond-card);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
    transition: border-color .2s, box-shadow .2s, transform .2s;
}

.offre:hover {
    border-color: var(--color-primary-line);
    box-shadow: var(--shadow-md);
    transform: translateY(-2px);
}

.offre__tete { display: flex; align-items: center; gap: 12px; }

.offre__avatar {
    display: grid;
    place-items: center;
    flex: none;
    width: 42px;
    height: 42px;
    border-radius: 12px;
    color: #fff;
    font-weight: 700;
    background: linear-gradient(135deg, var(--color-primary), var(--color-secondary));
}

.offre h2 {
    margin: 0;
    font-size: 1.2rem;
}

.bouton--accent {
  color: #fff;
  background: var(--principale);
  border-color: var(--principale);
}

.bouton--accent:hover {
  background: #014d07;
  border-color: #014d07;
}

.offre__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin: 0;
}

.offre__meta span {
    padding: 3px 10px;
    font-size: .85rem;
    color: var(--color-primary);
    background: var(--color-primary-soft);
    border-radius: 999px;
}

.offre__texte {
    margin: 0;
    color: var(--texte-doux);
    flex: 1;
}

.offre__salaire {
    margin: 0;
    font-weight: 600;
}

.offre__limite {
    margin: 0;
    font-size: .9rem;
    color: var(--texte-doux);
}

.offre__actions {
    display: flex;
    gap: 10px;
    margin-top: 6px;
}

.offre__actions .bouton {
    padding: 9px 18px;
    text-align: center;
}
</style>