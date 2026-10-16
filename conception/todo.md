# Todo

## Système RH

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
  - **Lors du recrutement**:
    - Page pour upload CV ou formulaire de CV en ligne avec preview du rendu
    - checking du numéro du CIN du candidat
    - système email si possible pour le résultat du recrutement pour un future interview, et création contrat

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
