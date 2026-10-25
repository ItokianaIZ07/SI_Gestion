<script setup>
import { computed } from "vue";

const props = defineProps({
    selectCandidat: {
        type: Function,
        required: true,
    },
    candidat: {
        type: Object,
        required: true,
    },
    candidature: {
        type: Object,
        required: true,
    },
    changerStatut: {
        type: Function,
        required: true,
    },
    actionEnCours: {
        type: Boolean,
        default: false,
    },
});

const prenom = computed(() => props.candidat?.prenom ?? "");
const nom = computed(() => props.candidat?.nom ?? "");
const statut = computed(() => props.candidature?.statut ?? "EN_ATTENTE");

const nomComplet = computed(() => {
    return `${prenom.value} ${nom.value}`.trim() || "Nom inconnu";
});

const initialesCandidat = computed(() => {
    const initiales = `${prenom.value.charAt(0)}${nom.value.charAt(0)}`;
    return initiales.toUpperCase() || "?";
});

function dateLisible(date) {
    if (!date) return "Date non disponible";

    const valeur = new Date(date);

    if (Number.isNaN(valeur.getTime())) {
        return String(date);
    }

    return new Intl.DateTimeFormat("fr-FR", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
    }).format(valeur);
}

function ouvrirCandidat() {
    props.selectCandidat(props.candidat, props.candidature);
}

function appliquerStatut(nouveauStatut) {
    props.changerStatut(props.candidature, nouveauStatut);
}

const selectionDesactivee = computed(() =>
    props.actionEnCours ||
    ["PRESELECTIONNE", "REJETE", "RETIRE"].includes(statut.value)
);

const rejetDesactive = computed(() =>
    props.actionEnCours ||
    ["REJETE", "RETIRE"].includes(statut.value)
);
</script>

<template>
    <article class="cv-item">
        <button type="button" class="cv-main" @click="ouvrirCandidat">
            <div class="cv-avatar">
                {{ initialesCandidat }}
            </div>

            <div class="cv-info">
                <div class="cv-name-row">
                    <h3>{{ nomComplet }}</h3>

                    <span class="cv-status">
                        {{ statut }}
                    </span>
                </div>

                <p v-if="candidat.email" class="cv-detail">
                    {{ candidat.email }}
                </p>

                <p v-if="candidat.telephone" class="cv-detail">
                    {{ candidat.telephone }}
                </p>

                <span class="cv-date">
                    Candidat créé le {{ dateLisible(candidat.dateCreation) }}
                </span>
            </div>
        </button>

        <div class="cv-actions">
            <button type="button" class="cv-action cv-action--selection" :disabled="selectionDesactivee"
                @click.stop="appliquerStatut('PRESELECTIONNE')">
                {{
                    statut === "PRESELECTIONNE"
                        ? "Présélectionné"
                        : "Présélectionner"
                }}
            </button>

            <button type="button" class="cv-action cv-action--rejet" :disabled="rejetDesactive"
                @click.stop="appliquerStatut('REJETE')">
                {{ statut === "REJETE" ? "Rejeté" : "Rejeter" }}
            </button>
        </div>
    </article>
</template>

<style scoped>
.cv-item {
    padding: 14px;
    border: 1px solid #e1e6e1;
    border-radius: 10px;
    background: white;
}

.cv-item:hover {
    border-color: var(--color-primary-line);
}

.cv-main {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    width: 100%;
    padding: 0;
    border: none;
    background: transparent;
    color: inherit;
    text-align: left;
    cursor: pointer;
    font: inherit;
}

.cv-main:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 3px;
}

.cv-avatar {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 42px;
    height: 42px;
    border-radius: 10px;
    background: var(--color-primary-soft);
    color: var(--color-primary);
    font-size: 13px;
    font-weight: 700;
}

.cv-info {
    flex: 1;
    min-width: 0;
}

.cv-name-row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 6px;
}

.cv-name-row h3 {
    margin: 0;
    font-size: 14px;
    font-weight: 650;
    overflow-wrap: anywhere;
}

.cv-detail {
    margin: 6px 0 0;
    color: #777e78;
    font-size: 12px;
    overflow-wrap: anywhere;
}

.cv-date {
    display: block;
    margin-top: 8px;
    color: #989e98;
    font-size: 11px;
}

.cv-status {
    padding: 4px 7px;
    border-radius: 5px;
    background: #f0f1ef;
    color: #555d56;
    font-size: 10px;
    font-weight: 600;
}

.cv-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 14px;
}

.cv-action {
    padding: 7px 10px;
    border: 1px solid #e1e6e1;
    border-radius: 7px;
    background: white;
    cursor: pointer;
    font-size: 12px;
    font-weight: 600;
}

.cv-action--selection {
    color: #216b35;
}

.cv-action--rejet {
    color: #a52a22;
}

.cv-action:disabled {
    opacity: 0.55;
    cursor: not-allowed;
}
</style>
