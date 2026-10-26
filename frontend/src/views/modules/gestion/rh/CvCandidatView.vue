
<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";
import { useRoute, useRouter } from "vue-router";

import { candidatsApi, cvsApi } from "@/services/ressources";

const route = useRoute();
const router = useRouter();

const candidat = ref(null);
const cv = ref(null);
const urlPdf = ref("");
const chargement = ref(true);
const erreur = ref("");

function extraireDonnees(reponse) {
    return reponse?.data ?? reponse;
}

function extraireListe(reponse) {
    const donnees = extraireDonnees(reponse);

    if (Array.isArray(donnees)) {
        return donnees;
    }

    // Certaines API renvoient une liste dans un objet paginé.
    if (Array.isArray(donnees?.content)) {
        return donnees.content;
    }

    return donnees ? [donnees] : [];
}

function libererUrlPdf() {
    if (urlPdf.value) {
        URL.revokeObjectURL(urlPdf.value);
        urlPdf.value = "";
    }
}

async function chargerCv() {
    chargement.value = true;
    erreur.value = "";

    candidat.value = null;
    cv.value = null;
    libererUrlPdf();

    try {
        const candidatId = route.params.candidatId;
        const cvId = route.query.cvId;

        if (!candidatId) {
            throw new Error("L'identifiant du candidat est absent de l'URL.");
        }

        if (!cvId) {
            throw new Error("L'identifiant du CV est absent de l'URL.");
        }

        const [candidatsRep, cvsRep] = await Promise.all([
            candidatsApi.liste(),
            cvsApi.parCandidat(candidatId),
        ]);

        const listeCandidats = extraireListe(candidatsRep);
        const listeCvs = extraireListe(cvsRep);

        candidat.value = listeCandidats.find(
            (item) => String(item.id ?? item.candidatId) === String(candidatId)
        );

        if (!candidat.value) {
            throw new Error("Candidat introuvable.");
        }

        cv.value = listeCvs.find(
            (item) => String(item.id ?? item.cvId) === String(cvId)
        );

        if (!cv.value) {
            throw new Error("CV introuvable pour ce candidat.");
        }

        // Vérification supplémentaire si l'API expose l'identifiant du candidat
        // sur l'objet CV.
        const proprietaireId =
            cv.value.candidatId ??
            cv.value.idCandidat ??
            cv.value.candidat?.id;

        if (
            proprietaireId != null &&
            String(proprietaireId) !== String(candidatId)
        ) {
            throw new Error("Ce CV n'appartient pas au candidat sélectionné.");
        }

        const fichierRep = await cvsApi.fichier(cv.value.id ?? cv.value.cvId);
        const fichier = extraireDonnees(fichierRep);

        if (!(fichier instanceof Blob)) {
            throw new Error(
                "Le fichier reçu n'est pas un Blob. Vérifie que cvsApi.fichier utilise responseType: 'blob'."
            );
        }

        if (fichier.size === 0) {
            throw new Error("Le fichier CV reçu est vide.");
        }

        urlPdf.value = URL.createObjectURL(fichier);
    } catch (e) {
        console.error("Erreur lors du chargement du CV :", e);

        erreur.value =
            e.response?.data?.message ??
            e.message ??
            "Impossible de charger le CV.";
    } finally {
        chargement.value = false;
    }
}

onMounted(chargerCv);
onBeforeUnmount(libererUrlPdf);
</script>

<template>
    <main class="page-cv">
        <button class="retour" type="button" @click="router.back()">
            ← Retour aux candidatures
        </button>

        <p v-if="chargement">Chargement du CV...</p>

        <p v-else-if="erreur" class="erreur">
            {{ erreur }}
        </p>

        <template v-else-if="candidat && cv">
            <header class="cv-header">
                <div>
                    <h1>
                        {{ candidat.prenom ?? "" }}
                        {{ candidat.nom ?? "" }}
                    </h1>

                    <p v-if="candidat.email">{{ candidat.email }}</p>
                    <p v-if="candidat.telephone">{{ candidat.telephone }}</p>

                    <p class="nom-fichier">
                        {{ cv.nomFichier ?? cv.nom ?? "CV du candidat" }}
                    </p>
                </div>

                <a
                    v-if="urlPdf"
                    :href="urlPdf"
                    target="_blank"
                    rel="noopener noreferrer"
                    class="ouvrir-pdf"
                >
                    Ouvrir le PDF
                </a>
            </header>

            <section class="lecteur-pdf">
                <iframe
                    v-if="urlPdf"
                    :src="urlPdf"
                    title="CV du candidat"
                    class="pdf-frame"
                />
            </section>
        </template>
    </main>
</template>

<style scoped>
.page-cv {
    padding: 24px;
}

.retour {
    margin-bottom: 20px;
    padding: 8px 12px;
    border: 1px solid #ddd;
    border-radius: 8px;
    background: white;
    cursor: pointer;
}

.cv-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 16px;
    margin-bottom: 20px;
}

.cv-header h1 {
    margin: 0 0 8px;
}

.cv-header p {
    margin: 5px 0;
    color: #707770;
}

.nom-fichier {
    font-size: 13px;
}

.ouvrir-pdf {
    padding: 10px 14px;
    border-radius: 8px;
    background: var(--color-primary);
    color: white;
    text-decoration: none;
}

.lecteur-pdf {
    overflow: hidden;
    width: 100%;
    min-height: 70vh;
    border: 1px solid #ddd;
    border-radius: 10px;
    background: #f5f5f5;
}

.pdf-frame {
    display: block;
    width: 100%;
    height: 75vh;
    min-height: 500px;
    border: 0;
}

.erreur {
    color: #a52a22;
}
</style>
