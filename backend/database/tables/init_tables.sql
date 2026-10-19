
-- ============================================================
-- PROJET : SYSTEME D'INFORMATION DE GESTION RH
-- SGBD   : PostgreSQL
-- FICHIER: init_table.sql
-- ============================================================
--
-- OBJECTIFS :
--   1. Gérer les utilisateurs, les rôles et les départements.
--   2. Gérer les besoins de recrutement et leurs validations.
--   3. Gérer les candidats, les candidatures et les CV.
--   4. Gérer les employés, les contrats et leur historique.
--   5. Gérer les grilles salariales, les budgets et les dépenses.
--   6. Gérer les notifications internes.
--
-- ATTENTION :
-- Ce script supprime les tables existantes du même nom.
-- Il est destiné à l'initialisation d'une base de développement.
-- Ne pas exécuter tel quel sur une base de production contenant
-- des données à conserver.
--
-- Les commentaires "MODIFICATION" signalent les changements
-- apportés par rapport au script initial.
--
-- Les commentaires "ROLE" décrivent la responsabilité métier
-- de chaque table avec un exemple.
-- ============================================================


-- ============================================================
-- 1. SUPPRESSION DES TABLES EXISTANTES
-- ============================================================
--
-- MODIFICATION :
-- Les tables dépendantes sont supprimées AVANT les tables
-- auxquelles elles font référence avec leurs clés étrangères.
-- Cela évite les erreurs de dépendances pendant la suppression.
-- ============================================================

DROP TABLE IF EXISTS historique_contrat;
DROP TABLE IF EXISTS contrat;
DROP TABLE IF EXISTS statut_contract;
DROP TABLE IF EXISTS type_contrat;

DROP TABLE IF EXISTS employe;
DROP TABLE IF EXISTS candidature;
DROP TABLE IF EXISTS cv;
DROP TABLE IF EXISTS candidat;

DROP TABLE IF EXISTS decision_recrutement;

DROP TABLE IF EXISTS notification;

DROP TABLE IF EXISTS depense_departement;
DROP TABLE IF EXISTS budget_departement;

DROP TABLE IF EXISTS validation_besoin;
DROP TABLE IF EXISTS decision;
DROP TABLE IF EXISTS etat_validation;

DROP TABLE IF EXISTS besoin;
DROP TABLE IF EXISTS type_besoin;

DROP TABLE IF EXISTS grille_salaire;
DROP TABLE IF EXISTS poste;
DROP TABLE IF EXISTS departement;

DROP TABLE IF EXISTS utilisateur;
DROP TABLE IF EXISTS role;


-- ============================================================
-- 2. TABLE ROLE
-- ============================================================
--
-- ROLE :
-- Définit les catégories d'utilisateurs de l'application.
--
-- EXEMPLES :
--   MANAGER    : peut soumettre un besoin de recrutement.
--   RH         : peut gérer le recrutement et les contrats.
--   FINANCE    : peut gérer les budgets et valider les besoins.
--   DIRECTION  : peut effectuer la validation finale.
--
-- MODIFICATION :
-- Ajout de la colonne "code" pour identifier les rôles de manière
-- stable dans le backend, sans dépendre de leur libellé affiché.
-- ============================================================

CREATE TABLE role (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);


-- ============================================================
-- 3. TABLE UTILISATEUR
-- ============================================================
--
-- ROLE :
-- Contient les comptes des personnes autorisées à utiliser
-- l'application interne.
--
-- EXEMPLE :
-- Un utilisateur nommé "Rakoto Jean" possède le rôle MANAGER
-- et peut se connecter pour demander deux développeurs.
--
-- MODIFICATIONS :
--   - "email" est UNIQUE pour empêcher les comptes en double.
--   - "mdp" passe à VARCHAR(255) pour stocker un hash sécurisé,
--     par exemple un hash BCrypt, et non un mot de passe en clair.
--   - La clé étrangère est déclarée correctement dans CREATE TABLE.
--   - "code" du rôle reste dans la table role, pas ici.
--
-- SECURITE :
-- Le mot de passe doit être haché par le backend avant stockage.
-- Ne jamais enregistrer le mot de passe en clair.
-- ============================================================

CREATE TABLE utilisateur (
    id SERIAL PRIMARY KEY,
    id_role INTEGER NOT NULL,
    nom VARCHAR(50) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    mdp VARCHAR(255) NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_utilisateur_role
        FOREIGN KEY (id_role)
        REFERENCES role(id)
);


-- ============================================================
-- 4. TABLE DEPARTEMENT
-- ============================================================
--
-- ROLE :
-- Représente les unités organisationnelles de l'entreprise.
-- Chaque département possède un responsable et un effectif maximal.
--
-- EXEMPLE :
-- Le département Informatique est dirigé par un utilisateur
-- ayant le rôle MANAGER et peut accueillir au maximum 15 employés.
--
-- MODIFICATIONS :
--   - Correction de la syntaxe de la clé étrangère.
--   - "effectif_total" n'est pas stocké : il doit être calculé
--     à partir des employés actifs pour éviter les incohérences.
--   - "effectif_max" ne peut pas être négatif.
--
-- REGLE METIER :
-- Le backend doit vérifier que le responsable possède bien
-- les autorisations attendues pour être manager.
-- ============================================================

CREATE TABLE departement (
    id SERIAL PRIMARY KEY,
    id_utilisateur_manager INTEGER NOT NULL,
    nom VARCHAR(100) NOT NULL UNIQUE,
    effectif_max INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT chk_departement_effectif_max
        CHECK (effectif_max >= 0),

    CONSTRAINT fk_departement_manager
        FOREIGN KEY (id_utilisateur_manager)
        REFERENCES utilisateur(id)
);


-- ============================================================
-- 5. TABLE POSTE
-- ============================================================
--
-- ROLE :
-- Liste les postes de travail disponibles dans chaque département.
--
-- EXEMPLES :
--   Informatique -> Développeur Java
--   Informatique -> Administrateur réseau
--   Finance      -> Comptable
--
-- MODIFICATION :
-- La relation entre poste et département est conservée.
-- Un poste appartient à un département.
-- ============================================================

CREATE TABLE poste (
    id SERIAL PRIMARY KEY,
    id_departement INTEGER NOT NULL,
    nom VARCHAR(100) NOT NULL,

    CONSTRAINT fk_poste_departement
        FOREIGN KEY (id_departement)
        REFERENCES departement(id),

    CONSTRAINT uq_poste_departement_nom
        UNIQUE (id_departement, nom)
);


-- ============================================================
-- 6. TABLE GRILLE_SALAIRE
-- ============================================================
--
-- ROLE :
-- Définit les fourchettes de rémunération associées aux postes.
-- Plusieurs grilles peuvent exister pour un même poste au fil
-- du temps, selon les dates de validité.
--
-- EXEMPLE :
-- Développeur Java :
--   minimum = 800 000 Ar
--   maximum = 1 500 000 Ar
--
-- MODIFICATIONS :
--   - Correction de la clé étrangère.
--   - Ajout de contraintes de cohérence des montants et des dates.
--   - Conservation de id_poste : un poste peut avoir plusieurs
--     grilles successives.
--
-- REGLE METIER :
-- Le backend doit sélectionner la grille valable à la date
-- concernée et gérer les éventuels chevauchements de périodes.
-- ============================================================

CREATE TABLE grille_salaire (
    id SERIAL PRIMARY KEY,
    id_poste INTEGER NOT NULL,
    valeur_min NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    valeur_max NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    date_debut_validite DATE NOT NULL DEFAULT CURRENT_DATE,
    date_fin_validite DATE,

    CONSTRAINT chk_grille_valeurs
        CHECK (
            valeur_min >= 0
            AND valeur_max >= valeur_min
        ),

    CONSTRAINT chk_grille_dates
        CHECK (
            date_fin_validite IS NULL
            OR date_fin_validite >= date_debut_validite
        ),

    CONSTRAINT fk_grille_salaire_poste
        FOREIGN KEY (id_poste)
        REFERENCES poste(id)
);


-- ============================================================
-- 7. TABLE TYPE_BESOIN
-- ============================================================
--
-- ROLE :
-- Définit les catégories de besoins en recrutement.
--
-- EXEMPLES :
--   REMPLACEMENT : remplacer un employé parti.
--   RENFORT      : augmenter l'effectif d'une équipe.
--   CREATION     : créer un nouveau poste.
--
-- MODIFICATION :
-- La table est conservée. Les codes facilitent les traitements
-- métier et évitent de dépendre uniquement du libellé.
-- ============================================================

CREATE TABLE type_besoin (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);


-- ============================================================
-- 8. TABLE BESOIN
-- ============================================================
--
-- ROLE :
-- Enregistre une demande de recrutement déposée par un manager.
-- Elle constitue le point de départ du processus RH.
--
-- EXEMPLE :
-- Le manager Informatique demande 2 développeurs Java,
-- avec une arrivée souhaitée le 1er décembre.
-- La demande attend d'abord la validation RH.
--
-- MODIFICATIONS :
--   - Ajout de "statut_global" pour faciliter les filtres
--     et l'affichage dans les tableaux de bord.
--   - Ajout de contraintes sur l'effectif et le salaire estimé.
--   - Correction des clés étrangères.
--
-- IMPORTANT :
-- Le statut global doit rester cohérent avec les validations.
-- Le backend est responsable de cette cohérence.
--
-- STATUTS POSSIBLES :
--   EN_ATTENTE_RH
--   EN_ATTENTE_FINANCE
--   EN_ATTENTE_DIRECTION
--   ACCEPTE
--   REFUSE
--
-- "date_demande" est la date de création de la demande.
-- "date_modification" doit être mise à jour par le backend
-- lors d'une modification.
-- ============================================================

CREATE TABLE besoin (
    id SERIAL PRIMARY KEY,
    id_type_besoin INTEGER NOT NULL,
    id_poste INTEGER NOT NULL,
    id_utilisateur_demandeur INTEGER NOT NULL,

    effectif INTEGER NOT NULL DEFAULT 1,
    date_arrivee_souhaitee DATE,
    date_demande TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lieu_travail VARCHAR(150),
    motif TEXT NOT NULL,
    experience_requise VARCHAR(255),
    salaire_estime NUMERIC(12, 2),

    statut_global VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE_RH',
    date_modification TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_besoin_effectif
        CHECK (effectif > 0),

    CONSTRAINT chk_besoin_salaire
        CHECK (salaire_estime IS NULL OR salaire_estime >= 0),

    CONSTRAINT chk_besoin_statut
        CHECK (
            statut_global IN (
                'EN_ATTENTE_RH',
                'EN_ATTENTE_FINANCE',
                'EN_ATTENTE_DIRECTION',
                'ACCEPTE',
                'REFUSE'
            )
        ),

    CONSTRAINT fk_besoin_type
        FOREIGN KEY (id_type_besoin)
        REFERENCES type_besoin(id),

    CONSTRAINT fk_besoin_poste
        FOREIGN KEY (id_poste)
        REFERENCES poste(id),

    CONSTRAINT fk_besoin_demandeur
        FOREIGN KEY (id_utilisateur_demandeur)
        REFERENCES utilisateur(id)
);


-- ============================================================
-- 9. TABLE ETAT_VALIDATION
-- ============================================================
--
-- ROLE :
-- Définit les étapes du circuit de validation d'une demande.
--
-- EXEMPLE :
--   RH         -> ordre 1
--   FINANCE    -> ordre 2
--   DIRECTION  -> ordre 3
--
-- MODIFICATION :
-- "etat_validation" décrit ici l'étape à effectuer, et non
-- la décision prise. La décision est stockée séparément dans
-- la table "decision".
--
-- REGLE METIER :
-- Le backend doit empêcher Finance de valider avant RH et
-- empêcher Direction de valider avant Finance.
-- ============================================================

CREATE TABLE etat_validation (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL,
    ordre INTEGER NOT NULL UNIQUE,

    CONSTRAINT chk_etat_validation_ordre
        CHECK (ordre > 0)
);


-- ============================================================
-- 10. TABLE DECISION
-- ============================================================
--
-- ROLE :
-- Contient les résultats possibles d'une étape de validation.
--
-- EXEMPLES :
--   EN_ATTENTE : l'étape n'a pas encore été traitée.
--   ACCEPTE    : l'étape a été approuvée.
--   REFUSE     : l'étape a été refusée.
--
-- MODIFICATION :
-- Ajout de "code" unique afin que le backend utilise des valeurs
-- stables plutôt que des libellés susceptibles de changer.
-- ============================================================

CREATE TABLE decision (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);


-- ============================================================
-- 11. TABLE VALIDATION_BESOIN
-- ============================================================
--
-- ROLE :
-- Enregistre l'état de chaque étape du processus de validation
-- pour une demande de recrutement.
--
-- EXEMPLE :
-- Besoin n° 10 :
--   RH         -> ACCEPTE
--   FINANCE    -> EN_ATTENTE
--   DIRECTION  -> EN_ATTENTE
--
-- MODIFICATIONS IMPORTANTES :
--   - "id_utilisateur_validateur" devient nullable :
--     une étape en attente n'a pas encore de validateur effectif.
--   - "date_decision" devient nullable : aucune date ne doit être
--     enregistrée comme décision tant que l'étape est en attente.
--   - "id_decision" reste obligatoire et pointe vers EN_ATTENTE
--     lors de l'initialisation de l'étape.
--   - Ajout de UNIQUE(id_besoin, id_etat_validation) pour éviter
--     plusieurs lignes pour la même étape d'une même demande.
--
-- INITIALISATION :
-- À la création d'un besoin, le backend peut créer trois lignes :
-- RH, Finance et Direction, toutes en EN_ATTENTE.
--
-- REGLE METIER :
-- Une étape future ne peut être traitée que lorsque les étapes
-- précédentes ont été acceptées. Si Finance refuse, le besoin
-- passe à REFUSE et Direction ne peut pas valider.
-- ============================================================

CREATE TABLE validation_besoin (
    id SERIAL PRIMARY KEY,
    id_besoin INTEGER NOT NULL,
    id_utilisateur_validateur INTEGER,
    id_etat_validation INTEGER NOT NULL,
    id_decision INTEGER NOT NULL,

    date_decision TIMESTAMPTZ,
    remarque TEXT,

    CONSTRAINT fk_validation_besoin
        FOREIGN KEY (id_besoin)
        REFERENCES besoin(id),

    CONSTRAINT fk_validation_validateur
        FOREIGN KEY (id_utilisateur_validateur)
        REFERENCES utilisateur(id),

    CONSTRAINT fk_validation_etape
        FOREIGN KEY (id_etat_validation)
        REFERENCES etat_validation(id),

    CONSTRAINT fk_validation_decision
        FOREIGN KEY (id_decision)
        REFERENCES decision(id),

    CONSTRAINT uq_validation_besoin_etape
        UNIQUE (id_besoin, id_etat_validation),

    CONSTRAINT chk_validation_date
        CHECK (
            (date_decision IS NULL AND id_utilisateur_validateur IS NULL)
            OR
            (date_decision IS NOT NULL AND id_utilisateur_validateur IS NOT NULL)
        )
);

CREATE TABLE recrutement (
    id SERIAL PRIMARY KEY,
    id_besoin INTEGER NOT NULL REFERENCES besoin(id),
    reference VARCHAR(50) NOT NULL UNIQUE,
    titre VARCHAR(150) NOT NULL,
    description TEXT,
    date_ouverture DATE NOT NULL DEFAULT CURRENT_DATE,
    date_cloture DATE,
    statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON'
        CHECK (statut IN (
            'BROUILLON',
            'OUVERT',
            'SUSPENDU',
            'TERMINE',
            'ANNULE'
        )),
    id_utilisateur_responsable INTEGER
        REFERENCES utilisateur(id),
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CHECK (
        date_cloture IS NULL
        OR date_cloture >= date_ouverture
    )
);

CREATE TABLE offre_emploi (
    id SERIAL PRIMARY KEY,
    id_recrutement INTEGER NOT NULL
        REFERENCES recrutement(id),
    titre VARCHAR(150) NOT NULL,
    description_poste TEXT NOT NULL,
    missions TEXT NOT NULL,
    competences_requises TEXT,
    niveau_etude VARCHAR(100),
    experience_min_annees INTEGER DEFAULT 0
        CHECK (experience_min_annees >= 0),
    salaire_min NUMERIC(12,2)
        CHECK (salaire_min IS NULL OR salaire_min >= 0),
    salaire_max NUMERIC(12,2),
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON'
        CHECK (statut IN (
            'BROUILLON',
            'VALIDEE',
            'ARCHIVEE'
        )),

    CHECK (
        salaire_min IS NULL
        OR salaire_max IS NULL
        OR salaire_max >= salaire_min
    ),

    UNIQUE (id_recrutement)
);

CREATE TABLE publication_offre (
    id SERIAL PRIMARY KEY,
    id_offre INTEGER NOT NULL
        REFERENCES offre_emploi(id),
    plateforme VARCHAR(100) NOT NULL,
    url_publication TEXT,
    date_publication DATE NOT NULL DEFAULT CURRENT_DATE,
    date_expiration DATE,
    statut VARCHAR(20) NOT NULL DEFAULT 'PUBLIEE'
        CHECK (statut IN (
            'BROUILLON',
            'PUBLIEE',
            'EXPIREE',
            'RETIRÉE'
        )),

    CHECK (
        date_expiration IS NULL
        OR date_expiration >= date_publication
    )
);


-- ============================================================
-- 12. TABLE CANDIDAT
-- ============================================================
--
-- ROLE :
-- Contient les informations personnelles et de contact des
-- personnes qui postulent à un emploi.
--
-- EXEMPLE :
-- Une personne dépose une candidature pour le poste de
-- Développeur Java et fournit son adresse e-mail et son CV.
--
-- MODIFICATIONS :
--   - "numero_cin" reste UNIQUE, tout en pouvant être NULL.
--   - "experience" représente ici une expérience totale en années.
--   - Les informations sensibles comme le CIN doivent être protégées.
--
-- IMPORTANT :
-- Le backend doit vérifier les données et ne jamais exposer
-- publiquement le numéro CIN d'un candidat.
-- ============================================================

CREATE TABLE candidat (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    telephone VARCHAR(30),
    numero_cin VARCHAR(30) UNIQUE,
    experience INTEGER,
    date_creation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_candidat_experience
        CHECK (experience IS NULL OR experience >= 0)
);


-- ============================================================
-- 13. TABLE CV
-- ============================================================
--
-- ROLE :
-- Stocke les fichiers CV déposés par les candidats, ainsi que
-- quelques métadonnées nécessaires à leur consultation.
--
-- EXEMPLE :
-- Un candidat envoie "CV_Jean.pdf".
-- Le nom, le type MIME, les octets du fichier et la date
-- de dépôt sont conservés.
--
-- MODIFICATIONS :
--   - "bytes BYTEA" est conservé pour stocker le contenu binaire.
--   - "mime_type" et "nom_fichier" décrivent le fichier.
--   - "statut_lecture" est contrôlé par une contrainte.
--
-- SECURITE :
-- Le backend doit vérifier la taille, le type réel et le contenu
-- du fichier, et pas seulement l'extension ou le MIME déclaré.
-- Les endpoints de consultation doivent être protégés.
-- ============================================================

CREATE TABLE cv (
    id SERIAL PRIMARY KEY,
    id_candidat INTEGER NOT NULL,
    details TEXT,
    nom_fichier VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    bytes BYTEA NOT NULL,
    date_depot TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut_lecture VARCHAR(20) NOT NULL DEFAULT 'NON_LU',

    CONSTRAINT chk_cv_statut_lecture
        CHECK (statut_lecture IN ('NON_LU', 'LU')),

    CONSTRAINT fk_cv_candidat
        FOREIGN KEY (id_candidat)
        REFERENCES candidat(id),

    CONSTRAINT uq_cv_id_candidat UNIQUE (id, id_candidat)
);


-- ============================================================
-- 14. TABLE CANDIDATURE
-- ============================================================
--
-- ROLE :
-- Relie un candidat à un besoin de recrutement et au CV utilisé
-- pour sa candidature.
--
-- EXEMPLE :
-- Le candidat n° 4 postule au besoin n° 12 avec le CV n° 7.
--
-- MODIFICATIONS :
--   - Ajout de "statut" pour suivre le processus de recrutement.
--   - Ajout de "remarque" pour les commentaires RH.
--   - Conservation de l'unicité candidat + besoin : un candidat
--     ne peut pas postuler deux fois au même besoin.
--
-- STATUTS PROPOSES :
--   EN_ATTENTE, PRESELECTIONNE, RETENU, REJETE, RETIRE.
--
-- IMPORTANT :
-- Le backend doit vérifier que le CV référencé appartient
-- réellement au candidat qui dépose la candidature.
-- ============================================================

CREATE TABLE candidature (
    id SERIAL PRIMARY KEY,
    id_candidat INTEGER NOT NULL,
    id_recrutement INTEGER NOT NULL,
    id_cv INTEGER NOT NULL,
    date_candidature TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',
    remarque TEXT,

    CONSTRAINT chk_candidature_statut
        CHECK (
            statut IN (
                'EN_ATTENTE',
                'PRESELECTIONNE',
                'RETENU',
                'REJETE',
                'RETIRE'
            )
        ),

    CONSTRAINT fk_candidature_candidat
        FOREIGN KEY (id_candidat)
        REFERENCES candidat(id),

    CONSTRAINT fk_candidature_recrutement
        FOREIGN KEY (id_recrutement)
        REFERENCES recrutement(id),

    CONSTRAINT fk_candidature_cv_candidat
    FOREIGN KEY (id_cv, id_candidat)
    REFERENCES cv(id, id_candidat),

    CONSTRAINT uq_candidature_candidat_recrutement
    UNIQUE (id_candidat, id_recrutement)
);



-- ============================================================
-- 15. TABLE EMPLOYE
-- ============================================================
--
-- ROLE :
-- Représente une personne effectivement embauchée dans
-- l'entreprise. Une candidature ne devient pas automatiquement
-- un employé : la décision d'embauche doit être confirmée.
--
-- EXEMPLE :
-- Après sélection du candidat n° 4, le RH crée sa fiche employé
-- et l'affecte au département Informatique et au poste
-- Développeur Java.
--
-- MODIFICATIONS :
--   - Ajout de la clé étrangère manquante vers poste.
--   - "id_candidat" est UNIQUE pour éviter de créer plusieurs
--     fiches employé pour le même candidat.
--   - Ajout de contraintes sur le statut.
--
-- REGLE METIER :
-- Le backend doit vérifier que le poste appartient au même
-- département que celui de l'employé.
-- ============================================================

CREATE TABLE decision_recrutement (
    id SERIAL PRIMARY KEY,
    id_candidature INTEGER NOT NULL UNIQUE
        REFERENCES candidature(id),
    id_utilisateur_decideur INTEGER
        REFERENCES utilisateur(id),
    decision VARCHAR(20) NOT NULL
        CHECK (decision IN (
            'EN_ATTENTE',
            'RETENU',
            'REJETE',
            'LISTE_ATTENTE'
        )),
    motif TEXT,
    date_decision TIMESTAMP,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CHECK (
        (decision = 'EN_ATTENTE' AND date_decision IS NULL)
        OR
        (decision <> 'EN_ATTENTE' AND date_decision IS NOT NULL)
    )
);

CREATE TABLE employe (
    id SERIAL PRIMARY KEY,
    id_candidat INTEGER UNIQUE,
    id_departement INTEGER NOT NULL,
    id_poste INTEGER NOT NULL,
    date_embauche DATE NOT NULL DEFAULT CURRENT_DATE,
    statut VARCHAR(20) NOT NULL DEFAULT 'ACTIF',

    CONSTRAINT chk_employe_statut
        CHECK (statut IN ('ACTIF', 'SUSPENDU', 'PARTI')),

    CONSTRAINT fk_employe_candidat
        FOREIGN KEY (id_candidat)
        REFERENCES candidat(id),

    CONSTRAINT fk_employe_departement
        FOREIGN KEY (id_departement)
        REFERENCES departement(id),

    CONSTRAINT fk_employe_poste
        FOREIGN KEY (id_poste)
        REFERENCES poste(id)
);


-- ============================================================
-- 16. TABLE STATUT_CONTRACT
-- ============================================================
--
-- ROLE :
-- Définit les statuts possibles d'un contrat de travail.
--
-- EXEMPLES :
--   BROUILLON : contrat en préparation.
--   EN_ATTENTE_SIGNATURE : contrat envoyé, pas encore signé.
--   ACTIF : contrat en vigueur.
--   TERMINE : contrat arrivé à son terme.
--   RESILIE : contrat interrompu avant son terme.
--
-- MODIFICATION :
-- Le nom "statut_contract" est conservé pour rester compatible
-- avec le nom de la table du script initial.
-- Un code unique est ajouté pour identifier les statuts.
-- ============================================================

CREATE TABLE statut_contract (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);


-- ============================================================
-- 17. TABLE TYPE_CONTRAT
-- ============================================================
--
-- ROLE :
-- Définit les catégories de contrats proposées par l'entreprise.
--
-- EXEMPLES :
--   CDI : contrat à durée indéterminée.
--   CDD : contrat à durée déterminée.
--   STAGE : convention ou contrat de stage, selon les règles
--           applicables dans l'entreprise.
--
-- MODIFICATION :
-- "duree" devient nullable : un CDI n'a pas nécessairement
-- une durée fixe exprimée en nombre de jours.
--
-- ATTENTION :
-- Les unités de durée doivent être définies par le projet.
-- Ici, la durée est exprimée en jours lorsqu'elle est renseignée.
-- ============================================================

CREATE TABLE type_contrat (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL,
    duree INTEGER,

    CONSTRAINT chk_type_contrat_duree
        CHECK (duree IS NULL OR duree > 0)
);


-- ============================================================
-- 18. TABLE CONTRAT
-- ============================================================
--
-- ROLE :
-- Contient les informations du contrat lié à un employé :
-- type, statut, dates, salaire et contenu du document.
--
-- EXEMPLE :
-- Un employé signe un CDD de 12 mois avec un salaire mensuel
-- de 1 200 000 Ar. Le document PDF peut être stocké dans contenu.
--
-- MODIFICATIONS :
--   - Conservation du contenu BYTEA.
--   - Correction des clés étrangères.
--   - Ajout de contraintes sur le salaire et les dates.
--
-- SECURITE :
-- Le fichier du contrat ne doit être accessible qu'aux
-- utilisateurs autorisés. Le backend doit contrôler l'accès
-- avant de renvoyer les octets du PDF.
-- ============================================================

CREATE TABLE contrat (
    id SERIAL PRIMARY KEY,
    id_employe INTEGER NOT NULL,
    id_type_contrat INTEGER NOT NULL,
    id_statut_contrat INTEGER NOT NULL,
    date_debut DATE NOT NULL DEFAULT CURRENT_DATE,
    date_fin DATE,
    salaire NUMERIC(12, 2),
    contenu BYTEA,
    date_creation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_contrat_salaire
        CHECK (salaire IS NULL OR salaire >= 0),

    CONSTRAINT chk_contrat_dates
        CHECK (date_fin IS NULL OR date_fin >= date_debut),

    CONSTRAINT fk_contrat_employe
        FOREIGN KEY (id_employe)
        REFERENCES employe(id),

    CONSTRAINT fk_contrat_type
        FOREIGN KEY (id_type_contrat)
        REFERENCES type_contrat(id),

    CONSTRAINT fk_contrat_statut
        FOREIGN KEY (id_statut_contrat)
        REFERENCES statut_contract(id)
);


-- ============================================================
-- 19. TABLE HISTORIQUE_CONTRAT
-- ============================================================
--
-- ROLE :
-- Conserve les changements importants effectués sur un contrat.
-- Cela permet de comprendre son évolution et d'assurer une trace
-- des modifications.
--
-- EXEMPLE :
-- Le salaire passe de 1 000 000 Ar à 1 200 000 Ar.
-- Une ligne d'historique enregistre l'ancien et le nouveau salaire.
--
-- MODIFICATIONS :
--   - Correction des clés étrangères.
--   - Ajout de "evenement", "motif" et "id_utilisateur_auteur"
--     pour préciser qui a effectué le changement et pourquoi.
--
-- REGLE METIER :
-- L'application doit insérer l'historique au même moment que
-- la modification du contrat, dans une transaction.
-- ============================================================

CREATE TABLE historique_contrat (
    id SERIAL PRIMARY KEY,
    id_contrat INTEGER NOT NULL,
    id_statut_contrat INTEGER,
    id_utilisateur_auteur INTEGER,
    evenement VARCHAR(100) NOT NULL,
    motif TEXT,
    date_modification TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    ancien_salaire NUMERIC(12, 2),
    nouveau_salaire NUMERIC(12, 2),
    ancienne_date_fin DATE,
    nouvelle_date_fin DATE,

    CONSTRAINT fk_historique_contrat
        FOREIGN KEY (id_contrat)
        REFERENCES contrat(id),

    CONSTRAINT fk_historique_statut
        FOREIGN KEY (id_statut_contrat)
        REFERENCES statut_contract(id),

    CONSTRAINT fk_historique_auteur
        FOREIGN KEY (id_utilisateur_auteur)
        REFERENCES utilisateur(id)
);


-- ============================================================
-- 20. TABLE BUDGET_DEPARTEMENT
-- ============================================================
--
-- ROLE :
-- Définit le budget annuel accordé à chaque département.
--
-- EXEMPLE :
-- Le département Informatique dispose d'un budget de
-- 120 000 000 Ar pour l'année 2027.
--
-- MODIFICATIONS :
--   - Ajout de UNIQUE(id_departement, annee) : un seul budget
--     annuel par département.
--   - Ajout d'une contrainte sur le montant.
--   - Correction de la clé étrangère.
--
-- IMPORTANT :
-- La validation financière d'un besoin doit comparer le coût
-- prévisionnel au budget disponible, selon les règles décidées
-- par l'entreprise.
-- ============================================================

CREATE TABLE budget_departement (
    id SERIAL PRIMARY KEY,
    id_departement INTEGER NOT NULL,
    annee INTEGER NOT NULL,
    montant NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    devise VARCHAR(10) NOT NULL DEFAULT 'MGA',

    CONSTRAINT chk_budget_annee
        CHECK (annee >= 2000),

    CONSTRAINT chk_budget_montant
        CHECK (montant >= 0),

    CONSTRAINT fk_budget_departement
        FOREIGN KEY (id_departement)
        REFERENCES departement(id),

    CONSTRAINT uq_budget_departement_annee
        UNIQUE (id_departement, annee)
);


-- ============================================================
-- 21. TABLE DEPENSE_DEPARTEMENT
-- ============================================================
--
-- ROLE :
-- Enregistre les dépenses imputées à un département pour
-- calculer le budget consommé.
--
-- EXEMPLES :
--   - Achat d'ordinateurs.
--   - Achat de licences logicielles.
--   - Frais de formation.
--
-- MODIFICATIONS :
--   - Ajout de "libelle", "categorie" et "date_depense" pour
--     savoir à quoi correspond chaque dépense.
--   - "annee" est conservée pour faciliter les bilans annuels.
--   - Correction de la clé étrangère et du montant.
--
-- REGLE METIER :
-- Le backend doit garder "annee" cohérente avec "date_depense".
-- ============================================================

CREATE TABLE depense_departement (
    id SERIAL PRIMARY KEY,
    id_departement INTEGER NOT NULL,
    libelle VARCHAR(150) NOT NULL,
    categorie VARCHAR(100),
    montant NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    date_depense DATE NOT NULL DEFAULT CURRENT_DATE,
    annee INTEGER NOT NULL,

    CONSTRAINT chk_depense_montant
        CHECK (montant >= 0),

    CONSTRAINT chk_depense_annee
        CHECK (annee >= 2000),

    CONSTRAINT fk_depense_departement
        FOREIGN KEY (id_departement)
        REFERENCES departement(id)
);


-- ============================================================
-- 22. TABLE NOTIFICATION
-- ============================================================
--
-- ROLE :
-- Stocke les notifications visibles par les utilisateurs
-- de l'application.
--
-- EXEMPLES :
--   - Le RH est averti qu'un nouveau besoin a été soumis.
--   - Le manager est informé qu'une demande a été refusée.
--   - La Direction reçoit une notification lorsqu'une demande
--     atteint son étape de validation.
--
-- MODIFICATIONS :
--   - Ajout de "type_notification" pour catégoriser les messages.
--   - "date_lecture" sert à déterminer si une notification a
--     été lue : NULL signifie non lue.
--   - Correction des clés étrangères.
--
-- IMPORTANT :
-- La création de notifications doit être déclenchée par le
-- backend aux événements métier concernés.
-- ============================================================

CREATE TABLE notification (
    id SERIAL PRIMARY KEY,
    id_utilisateur INTEGER NOT NULL,
    id_besoin INTEGER,
    type_notification VARCHAR(50) NOT NULL DEFAULT 'INFORMATION',
    titre VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_lecture TIMESTAMPTZ,

    CONSTRAINT fk_notification_utilisateur
        FOREIGN KEY (id_utilisateur)
        REFERENCES utilisateur(id),

    CONSTRAINT fk_notification_besoin
        FOREIGN KEY (id_besoin)
        REFERENCES besoin(id)
);


-- ============================================================
-- 23. INDEX POUR LES REQUETES FREQUENTES
-- ============================================================
--
-- ROLE :
-- Les index accélèrent certaines recherches fréquentes.
-- Ils ne remplacent pas les contraintes UNIQUE ou les clés
-- étrangères : ils répondent principalement aux besoins de lecture.
--
-- EXEMPLES :
--   - Afficher les besoins en attente.
--   - Lister les validations d'une demande.
--   - Afficher les notifications non lues d'un utilisateur.
--   - Retrouver les candidatures d'un besoin.
-- ============================================================

CREATE INDEX idx_besoin_statut
    ON besoin(statut_global);

CREATE INDEX idx_besoin_demandeur
    ON besoin(id_utilisateur_demandeur);

CREATE INDEX idx_validation_besoin
    ON validation_besoin(id_besoin);

CREATE INDEX idx_validation_decision_etape
    ON validation_besoin(id_etat_validation, id_decision);

CREATE INDEX idx_candidature_recrutement
    ON candidature(id_recrutement);

CREATE INDEX idx_candidature_candidat
    ON candidature(id_candidat);

CREATE INDEX idx_employe_departement
    ON employe(id_departement);

CREATE INDEX idx_contrat_employe
    ON contrat(id_employe);

CREATE INDEX idx_notification_utilisateur_date
    ON notification(id_utilisateur, date_creation DESC);

CREATE INDEX idx_notification_non_lue
    ON notification(id_utilisateur)
    WHERE date_lecture IS NULL;


-- ============================================================
-- 24. DONNEES DE REFERENCE INITIALES
-- ============================================================
--
-- ROLE :
-- Insère les rôles, types et décisions standards utilisés par
-- l'application.
--
-- MODIFICATION :
-- Les identifiants ne sont pas codés en dur dans le backend :
-- le backend doit rechercher les lignes à partir de leur code.
--
-- ON CONFLICT DO NOTHING permet de relancer ces INSERT sans
-- dupliquer les codes existants.
-- ============================================================

INSERT INTO role (code, libelle) VALUES
    ('MANAGER', 'Manager'),
    ('RH', 'Ressources humaines'),
    ('FINANCE', 'Finance'),
    ('DIRECTION', 'Direction')
ON CONFLICT (code) DO NOTHING;


INSERT INTO type_besoin (code, libelle) VALUES
    ('REMPLACEMENT', 'Remplacement d''un employé'),
    ('RENFORT', 'Renforcement de l''effectif'),
    ('CREATION', 'Création d''un besoin ou poste')
ON CONFLICT (code) DO NOTHING;


INSERT INTO etat_validation (code, libelle, ordre) VALUES
    ('RH', 'Validation des ressources humaines', 1),
    ('FINANCE', 'Validation financière', 2),
    ('DIRECTION', 'Validation de la direction', 3)
ON CONFLICT (code) DO NOTHING;


INSERT INTO decision (code, libelle) VALUES
    ('EN_ATTENTE', 'En attente de décision'),
    ('ACCEPTE', 'Demande acceptée'),
    ('REFUSE', 'Demande refusée')
ON CONFLICT (code) DO NOTHING;


INSERT INTO statut_contract (code, libelle) VALUES
    ('BROUILLON', 'Contrat en préparation'),
    ('EN_ATTENTE_SIGNATURE', 'En attente de signature'),
    ('ACTIF', 'Contrat actif'),
    ('TERMINE', 'Contrat terminé'),
    ('RESILIE', 'Contrat résilié')
ON CONFLICT (code) DO NOTHING;


INSERT INTO type_contrat (code, libelle, duree) VALUES
    ('CDI', 'Contrat à durée indéterminée', NULL),
    ('CDD', 'Contrat à durée déterminée', 365),
    ('STAGE', 'Stage', 90)
ON CONFLICT (code) DO NOTHING;


-- ============================================================
-- FIN DU SCRIPT
-- ============================================================
--
-- PROCHAINES ETAPES COTE BACKEND :
--
-- 1. Créer un besoin et ses trois validations EN_ATTENTE.
-- 2. Initialiser le statut_global à EN_ATTENTE_RH.
-- 3. Autoriser la validation RH en premier.
-- 4. Après acceptation RH, autoriser Finance.
-- 5. Après acceptation Finance, autoriser Direction.
-- 6. Si Finance refuse, passer le besoin à REFUSE et bloquer
--    les validations suivantes.
-- 7. Lors de chaque décision, renseigner le validateur,
--    date_decision, id_decision et éventuellement remarque.
-- 8. Mettre à jour le statut_global et créer les notifications
--    dans la même transaction que la décision.
-- 9. Après embauche confirmée, créer la fiche employé et le contrat.
--
-- RAPPEL :
-- Les clés étrangères garantissent l'existence des références,
-- mais elles ne suffisent pas à faire respecter toutes les
-- règles métier. Ces règles doivent être contrôlées par le backend.
-- ============================================================
