# Todo

## Module RH

### Création rôle

- **Manager**: qui envoie le besoin avec un departement spécifique
- **RH**
- **Finance**
- **Direction**

### Demande de manifestion par le manager

- **View**:
  - Champ:
    - departement du manager`*`
    - poste demandé`*`
    - nombre de personnes`*`
    - type de besoin`*`
    - motif détaillé
    - date souhaité d'arrivé`*`
    - lieu de travail
    - date de la manifestion`*`
    - Experience requis
  - Après validation:
    - on envoie une notification au RH pour le notifier qu'il y a une nouvelle demande **après insertion dans la base de donnée**

### RH

- **RM**: le refus par tous les autres rôle refuse automatiquement la demande
- **View**:
  - **Section notification**:
    - liste des demandes(avec option validé, en attente, refusé)
    - Salaire estimé`*` pour une demande spécifique
  - **Dahsboard**:
    - Effectif par poste par departement / effectif maximum supporté
    - Effectif totaux par departement / effectif maximum supporté
    - Liste des demandes (détaillé)
  - **Page d'offre vu par le public**: 
    - Page d'offre
  - **Lors du recrutement**:
    - Page pour upload CV ou formulaire de CV en ligne avec preview du rendu
    - checking du numéro du CIN du candidat
    - système email si possible pour le résultat du recrutement pour un future interview, et création contrat
  - **Liste des CV**:
    - charger le blob envoyé par spring boot
    - parser en object url avec vue:

```js
  <template>
  <div class="pdf-section">
    <!-- On n'affiche l'iframe que lorsque l'URL du PDF est prête -->
    <iframe 
      v-if="pdfUrl" 
      :src="pdfUrl" 
      width="100%" 
      height="700px" 
      style="border: none;"
    ></iframe>
    <p v-else-if="loading">Chargement du PDF...</p>
    <p v-else>Erreur lors du chargement du document.</p>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import axios from 'axios' // ou fetch selon vos habitudes

const pdfUrl = ref(null)
const loading = ref(true)

onMounted(async () => {
  try {
    // TRÈS IMPORTANT : Préciser responseType: 'blob'
    const response = await axios.get('/api/pdf', { responseType: 'blob' })
    
    // Création d'une URL de blob locale utilisable par l'iframe
    pdfUrl.value = URL.createObjectURL(response.data)
  } catch (error) {
    console.error("Impossible de charger le PDF", error)
  } finally {
    loading.value = false
  }
})

// Nettoyage de la mémoire quand le composant est détruit
onUnmounted(() => {
  if (pdfUrl.value) {
    URL.revokeObjectURL(pdfUrl.value)
  }
})
</script>

```

### Finance

- **View**:
  - Définition grille salariale par poste
  - liste demande avec le montant défini par RH avec bouton valider ou refuser selon son analyse en fonction du budget
  - Budget annuel calculé par rapport au salaire de chaque employé, et des dépenses interne de l'entreprise

### Direction

- **View**
  - Validation de la demande puis renvoie vers RH pour qu'il confirme la demande pour un recrutement

**RM**:

- le rôle direction assure la validation finale de la demande.
- si le rôle finance refuse la demande alors RH refuse automatiquement
- l'état (enable ou disable) des boutons valider dependra de l'état de validation de la demande(qui a validé)
- chaque validation attribuera un code au statut de la demande pour la validation des autres rôle
- les pages vu par le client et par les rôles seront attribué à deux routes différentes(ex: /si/client pour le client et pour l'application de gestion /si/app)

### Conception table

- **role**:
  - id
  - libelle

- **utilisateur**:
  - id
  - id_role
  - nom
  - mdp

- **etat_validation**:
  - id
  - numero_validation
  - (état)

- **departement**:
  - id
  - nom
  - effectif total
  - (effect max)
  - id_utilisateur(manager qui gère le département)

- **poste**
  - id
  - nom
  - id_departement
  - id_grille_salaire

- **grille_salaire**:
  - id
  - valeur_min
  - valeur_max

- **type_besoin**:
  - id
  - libelle

- **besoin**:
  - id
  - id_type_besoin
  - effectif
  - date d'arrivé souhaité
  - date_demande
  - id_poste
  - motif
  - experience_requis

- **rel_besoin_etat**
  - id
  - id_besoin
  - id_etat
  - date
  - remarque(facultatif)

- **CV**
  - id
  - details
  - data(blob)
  - date
  - status(lue, non lue)

- **Budget_departemet**
  - id
  - montant
  - id_departement

- **Dépense_departement**
  - id
  - montant
  - id_departement
