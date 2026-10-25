<script setup>
import { computed, onMounted, ref } from "vue";
import api from "@/services/api";

const props = defineProps({
    demande: {
        type: Object,
        required: true,
    },
});

const emit = defineEmits(["fermer", "confirmer"]);

const grilles = ref([]);
const grilleSelectionnee = ref(null);
const salaire = ref("");
const chargement = ref(false);
const erreur = ref("");
const idDepartementManquant = ref(false);

const grilleActive = computed(() =>
    grilles.value.find(
        (g) => g.id === grilleSelectionnee.value
    )
);

const montantValide = computed(() => {
    const montant = Number(salaire.value);
    const grille = grilleActive.value;

    return (
        grille &&
        salaire.value !== "" &&
        Number.isFinite(montant) &&
        montant >= Number(grille.valeurMin) &&
        montant <= Number(grille.valeurMax)
    );
});

function choisirGrille(grille) {
    grilleSelectionnee.value = grille.id;

    // Préremplissage avec le salaire demandé s'il appartient
    // à la fourchette, sinon avec le minimum de la grille.
    const demandeInitiale = Number(props.demande.salaire);

    salaire.value =
        Number.isFinite(demandeInitiale) &&
            demandeInitiale >= Number(grille.valeurMin) &&
            demandeInitiale <= Number(grille.valeurMax)
            ? String(demandeInitiale)
            : String(grille.valeurMin);

    erreur.value = "";
}

async function chargerGrilles() {
    // Le DTO du besoin doit exposer l'identifiant numérique
    // du département, et pas seulement son nom.
    const idDepartement = props.demande.idDepartement;

    if (idDepartement == null) {
        idDepartementManquant.value = true;
        erreur.value =
            "L'identifiant du département est absent des données du besoin.";
        return;
    }

    chargement.value = true;
    erreur.value = "";

    try {
        const response = await api.get(
            `/finance/grilles-salaire/departement/${idDepartement}`
        );

        // Compatible avec une API renvoyant directement un tableau
        // ou une réponse enveloppée dans { data: [...] }.
        const donnees = response.data ?? response;

        grilles.value = Array.isArray(donnees)
            ? donnees
            : Array.isArray(donnees?.data)
                ? donnees.data
                : [];

        if (grilles.value.length === 0) {
            erreur.value =
                "Aucune grille salariale n'est disponible pour ce département.";
        }
    } catch (e) {
        erreur.value =
            e.response?.data?.message ??
            e.response?.data?.detail ??
            e.message ??
            "Impossible de charger les grilles salariales.";
    } finally {
        chargement.value = false;
    }
}

function confirmer() {
    if (!montantValide.value) {
        erreur.value =
            "Choisissez une grille et saisissez un salaire compris dans sa fourchette.";
        return;
    }

    emit("confirmer", {
        idBesoin: props.demande.id,
        idGrilleSalaire: grilleSelectionnee.value,
        salaire: Number(salaire.value),
    });
}

function formatMontant(valeur) {
    return new Intl.NumberFormat("fr-FR", {
        maximumFractionDigits: 2,
    }).format(Number(valeur)) + " Ar";
}

function formatDate(valeur) {
    if (!valeur) return "Sans date de fin";

    return new Date(`${valeur}T00:00:00`).toLocaleDateString("fr-FR");
}

onMounted(chargerGrilles);
</script>

<template>
    <div class="modal-overlay" @click.self="emit('fermer')">
        <section class="salary-modal" role="dialog" aria-modal="true" aria-labelledby="salary-title">
            <header class="modal-header">
                <div>
                    <span class="eyebrow">VALIDATION FINANCIÈRE</span>
                    <h2 id="salary-title">Valider le salaire</h2>
                    <p>
                        {{ demande.poste }} · {{ demande.departement }}
                    </p>
                </div>

                <button type="button" class="close-button" aria-label="Fermer" @click="emit('fermer')">
                    &times;
                </button>
            </header>

            <div class="modal-body">
                <div class="requested-salary">
                    <span>Salaire initial de la demande</span>
                    <strong>{{ formatMontant(demande.salaire) }}</strong>
                </div>

                <div class="section-heading">
                    <div>
                        <h3>Grilles salariales du département</h3>
                        <p>Choisissez la fourchette applicable à ce poste.</p>
                    </div>
                    <span class="count">{{ grilles.length }} grille(s)</span>
                </div>

                <div v-if="chargement" class="feedback">
                    Chargement des grilles salariales...
                </div>

                <div v-else-if="erreur && grilles.length === 0" class="error-box">
                    <p>{{ erreur }}</p>
                    <button v-if="!idDepartementManquant" type="button" class="retry-button" @click="chargerGrilles">
                        Réessayer
                    </button>
                </div>

                <div v-else class="grid-list">
                    <button v-for="grille in grilles" :key="grille.id" type="button" class="grid-card" :class="{
                        selected: grilleSelectionnee === grille.id,
                    }" @click="choisirGrille(grille)">
                        <span class="grid-radio">
                            <span v-if="grilleSelectionnee === grille.id"></span>
                        </span>

                        <span class="grid-content">
                            <strong>{{ grille.poste }}</strong>

                            <span class="grid-range">
                                {{ formatMontant(grille.valeurMin) }}
                                <span>à</span>
                                {{ formatMontant(grille.valeurMax) }}
                            </span>

                            <span class="grid-dates">
                                Du {{ formatDate(grille.dateDebutValidite) }}
                                au {{ formatDate(grille.dateFinValidite) }}
                            </span>
                        </span>
                    </button>
                </div>

                <div v-if="grilleActive" class="salary-editor">
                    <label for="salaire-retenu">Salaire mensuel retenu (Ar)</label>

                    <input id="salaire-retenu" v-model="salaire" type="number" min="0" step="any"
                        :min="grilleActive.valeurMin" :max="grilleActive.valeurMax"
                        placeholder="Saisissez le salaire" />

                    <div class="range-hint">
                        Fourchette autorisée :
                        <strong>{{ formatMontant(grilleActive.valeurMin) }}</strong>
                        –
                        <strong>{{ formatMontant(grilleActive.valeurMax) }}</strong>
                    </div>

                    <p v-if="salaire !== '' && !montantValide" class="field-error">
                        Le salaire doit respecter les limites de la grille sélectionnée.
                    </p>

                    <div v-if="montantValide" class="salary-summary">
                        <span>Nouveau salaire du besoin</span>
                        <strong>{{ formatMontant(salaire) }}</strong>
                    </div>
                </div>

                <p v-if="erreur && grilles.length > 0" class="field-error">
                    {{ erreur }}
                </p>
            </div>

            <footer class="modal-footer">
                <button type="button" class="cancel-button" @click="emit('fermer')">
                    Annuler
                </button>

                <button type="button" class="confirm-button" :disabled="!montantValide || chargement"
                    @click="confirmer">
                    Confirmer l'acceptation
                </button>
            </footer>
        </section>
    </div>
</template>

<style scoped>
.modal-overlay {
    position: fixed;
    inset: 0;
    z-index: 1000;
    display: grid;
    place-items: center;
    padding: 20px;
    background: rgb(20 29 22 / 55%);
    backdrop-filter: blur(4px);
}

.salary-modal {
    width: min(100%, 680px);
    max-height: 90vh;
    overflow-y: auto;
    border: 1px solid #e6eae5;
    border-radius: 18px;
    background: #fff;
    box-shadow: 0 24px 80px rgb(0 0 0 / 20%);
}

.modal-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    padding: 24px;
    border-bottom: 1px solid #edf0eb;
}

.eyebrow {
    color: #278044;
    font-size: 10px;
    font-weight: 800;
    letter-spacing: 1.2px;
}

.modal-header h2 {
    margin: 8px 0;
    color: #202c23;
    font-size: 22px;
}

.modal-header p,
.section-heading p {
    margin: 0;
    color: #778078;
    font-size: 12px;
}

.close-button {
    width: 34px;
    height: 34px;
    border: 0;
    border-radius: 9px;
    background: #f3f5f2;
    color: #505950;
    font-size: 25px;
    cursor: pointer;
}

.modal-body {
    padding: 24px;
}

.requested-salary,
.salary-summary {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 15px;
    border-radius: 11px;
}

.requested-salary {
    background: #f5f7f4;
}

.requested-salary span,
.salary-summary span {
    color: #69736a;
    font-size: 12px;
}

.requested-salary strong {
    color: #343e35;
    font-size: 17px;
}

.section-heading {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    margin: 25px 0 14px;
}

.section-heading h3 {
    margin: 0 0 5px;
    font-size: 14px;
    color: #273329;
}

.count {
    white-space: nowrap;
    color: #667268;
    font-size: 11px;
}

.grid-list {
    display: grid;
    gap: 10px;
}

.grid-card {
    display: flex;
    width: 100%;
    gap: 12px;
    align-items: flex-start;
    padding: 16px;
    text-align: left;
    border: 1px solid #e5e9e3;
    border-radius: 12px;
    background: #fff;
    cursor: pointer;
    transition: border-color 0.2s, background 0.2s;
}

.grid-card:hover,
.grid-card.selected {
    border-color: #28834a;
    background: #f5faf5;
}

.grid-radio {
    display: grid;
    flex-shrink: 0;
    place-items: center;
    width: 18px;
    height: 18px;
    margin-top: 2px;
    border: 1.5px solid #c4ccc3;
    border-radius: 50%;
}

.selected .grid-radio {
    border-color: #28834a;
}

.grid-radio span {
    width: 9px;
    height: 9px;
    border-radius: 50%;
    background: #28834a;
}

.grid-content {
    display: grid;
    gap: 7px;
    min-width: 0;
}

.grid-content>strong {
    color: #303b31;
    font-size: 13px;
}

.grid-range {
    display: flex;
    flex-wrap: wrap;
    gap: 5px;
    color: #24733c;
    font-size: 13px;
    font-weight: 750;
}

.grid-range span {
    color: #7c857c;
    font-weight: 400;
}

.grid-dates {
    color: #7d867d;
    font-size: 11px;
}

.salary-editor {
    display: grid;
    gap: 10px;
    margin-top: 24px;
}

.salary-editor label {
    color: #344035;
    font-size: 12px;
    font-weight: 700;
}

.salary-editor input {
    width: 100%;
    box-sizing: border-box;
    padding: 13px;
    border: 1px solid #dce3da;
    border-radius: 9px;
    outline: none;
    font: inherit;
}

.salary-editor input:focus {
    border-color: #28834a;
    box-shadow: 0 0 0 3px rgb(40 131 74 / 10%);
}

.range-hint {
    color: #737e74;
    font-size: 11px;
    line-height: 1.8;
}

.range-hint strong {
    color: #465247;
}

.salary-summary {
    margin-top: 6px;
    border: 1px solid #d9ead9;
    background: #f0f8ef;
}

.salary-summary strong {
    color: #24733c;
    font-size: 17px;
}

.feedback {
    padding: 25px 10px;
    color: #69736a;
    text-align: center;
    font-size: 13px;
}

.error-box {
    padding: 14px;
    border-radius: 10px;
    background: #fff4f2;
    color: #a53e32;
    font-size: 12px;
}

.field-error {
    margin: 0;
    color: #b42318;
    font-size: 12px;
}

.retry-button {
    padding: 8px 12px;
    border: 1px solid #dca49e;
    border-radius: 7px;
    background: #fff;
    color: #a53e32;
    cursor: pointer;
}

.modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    padding: 18px 24px;
    border-top: 1px solid #edf0eb;
}

.modal-footer button {
    padding: 11px 15px;
    border-radius: 9px;
    font-size: 12px;
    font-weight: 700;
    cursor: pointer;
}

.cancel-button {
    border: 1px solid #dce2da;
    background: #fff;
    color: #495349;
}

.confirm-button {
    border: 1px solid #24763b;
    background: #24763b;
    color: white;
}

.confirm-button:disabled {
    cursor: not-allowed;
    opacity: 0.45;
}

@media (max-width: 520px) {
    .modal-overlay {
        padding: 10px;
    }

    .modal-header,
    .modal-body {
        padding: 17px;
    }

    .modal-footer {
        flex-direction: column-reverse;
        padding: 16px;
    }

    .modal-footer button {
        width: 100%;
    }

    .requested-salary,
    .salary-summary {
        align-items: flex-start;
        flex-direction: column;
    }
}
</style>
