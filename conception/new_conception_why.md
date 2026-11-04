# WHY — Script SIG RH, expliqué par rapport aux blocs 5, 6 et 7

Ce document relie chaque objet du fichier `SIG_RH_schema_complet_corrige.sql` (tables, contraintes, index, fonctions, triggers, vues) à l'exigence du cours qu'il satisfait. Pour chaque exigence : **ce qui est demandé**, **les objets SQL qui y répondent**, **leur rôle**.

> Le script est une **extension** du schéma de base (v3). Il suppose l'existence de `employe`, `utilisateur`, `role`, `departement`, `poste`, `contrat`, `statut_contract`, `document_employe`, `notification`, `tache_onboarding`, etc.
> Les tables `permission` et `role_permission` n'existent pas : les **autorisations d'accès sont gérées dans le backend** (Spring Security).

---

## 0. Remarque transversale : supérieur et subordonnés dans l'organigramme

> *« Pour chaque salarié, indiquer son supérieur hiérarchique ainsi que les personnes sous sa responsabilité. »*

| Objet | Type | Rôle |
|---|---|---|
| `historique_affectation` | Table | Une ligne = une période où un employé occupe un poste dans un département **sous un supérieur** (`id_superieur`). Le supérieur actuel est la ligne où `date_fin IS NULL`. Les subordonnés d'un employé sont les lignes en cours où il est `id_superieur`. |
| `chk_superieur_different` | Contrainte | Personne ne peut être son propre supérieur. |
| `uq_affectation_courante` | Index unique partiel | Un seul supérieur courant par employé (une seule affectation en cours). |
| `idx_affectation_superieur` | Index partiel | Retrouve vite les subordonnés directs d'un supérieur. |
| `verifier_hierarchie` + `trg_verifier_hierarchie` | Fonction + trigger | Le supérieur doit être encore en poste ; interdit les **boucles** (A supérieur de B, B de C, C de A) en remontant la chaîne (50 niveaux max). |
| `manager_employe_du_departement` | Fonction | Trouve le manager d'un département : supérieur par défaut d'un nouvel arrivant ou d'un muté. |
| `v_organigramme` | Vue | Pour chaque salarié non parti : département, poste, **supérieur** (nom, prénom) et **nombre de subordonnés directs**. C'est la réponse directe à la remarque. |
| `lister_subordonnes(id, directs_seulement)` | Fonction | Liste les personnes **sous la responsabilité** d'un salarié, directes seulement ou sur toute la descendance (20 niveaux), avec le niveau. |
| `lister_superieurs(id)` | Fonction | Liste toute la **chaîne de supérieurs** d'un salarié jusqu'au sommet. |

---

## 1. Bloc 5 — Dossier administratif du salarié

*« Gestion des informations administratives et professionnelles : suivi de la carrière du salarié. Le dossier salarié devient le référentiel central du SIRH. »*

### 1.1 Centraliser identité, coordonnées, documents et situation professionnelle ; référentiel central

`employe` (table de base) est le point d'ancrage : presque toutes les tables de l'extension pointent vers `employe(id)`. L'extension ajoute la fabrication automatique du dossier complet :

| Objet | Type | Rôle |
|---|---|---|
| `initialiser_dossier_employe` | Fonction | À partir d'un employé, crée d'un coup son **affectation initiale** (motif « Embauche »), son **droit au congé annuel** (2,5 j/mois, plafond 90) et son **planning de travail standard** temps plein. Gère aussi un employé déjà parti. |
| `trg_employe_apres_insert` + `trg_employe_dossier_initial` | Fonction + trigger | Lance cette initialisation **automatiquement** à chaque nouvel employé : un dossier n'est jamais « vide ». |
| `SELECT initialiser_dossier_employe(e) …` (fin du script) | Requête de rattrapage | Construit le dossier des employés **déjà existants** sans historique. |
| `chk_employe_statut_depart` | Contrainte | `PARTI` ⇔ date de départ renseignée ; `ACTIF`/`SUSPENDU` ⇒ pas de date de départ. Situation professionnelle toujours cohérente. |

### 1.2 Conserver contrats, affectations, salaire et historique de carrière

| Objet | Type | Rôle |
|---|---|---|
| `historique_affectation` | Table | **Historique de carrière** : chaque changement de poste, département ou supérieur est une nouvelle ligne datée avec un `motif`. On ne perd jamais l'ancienne affectation. |
| `fk_affectation_poste_dept` | Clé étrangère composite | Le poste choisi doit réellement appartenir au département. (Prérequis : `UNIQUE (id, id_departement)` sur `poste`.) |
| `chk_affectation_dates` | Contrainte | Date de fin ≥ date de début. |
| `ex_affectation_periode` | Contrainte d'exclusion (`btree_gist`) | Deux périodes d'affectation d'un même employé ne peuvent **pas se chevaucher** (intervalle `[)` : changement possible le jour même). |
| `idx_affectation_employe_dates` | Index | Lecture rapide de la carrière d'un employé, du plus récent au plus ancien. |
| `verifier_contrat_actif` + `trg_verifier_contrat_actif` | Fonction + trigger | Un employé ne peut avoir qu'**un seul contrat ACTIF** ; verrouille la ligne employé pour éviter deux contrats créés en même temps. |
| `calculer_indemnite_conge` | Fonction | Lit le **salaire** du contrat le plus pertinent à une date (voir bloc 7, §4). |

### 1.3 Gestion des affectations : changements de poste ou d'affectation

| Objet | Type | Rôle |
|---|---|---|
| `ouvrir_affectation` | Fonction | Brique de base : **clôture** l'affectation en cours (sans jamais mettre une fin avant le début) puis **ouvre** la nouvelle. |
| `changer_affectation(employé, département, poste, supérieur, date, motif)` | Fonction | Fonction métier à appeler pour muter quelqu'un : refuse un employé parti, choisit le supérieur (celui fourni ; sinon le même si le département ne change pas ; sinon le manager du nouveau département), met à jour `employe` et ouvre l'affectation. Le paramètre de session `app.affectation_en_cours` évite le doublon avec le trigger ci-dessous. |
| `trg_employe_suivre_changements` + `trg_employe_changements` | Fonction + trigger | Filet de sécurité : si quelqu'un modifie **directement** département/poste sur la fiche employé, une nouvelle affectation est ouverte (« Changement direct sur la fiche employé »). Si l'employé passe à `PARTI`, ferme à la date de départ l'affectation, les droits de congé et le planning. |

### 1.4 Gérer les documents administratifs et leur validité ; alertes de renouvellement

> *« Pour chaque document déposé, alerter de son renouvellement : directement le salarié s'il a un espace personnel, sinon les RH. »*

| Objet | Type | Rôle |
|---|---|---|
| `document_employe.date_expiration` | Colonne ajoutée | Date de fin de validité du document. |
| `document_employe.date_renouvellement` | Colonne ajoutée | Renseignée quand le document est renouvelé : l'alerte s'arrête. |
| `document_employe.delai_alerte_jours` (défaut 30) + `chk_document_delai_alerte` | Colonne + contrainte | Nombre de jours **avant** expiration où l'alerte commence ; ne peut pas être négatif. |
| `idx_document_expiration` | Index partiel | Ne couvre que les documents qui expirent et ne sont pas renouvelés : exactement ce que lit l'alerte. |
| `notification.id_document_employe` (FK, `ON DELETE SET NULL`) + `idx_notification_document` | Colonne + index | Lie une notification à son document, ce qui permet d'éviter les doublons. |
| `generer_alertes_documents(fréquence)` | Fonction | Cherche les documents dont la date d'alerte est atteinte (non renouvelés, non refusés, employé non parti). **Destinataire** : le salarié lui-même s'il a un compte actif (*espace personnel*), **sinon tous les utilisateurs du rôle RH actifs**. Titre « Document bientôt expiré » ou « Document expiré » selon le cas. Anti‑doublon : pas de nouvelle notification pour le même document et le même destinataire dans les `fréquence` derniers jours (7 par défaut). Retourne le nombre créé. À planifier (job quotidien). |
| `chk_notification_lecture` | Contrainte | Une notification ne peut pas être lue avant d'avoir été créée. |

### 1.5 Traçabilité : chaque tâche accomplie doit être entièrement traçable

| Objet | Type | Rôle |
|---|---|---|
| `chk_tache_realisation` | Contrainte | Une tâche d'onboarding `FAIT` doit avoir une date de réalisation ; sinon elle n'en a pas. |
| `historique_tache_onboarding` | Table | Pour chaque modification d'une tâche : **ancien/nouveau statut, échéance, remarque, auteur, date**. |
| `tracer_modification_tache_onboarding` + `trg_historique_tache_onboarding` | Fonction + trigger | Après chaque `UPDATE` de `tache_onboarding`, écrit une ligne **seulement si** statut, échéance ou remarque ont réellement changé. L'auteur vient de `app.user_id` (posé par le backend). |
| `idx_historique_tache_date` | Index | Historique d'une tâche, du plus récent au plus ancien. |
| `journal_audit` (voir §1.6) | Table | Trace générale, au-delà des tâches. |

### 1.6 Confidentialité, habilitations et journal d'audit

| Objet | Type | Rôle |
|---|---|---|
| `journal_audit` | Table | Qui (`id_utilisateur`), quoi (`action`), sur quelle table et quel enregistrement, **valeurs avant/après** (JSONB), adresse IP, date. |
| `idx_audit_utilisateur_date`, `idx_audit_table_enregistrement` | Index | Rechercher par utilisateur, ou l'historique d'un enregistrement précis. |
| `auditer_modification` | Fonction trigger générique | Enregistre chaque `INSERT`/`UPDATE`/`DELETE`. **Confidentialité** : retire du journal les champs sensibles ou lourds (`bytes`, `contenu`, `mdp`). Lit `app.user_id` et `app.client_ip`. |
| Bloc `DO $$ … $$` → `trg_audit_<table>` | Création en boucle | Pose le trigger d'audit sur 18 tables sensibles : utilisateur, employe, contrat, document_employe, historique_affectation, tache_onboarding, besoin, validation_besoin, candidature, decision_recrutement, demande_conge, validation_conge, solde_conge, absence, mission, remplacement_conge, temps_travail_journalier, indemnite_conge. |
| `interdire_modification_journal` + `trg_journal_audit_lecture_seule` | Fonction + trigger | Le journal est **en lecture seule** : aucune modification ni suppression, même par un administrateur applicatif. |

**Habilitations** : le cours demande de les « prévoir ». Choix retenu : elles sont **entièrement gérées dans le backend** (rôles, droits par API), donc il n'y a pas de tables `permission` / `role_permission`. La base ne garde que les règles métier de workflow (ex. qui peut valider un congé, voir bloc 7).

---

## 2. Bloc 6 — Temps de travail et pointage

### 2.1 Définir horaires, planning et régime de travail

| Objet | Type | Rôle |
|---|---|---|
| `regime_travail` | Table | Régime (temps plein, temps partiel) et heures hebdomadaires (> 0). |
| `horaire_travail` | Table | Heures de début/fin, pause, **tolérance de retard**, jours travaillés (tableau ISO 1 à 7), actif ou non. Contrôles : fin > début ; pause complète et incluse dans la journée ; jours valides. |
| `jour_ferie` | Table | Jours fériés (clé = la date), exclus du temps prévu et du décompte des congés. |
| `planning_travail` | Table | Quel horaire et quel régime pour quel employé, sur quelle période. |
| `ex_planning_periode` | Contrainte d'exclusion | Un employé ne peut pas avoir deux plannings qui se chevauchent. |
| `idx_planning_employe_dates` | Index | Retrouver le planning applicable à une date. |
| Données initiales | `INSERT` | Régimes `TEMPS_PLEIN` (40 h) et `TEMPS_PARTIEL` (20 h) ; horaire `STANDARD` 08h‑17h, pause 12h‑13h, tolérance 10 min, lundi‑vendredi ; jours fériés fixes de Madagascar 2026‑2030. |

### 2.2 Enregistrer entrées, sorties, retards et heures supplémentaires

| Objet | Type | Rôle |
|---|---|---|
| `pointage` | Table | Événements `ENTREE`, `SORTIE`, `DEBUT_PAUSE`, `FIN_PAUSE` avec source (`MANUEL` par défaut) et remarque. |
| `UNIQUE (id_employe, date_heure, type_pointage)` | Contrainte | Empêche les doublons de pointage identique. |
| `idx_pointage_employe_date` | Index | Pointages récents d'un employé. |
| `temps_travail_journalier` | Table | **Résultat calculé par jour** : temps prévu, temps réel, **retard** (minutes), **heures supplémentaires** (minutes) et leur statut (`AUCUNE`, `A_VALIDER`, `VALIDEE`, `REFUSEE`). Unique par (employé, date). |
| `CHECK (statut sup VALIDEE/REFUSEE) = (validateur non nul)` | Contrainte | Une heure sup validée ou refusée a toujours un validateur. |
| `idx_temps_journalier_date` | Index | Calculs et rapports par date. |

### 2.3 Calculer le temps réellement travaillé ; gestion des retards et heures supplémentaires

| Objet | Type | Rôle |
|---|---|---|
| `calculer_temps_journalier(employé, date)` | Fonction | **Cœur du bloc.** Trouve l'horaire du jour ; décide si le jour est hors service (absence, mission, jour férié) ; calcule le **temps prévu** (durée − pause) ; parcourt les pointages du jour (fuseau `Indian/Antananarivo`) et cumule le **temps réel travaillé** (entrée→pause, reprise→sortie) ; calcule le **retard** = première entrée − heure de début − tolérance (jamais négatif) ; calcule les **heures supplémentaires** = réel − prévu (jamais négatif). Si une heure sup a déjà été validée/refusée pour le même volume, la décision est conservée ; sinon le statut repasse à `A_VALIDER`. Enregistre ou met à jour la ligne du jour. |
| `calculer_temps_periode(début, fin)` | Fonction | Lance le calcul sur une période pour **tous les employés actifs** depuis leur embauche (job de nuit). |
| `valider_heures_supplementaires(id, utilisateur, décision)` | Fonction | **Décompte contrôlé** des heures sup : accepte `VALIDEE` ou `REFUSEE` uniquement sur une ligne `A_VALIDER`, et enregistre validateur et date. |

### 2.4 Détecter les anomalies de pointage

| Objet | Type | Rôle |
|---|---|---|
| `anomalie_pointage` | Table | Anomalie par employé et jour, avec type, description, statut (`A_TRAITER`, `EN_COURS`, `RESOLUE`, `IGNOREE`), justification, responsable. Unique par (employé, date, type). |
| `idx_anomalie_employe_statut` | Index | Liste des anomalies à traiter d'un employé. |
| Détection dans `calculer_temps_journalier` | Logique | Crée : `RETARD` ; `JOUR_NON_POINTE` (jour travaillé sans aucun pointage, ni absence ni mission) ; `SORTIE_MANQUANTE` (journée qui ne finit pas par une sortie) ; `SEQUENCE_INVALIDE` (ordre incohérent, ex. sortie sans entrée). Les deux anomalies de journée incomplète ne sont créées que pour les **jours passés**. `ON CONFLICT DO NOTHING` évite de les recréer à chaque recalcul. |

### 2.5 Les données de temps alimentent les absences et les variables de paie

| Objet | Type | Rôle |
|---|---|---|
| `v_variables_paie_mensuelles` | Vue | Par employé et par mois : minutes prévues, travaillées, de retard, nombre de retards, heures sup **validées** (à payer) et **à valider**. Prête pour la paie. |
| Lien avec les absences | Logique | `calculer_temps_journalier` met le temps prévu à 0 les jours d'absence, de mission ou férié : une absence n'est donc jamais comptée comme un manquement. |

---

## 3. Bloc 7 — Congés et absences

### 3.1 Règles d'acquisition et plafonds

> *Acquisition : 2,5 jours/mois (30 jours/an). Plafond du solde : 90 jours. Les missions ne sont pas déduites du solde.*

| Objet | Type | Rôle |
|---|---|---|
| `type_conge` | Table | Types de congé et leurs règles : `deduit_solde`, `justificatif_obligatoire`, `remplacant_obligatoire`. Données : `ANNUEL` (déduit du solde), `MALADIE`, `EXCEPTIONNEL`, `SANS_SOLDE` (non déduits). |
| `droit_conge` | Table | Droit d'un employé à un type : `acquisition_mensuelle` (défaut **2,5** posé par `initialiser_dossier_employe`), `plafond_jours` (≤ **90** par contrainte), période de validité. |
| `solde_conge` | Table | Solde par employé, type et année. `jours_disponibles` est une **colonne générée** = acquis + ajustement − pris. |
| `CHECK (acquis + ajustement ≤ 90)` et `CHECK (disponible ≥ 0)` | Contraintes | **Le plafond de 90 jours** et l'impossibilité d'un solde négatif sont garantis par la base. |
| `mouvement_solde_conge` | Table | Journal de chaque mouvement (`ACQUISITION`, `PRISE`, `AJUSTEMENT`, `ANNULATION`) avec référence externe. L'unicité (solde, type, référence) rend les opérations **idempotentes** : rejouer une acquisition ne double pas les jours. |
| `acquerir_conges_mensuels(année, mois)` | Fonction | À lancer chaque mois : pour les droits actifs des employés actifs, ajoute l'acquisition mensuelle **sans dépasser le plafond** (le total disponible, toutes années, est limité à 90). Crée le solde de l'année au besoin, journalise le mouvement (`ACQUISITION:AAAA-MM`). |
| `mission` | Table | Missions professionnelles (référence unique, lieu, objet, dates, statut). Une mission **n'a aucun lien avec le solde** : aucun mouvement de solde n'est créé, donc les périodes de mission ne sont **jamais déduites** du solde de congés. |
| `ex_mission_periode` | Contrainte d'exclusion | Pas deux missions actives qui se chevauchent pour un même employé. |
| `verifier_mission_absence` + `trg_verifier_mission_absence` | Fonction + trigger | Une mission active ne peut pas chevaucher une absence : un salarié ne peut être en congé et en mission en même temps. |

### 3.2 Planning et organisation : suivi des départs et désignation systématique des remplaçants

| Objet | Type | Rôle |
|---|---|---|
| `absence` | Table | **Planning des départs et absences** : type (`CONGE`, `MALADIE`, `AUTORISEE`, `INJUSTIFIEE`, `AUTRE`), période, jours, statut. Lien unique avec une demande de congé ; `type_absence = 'CONGE'` ⇔ demande liée. |
| `ex_absence_periode` | Contrainte d'exclusion | Pas d'absences qui se chevauchent (sauf annulées). |
| `idx_absence_employe_dates`, `idx_mission_employe_dates` | Index | Lire le planning d'un employé sur une période. |
| `justificatif_absence` | Table | Pièces justificatives (fichier `BYTEA`) d'une absence, avec statut de vérification (`A_VERIFIER`, `VALIDE`, `REFUSE`). Utile quand `type_conge.justificatif_obligatoire` l'exige. |
| `remplacement_conge` | Table | **Remplaçant désigné** pour une demande de congé, avec ses responsabilités et son statut (`PROPOSE`, `ACCEPTE`, `REFUSE`, `ANNULE`). Unique par (demande, remplaçant). |
| `idx_remplacement_demande` | Index | Retrouver les remplaçants d'une demande. |
| `verifier_remplacement` + `trg_verifier_remplacement` | Fonction + trigger | Le remplaçant doit être **différent** du demandeur, **actif**, et **lui-même présent** sur la période. |
| Désignation « systématique » | Règle | `confirmer_demande_conge` refuse de valider si le type exige un remplaçant (`remplacant_obligatoire`) et qu'aucun n'a **accepté**. |

### 3.3 Workflow de demande et contrôle du solde

| Objet | Type | Rôle |
|---|---|---|
| `demande_conge` | Table | Demande avec référence unique et statut du circuit : `EN_ATTENTE_MANAGER` → `EN_ATTENTE_RH` → `ACCEPTEE`, ou `REFUSEE` / `ANNULEE`. |
| `ex_demande_periode` | Contrainte d'exclusion | Deux demandes actives du même employé ne peuvent pas se chevaucher. |
| `validation_conge` | Table | Une ligne par étape (`MANAGER`, `RH`), unique par demande et étape. Le `CHECK` impose : en attente ⇒ ni validateur ni date ; accepté/refusé ⇒ les deux. |
| `idx_demande_conge_employe_statut`, `idx_validation_conge_demande` | Index | Demandes par employé/statut ; étapes d'une demande. |
| `calculer_jours_conge(employé, début, fin)` | Fonction | Compte les **jours réellement décomptés** : jours travaillés selon le planning de l'employé (défaut lundi‑vendredi), jours fériés exclus. |
| `preparer_demande_conge` + `trg_preparer_demande_conge` | Fonction + trigger | **Vérification automatique du solde.** À l'insertion : calcule `jours_demandes`, refuse une période sans jour travaillé, vérifie le type de congé, et compare au solde **disponible − jours déjà réservés** par les demandes en attente. Message d'erreur détaillé si insuffisant. |
| `initialiser_validations_conge` + `trg_initialiser_validations_conge` | Fonction + trigger | Crée automatiquement les deux étapes `MANAGER` et `RH` pour chaque nouvelle demande. |
| `utilisateur_est_manager_de(utilisateur, employé)` | Fonction | Dit si l'utilisateur peut valider **à l'étape manager** : supérieur direct (via l'organigramme), manager du département, ou rôle `DIRECTION` en dernier recours pour un employé sans supérieur ; jamais pour sa propre demande. |
| `decider_validation_conge` | Fonction | **Moteur du circuit.** Vérifie le statut de la demande et l'habilitation de l'étape (manager via la fonction ci-dessus ; RH = utilisateur du rôle RH actif, différent du demandeur). Enregistre la décision. Un refus clôture la demande ; l'acceptation du manager envoie vers le RH ; l'acceptation du RH déclenche la confirmation. |
| `confirmer_demande_conge` | Fonction | Dernière étape : vérifie les deux acceptations, le remplaçant si exigé, l'absence de chevauchement (absences, missions) et **re-vérifie le solde**. Débite les soldes **de l'année la plus ancienne à la plus récente**, journalise chaque `PRISE`, crée l'`absence` et passe la demande à `ACCEPTEE`. |
| `annuler_demande_conge` | Fonction | Annulation par l'employé ou le RH. Si le congé était accepté et pas commencé : **restitue les jours** (mouvement `ANNULATION`) et annule l'absence. Un congé déjà commencé ne peut plus être annulé. |

### 3.4 Indemnisation et départ

> *Valeur du droit au congé = (salaire brut × nombre de jours de congé non pris) / 30*

| Objet | Type | Rôle |
|---|---|---|
| `indemnite_conge` | Table | Mémorise un calcul : employé, **contrat utilisé**, date de référence, salaire brut, jours non pris, montant, auteur, date. |
| `idx_indemnite_employe` | Index | Historique des indemnités d'un employé. |
| `calculer_indemnite_conge(employé, date, enregistrer, utilisateur)` | Fonction | Date de référence par défaut = **date de départ** de l'employé (sinon aujourd'hui). Choisit le contrat avec salaire le plus pertinent à cette date. **Jours non pris** = somme des jours disponibles des types qui déduisent du solde. **Montant = `ROUND(salaire × jours / 30, 2)`**, soit exactement la formule demandée. Peut enregistrer le résultat. |
| `trg_employe_suivre_changements` (statut `PARTI`) | Trigger | Au départ, ferme les droits de congé : plus d'acquisition après la date de départ, le solde restant sert au calcul de l'indemnité. |

---

## 4. Fiabilisation du socle (transverse)

Ces objets ne répondent pas à un bloc précis, mais sécurisent les données des blocs précédents.

| Objet | Type | Rôle |
|---|---|---|
| `btree_gist` | Extension | Nécessaire à toutes les contraintes `EXCLUDE USING gist` (chevauchements de périodes). |
| `chk_decision_decideur` | Contrainte | Dans `decision_recrutement`, toute décision hors `EN_ATTENTE` doit avoir un décideur. |
| `verifier_validation_besoin` + `trg_verifier_validation_besoin` | Fonction + trigger | Une validation de besoin `EN_ATTENTE` n'a ni validateur ni date ; `ACCEPTE`/`REFUSE` a les deux. |
| `verifier_publication_candidature` + `trg_verifier_publication_candidature` | Fonction + trigger | La publication choisie pour une candidature doit appartenir au même recrutement. |
| `NOT VALID` puis `VALIDATE CONSTRAINT` (4 contraintes) | Technique | Les contraintes sont posées sans bloquer, puis validées en fin de script sur les données existantes, après la reconstruction des dossiers. |
| `BEGIN … COMMIT` | Transaction | Tout le script est appliqué ou rien. |

---

## 5. Tableau récapitulatif : exigence → mécanisme

| Exigence | Mécanisme principal |
|---|---|
| Supérieur et subordonnés dans l'organigramme | `historique_affectation`, `v_organigramme`, `lister_subordonnes`, `lister_superieurs`, `verifier_hierarchie` |
| Alerte de renouvellement de document (salarié ou RH) | colonnes d'expiration, `generer_alertes_documents`, `notification.id_document_employe` |
| Tâche entièrement traçable | `historique_tache_onboarding`, `trg_historique_tache_onboarding`, `journal_audit` |
| Suivi des changements d'affectation | `changer_affectation`, `ouvrir_affectation`, `trg_employe_changements` |
| Retards | tolérance dans `horaire_travail`, `retard_minutes`, anomalie `RETARD` |
| Heures supplémentaires | `heures_supplementaires_minutes`, `valider_heures_supplementaires`, `v_variables_paie_mensuelles` |
| Temps réellement travaillé, anomalies | `calculer_temps_journalier`, `calculer_temps_periode`, `anomalie_pointage` |
| Acquisition 2,5 j/mois, plafond 90 j | `droit_conge`, `acquerir_conges_mensuels`, contraintes de `solde_conge` |
| Missions non déduites du solde | `mission` sans mouvement de solde, `verifier_mission_absence` |
| Planning des départs et remplaçants | `absence`, `remplacement_conge`, `verifier_remplacement`, contrôle dans `confirmer_demande_conge` |
| Circuit de validation | `demande_conge`, `validation_conge`, `decider_validation_conge`, `utilisateur_est_manager_de` |
| Vérification du solde | `preparer_demande_conge` (à la demande), `confirmer_demande_conge` (à la validation) |
| Indemnité de congé non pris | `calculer_indemnite_conge`, `indemnite_conge` |
| Confidentialité, audit | `journal_audit`, `auditer_modification`, journal en lecture seule |

---

## 6. Ce qui relève du backend et points d'attention

- **Habilitations** : gérées par Spring Security. La base n'en contient aucune table.
- **Contexte d'audit** : le backend doit exécuter, au début de chaque transaction, `SET LOCAL app.user_id = '…'` et `SET LOCAL app.client_ip = '…'`. Sans cela, l'audit et l'historique des tâches n'ont pas d'auteur.
- **Jobs à planifier** : `calculer_temps_periode` (nuit), `acquerir_conges_mensuels` (chaque mois), `generer_alertes_documents` (chaque jour).
- **Planning des départs** : il n'existe pas de vue dédiée ; il se lit dans `absence` + `remplacement_conge`. Une vue `v_planning_departs` serait un ajout simple si le cours l'exige explicitement.
- **Alertes** : elles ne concernent que les documents ayant une `date_expiration`. Un document déposé sans date d'expiration ne génère pas d'alerte.
- **Prérequis du schéma de base** : `UNIQUE (id, id_departement)` sur `poste` ; nom de la colonne mot de passe (`mdp`) à vérifier dans `auditer_modification`.


