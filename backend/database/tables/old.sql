DROP TABLE IF EXISTS role;
CREATE TABLE role (
    id      SERIAL PRIMARY KEY,
    libelle VARCHAR(100) NOT NULL
);

DROP TABLE IF EXISTS utilisateur;
CREATE TABLE utilisateur (
    id              SERIAL PRIMARY KEY,
    id_role         INTEGER NOT NULL,
    nom             VARCHAR(50) NOT NULL,
    prenom          VARCHAR(50) NOT NULL,
    email           VARCHAR(100) NOT NULL,
    mdp             VARCHAR(100) NOT NULL,
    actif           BOOLEAN DEFAULT true,
    date_creation   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    ADD CONSTRAINT fk_role 
    FOREIGN KEY (id_role) REFERENCES role(id)
);

DROP TABLE IF EXISTS departement;
CREATE TABLE departement (
    id                      SERIAL PRIMARY KEY,
    id_utilisateur_manager  INTEGER NOT NULL,
    nom                     VARCHAR(100) NOT NULL,
    effectif_max            INTEGER DEFAULT 0,

    ADD CONSTRAINT fk_utilisateur_manager 
    FOREIGN KEY (id_utilisateur_manager) REFERENCES utilisateur(id)
);

DROP TABLE IF EXISTS poste;
CREATE TABLE poste (
    id              SERIAL PRIMARY KEY,
    id_departement  INTEGER NOT NULL,
    nom             VARCHAR(100) NOT NULL,

    ADD CONSTRAINT fk_departement
    FOREIGN KEY (id_departement) REFERENCES departement(id)
);

DROP TABLE IF EXISTS grille_salaire;
CREATE TABLE grille_salaire (
    id                  SERIAL PRIMARY KEY,
    id_poste            INTEGER NOT NULL,
    valeur_min          NUMERIC(10, 2) DEFAULT 0.00,
    valeur_max          NUMERIC(10, 2) DEFAULT 0.00,
    date_debut_validite TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    date_fin_validite   TIMESTAMPTZ,
    
    ADD CONSTRAINT fk_poste
    FOREIGN KEY (id_poste) REFERENCES poste(id)
);

DROP TABLE IF EXISTS type_besoin;
CREATE TABLE type_besoin (
    id      SERIAL PRIMARY KEY,
    libelle VARCHAR(100) NOT NULL
);

DROP TABLE IF EXISTS besoin;
CREATE TABLE besoin (
    id                          SERIAL PRIMARY KEY,
    id_type_besoin              INTEGER NOT NULL,
    id_poste                    INTEGER NOT NULL,
    id_utilisateur_demandeur    INTEGER NOT NULL,
    effectif                    INTEGER DEFAULT 1,
    date_arrivee_souhaitee      DATE,
    date_demande                DATE DEFAULT CURRENT_DATE,
    lieu_travail                VARCHAR(100),
    motif                       VARCHAR(255),
    experience_requise          VARCHAR(255),
    salaire_estime              NUMERIC(10, 2),
    date_modification           TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    ADD CONSTRAINT fk_type_besoin
    FOREIGN KEY (id_type_besoin) REFERENCES type_besoin(id),

    ADD CONSTRAINT fk_poste
    FOREIGN KEY (id_poste) REFERENCES poste(id),

    ADD CONSTRAINT fk_utilisateur_demandeur
    FOREIGN KEY (id_utilisateur_demandeur) REFERENCES utilisateur(id)
);

DROP TABLE IF EXISTS etat_validation;
CREATE TABLE etat_validation (
    id      SERIAL PRIMARY KEY,
    code    VARCHAR(50) NOT NULL,
    libelle VARCHAR(100) NOT NULL,
    ordre   INTEGER
);

DROP TABLE IF EXISTS decision;
CREATE TABLE decision (
    id      SERIAL PRIMARY KEY,
    libelle VARCHAR(100) NOT NULL
);

DROP TABLE IF EXISTS validation_besoin;
CREATE TABLE validation_besoin (
    id                          SERIAL PRIMARY KEY,
    id_besoin                   INTEGER NOT NULL,
    id_utilisateur_validateur   INTEGER NOT NULL,
    id_etat_validation          INTEGER NOT NULL,
    id_decision                 INTEGER NOT NULL,
    date_decision               TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    remarque                    VARCHAR(255),

    ADD CONSTRAINT fk_besoin
    FOREIGN KEY (id_besoin) REFERENCES besoin(id),

    ADD CONSTRAINT fk_utilisateur_validateur
    FOREIGN KEY (id_utilisateur_validateur) REFERENCES utilisateur(id),

    ADD CONSTRAINT fk_etat_validation
    FOREIGN KEY (id_etat_validation) REFERENCES etat_validation(id),

    ADD CONSTRAINT fk_decision 
    FOREIGN KEY (id_decision) REFERENCES decision(id)
);

DROP TABLE IF EXISTS candidat;
CREATE TABLE candidat (
    id              SERIAL PRIMARY KEY,
    nom             VARCHAR(50) NOT NULL,
    prenom          VARCHAR(50) NOT NULL,
    email           VARCHAR(100) NOT NULL,
    telephone       VARCHAR(20),
    numero_cin      VARCHAR(20) UNIQUE,
    experience      INTEGER,
    date_creation   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS cv;
CREATE TABLE cv (
    id              SERIAL PRIMARY KEY,
    id_candidat     INTEGER NOT NULL,
    details         VARCHAR(255),
    nom_fichier     VARCHAR(255) NOT NULL,
    mime_type       VARCHAR(100) DEFAULT 'application/pdf',
    bytes           BYTEA NOT NULL,
    date_depot      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    statut_lecture  VARCHAR(50) DEFAULT 'NON_LU',

    ADD CONSTRAINT fk_candidat
    FOREIGN KEY (id_candidat) REFERENCES candidat(id)
);

DROP TABLE IF EXISTS candidature;
CREATE TABLE candidature (
    id                  SERIAL PRIMARY KEY,
    id_candidat         INTEGER NOT NULL,
    id_besoin           INTEGER NOT NULL,
    id_cv               INTEGER NOT NULL,
    date_candidature    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    ADD CONSTRAINT fk_candidat 
    FOREIGN KEY (id_candidat) REFERENCES candidat(id),

    ADD CONSTRAINT fk_besoin
    FOREIGN KEY (id_besoin) REFERENCES besoin(id),

    ADD CONSTRAINT fk_cv 
    FOREIGN KEY (id_cv) REFERENCES cv(id),

    ADD CONSTRAINT unique_candidature UNIQUE (id_candidat, id_besoin)
);

DROP TABLE IF EXISTS employe;
CREATE TABLE employe (
    id SERIAL PRIMARY KEY,
    id_candidat INTEGER,
    id_departement INTEGER,
    id_poste INTEGER,
    date_embauche TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    statut VARCHAR(50) DEFAULT 'ACTIF',

    ADD CONSTRAINT fk_candidat
    FOREIGN KEY (id_candidat) REFERENCES candidat(id),

    ADD CONSTRAINT fk_departement
    FOREIGN KEY (id_departement) REFERENCES departement(id)
);

DROP TABLE IF EXISTS statut_contract;
CREATE TABLE statut_contract (
    id SERIAL PRIMARY KEY,
    libelle VARCHAR(100) NOT NULL
);

DROP TABLE IF EXISTS type_contrat;
CREATE TABLE type_contrat (
    id      SERIAL PRIMARY KEY,
    libelle VARCHAR(100) NOT NULL,
    cigle   VARCHAR(50) NOT NULL,
    duree   INTEGER NOT NULL
);

DROP TABLE IF EXISTS contrat;
CREATE TABLE contrat (
    id                  SERIAL PRIMARY KEY,
    id_employe          INTEGER NOT NULL,
    id_type_contrat     INTEGER NOT NULL,
    id_statut_contrat   INTEGER NOT NULL,
    date_debut          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    date_fin            TIMESTAMPTZ,
    salaire             NUMERIC(10, 2),
    contenu             BYTEA,
    date_creation       TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    ADD CONSTRAINT fk_employe
    FOREIGN KEY (id_employe) REFERENCES employe(id),

    ADD CONSTRAINT fk_type_contrat
    FOREIGN KEY (id_type_contrat) REFERENCES type_contrat(id),

    ADD CONSTRAINT fk_statut_contrat
    FOREIGN KEY (id_statut_contrat) REFERENCES statut_contract(id)
);

DROP TABLE IF EXISTS historique_contrat;
CREATE TABLE historique_contrat (
    id                  SERIAL PRIMARY KEY,
    id_contrat          INTEGER NOT NULL,
    id_statut_contrat   INTEGER NOT NULL,
    date_modification   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ancien_salaire      NUMERIC(10, 2),
    nouveau_salaire     NUMERIC(10, 2),
    ancien_date_fin     TIMESTAMPTZ,
    nouveau_date_fin    TIMESTAMPTZ,

    ADD CONSTRAINT fk_contrat
    FOREIGN KEY (id_contrat) REFERENCES contrat(id),

    ADD CONSTRAINT fk_statut_contrat
    FOREIGN KEY (id_statut_contrat) REFERENCES statut_contract(id)
);

DROP TABLE IF EXISTS budget_departement;
CREATE TABLE budget_departement (
    id              SERIAL PRIMARY KEY,
    id_departement  INTEGER NOT NULL,
    annee           INTEGER NOT NULL,
    montant         NUMERIC(10, 2) DEFAULT 0.00,
    devise          VARCHAR(10) NOT NULL DEFAULT 'AR',

    ADD CONSTRAINT fk_departement
    FOREIGN KEY (id_departement) REFERENCES departement(id)
);

DROP TABLE IF EXISTS depense_departement;
CREATE TABLE depense_departement (
    id              SERIAL PRIMARY KEY,
    id_departement  INTEGER NOT NULL,
    montant         NUMERIC(10, 2) DEFAULT 0.00,
    annee           INTEGER NOT NULL,

    ADD CONSTRAINT fk_departement
    FOREIGN KEY (id_departement) REFERENCES departement(id)
);

DROP TABLE IF EXISTS 

DROP TABLE IF EXISTS notification;
CREATE TABLE notification (
    id SERIAL PRIMARY KEY,
    id_utilisateur INTEGER NOT NULL,
    id_besoin INTEGER,
    titre VARCHAR(100) NOT NULL,
    message VARCHAR(255) NOT NULL,
    date_creation TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    date_lecture TIMESTAMPTZ,

    ADD CONSTRAINT fk_utilisateur
    FOREIGN KEY (id_utilisateur) REFERENCES utilisateur(id),

    ADD CONSTRAINT fk_besoin
    FOREIGN KEY (id_besoin) REFERENCES besoin(id)
)