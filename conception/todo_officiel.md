## 👥 Répartition des Tâches pour 4 Personnes

### 🛠️ Membre 1 : Architecture Backend, Base de Données, Gestion des Fichiers (PDF/BLOB) & Notifications

*Objectif : Mettre en place le socle technique serveur, la gestion des types MIME/BYTEA, les flux de stockage et de restitution des CVs et contrats.*

* [ ] **Base de Données et Sécurité**
* [ ] Finaliser l'exécution et l'ajustement du script SQL initial (`init_table.sql`) (correction de la fin du script avec la table `notification` et les contraintes manquantes).


* [ ] Mettre en place la table et la logique de gestion des rôles (`role`, `utilisateur`) et l'authentification (Spring Security / JWT ou sessions).




* [ ] **Gestion des Fichiers Binaires (BLOB)**
* [ ] Développer le service Spring Boot pour l'upload de fichiers PDF (`cv`, `contrat`) et leur conversion en format binaire (`BYTEA`) dans PostgreSQL.


* [ ] Créer l'endpoint de récupération/déconversion des binaires en flux de données pour le rendu dans le composant Vue.js (`/api/pdf/...`).




* [ ] **Système de Notification Serveur**
* [ ] Implémenter la logique métier pour générer automatiquement une entrée dans la table `notification` lors de l'insertion d'un nouveau besoin par un manager.


* [ ] Créer les routes API pour récupérer et marquer les notifications comme lues.



---

### 👔 Membre 2 : Module Manager & Page Publique des Offres (Côté Client)

*Objectif : Permettre aux managers de soumettre leurs besoins par département et de gérer leur vision interne, tout en gérant l'interface publique des offres d'emploi.*

* [ ] **Espace Manager (Création de Demande de Besoin)**
* [ ] Créer la vue du formulaire de manifestation de besoin (champs : département, poste, nombre de personnes, type de besoin, motif détaillé, date souhaitée d'arrivée, lieu de travail, date de la manifestation, expérience requise).


* [ ] Implémenter le contrôle de saisie (champs obligatoires `*`) et l'appel API pour l'insertion en base de données.


* [ ] Développer la page de vision globale du département pour le manager (effectifs, postes, état des demandes).


* [ ] **Page d'Offre Vue par le Public (`/si/client`)**
* [ ] Concevoir la page d'accueil publique listant les offres d'emploi actives (caractéristiques du poste, description).
* [ ] Intégrer le bouton "Postuler" ouvrant un formulaire en ligne.
* [ ] Implémenter le système de vérification du numéro de CIN du candidat et l'upload du CV.





---

### 🧑‍💼 Membre 3 : Module Ressources Humaines (RH) & Processus de Recrutement

*Objectif : Centraliser la gestion administrative des demandes, le suivi des recrutements, l'affichage des CVs et la communication avec les candidats.*

* [ ] **Tableau de Bord et Suivi des Demandes (RH)**
* [ ] Créer la section de notification listant les demandes avec statuts (validé, en attente, refusé).


* [ ] Intégrer la saisie du salaire estimé pour chaque demande spécifique.


* [ ] Développer le dashboard RH : effectif par poste par département / effectif maximum supporté, effectifs totaux.




* [ ] **Visualisation des CVs (Intégration Vue.js)**
* [ ] Intégrer le composant Vue.js basé sur `iframe` et `URL.createObjectURL(response.data)` avec le type de réponse `blob` pour charger et prévisualiser les CVs stockés en base.




* [ ] **Gestion des Entretiens et Contrats**
* [ ] Mettre en place le système d'envoi d'e-mails automatisés pour les convocations d'entretiens et la notification des résultats.


* [ ] Gérer la transition post-recrutement vers la création du contrat et de l'employé dans la base (`employe`, `contrat`).





---

### 💰 Membre 4 : Module Finance & Direction (Validation Finale)

*Objectif : Maîtriser le budget global de l'entreprise, valider les grilles salariales et assurer la décision finale sur les recrutements.*

* [ ] **Module Finance**
* [ ] Développer l'interface de définition de la grille salariale par poste (`grille_salaire`).


* [ ] Créer la vue de liste des demandes avec le montant défini par les RH, incluant les boutons "Valider" ou "Refuser" basés sur l'analyse budgétaire.


* [ ] Implémenter le calcul automatique du budget annuel de l'entreprise (basé sur le salaire des employés de la table `contrat`/`employe` et des dépenses internes de la table `depense_departement`).




* [ ] **Module Direction & Règles Métier Globales**
* [ ] Développer l'interface de validation finale de la direction (`validation_besoin`).


* [ ] Implémenter la règle métier de cascade des refus : *si le rôle Finance refuse la demande, le RH/Direction doit la refuser automatiquement*.


* [ ] Gérer l'état dynamique (`enable`/`disable`) des boutons de validation selon le workflow séquentiel et les codes de statut attribués.


* [ ] Configurer la séparation stricte des routes de l'application (`/si/client` pour le public et `/si/app` pour l'application de gestion interne).