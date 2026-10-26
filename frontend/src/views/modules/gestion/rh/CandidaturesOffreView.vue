<script setup>
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";

import CandidatItem from "@/components/CandidatItem.vue";
import {
    offresApi,
    candidaturesApi,
    candidatsApi,
} from "@/services/ressources";

const route = useRoute();
const router = useRouter();

const offre = ref(null);
const candidatures = ref([]);
const candidats = ref([]);
const chargement = ref(true);
const erreur = ref("");
const message = ref("");
const actionEnCours = ref(null);

function candidatDe(candidature) {
    return candidats.value.find(
        (c) => String(c.id) === String(candidature.candidatId)
    ) ?? null;
}

function extraireListe(reponse) {
    const donnees = reponse?.data ?? reponse;
    return Array.isArray(donnees) ? donnees : [];
}

async function chargerDonnees() {
    chargement.value = true;
    erreur.value = "";

    try {
        const [offresRep, candidatsRep] = await Promise.all([
            offresApi.liste(),
            candidatsApi.liste(),
        ]);

        const offres = extraireListe(offresRep);
        candidats.value = extraireListe(candidatsRep);

        offre.value = offres.find(
            (o) => String(o.id) === String(route.params.offreId)
        );

        if (!offre.value) {
            throw new Error(
                `Offre ${route.params.offreId} introuvable.`
            );
        }

        if (!offre.value.recrutementId) {
            throw new Error(
                "Cette offre n'est associée à aucun recrutement."
            );
        }

        const candidaturesRep =
            await candidaturesApi.parRecrutement(
                offre.value.recrutementId
            );

        candidatures.value = extraireListe(candidaturesRep);
    } catch (e) {
        console.error("Erreur de chargement :", e);
        erreur.value =
            e.response?.data?.message ??
            e.message ??
            "Impossible de charger les candidatures.";
    } finally {
        chargement.value = false;
    }
}

function ouvrirCvCandidat(candidat, candidature) {
    if (!candidat || !candidature) {
        erreur.value = "Candidat ou candidature introuvable.";
        return;
    }

    if (!candidature.cvId) {
        erreur.value = "Aucun CV n'est associé à cette candidature.";
        return;
    }

    router.push({
        name: "CvCandidat",
        params: { candidatId: candidat.id },
        query: {
            cvId: candidature.cvId,
            candidatureId: candidature.id,
        },
    });
}

async function changerStatut(candidature, nouveauStatut) {
    if (!candidature?.id || actionEnCours.value !== null) return;

    const statutInitial = candidature.statut;
    actionEnCours.value = candidature.id;
    erreur.value = "";
    message.value = "";

    try {
        const reponse = await candidaturesApi.changerStatut(
            candidature.id,
            nouveauStatut
        );

        // Si l'API renvoie la candidature mise à jour, on utilise sa réponse.
        const candidatureMiseAJour = reponse.data;

        const index = candidatures.value.findIndex(
            (c) => String(c.id) === String(candidature.id)
        );

        if (index !== -1) {
            candidatures.value[index] = {
                ...candidatures.value[index],
                ...(candidatureMiseAJour &&
                    typeof candidatureMiseAJour === "object"
                    ? candidatureMiseAJour
                    : {}),
                statut:
                    candidatureMiseAJour?.statut ??
                    nouveauStatut,
            };
        }

        message.value = `Statut modifié : ${statutInitial ?? "inconnu"} → ${nouveauStatut}.`;
    } catch (e) {
        console.error(e);
        erreur.value =
            e.response?.data?.message ??
            "Échec du changement de statut.";
    } finally {
        actionEnCours.value = null;
    }
}

onMounted(chargerDonnees);
</script>

<template>
    <main class="page-candidatures">
        <button class="retour" type="button" @click="router.back()">
            ← Retour aux offres
        </button>

        <header class="page-header">
            <h1>Candidatures de l'offre</h1>
            <p v-if="offre">{{ offre.titre ?? offre.intitule ?? offre.nom }}</p>
        </header>

        <p v-if="chargement">Chargement des candidatures...</p>

        <p v-else-if="erreur" class="message erreur">
            {{ erreur }}
        </p>

        <p v-if="message" class="message succes">
            {{ message }}
        </p>

        <section v-if="!chargement" class="liste-candidats">
            <p v-if="candidatures.length === 0">
                Aucune candidature pour cette offre.
            </p>

            <p v-else-if="candidatures.every((c) => !candidatDe(c))" class="message erreur">
                Les candidatures ont été chargées, mais aucun candidat correspondant
                n'a été retrouvé. Vérifie les identifiants renvoyés par l'API.
            </p>

            <CandidatItem v-for="c in candidatures.filter((c) => candidatDe(c))" :key="c.id" :candidat="candidatDe(c)"
                :candidature="c" :select-candidat="ouvrirCvCandidat" :changer-statut="changerStatut"
                :action-en-cours="actionEnCours === c.id" />
        </section>
    </main>
</template>

<style scoped>
.page-candidatures {
    padding: 24px;
}

.retour {
    margin-bottom: 18px;
    padding: 8px 12px;
    border: 1px solid #ddd;
    border-radius: 8px;
    background: white;
    cursor: pointer;
}

.page-header {
    margin-bottom: 22px;
}

.page-header h1 {
    margin-bottom: 6px;
}

.page-header p {
    color: #707770;
}

.liste-candidats {
    display: grid;
    gap: 12px;
}

.message {
    margin: 12px 0;
    font-size: 14px;
}

.erreur {
    color: #a52a22;
}

.succes {
    color: #216b35;
}
</style>
