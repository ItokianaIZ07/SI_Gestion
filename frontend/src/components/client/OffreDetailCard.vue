<script setup>
import { computed } from "vue";
import { formatDate, formatSalaire } from "@/config/libelles";

const props = defineProps({
    annonce: {
        type: Object,
        required: true,
    },
});

const offre = computed(() => props.annonce?.offre);

const competences = computed(() =>
    (offre.value?.competencesRequises ?? "")
        .split(/[,;\n]/)
        .map((c) => c.trim())
        .filter(Boolean)
);
</script>

<template>
    <article class="detail">
        <header class="detail__entete">
            <h1>{{ offre.titre }}</h1>
            <p class="detail__meta">
                <span v-if="offre.niveauEtude">{{ offre.niveauEtude }}</span>
                <span>{{ offre.experienceMinAnnees }} an(s) d'expérience min.</span>
                <span>{{ formatSalaire(offre.salaireMin, offre.salaireMax) }}</span>
            </p>
            <p v-if="annonce.publication.dateExpiration" class="detail__limite">
                Candidatures jusqu'au {{ formatDate(annonce.publication.dateExpiration) }}
            </p>
        </header>

        <section>
            <h2>Description du poste</h2>
            <p class="detail__texte">{{ offre.descriptionPoste }}</p>
        </section>

        <section>
            <h2>Missions</h2>
            <p class="detail__texte">{{ offre.missions }}</p>
        </section>

        <section v-if="competences.length">
            <h2>Compétences requises</h2>
            <ul class="detail__tags">
                <li v-for="c in competences" :key="c">{{ c }}</li>
            </ul>
        </section>

        <footer class="detail__pied">
            <RouterLink class="bouton bouton--accent" :to="`/si/client/offres/${annonce.publication.id}/postuler`">
                Postuler à cette offre
            </RouterLink>
        </footer>
    </article>
</template>

<style scoped>
.detail {
    width: 100%;
    max-width: 760px;
    display: flex;
    flex-direction: column;
    gap: 28px;
    padding: 36px;
    background: var(--fond-card);
    border: 1.5px solid var(--couleur-texte);
    border-radius: 12px;
}

.detail__entete h1 {
    margin: 0 0 12px;
    font-size: 1.9rem;
    line-height: 1.2;
}

.detail__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin: 0;
}

.detail__meta span,
.detail__tags li {
    padding: 4px 12px;
    font-size: .9rem;
    color: var(--texte-doux);
    background: var(--fond);
    border-radius: 999px;
}

.detail__limite {
    margin: 12px 0 0;
    color: var(--texte-doux);
}

.detail h2 {
    margin: 0 0 8px;
    font-size: 1.15rem;
}

.detail__texte {
    margin: 0;
    white-space: pre-line;
    color: var(--texte-doux);
}

.detail__tags {
    list-style: none;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin: 0;
    padding: 0;
}

.detail__pied {
    display: flex;
    justify-content: flex-end;
    padding-top: 8px;
    border-top: 1px solid var(--fond);
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

@media (max-width: 800px) {
    .detail {
        padding: 24px 20px;
    }
}
</style>