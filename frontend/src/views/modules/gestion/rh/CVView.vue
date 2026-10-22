<script setup>
import { ref } from 'vue';
import ApercuPDF from '@/components/ApercuPDF.vue';
import '@/assets/theme.css';
import CVItem from '@/components/CVItem.vue';

const cvList = ref([
    {
        id: 1,
        nom: 'Rakoto',
        prenom: 'Jean',
        poste: 'Développeur Java',
        date: '20/10/2026',
        statut: 'Nouveau',
        fichier: '/cv/jean-rakoto.pdf'
    },
    {
        id: 2,
        nom: 'Rabe',
        prenom: 'Marie',
        poste: 'Développeuse Frontend',
        date: '19/10/2026',
        statut: 'Consulté',
        fichier: '/cv/marie-rabe.pdf'
    },
    {
        id: 3,
        nom: 'Andria',
        prenom: 'Hery',
        poste: 'Technicien informatique',
        date: '18/10/2026',
        statut: 'Nouveau',
        fichier: '/cv/hery-andria.pdf'
    },
    {
        id: 4,
        nom: 'Rasoa',
        prenom: 'Sarah',
        poste: 'Analyste de données',
        date: '17/10/2026',
        statut: 'Consulté',
        fichier: '/cv/sarah-rasoa.pdf'
    }
]);

const selectedPDF = ref('');
const selectedCV = ref(null);

function selectCV(cv) {
    selectedCV.value = cv;
    selectedPDF.value = cv.fichier;
}
</script>

<template>
    <div class="cv-container">
        <!-- Panneau gauche : liste des CV -->
        <section class="container cv-list">
            <header class="panel-header">
                <div>
                    <h2>CV reçus</h2>
                    <p>Consultez les candidatures reçues.</p>
                </div>

                <span class="cv-count">
                    {{ cvList.length }}
                </span>
            </header>

            <div class="cv-list-content">
                <CVItem v-for="cv in cvList" :key="cv.id" :cv="cv" :selectCV="selectCV" :class="{ active: selectedCV?.id === cv.id }"/>
            </div>
        </section>

        <!-- Panneau droit : aperçu du CV -->
        <section class="container cv-preview">
            <header class="panel-header preview-header">
                <div>
                    <h2>Aperçu du CV</h2>
                    <p>
                        {{
                            selectedCV
                                ? `${selectedCV.prenom} ${selectedCV.nom}`
                                : 'Sélectionnez un candidat pour consulter son CV.'
                        }}
                    </p>
                </div>

                <span v-if="selectedCV" class="file-type">
                    PDF
                </span>
            </header>

            <div class="preview-content">
                <ApercuPDF
                    v-if="selectedPDF"
                    :file="selectedPDF"
                />

                <div v-else class="empty-preview">
                    <div class="empty-icon">
                        <svg
                            viewBox="0 0 48 48"
                            width="48"
                            height="48"
                            fill="none"
                            aria-hidden="true"
                        >
                            <path
                                d="M13 5H28L38 15V40C38 41.7 36.7 43 35 43H13C11.3 43 10 41.7 10 40V8C10 6.3 11.3 5 13 5Z"
                                stroke="currentColor"
                                stroke-width="2"
                                stroke-linejoin="round"
                            />
                            <path
                                d="M27 5V16H38M17 25H31M17 32H31"
                                stroke="currentColor"
                                stroke-width="2"
                                stroke-linecap="round"
                            />
                        </svg>
                    </div>

                    <h3>Aucun CV sélectionné</h3>
                    <p>
                        Choisissez un candidat dans la liste pour
                        afficher son document ici.
                    </p>
                </div>
            </div>
        </section>
    </div>
</template>

<style scoped>
.cv-container {
    display: grid;
    grid-template-columns: minmax(300px, 0.85fr) minmax(0, 1.8fr);
    gap: 20px;
    align-items: stretch;
    width: 100%;
    min-height: 650px;
    padding: 20px;
    box-sizing: border-box;
    background-color: #f8f9f8;
}

/* Panneaux principaux */
.container {
    min-width: 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    background-color: #ffffff;
    border: 1px solid #e8ebe8;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(25, 45, 25, 0.025);
}

.cv-list {
    max-height: 780px;
}

.cv-preview {
    min-height: 650px;
}

/* En-têtes */
.panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 20px;
    background-color: #ffffff;
    border-bottom: 1px solid #f0f1ef;
}

.panel-header h2 {
    margin: 0;
    color: #252a26;
    font-size: 18px;
    font-weight: 700;
}

.panel-header p {
    margin: 6px 0 0;
    color: #858b86;
    font-size: 13px;
    line-height: 1.5;
}

/* Compteur */
.cv-count {
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 32px;
    height: 32px;
    padding: 0 8px;
    box-sizing: border-box;
    border: 1px solid #dcebdc;
    border-radius: 8px;
    background-color: #f0f7f0;
    color: var(--color-primary);
    font-size: 13px;
    font-weight: 700;
}

/* Liste */
.cv-list-content {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 12px;
    overflow-y: auto;
    background-color: #fdfdfc;
}

/* En-tête de l'aperçu */
.preview-header {
    background-color: #ffffff;
}

.file-type {
    padding: 5px 9px;
    border: 1px solid #e5e7e3;
    border-radius: 5px;
    background-color: #f7f8f6;
    color: #697169;
    font-size: 11px;
    font-weight: 700;
}

/* Zone d'affichage du PDF */
.preview-content {
    flex: 1;
    min-height: 0;
    padding: 16px;
    background-color: #f4f5f3;
}

.preview-content > :deep(*) {
    width: 100%;
}

/* État vide */
.empty-preview {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 480px;
    padding: 24px;
    text-align: center;
    border: 1px dashed #dfe3dc;
    border-radius: 10px;
    background-color: #fafbf9;
}

.empty-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 84px;
    height: 84px;
    margin-bottom: 20px;
    border: 1px solid #e5eee2;
    border-radius: 18px;
    background-color: #eef5eb;
    color: var(--color-primary);
}

.empty-preview h3 {
    margin: 0;
    color: #343a35;
    font-size: 17px;
    font-weight: 650;
}

.empty-preview p {
    max-width: 300px;
    margin: 10px 0 0;
    color: #858b85;
    font-size: 13px;
    line-height: 1.7;
}

/* Focus clavier */
.cv-item:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
}

/* Responsive */
@media (max-width: 850px) {
    .cv-container {
        grid-template-columns: 1fr;
        gap: 16px;
        padding: 12px;
    }

    .cv-list {
        max-height: 400px;
    }

    .cv-preview {
        min-height: 500px;
    }

    .empty-preview {
        min-height: 350px;
    }
}
</style>