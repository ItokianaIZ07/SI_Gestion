-- ============================================================
-- SIG RH - SCRIPT D'INITIALISATION PostgreSQL 12+
-- Version corrigee : structure, contraintes, index et donnees initiales
-- ATTENTION : le bloc DROP TABLE efface les donnees existantes.
-- A executer uniquement sur une base de developpement.
-- ============================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;

BEGIN;

-- 1. Nettoyage (destructif)
DROP TABLE IF EXISTS
    notification, document_employe, tache_onboarding, onboarding,
    modele_tache_onboarding, depense_departement, budget_departement,
    historique_contrat, contrat, statut_contract, type_contrat,
    decision_recrutement, entretien, candidature, cv, candidat,
    publication_offre, offre_emploi, recrutement, validation_besoin,
    besoin, type_besoin, etat_validation, decision, employe,
    grille_salaire, poste, departement, utilisateur, role
CASCADE;

-- 2. Referentiels
CREATE TABLE role (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE type_besoin (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE etat_validation (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL,
    ordre INTEGER NOT NULL UNIQUE CHECK (ordre > 0)
);

CREATE TABLE decision (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE statut_contract (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE type_contrat (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL,
    duree INTEGER CHECK (duree IS NULL OR duree > 0)
);

-- 3. Utilisateurs et organisation
CREATE TABLE utilisateur (
    id SERIAL PRIMARY KEY,
    id_role INTEGER NOT NULL REFERENCES role(id),
    nom VARCHAR(50) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    mdp VARCHAR(255) NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE departement (
    id SERIAL PRIMARY KEY,
    id_utilisateur_manager INTEGER NOT NULL REFERENCES utilisateur(id),
    nom VARCHAR(100) NOT NULL UNIQUE,
    effectif_max INTEGER NOT NULL DEFAULT 0 CHECK (effectif_max >= 0)
);

CREATE TABLE poste (
    id SERIAL PRIMARY KEY,
    id_departement INTEGER NOT NULL REFERENCES departement(id),
    nom VARCHAR(100) NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_poste_departement_nom UNIQUE (id_departement, nom),
    CONSTRAINT uq_poste_id_departement UNIQUE (id, id_departement)
);

CREATE TABLE grille_salaire (
    id SERIAL PRIMARY KEY,
    id_poste INTEGER NOT NULL REFERENCES poste(id),
    valeur_min NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (valeur_min >= 0),
    valeur_max NUMERIC(12,2) NOT NULL DEFAULT 0,
    date_debut_validite DATE NOT NULL DEFAULT CURRENT_DATE,
    date_fin_validite DATE,
    CONSTRAINT chk_grille_valeurs CHECK (valeur_max >= valeur_min),
    CONSTRAINT chk_grille_dates CHECK (
        date_fin_validite IS NULL OR date_fin_validite >= date_debut_validite
    ),
    CONSTRAINT ex_grille_periode EXCLUDE USING gist (
        id_poste WITH =,
        daterange(date_debut_validite, date_fin_validite, '[]') WITH &&
    )
);

-- 4. Employes (la FK vers candidature sera ajoutee apres la creation de candidature)
CREATE TABLE employe (
    id SERIAL PRIMARY KEY,
    matricule VARCHAR(30) NOT NULL UNIQUE,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    telephone VARCHAR(30),
    numero_cin VARCHAR(30) UNIQUE,
    date_naissance DATE,
    adresse TEXT,
    id_candidature INTEGER UNIQUE,
    id_utilisateur INTEGER UNIQUE REFERENCES utilisateur(id),
    id_departement INTEGER NOT NULL REFERENCES departement(id),
    id_poste INTEGER NOT NULL,
    date_embauche DATE NOT NULL DEFAULT CURRENT_DATE,
    date_depart DATE,
    statut VARCHAR(20) NOT NULL DEFAULT 'ACTIF'
        CHECK (statut IN ('ACTIF', 'SUSPENDU', 'PARTI')),
    date_creation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_employe_poste_dept
        FOREIGN KEY (id_poste, id_departement)
        REFERENCES poste(id, id_departement),
    CONSTRAINT chk_employe_dates
        CHECK (date_depart IS NULL OR date_depart >= date_embauche),
    CONSTRAINT chk_employe_statut_depart
        CHECK (
            (statut = 'PARTI' AND date_depart IS NOT NULL)
            OR (statut IN ('ACTIF', 'SUSPENDU') AND date_depart IS NULL)
        )
);

-- 5. Expression du besoin et validations
CREATE TABLE besoin (
    id SERIAL PRIMARY KEY,
    id_type_besoin INTEGER NOT NULL REFERENCES type_besoin(id),
    id_poste INTEGER NOT NULL REFERENCES poste(id),
    id_utilisateur_demandeur INTEGER NOT NULL REFERENCES utilisateur(id),
    id_employe_remplace INTEGER REFERENCES employe(id),
    id_type_contrat_souhaite INTEGER REFERENCES type_contrat(id),
    effectif INTEGER NOT NULL DEFAULT 1 CHECK (effectif > 0),
    priorite VARCHAR(10) NOT NULL DEFAULT 'NORMALE'
        CHECK (priorite IN ('BASSE', 'NORMALE', 'HAUTE', 'URGENTE')),
    date_arrivee_souhaitee DATE,
    date_demande TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lieu_travail VARCHAR(150),
    motif TEXT NOT NULL,
    experience_requise VARCHAR(255),
    niveau_etude VARCHAR(100),
    competences_requises TEXT,
    salaire_estime NUMERIC(12,2) CHECK (salaire_estime IS NULL OR salaire_estime >= 0),
    statut_global VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE_RH'
        CHECK (statut_global IN (
            'EN_ATTENTE_RH', 'EN_ATTENTE_FINANCE', 'EN_ATTENTE_DIRECTION',
            'ACCEPTE', 'REFUSE', 'ANNULE'
        )),
    date_modification TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE validation_besoin (
    id SERIAL PRIMARY KEY,
    id_besoin INTEGER NOT NULL REFERENCES besoin(id) ON DELETE CASCADE,
    id_utilisateur_validateur INTEGER REFERENCES utilisateur(id),
    id_etat_validation INTEGER NOT NULL REFERENCES etat_validation(id),
    id_decision INTEGER NOT NULL REFERENCES decision(id),
    date_decision TIMESTAMPTZ,
    remarque TEXT,
    CONSTRAINT chk_validation_date CHECK (
        (date_decision IS NULL AND id_utilisateur_validateur IS NULL)
        OR (date_decision IS NOT NULL AND id_utilisateur_validateur IS NOT NULL)
    )
);

-- Garantit la coherence entre le code de decision et les champs de validation.
CREATE FUNCTION verifier_validation_besoin() RETURNS trigger AS $$
DECLARE code_decision VARCHAR(50);
BEGIN
    SELECT code INTO code_decision FROM decision WHERE id = NEW.id_decision;
    IF code_decision = 'EN_ATTENTE'
       AND (NEW.date_decision IS NOT NULL OR NEW.id_utilisateur_validateur IS NOT NULL) THEN
        RAISE EXCEPTION 'Une validation EN_ATTENTE ne doit pas avoir de validateur ni de date.';
    ELSIF code_decision IN ('ACCEPTE', 'REFUSE')
       AND (NEW.date_decision IS NULL OR NEW.id_utilisateur_validateur IS NULL) THEN
        RAISE EXCEPTION 'Une validation ACCEPTE ou REFUSE doit avoir un validateur et une date.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_verifier_validation_besoin
BEFORE INSERT OR UPDATE ON validation_besoin
FOR EACH ROW EXECUTE FUNCTION verifier_validation_besoin();

-- 6. Recrutement et offres
CREATE TABLE recrutement (
    id SERIAL PRIMARY KEY,
    id_besoin INTEGER NOT NULL UNIQUE REFERENCES besoin(id),
    reference VARCHAR(50) NOT NULL UNIQUE,
    titre VARCHAR(150) NOT NULL,
    description TEXT,
    date_ouverture DATE NOT NULL DEFAULT CURRENT_DATE,
    date_cloture DATE,
    statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON'
        CHECK (statut IN ('BROUILLON', 'OUVERT', 'SUSPENDU', 'TERMINE', 'ANNULE')),
    id_utilisateur_responsable INTEGER REFERENCES utilisateur(id),
    date_creation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (date_cloture IS NULL OR date_cloture >= date_ouverture)
);

CREATE TABLE offre_emploi (
    id SERIAL PRIMARY KEY,
    id_recrutement INTEGER NOT NULL UNIQUE REFERENCES recrutement(id),
    titre VARCHAR(150) NOT NULL,
    description_poste TEXT NOT NULL,
    missions TEXT NOT NULL,
    competences_requises TEXT,
    niveau_etude VARCHAR(100),
    experience_min_annees INTEGER NOT NULL DEFAULT 0 CHECK (experience_min_annees >= 0),
    salaire_min NUMERIC(12,2) CHECK (salaire_min IS NULL OR salaire_min >= 0),
    salaire_max NUMERIC(12,2) CHECK (salaire_max IS NULL OR salaire_max >= 0),
    date_creation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON'
        CHECK (statut IN ('BROUILLON', 'VALIDEE', 'ARCHIVEE')),
    CHECK (salaire_min IS NULL OR salaire_max IS NULL OR salaire_max >= salaire_min)
);

CREATE TABLE publication_offre (
    id SERIAL PRIMARY KEY,
    id_offre INTEGER NOT NULL REFERENCES offre_emploi(id),
    plateforme VARCHAR(100) NOT NULL,
    url_publication TEXT,
    date_publication DATE NOT NULL DEFAULT CURRENT_DATE,
    date_expiration DATE,
    statut VARCHAR(20) NOT NULL DEFAULT 'PUBLIEE'
        CHECK (statut IN ('BROUILLON', 'PUBLIEE', 'EXPIREE', 'RETIREE')),
    CHECK (date_expiration IS NULL OR date_expiration >= date_publication)
);

-- 7. Candidats, CV et candidatures
CREATE TABLE candidat (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    telephone VARCHAR(30),
    numero_cin VARCHAR(30) UNIQUE,
    experience INTEGER CHECK (experience IS NULL OR experience >= 0),
    date_creation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cv (
    id SERIAL PRIMARY KEY,
    id_candidat INTEGER NOT NULL REFERENCES candidat(id),
    details TEXT,
    nom_fichier VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    bytes BYTEA NOT NULL,
    date_depot TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut_lecture VARCHAR(20) NOT NULL DEFAULT 'NON_LU'
        CHECK (statut_lecture IN ('NON_LU', 'LU')),
    CONSTRAINT uq_cv_id_candidat UNIQUE (id, id_candidat)
);

CREATE TABLE candidature (
    id SERIAL PRIMARY KEY,
    id_candidat INTEGER NOT NULL REFERENCES candidat(id),
    id_recrutement INTEGER NOT NULL REFERENCES recrutement(id),
    id_cv INTEGER NOT NULL,
    id_publication INTEGER REFERENCES publication_offre(id),
    date_candidature TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE'
        CHECK (statut IN ('EN_ATTENTE', 'PRESELECTIONNE', 'RETENU', 'REJETE', 'RETIRE')),
    score NUMERIC(5,2) CHECK (score IS NULL OR score BETWEEN 0 AND 100),
    remarque TEXT,
    CONSTRAINT fk_candidature_cv_candidat
        FOREIGN KEY (id_cv, id_candidat) REFERENCES cv(id, id_candidat),
    CONSTRAINT uq_candidature_candidat_recrutement UNIQUE (id_candidat, id_recrutement)
);

ALTER TABLE employe
    ADD CONSTRAINT fk_employe_candidature
    FOREIGN KEY (id_candidature) REFERENCES candidature(id);

-- Vérifie qu'une publication utilisée par une candidature appartient bien
-- au recrutement de cette candidature.
CREATE FUNCTION verifier_publication_candidature() RETURNS trigger AS $$
BEGIN
    IF NEW.id_publication IS NOT NULL AND NOT EXISTS (
        SELECT 1
        FROM publication_offre p
        JOIN offre_emploi o ON o.id = p.id_offre
        WHERE p.id = NEW.id_publication
          AND o.id_recrutement = NEW.id_recrutement
    ) THEN
        RAISE EXCEPTION 'La publication choisie ne correspond pas au recrutement de la candidature.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_verifier_publication_candidature
BEFORE INSERT OR UPDATE OF id_publication, id_recrutement ON candidature
FOR EACH ROW EXECUTE FUNCTION verifier_publication_candidature();

CREATE TABLE entretien (
    id SERIAL PRIMARY KEY,
    id_candidature INTEGER NOT NULL REFERENCES candidature(id),
    id_utilisateur_intervieweur INTEGER REFERENCES utilisateur(id),
    type_entretien VARCHAR(20) NOT NULL
        CHECK (type_entretien IN ('TELEPHONE', 'TEST_TECHNIQUE', 'RH', 'TECHNIQUE', 'DIRECTION')),
    date_entretien TIMESTAMPTZ NOT NULL,
    lieu VARCHAR(150),
    statut VARCHAR(20) NOT NULL DEFAULT 'PLANIFIE'
        CHECK (statut IN ('PLANIFIE', 'REALISE', 'ANNULE')),
    note NUMERIC(4,2) CHECK (note IS NULL OR note BETWEEN 0 AND 20),
    commentaire TEXT,
    CHECK (statut <> 'REALISE' OR note IS NOT NULL OR commentaire IS NOT NULL)
);

CREATE TABLE decision_recrutement (
    id SERIAL PRIMARY KEY,
    id_candidature INTEGER NOT NULL UNIQUE REFERENCES candidature(id),
    id_utilisateur_decideur INTEGER REFERENCES utilisateur(id),
    decision VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE'
        CHECK (decision IN ('EN_ATTENTE', 'RETENU', 'REJETE', 'LISTE_ATTENTE')),
    motif TEXT,
    date_decision TIMESTAMPTZ,
    date_creation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (
        (decision = 'EN_ATTENTE' AND date_decision IS NULL)
        OR (decision <> 'EN_ATTENTE' AND date_decision IS NOT NULL
            AND id_utilisateur_decideur IS NOT NULL)
    )
);

-- 8. Contrats et historique
CREATE TABLE contrat (
    id SERIAL PRIMARY KEY,
    reference VARCHAR(50) NOT NULL UNIQUE,
    id_employe INTEGER NOT NULL REFERENCES employe(id),
    id_type_contrat INTEGER NOT NULL REFERENCES type_contrat(id),
    id_statut_contrat INTEGER NOT NULL REFERENCES statut_contract(id),
    id_contrat_precedent INTEGER REFERENCES contrat(id),
    id_utilisateur_createur INTEGER REFERENCES utilisateur(id),
    date_debut DATE NOT NULL DEFAULT CURRENT_DATE,
    date_fin DATE,
    periode_essai_jours INTEGER CHECK (periode_essai_jours IS NULL OR periode_essai_jours >= 0),
    date_signature DATE,
    salaire NUMERIC(12,2) CHECK (salaire IS NULL OR salaire >= 0),
    nom_fichier VARCHAR(255),
    mime_type VARCHAR(100),
    contenu BYTEA,
    date_creation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_contrat_dates CHECK (date_fin IS NULL OR date_fin >= date_debut),
    CONSTRAINT chk_contrat_signature CHECK (date_signature IS NULL OR date_signature >= date_debut),
    CONSTRAINT chk_contrat_fichier CHECK (
        (contenu IS NULL AND nom_fichier IS NULL AND mime_type IS NULL)
        OR (contenu IS NOT NULL AND nom_fichier IS NOT NULL AND mime_type IS NOT NULL)
    )
);

-- Empêche deux contrats simultanément ACTIF pour le même employé.
CREATE FUNCTION verifier_contrat_actif() RETURNS trigger AS $$
DECLARE code_statut VARCHAR(50);
BEGIN
    SELECT code INTO code_statut FROM statut_contract WHERE id = NEW.id_statut_contrat;
    IF code_statut = 'ACTIF' AND EXISTS (
        SELECT 1 FROM contrat c
        JOIN statut_contract s ON s.id = c.id_statut_contrat
        WHERE c.id_employe = NEW.id_employe
          AND s.code = 'ACTIF'
          AND c.id <> COALESCE(NEW.id, -1)
    ) THEN
        RAISE EXCEPTION 'Cet employe possede deja un contrat ACTIF.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_verifier_contrat_actif
BEFORE INSERT OR UPDATE OF id_employe, id_statut_contrat ON contrat
FOR EACH ROW EXECUTE FUNCTION verifier_contrat_actif();

CREATE TABLE historique_contrat (
    id SERIAL PRIMARY KEY,
    id_contrat INTEGER NOT NULL REFERENCES contrat(id),
    id_ancien_statut INTEGER REFERENCES statut_contract(id),
    id_statut_contrat INTEGER REFERENCES statut_contract(id),
    id_utilisateur_auteur INTEGER REFERENCES utilisateur(id),
    evenement VARCHAR(100) NOT NULL,
    motif TEXT,
    date_modification TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ancien_salaire NUMERIC(12,2) CHECK (ancien_salaire IS NULL OR ancien_salaire >= 0),
    nouveau_salaire NUMERIC(12,2) CHECK (nouveau_salaire IS NULL OR nouveau_salaire >= 0),
    ancienne_date_fin DATE,
    nouvelle_date_fin DATE
);

-- 9. Onboarding et documents
CREATE TABLE modele_tache_onboarding (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(150) NOT NULL,
    categorie VARCHAR(20) NOT NULL
        CHECK (categorie IN ('ADMINISTRATIF', 'INFORMATIQUE', 'ACCUEIL', 'FORMATION', 'AUTRE')),
    delai_jours INTEGER NOT NULL DEFAULT 0 CHECK (delai_jours >= 0),
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE onboarding (
    id SERIAL PRIMARY KEY,
    id_employe INTEGER NOT NULL UNIQUE REFERENCES employe(id),
    id_utilisateur_responsable INTEGER REFERENCES utilisateur(id),
    date_debut DATE NOT NULL DEFAULT CURRENT_DATE,
    date_fin_prevue DATE,
    statut VARCHAR(20) NOT NULL DEFAULT 'A_FAIRE'
        CHECK (statut IN ('A_FAIRE', 'EN_COURS', 'TERMINE')),
    date_creation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (date_fin_prevue IS NULL OR date_fin_prevue >= date_debut)
);

CREATE TABLE tache_onboarding (
    id SERIAL PRIMARY KEY,
    id_onboarding INTEGER NOT NULL REFERENCES onboarding(id) ON DELETE CASCADE,
    id_modele INTEGER REFERENCES modele_tache_onboarding(id),
    id_utilisateur_responsable INTEGER REFERENCES utilisateur(id),
    libelle VARCHAR(150) NOT NULL,
    categorie VARCHAR(20) NOT NULL DEFAULT 'AUTRE'
        CHECK (categorie IN ('ADMINISTRATIF', 'INFORMATIQUE', 'ACCUEIL', 'FORMATION', 'AUTRE')),
    date_echeance DATE,
    statut VARCHAR(20) NOT NULL DEFAULT 'A_FAIRE'
        CHECK (statut IN ('A_FAIRE', 'EN_COURS', 'FAIT', 'ANNULE')),
    date_realisation TIMESTAMPTZ,
    remarque TEXT,
    CHECK (
        (statut = 'FAIT' AND date_realisation IS NOT NULL)
        OR (statut <> 'FAIT' AND date_realisation IS NULL)
    )
);

CREATE TABLE document_employe (
    id SERIAL PRIMARY KEY,
    id_employe INTEGER NOT NULL REFERENCES employe(id),
    type_document VARCHAR(30) NOT NULL
        CHECK (type_document IN ('CIN', 'DIPLOME', 'RIB', 'CV', 'CERTIFICAT_MEDICAL', 'PHOTO', 'AUTRE')),
    nom_fichier VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    bytes BYTEA NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'FOURNI'
        CHECK (statut IN ('FOURNI', 'VALIDE', 'REFUSE')),
    date_depot TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. Budget, depenses, notifications
CREATE TABLE budget_departement (
    id SERIAL PRIMARY KEY,
    id_departement INTEGER NOT NULL REFERENCES departement(id),
    annee INTEGER NOT NULL CHECK (annee >= 2000),
    montant NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (montant >= 0),
    devise VARCHAR(10) NOT NULL DEFAULT 'MGA',
    CONSTRAINT uq_budget_departement_annee UNIQUE (id_departement, annee)
);

CREATE TABLE depense_departement (
    id SERIAL PRIMARY KEY,
    id_departement INTEGER NOT NULL REFERENCES departement(id),
    id_besoin INTEGER REFERENCES besoin(id),
    id_contrat INTEGER REFERENCES contrat(id),
    libelle VARCHAR(150) NOT NULL,
    categorie VARCHAR(100),
    montant NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (montant >= 0),
    date_depense DATE NOT NULL DEFAULT CURRENT_DATE,
    annee INTEGER GENERATED ALWAYS AS (EXTRACT(YEAR FROM date_depense)::INTEGER) STORED,
    CHECK (id_besoin IS NULL OR id_contrat IS NULL)
);

CREATE TABLE notification (
    id SERIAL PRIMARY KEY,
    id_utilisateur INTEGER NOT NULL REFERENCES utilisateur(id),
    id_besoin INTEGER REFERENCES besoin(id),
    id_candidature INTEGER REFERENCES candidature(id),
    type_notification VARCHAR(50) NOT NULL DEFAULT 'INFORMATION',
    titre VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_lecture TIMESTAMPTZ,
    CHECK (date_lecture IS NULL OR date_lecture >= date_creation)
);

-- 11. Index utiles (les contraintes UNIQUE/PK ont deja leurs index)
CREATE INDEX idx_utilisateur_role ON utilisateur(id_role);
CREATE INDEX idx_departement_manager ON departement(id_utilisateur_manager);
CREATE INDEX idx_poste_departement ON poste(id_departement);
CREATE INDEX idx_grille_salaire_poste ON grille_salaire(id_poste);
CREATE INDEX idx_besoin_statut ON besoin(statut_global);
CREATE INDEX idx_besoin_demandeur ON besoin(id_utilisateur_demandeur);
CREATE INDEX idx_besoin_poste ON besoin(id_poste);
CREATE INDEX idx_besoin_remplace ON besoin(id_employe_remplace);
CREATE INDEX idx_validation_decision_etape ON validation_besoin(id_etat_validation, id_decision);
CREATE INDEX idx_recrutement_statut ON recrutement(statut);
CREATE INDEX idx_recrutement_responsable ON recrutement(id_utilisateur_responsable);
CREATE INDEX idx_publication_offre ON publication_offre(id_offre);
CREATE INDEX idx_candidat_email ON candidat(email);
CREATE INDEX idx_cv_candidat ON cv(id_candidat);
CREATE INDEX idx_candidature_recrutement_statut ON candidature(id_recrutement, statut);
CREATE INDEX idx_candidature_candidat ON candidature(id_candidat);
CREATE INDEX idx_candidature_publication ON candidature(id_publication);
CREATE INDEX idx_entretien_candidature_date ON entretien(id_candidature, date_entretien);
CREATE INDEX idx_decision_decideur ON decision_recrutement(id_utilisateur_decideur);
CREATE INDEX idx_employe_departement_statut ON employe(id_departement, statut);
CREATE INDEX idx_contrat_employe ON contrat(id_employe);
CREATE INDEX idx_contrat_statut ON contrat(id_statut_contrat);
CREATE INDEX idx_historique_contrat ON historique_contrat(id_contrat, date_modification DESC);
CREATE INDEX idx_onboarding_responsable ON onboarding(id_utilisateur_responsable);
CREATE INDEX idx_tache_onboarding_statut ON tache_onboarding(id_onboarding, statut);
CREATE INDEX idx_tache_responsable ON tache_onboarding(id_utilisateur_responsable);
CREATE INDEX idx_document_employe ON document_employe(id_employe);
CREATE INDEX idx_depense_dept_annee ON depense_departement(id_departement, annee);
CREATE INDEX idx_depense_besoin ON depense_departement(id_besoin);
CREATE INDEX idx_depense_contrat ON depense_departement(id_contrat);
CREATE INDEX idx_notification_utilisateur_date ON notification(id_utilisateur, date_creation DESC);
CREATE INDEX idx_notification_non_lue ON notification(id_utilisateur) WHERE date_lecture IS NULL;

COMMIT;