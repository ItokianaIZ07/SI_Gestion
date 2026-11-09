-- ============================================================
-- SCRIPT FUSIONNE RH + GESTION DES CONGES / ABSENCES (CORRIGE)
-- PostgreSQL - execution en une seule transaction
-- Dependances preexistantes : employe, utilisateur, role, decision,
-- departement, poste (avec contrainte UNIQUE (id, id_departement)),
-- contrat, statut_contract, document_employe, notification, besoin,
-- validation_besoin, decision_recrutement, tache_onboarding,
-- candidature, offre_emploi, publication_offre.
-- ============================================================

BEGIN;

-- ============================================================
-- REEXECUTION : suppression des objets crees par ce script
-- ATTENTION : les donnees des tables ci-dessous seront effacees.
-- ============================================================

-- Suppression de TOUS les triggers d'audit (y compris sur les tables
-- preexistantes), sinon la re-execution echoue ("trigger already exists").
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['utilisateur', 'employe', 'contrat', 'document_employe',
        'historique_affectation', 'tache_onboarding', 'besoin', 'validation_besoin',
        'candidature', 'decision_recrutement', 'demande_conge', 'validation_conge', 'regle_conge',
        'mouvement_conge', 'absence', 'justificatif_absence',
        'mission', 'mission_employe', 'remplacement_conge',
        'temps_travail_journalier', 'indemnite_conge']
    LOOP
        IF to_regclass(t) IS NOT NULL THEN
            EXECUTE format('DROP TRIGGER IF EXISTS trg_audit_%1$s ON %1$I', t);
        END IF;
    END LOOP;

    IF to_regclass('validation_besoin') IS NOT NULL THEN
        EXECUTE 'DROP TRIGGER IF EXISTS trg_verifier_validation_besoin ON validation_besoin';
    END IF;
    IF to_regclass('candidature') IS NOT NULL THEN
        EXECUTE 'DROP TRIGGER IF EXISTS trg_verifier_publication_candidature ON candidature';
    END IF;
    IF to_regclass('contrat') IS NOT NULL THEN
        EXECUTE 'DROP TRIGGER IF EXISTS trg_verifier_contrat_actif ON contrat';
    END IF;
    IF to_regclass('historique_affectation') IS NOT NULL THEN
        EXECUTE 'DROP TRIGGER IF EXISTS trg_verifier_hierarchie ON historique_affectation';
    END IF;
    IF to_regclass('journal_audit') IS NOT NULL THEN
        EXECUTE 'DROP TRIGGER IF EXISTS trg_journal_audit_lecture_seule ON journal_audit';
    END IF;
    IF to_regclass('employe') IS NOT NULL THEN
        EXECUTE 'DROP TRIGGER IF EXISTS trg_employe_dossier_initial ON employe';
        EXECUTE 'DROP TRIGGER IF EXISTS trg_employe_changements ON employe';
    END IF;
END
$$;

DROP VIEW IF EXISTS v_organigramme, v_variables_paie_mensuelles;

DROP TABLE IF EXISTS indemnite_conge, mission_employe, mission, justificatif_absence, absence, type_absence, mouvement_conge, remplacement_conge, validation_conge, demande_conge, jour_ferie, regle_conge, type_conge, anomalie_pointage, temps_travail_journalier, pointage, planning_travail, horaire_travail, regime_travail, journal_audit, historique_affectation, historique_tache_onboarding CASCADE;

ALTER TABLE decision_recrutement DROP CONSTRAINT IF EXISTS chk_decision_decideur;
ALTER TABLE tache_onboarding DROP CONSTRAINT IF EXISTS chk_tache_realisation;
ALTER TABLE notification DROP CONSTRAINT IF EXISTS chk_notification_lecture;
ALTER TABLE document_employe DROP CONSTRAINT IF EXISTS chk_document_delai_alerte;

CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE employe DROP CONSTRAINT IF EXISTS chk_employe_statut_depart;
ALTER TABLE employe ADD CONSTRAINT chk_employe_statut_depart CHECK (
    (statut = 'PARTI' AND date_depart IS NOT NULL)
    OR (statut IN ('ACTIF', 'SUSPENDU') AND date_depart IS NULL)) NOT VALID;

ALTER TABLE decision_recrutement ADD CONSTRAINT chk_decision_decideur
    CHECK (decision = 'EN_ATTENTE' OR id_utilisateur_decideur IS NOT NULL) NOT VALID;

ALTER TABLE tache_onboarding ADD CONSTRAINT chk_tache_realisation CHECK (
    (statut = 'FAIT' AND date_realisation IS NOT NULL)
    OR (statut <> 'FAIT' AND date_realisation IS NULL)) NOT VALID;

ALTER TABLE notification ADD CONSTRAINT chk_notification_lecture
    CHECK (date_lecture IS NULL OR date_lecture >= date_creation) NOT VALID;

ALTER TABLE document_employe
    ADD COLUMN IF NOT EXISTS date_expiration DATE,
    ADD COLUMN IF NOT EXISTS date_renouvellement DATE,
    ADD COLUMN IF NOT EXISTS delai_alerte_jours INTEGER NOT NULL DEFAULT 30;
ALTER TABLE document_employe
    ADD CONSTRAINT chk_document_delai_alerte CHECK (delai_alerte_jours >= 0);

CREATE INDEX IF NOT EXISTS idx_document_expiration ON document_employe(date_expiration)
    WHERE date_expiration IS NOT NULL AND date_renouvellement IS NULL;

ALTER TABLE notification
    ADD COLUMN IF NOT EXISTS id_document_employe INTEGER REFERENCES document_employe(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_notification_document ON notification(id_document_employe)
    WHERE id_document_employe IS NOT NULL;

CREATE OR REPLACE FUNCTION verifier_validation_besoin() RETURNS trigger AS $$
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

CREATE OR REPLACE FUNCTION verifier_publication_candidature() RETURNS trigger AS $$
BEGIN
    IF NEW.id_publication IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM publication_offre p JOIN offre_emploi o ON o.id = p.id_offre
        WHERE p.id = NEW.id_publication AND o.id_recrutement = NEW.id_recrutement
    ) THEN
        RAISE EXCEPTION 'La publication choisie ne correspond pas au recrutement de la candidature.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_verifier_publication_candidature
BEFORE INSERT OR UPDATE OF id_publication, id_recrutement ON candidature
FOR EACH ROW EXECUTE FUNCTION verifier_publication_candidature();

CREATE OR REPLACE FUNCTION verifier_contrat_actif() RETURNS trigger AS $$
DECLARE code_statut VARCHAR(50);
BEGIN
    SELECT code INTO code_statut FROM statut_contract WHERE id = NEW.id_statut_contrat;
    IF code_statut = 'ACTIF' THEN
        PERFORM 1 FROM employe WHERE id = NEW.id_employe FOR UPDATE;
        IF EXISTS (
            SELECT 1 FROM contrat c JOIN statut_contract s ON s.id = c.id_statut_contrat
            WHERE c.id_employe = NEW.id_employe AND s.code = 'ACTIF'
              AND c.id <> COALESCE(NEW.id, -1)
        ) THEN
            RAISE EXCEPTION 'Cet employe possede deja un contrat ACTIF.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_verifier_contrat_actif
BEFORE INSERT OR UPDATE OF id_employe, id_statut_contrat ON contrat
FOR EACH ROW EXECUTE FUNCTION verifier_contrat_actif();


-- ============================================================
-- 1. TABLES RH
-- ============================================================

CREATE TABLE historique_tache_onboarding (
    id BIGSERIAL PRIMARY KEY,
    id_tache INTEGER NOT NULL REFERENCES tache_onboarding(id) ON DELETE CASCADE,
    id_utilisateur_auteur INTEGER REFERENCES utilisateur(id),
    ancien_statut VARCHAR(20), nouveau_statut VARCHAR(20),
    ancienne_echeance DATE, nouvelle_echeance DATE,
    ancienne_remarque TEXT, nouvelle_remarque TEXT,
    date_action TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE historique_affectation (
    id SERIAL PRIMARY KEY,
    id_employe INTEGER NOT NULL REFERENCES employe(id),
    id_departement INTEGER NOT NULL REFERENCES departement(id),
    id_poste INTEGER NOT NULL,
    id_superieur INTEGER REFERENCES employe(id),
    date_debut DATE NOT NULL,
    date_fin DATE,
    motif TEXT,
    CONSTRAINT fk_affectation_poste_dept FOREIGN KEY (id_poste, id_departement)
        REFERENCES poste(id, id_departement),
    CONSTRAINT chk_affectation_dates CHECK (date_fin IS NULL OR date_fin >= date_debut),
    CONSTRAINT chk_superieur_different CHECK (id_superieur IS NULL OR id_superieur <> id_employe),
    CONSTRAINT ex_affectation_periode EXCLUDE USING gist (
        id_employe WITH =, daterange(date_debut, date_fin, '[)') WITH &&)
);

CREATE TABLE journal_audit (
    id BIGSERIAL PRIMARY KEY,
    id_utilisateur INTEGER REFERENCES utilisateur(id),
    action VARCHAR(100) NOT NULL,
    table_concernee VARCHAR(100) NOT NULL,
    identifiant_enregistrement VARCHAR(100),
    anciennes_valeurs JSONB,
    nouvelles_valeurs JSONB,
    adresse_ip INET,
    date_action TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE regime_travail (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL,
    description TEXT,
    heures_hebdomadaires NUMERIC(5,2) CHECK (heures_hebdomadaires IS NULL OR heures_hebdomadaires > 0)
);

CREATE TABLE horaire_travail (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL,
    heure_debut TIME NOT NULL,
    heure_fin TIME NOT NULL,
    debut_pause TIME,
    fin_pause TIME,
    tolerance_retard_minutes INTEGER NOT NULL DEFAULT 0 CHECK (tolerance_retard_minutes >= 0),
    jours_travail SMALLINT[] NOT NULL DEFAULT ARRAY[1,2,3,4,5]::SMALLINT[],
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (heure_fin > heure_debut),
    CHECK ((debut_pause IS NULL AND fin_pause IS NULL)
        OR (debut_pause IS NOT NULL AND fin_pause IS NOT NULL
            AND debut_pause >= heure_debut AND fin_pause > debut_pause AND fin_pause <= heure_fin)),
    CHECK (jours_travail <@ ARRAY[1,2,3,4,5,6,7]::SMALLINT[])
);

CREATE TABLE planning_travail (
    id SERIAL PRIMARY KEY,
    id_employe INTEGER NOT NULL REFERENCES employe(id),
    id_horaire INTEGER NOT NULL REFERENCES horaire_travail(id),
    id_regime INTEGER REFERENCES regime_travail(id),
    date_debut DATE NOT NULL,
    date_fin DATE,
    CHECK (date_fin IS NULL OR date_fin >= date_debut),
    CONSTRAINT ex_planning_periode EXCLUDE USING gist (
        id_employe WITH =, daterange(date_debut, date_fin, '[]') WITH &&)
);

CREATE TABLE pointage (
    id BIGSERIAL PRIMARY KEY,
    id_employe INTEGER NOT NULL REFERENCES employe(id),
    date_heure TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type_pointage VARCHAR(20) NOT NULL
        CHECK (type_pointage IN ('ENTREE', 'SORTIE', 'DEBUT_PAUSE', 'FIN_PAUSE')),
    source VARCHAR(30) NOT NULL DEFAULT 'MANUEL',
    remarque TEXT,
    UNIQUE (id_employe, date_heure, type_pointage)
);

CREATE TABLE temps_travail_journalier (
    id BIGSERIAL PRIMARY KEY,
    id_employe INTEGER NOT NULL REFERENCES employe(id),
    date_travail DATE NOT NULL,
    temps_prevu_minutes INTEGER NOT NULL DEFAULT 0 CHECK (temps_prevu_minutes >= 0),
    temps_reel_minutes INTEGER NOT NULL DEFAULT 0 CHECK (temps_reel_minutes >= 0),
    retard_minutes INTEGER NOT NULL DEFAULT 0 CHECK (retard_minutes >= 0),
    heures_supplementaires_minutes INTEGER NOT NULL DEFAULT 0 CHECK (heures_supplementaires_minutes >= 0),
    statut_heures_sup VARCHAR(10) NOT NULL DEFAULT 'AUCUNE'
        CHECK (statut_heures_sup IN ('AUCUNE', 'A_VALIDER', 'VALIDEE', 'REFUSEE')),
    id_utilisateur_validateur INTEGER REFERENCES utilisateur(id),
    date_validation TIMESTAMPTZ,
    UNIQUE (id_employe, date_travail),
    CHECK ((statut_heures_sup IN ('VALIDEE', 'REFUSEE')) = (id_utilisateur_validateur IS NOT NULL))
);

CREATE TABLE anomalie_pointage (
    id BIGSERIAL PRIMARY KEY,
    id_employe INTEGER NOT NULL REFERENCES employe(id),
    date_anomalie DATE NOT NULL,
    type_anomalie VARCHAR(50) NOT NULL,
    description TEXT,
    statut VARCHAR(20) NOT NULL DEFAULT 'A_TRAITER'
        CHECK (statut IN ('A_TRAITER', 'EN_COURS', 'RESOLUE', 'IGNOREE')),
    justification TEXT,
    date_resolution TIMESTAMPTZ,
    id_utilisateur_responsable INTEGER REFERENCES utilisateur(id),
    UNIQUE (id_employe, date_anomalie, type_anomalie)
);


-- ============================================================
-- 2. TABLES DE CONGES
-- ============================================================

CREATE TABLE type_conge (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL,
    description TEXT,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE regle_conge (
    id SERIAL PRIMARY KEY,
    id_type_conge INTEGER NOT NULL REFERENCES type_conge(id),
    deduction_solde BOOLEAN NOT NULL DEFAULT FALSE,
    justificatif_obligatoire BOOLEAN NOT NULL DEFAULT FALSE,
    remplacant_obligatoire BOOLEAN NOT NULL DEFAULT FALSE,
    acquisition_autorisee BOOLEAN NOT NULL DEFAULT FALSE,
    jours_acquis_mois NUMERIC(6,2) NOT NULL DEFAULT 0,
    -- Convention : plafond_jours = 0 signifie "sans plafond".
    plafond_jours NUMERIC(6,2) NOT NULL DEFAULT 0,
    date_debut_validite DATE NOT NULL DEFAULT CURRENT_DATE,
    date_fin_validite DATE,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_regle_jours_acquis CHECK (jours_acquis_mois >= 0),
    CONSTRAINT ck_regle_plafond CHECK (plafond_jours >= 0),
    CONSTRAINT ck_regle_dates CHECK (
        date_fin_validite IS NULL OR date_fin_validite >= date_debut_validite)
);

CREATE TABLE jour_ferie (
    id SERIAL PRIMARY KEY,
    date_jour DATE NOT NULL UNIQUE,
    libelle VARCHAR(150) NOT NULL,
    description TEXT
);

CREATE TABLE demande_conge (
    id SERIAL PRIMARY KEY,
    id_employe INTEGER NOT NULL REFERENCES employe(id),
    id_type_conge INTEGER NOT NULL REFERENCES type_conge(id),
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    nombre_jours NUMERIC(6,2) NOT NULL,
    motif TEXT,
    date_demande TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut_global VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE_MANAGER',
    date_decision_finale TIMESTAMP,
    CONSTRAINT ck_demande_dates CHECK (date_fin >= date_debut),
    CONSTRAINT ck_demande_nombre_jours CHECK (nombre_jours > 0),
    CONSTRAINT ck_demande_statut CHECK (
        statut_global IN ('EN_ATTENTE_MANAGER', 'EN_ATTENTE_RH', 'VALIDEE', 'REFUSEE', 'ANNULEE'))
);

CREATE TABLE validation_conge (
    id SERIAL PRIMARY KEY,
    id_demande_conge INTEGER NOT NULL REFERENCES demande_conge(id),
    id_utilisateur INTEGER REFERENCES utilisateur(id),
    ordre SMALLINT NOT NULL,
    id_decision INTEGER NOT NULL REFERENCES decision(id),
    commentaire TEXT,
    date_validation TIMESTAMP,
    CONSTRAINT ck_validation_conge_ordre CHECK (ordre IN (1, 2)),
    CONSTRAINT uq_validation_conge_ordre UNIQUE (id_demande_conge, ordre)
);

CREATE TABLE remplacement_conge (
    id SERIAL PRIMARY KEY,
    id_demande_conge INTEGER NOT NULL UNIQUE REFERENCES demande_conge(id),
    id_employe_remplacant INTEGER NOT NULL REFERENCES employe(id),
    date_proposition TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decision VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    commentaire TEXT,
    date_decision TIMESTAMP,
    CONSTRAINT ck_remplacement_decision CHECK (decision IN ('EN_ATTENTE', 'ACCEPTE', 'REFUSE'))
);

CREATE TABLE mouvement_conge (
    id SERIAL PRIMARY KEY,
    id_employe INTEGER NOT NULL REFERENCES employe(id),
    id_type_conge INTEGER NOT NULL REFERENCES type_conge(id),
    id_demande_conge INTEGER REFERENCES demande_conge(id),
    id_utilisateur INTEGER REFERENCES utilisateur(id),
    date_mouvement TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type_mouvement VARCHAR(30) NOT NULL,
    nombre_jours NUMERIC(6,2) NOT NULL,
    periode_debut DATE,
    motif TEXT,
    CONSTRAINT ck_mouvement_type CHECK (type_mouvement IN (
        'ACQUISITION', 'CONSOMMATION', 'AJUSTEMENT', 'RESTITUTION', 'EXPIRATION', 'ANNULATION')),
    CONSTRAINT ck_mouvement_nombre CHECK (
        (type_mouvement = 'ANNULATION' AND nombre_jours = 0)
        OR (type_mouvement <> 'ANNULATION' AND nombre_jours <> 0)),
    CONSTRAINT ck_mouvement_signe CHECK (
        (type_mouvement = 'ACQUISITION' AND nombre_jours > 0)
        OR (type_mouvement = 'CONSOMMATION' AND nombre_jours < 0)
        OR (type_mouvement = 'RESTITUTION' AND nombre_jours > 0)
        OR (type_mouvement = 'AJUSTEMENT')
        OR (type_mouvement = 'ANNULATION' AND nombre_jours = 0)
        OR (type_mouvement = 'EXPIRATION' AND nombre_jours < 0))
);

CREATE TABLE type_absence (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL,
    justificatif_obligatoire BOOLEAN NOT NULL DEFAULT FALSE,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE absence (
    id SERIAL PRIMARY KEY,
    id_employe INTEGER NOT NULL REFERENCES employe(id),
    id_type_absence INTEGER NOT NULL REFERENCES type_absence(id),
    date_debut TIMESTAMP NOT NULL,
    date_fin TIMESTAMP NOT NULL,
    motif TEXT,
    statut VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',
    date_declaration TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_utilisateur_verificateur INTEGER REFERENCES utilisateur(id),
    commentaire_rh TEXT,
    CONSTRAINT ck_absence_dates CHECK (date_fin >= date_debut),
    CONSTRAINT ck_absence_statut CHECK (
        statut IN ('EN_ATTENTE', 'JUSTIFIEE', 'NON_JUSTIFIEE', 'REFUSEE'))
);

CREATE TABLE justificatif_absence (
    id SERIAL PRIMARY KEY,
    id_absence INTEGER NOT NULL REFERENCES absence(id),
    nom_fichier VARCHAR(255) NOT NULL,
    type_mime VARCHAR(100),
    contenu BYTEA NOT NULL,
    date_depot TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_utilisateur_verificateur INTEGER REFERENCES utilisateur(id),
    date_verification TIMESTAMP,
    statut_verification VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',
    commentaire TEXT,
    CONSTRAINT ck_justificatif_statut CHECK (statut_verification IN ('EN_ATTENTE', 'ACCEPTE', 'REFUSE'))
);

CREATE TABLE mission (
    id SERIAL PRIMARY KEY,
    reference VARCHAR(100) NOT NULL UNIQUE,
    objet VARCHAR(255) NOT NULL,
    description TEXT,
    lieu VARCHAR(255),
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    statut VARCHAR(30) NOT NULL DEFAULT 'PLANIFIEE',
    id_utilisateur_createur INTEGER REFERENCES utilisateur(id),
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_mission_dates CHECK (date_fin >= date_debut),
    CONSTRAINT ck_mission_statut CHECK (statut IN ('PLANIFIEE', 'EN_COURS', 'TERMINEE', 'ANNULEE'))
);

CREATE TABLE mission_employe (
    id SERIAL PRIMARY KEY,
    id_mission INTEGER NOT NULL REFERENCES mission(id),
    id_employe INTEGER NOT NULL REFERENCES employe(id),
    role_mission VARCHAR(100),
    commentaire TEXT,
    CONSTRAINT uq_mission_employe UNIQUE (id_mission, id_employe)
);

CREATE TABLE indemnite_conge (
    id SERIAL PRIMARY KEY,
    id_employe INTEGER NOT NULL REFERENCES employe(id),
    date_calcul TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    solde_jours NUMERIC(6,2) NOT NULL,
    montant_journalier NUMERIC(14,2) NOT NULL,
    montant_total NUMERIC(14,2) NOT NULL,
    methode_calcul VARCHAR(100),
    statut VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',
    id_utilisateur INTEGER REFERENCES utilisateur(id),
    commentaire TEXT,
    CONSTRAINT ck_indemnite_jours CHECK (solde_jours >= 0),
    CONSTRAINT ck_indemnite_montants CHECK (montant_journalier >= 0 AND montant_total >= 0),
    CONSTRAINT ck_indemnite_statut CHECK (statut IN ('EN_ATTENTE', 'VALIDEE', 'PAYEE', 'ANNULEE'))
);


-- ============================================================
-- 3. INDEX
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_historique_tache_date ON historique_tache_onboarding(id_tache, date_action DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_affectation_courante ON historique_affectation(id_employe) WHERE date_fin IS NULL;
CREATE INDEX IF NOT EXISTS idx_affectation_employe_dates ON historique_affectation(id_employe, date_debut DESC);
CREATE INDEX IF NOT EXISTS idx_affectation_superieur ON historique_affectation(id_superieur) WHERE date_fin IS NULL;
CREATE INDEX IF NOT EXISTS idx_audit_utilisateur_date ON journal_audit(id_utilisateur, date_action DESC);
CREATE INDEX IF NOT EXISTS idx_audit_table_enregistrement ON journal_audit(table_concernee, identifiant_enregistrement);
CREATE INDEX IF NOT EXISTS idx_planning_employe_dates ON planning_travail(id_employe, date_debut, date_fin);
CREATE INDEX IF NOT EXISTS idx_pointage_employe_date ON pointage(id_employe, date_heure DESC);
CREATE INDEX IF NOT EXISTS idx_temps_journalier_date ON temps_travail_journalier(date_travail, id_employe);
CREATE INDEX IF NOT EXISTS idx_anomalie_employe_statut ON anomalie_pointage(id_employe, statut, date_anomalie DESC);
CREATE INDEX IF NOT EXISTS idx_regle_conge_type ON regle_conge(id_type_conge);
CREATE INDEX IF NOT EXISTS idx_demande_conge_employe ON demande_conge(id_employe);
CREATE INDEX IF NOT EXISTS idx_demande_conge_type ON demande_conge(id_type_conge);
CREATE INDEX IF NOT EXISTS idx_demande_conge_statut ON demande_conge(statut_global);
CREATE INDEX IF NOT EXISTS idx_demande_conge_dates ON demande_conge(date_debut, date_fin);
CREATE INDEX IF NOT EXISTS idx_validation_conge_demande ON validation_conge(id_demande_conge);
CREATE INDEX IF NOT EXISTS idx_validation_conge_utilisateur ON validation_conge(id_utilisateur);
CREATE INDEX IF NOT EXISTS idx_remplacement_employe ON remplacement_conge(id_employe_remplacant);
CREATE INDEX IF NOT EXISTS idx_mouvement_employe ON mouvement_conge(id_employe);
CREATE INDEX IF NOT EXISTS idx_mouvement_type ON mouvement_conge(id_type_conge);
CREATE INDEX IF NOT EXISTS idx_mouvement_date ON mouvement_conge(date_mouvement);
CREATE INDEX IF NOT EXISTS idx_mouvement_demande ON mouvement_conge(id_demande_conge);
CREATE INDEX IF NOT EXISTS idx_absence_employe ON absence(id_employe);
CREATE INDEX IF NOT EXISTS idx_absence_dates ON absence(date_debut, date_fin);
CREATE INDEX IF NOT EXISTS idx_absence_statut ON absence(statut);
CREATE INDEX IF NOT EXISTS idx_justificatif_absence ON justificatif_absence(id_absence);
CREATE INDEX IF NOT EXISTS idx_mission_employe_employe ON mission_employe(id_employe);
CREATE INDEX IF NOT EXISTS idx_indemnite_employe ON indemnite_conge(id_employe);

-- Index uniques sur les mouvements de conges
CREATE UNIQUE INDEX IF NOT EXISTS uq_mouvement_annulation_demande
    ON mouvement_conge (id_demande_conge)
    WHERE type_mouvement = 'ANNULATION' AND id_demande_conge IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_mouvement_conso_demande
    ON mouvement_conge (id_demande_conge)
    WHERE type_mouvement = 'CONSOMMATION' AND id_demande_conge IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_mouvement_restitution_demande
    ON mouvement_conge (id_demande_conge)
    WHERE type_mouvement = 'RESTITUTION' AND id_demande_conge IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_acquisition_mensuelle
    ON mouvement_conge (id_employe, id_type_conge, periode_debut)
    WHERE type_mouvement = 'ACQUISITION';


-- ============================================================
-- 4. DONNEES INITIALES
-- ============================================================

INSERT INTO type_conge(code, libelle, description) VALUES
    ('ANNUEL', 'Congé annuel', 'Congé annuel acquis progressivement par le salarié.'),
    ('MALADIE', 'Congé maladie', 'Congé accordé en raison d''un problème de santé.'),
    ('EXCEPTIONNEL', 'Congé exceptionnel', 'Congé accordé pour un événement particulier.'),
    ('SANS_SOLDE', 'Congé sans solde', 'Congé qui n''est pas déduit du solde de congés.')
ON CONFLICT (code) DO NOTHING;

-- date_debut_validite fixee a une date ancienne : avec DEFAULT CURRENT_DATE,
-- l'acquisition d'une periode passee ne trouverait aucune regle valide.
INSERT INTO regle_conge (id_type_conge, deduction_solde, justificatif_obligatoire, remplacant_obligatoire,
                         acquisition_autorisee, jours_acquis_mois, plafond_jours, date_debut_validite)
SELECT t.id, r.deduction_solde, r.justificatif_obligatoire, r.remplacant_obligatoire,
       r.acquisition_autorisee, r.jours_acquis_mois, r.plafond_jours, DATE '2000-01-01'
FROM (VALUES
    ('ANNUEL',       TRUE,  FALSE, TRUE,  TRUE,  2.5, 90),
    ('MALADIE',      FALSE, TRUE,  FALSE, FALSE, 0,   0),
    ('EXCEPTIONNEL', FALSE, FALSE, FALSE, FALSE, 0,   0),
    ('SANS_SOLDE',   FALSE, FALSE, FALSE, FALSE, 0,   0)
) AS r(code, deduction_solde, justificatif_obligatoire, remplacant_obligatoire,
       acquisition_autorisee, jours_acquis_mois, plafond_jours)
JOIN type_conge t ON t.code = r.code
WHERE NOT EXISTS (SELECT 1 FROM regle_conge rc WHERE rc.id_type_conge = t.id AND rc.actif = TRUE);

INSERT INTO regime_travail (code, libelle, heures_hebdomadaires) VALUES
    ('TEMPS_PLEIN', 'Temps plein', 40),
    ('TEMPS_PARTIEL', 'Temps partiel', 20)
ON CONFLICT (code) DO NOTHING;

INSERT INTO horaire_travail (code, libelle, heure_debut, heure_fin, debut_pause, fin_pause,
                             tolerance_retard_minutes, jours_travail) VALUES
    ('STANDARD', 'Standard 08h-17h (pause 12h-13h)', '08:00', '17:00', '12:00', '13:00',
     10, ARRAY[1,2,3,4,5]::SMALLINT[])
ON CONFLICT (code) DO NOTHING;

-- Jours feries fixes uniquement (les jours mobiles : Lundi de Paques,
-- Ascension, Lundi de Pentecote... sont a ajouter chaque annee).
INSERT INTO jour_ferie (date_jour, libelle)
SELECT make_date(g.y, v.m, v.d), v.l
FROM generate_series(2026, 2030) AS g(y),
     (VALUES (1, 1, 'Jour de l''An'), (3, 29, 'Commémoration de 1947'),
             (5, 1, 'Fête du Travail'), (6, 26, 'Fête de l''Indépendance'),
             (8, 15, 'Assomption'), (11, 1, 'Toussaint'), (12, 25, 'Noël')) AS v(m, d, l)
ON CONFLICT DO NOTHING;

INSERT INTO role (code, libelle) VALUES ('EMPLOYE', 'Employé')
ON CONFLICT (code) DO NOTHING;

DO $$
BEGIN
    IF (SELECT COUNT(*) FROM role WHERE code = 'EMPLOYE') <> 1 THEN
        RAISE EXCEPTION 'Le role EMPLOYE est absent ou duplique.';
    END IF;
END
$$;


-- ============================================================
-- 5. FONCTIONS RH : affectation, audit, pointage et temps de travail
-- ============================================================

CREATE OR REPLACE FUNCTION verifier_hierarchie() RETURNS trigger AS $$
BEGIN
    IF NEW.id_superieur IS NULL OR NEW.date_fin IS NOT NULL THEN RETURN NEW; END IF;
    IF NOT EXISTS (SELECT 1 FROM employe WHERE id = NEW.id_superieur AND statut <> 'PARTI') THEN
        RAISE EXCEPTION 'Le supérieur hiérarchique doit être un employé encore en poste.';
    END IF;
    IF EXISTS (
        WITH RECURSIVE chaine(id, niveau) AS (
            SELECT NEW.id_superieur, 1
            UNION ALL
            SELECT h.id_superieur, c.niveau + 1
            FROM chaine c JOIN historique_affectation h
              ON h.id_employe = c.id AND h.date_fin IS NULL
            WHERE h.id_superieur IS NOT NULL AND c.niveau < 50)
        SELECT 1 FROM chaine WHERE id = NEW.id_employe
    ) THEN
        RAISE EXCEPTION 'Boucle hiérarchique détectée : un employé ne peut pas être son propre supérieur indirect.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_verifier_hierarchie
BEFORE INSERT OR UPDATE ON historique_affectation
FOR EACH ROW EXECUTE FUNCTION verifier_hierarchie();

CREATE OR REPLACE FUNCTION manager_employe_du_departement(p_id_departement INTEGER, p_exclure INTEGER)
RETURNS INTEGER AS $$
    SELECT e.id FROM departement d JOIN employe e ON e.id_utilisateur = d.id_utilisateur_manager
    WHERE d.id = p_id_departement AND e.id <> COALESCE(p_exclure, -1) AND e.statut <> 'PARTI'
    LIMIT 1;
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION ouvrir_affectation(p_id_employe INTEGER, p_id_departement INTEGER, p_id_poste INTEGER,
                                   p_id_superieur INTEGER, p_date DATE, p_motif TEXT)
RETURNS VOID AS $$
BEGIN
    UPDATE historique_affectation SET date_fin = GREATEST(p_date, date_debut)
    WHERE id_employe = p_id_employe AND date_fin IS NULL;
    INSERT INTO historique_affectation
        (id_employe, id_departement, id_poste, id_superieur, date_debut, motif)
    VALUES (p_id_employe, p_id_departement, p_id_poste, p_id_superieur, p_date, p_motif);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION changer_affectation(p_id_employe INTEGER, p_id_departement INTEGER, p_id_poste INTEGER,
                                    p_id_superieur INTEGER DEFAULT NULL, p_date DATE DEFAULT CURRENT_DATE,
                                    p_motif TEXT DEFAULT NULL)
RETURNS VOID AS $$
DECLARE e employe%ROWTYPE; h historique_affectation%ROWTYPE; v_sup INTEGER;
BEGIN
    SELECT * INTO e FROM employe WHERE id = p_id_employe FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Employé introuvable.'; END IF;
    IF e.statut = 'PARTI' THEN RAISE EXCEPTION 'Un employé parti ne peut plus être réaffecté.'; END IF;
    SELECT * INTO h FROM historique_affectation WHERE id_employe = p_id_employe AND date_fin IS NULL;
    v_sup := COALESCE(p_id_superieur,
                      CASE WHEN p_id_departement = e.id_departement THEN h.id_superieur
                           ELSE manager_employe_du_departement(p_id_departement, p_id_employe) END);
    PERFORM set_config('app.affectation_en_cours', '1', TRUE);
    UPDATE employe SET id_departement = p_id_departement, id_poste = p_id_poste WHERE id = p_id_employe;
    PERFORM ouvrir_affectation(p_id_employe, p_id_departement, p_id_poste, v_sup, p_date,
                               COALESCE(p_motif, 'Changement d''affectation'));
    PERFORM set_config('app.affectation_en_cours', '0', TRUE);
END;
$$ LANGUAGE plpgsql;

CREATE VIEW v_organigramme AS
SELECT e.id AS id_employe, e.matricule, e.nom, e.prenom,
       d.nom AS departement, p.nom AS poste,
       h.id_superieur, s.nom AS nom_superieur, s.prenom AS prenom_superieur,
       (SELECT COUNT(*) FROM historique_affectation x
         WHERE x.id_superieur = e.id AND x.date_fin IS NULL) AS nb_subordonnes_directs
FROM employe e
JOIN historique_affectation h ON h.id_employe = e.id AND h.date_fin IS NULL
JOIN departement d ON d.id = h.id_departement
JOIN poste p ON p.id = h.id_poste
LEFT JOIN employe s ON s.id = h.id_superieur
WHERE e.statut <> 'PARTI';

CREATE OR REPLACE FUNCTION lister_subordonnes(p_id_employe INTEGER, p_directs_seulement BOOLEAN DEFAULT FALSE)
RETURNS TABLE (sub_id INTEGER, sub_nom VARCHAR, sub_prenom VARCHAR, sub_superieur INTEGER, sub_niveau INTEGER) AS $$
    WITH RECURSIVE s(id_emp, id_sup, niveau) AS (
        SELECT h.id_employe, h.id_superieur, 1
        FROM historique_affectation h WHERE h.id_superieur = p_id_employe AND h.date_fin IS NULL
        UNION ALL
        SELECT h.id_employe, h.id_superieur, s.niveau + 1
        FROM s JOIN historique_affectation h ON h.id_superieur = s.id_emp AND h.date_fin IS NULL
        WHERE NOT p_directs_seulement AND s.niveau < 20)
    SELECT e.id, e.nom, e.prenom, s.id_sup, s.niveau
    FROM s JOIN employe e ON e.id = s.id_emp
    ORDER BY s.niveau, e.nom;
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION lister_superieurs(p_id_employe INTEGER)
RETURNS TABLE (sup_id INTEGER, sup_nom VARCHAR, sup_prenom VARCHAR, sup_niveau INTEGER) AS $$
    WITH RECURSIVE c(id_emp, niveau) AS (
        SELECT h.id_superieur, 1 FROM historique_affectation h
        WHERE h.id_employe = p_id_employe AND h.date_fin IS NULL AND h.id_superieur IS NOT NULL
        UNION ALL
        SELECT h.id_superieur, c.niveau + 1
        FROM c JOIN historique_affectation h ON h.id_employe = c.id_emp AND h.date_fin IS NULL
        WHERE h.id_superieur IS NOT NULL AND c.niveau < 20)
    SELECT e.id, e.nom, e.prenom, c.niveau FROM c JOIN employe e ON e.id = c.id_emp ORDER BY c.niveau;
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION generer_alertes_documents(p_frequence_jours INTEGER DEFAULT 7) RETURNS INTEGER AS $$
DECLARE v_nb INTEGER;
BEGIN
    WITH cible AS (
        SELECT d.id AS id_doc, e.nom, e.prenom, d.type_document, d.date_expiration,
               CASE WHEN u.actif IS TRUE THEN e.id_utilisateur END AS id_perso
        FROM document_employe d
        JOIN employe e ON e.id = d.id_employe AND e.statut <> 'PARTI'
        LEFT JOIN utilisateur u ON u.id = e.id_utilisateur
        WHERE d.date_expiration IS NOT NULL AND d.date_renouvellement IS NULL
          AND d.statut <> 'REFUSE'
          AND CURRENT_DATE >= d.date_expiration - d.delai_alerte_jours),
    dest AS (
        SELECT c.*, c.id_perso AS id_dest FROM cible c WHERE c.id_perso IS NOT NULL
        UNION ALL
        SELECT c.*, u.id FROM cible c
        JOIN utilisateur u ON u.actif JOIN role r ON r.id = u.id_role AND r.code = 'RH'
        WHERE c.id_perso IS NULL)
    INSERT INTO notification (id_utilisateur, id_document_employe, type_notification, titre, message)
    SELECT x.id_dest, x.id_doc, 'ALERTE_DOCUMENT',
           CASE WHEN x.date_expiration < CURRENT_DATE THEN 'Document expiré' ELSE 'Document bientôt expiré' END,
           format('Le document %s de %s %s %s le %s : renouvellement à prévoir.',
                  x.type_document, x.prenom, x.nom,
                  CASE WHEN x.date_expiration < CURRENT_DATE THEN 'a expiré' ELSE 'expire' END,
                  to_char(x.date_expiration, 'DD/MM/YYYY'))
    FROM dest x
    WHERE NOT EXISTS (
        SELECT 1 FROM notification n
        WHERE n.id_utilisateur = x.id_dest AND n.id_document_employe = x.id_doc
          AND n.type_notification = 'ALERTE_DOCUMENT'
          AND n.date_creation > CURRENT_TIMESTAMP - make_interval(days => p_frequence_jours));
    GET DIAGNOSTICS v_nb = ROW_COUNT;
    RETURN v_nb;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION auditer_modification() RETURNS trigger AS $$
DECLARE v_new JSONB; v_old JSONB;
BEGIN
    IF TG_OP <> 'DELETE' THEN v_new := to_jsonb(NEW) - 'bytes' - 'contenu' - 'mdp'; END IF;
    IF TG_OP <> 'INSERT' THEN v_old := to_jsonb(OLD) - 'bytes' - 'contenu' - 'mdp'; END IF;
    INSERT INTO journal_audit (id_utilisateur, action, table_concernee, identifiant_enregistrement,
                               anciennes_valeurs, nouvelles_valeurs, adresse_ip)
    VALUES (NULLIF(current_setting('app.user_id', TRUE), '')::INTEGER, TG_OP, TG_TABLE_NAME,
            COALESCE(v_new ->> 'id', v_old ->> 'id'), v_old, v_new,
            NULLIF(current_setting('app.client_ip', TRUE), '')::INET);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION interdire_modification_journal() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Le journal d''audit est en lecture seule.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_journal_audit_lecture_seule
BEFORE UPDATE OR DELETE ON journal_audit
FOR EACH ROW EXECUTE FUNCTION interdire_modification_journal();

DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['utilisateur', 'employe', 'contrat', 'document_employe',
        'historique_affectation', 'tache_onboarding', 'besoin', 'validation_besoin',
        'candidature', 'decision_recrutement', 'demande_conge', 'validation_conge', 'regle_conge',
        'mouvement_conge', 'absence', 'justificatif_absence',
        'mission', 'mission_employe', 'remplacement_conge',
        'temps_travail_journalier', 'indemnite_conge']
    LOOP
        EXECUTE format('CREATE TRIGGER trg_audit_%1$s AFTER INSERT OR UPDATE OR DELETE ON %1$I
                        FOR EACH ROW EXECUTE FUNCTION auditer_modification()', t);
    END LOOP;
END $$;

CREATE OR REPLACE FUNCTION calculer_temps_journalier(p_id_employe INTEGER, p_date DATE) RETURNS VOID AS $$
DECLARE
    c_tz CONSTANT TEXT := 'Indian/Antananarivo';
    h horaire_travail%ROWTYPE;
    r RECORD;
    v_deb TIMESTAMPTZ := p_date::TIMESTAMP AT TIME ZONE 'Indian/Antananarivo';
    v_fin TIMESTAMPTZ := (p_date + 1)::TIMESTAMP AT TIME ZONE 'Indian/Antananarivo';
    v_prevu INTEGER := 0; v_reel INTEGER := 0; v_retard INTEGER := 0; v_sup INTEGER := 0;
    v_nb INTEGER := 0; v_prem TIME; v_prev VARCHAR(20); v_prev_ts TIMESTAMPTZ;
    v_seq_ok BOOLEAN := TRUE; v_hors_service BOOLEAN;
    v_aujourdhui DATE := (CURRENT_TIMESTAMP AT TIME ZONE 'Indian/Antananarivo')::DATE;
    v_ancien temps_travail_journalier%ROWTYPE; v_statut VARCHAR(10); v_valid INTEGER; v_date_valid TIMESTAMPTZ;
BEGIN
    SELECT ht.* INTO h FROM planning_travail pl JOIN horaire_travail ht ON ht.id = pl.id_horaire
    WHERE pl.id_employe = p_id_employe AND p_date BETWEEN pl.date_debut AND COALESCE(pl.date_fin, DATE 'infinity');

    -- Jour non travaille : absence (hors REFUSEE), conge valide, mission ou jour ferie.
    -- absence.date_debut/date_fin sont des TIMESTAMP : comparaison faite sur la partie date.
    -- mission n'a pas de colonne id_employe : on passe par mission_employe.
    v_hors_service :=
        EXISTS (SELECT 1 FROM absence a
                WHERE a.id_employe = p_id_employe AND a.statut <> 'REFUSEE'
                  AND a.date_debut::DATE <= p_date AND a.date_fin::DATE >= p_date)
        OR EXISTS (SELECT 1 FROM demande_conge d
                   WHERE d.id_employe = p_id_employe AND d.statut_global = 'VALIDEE'
                     AND p_date BETWEEN d.date_debut AND d.date_fin)
        OR EXISTS (SELECT 1 FROM mission_employe me JOIN mission m ON m.id = me.id_mission
                   WHERE me.id_employe = p_id_employe
                     AND m.statut IN ('PLANIFIEE', 'EN_COURS', 'TERMINEE')
                     AND p_date BETWEEN m.date_debut AND m.date_fin)
        OR EXISTS (SELECT 1 FROM jour_ferie f WHERE f.date_jour = p_date);

    IF h.id IS NOT NULL AND EXTRACT(ISODOW FROM p_date)::SMALLINT = ANY (h.jours_travail) AND NOT v_hors_service THEN
        v_prevu := (EXTRACT(EPOCH FROM (h.heure_fin - h.heure_debut)) / 60)::INTEGER
                   - COALESCE((EXTRACT(EPOCH FROM (h.fin_pause - h.debut_pause)) / 60)::INTEGER, 0);
    END IF;

    FOR r IN SELECT type_pointage, date_heure FROM pointage
             WHERE id_employe = p_id_employe AND date_heure >= v_deb AND date_heure < v_fin
             ORDER BY date_heure, id
    LOOP
        v_nb := v_nb + 1;
        IF v_prev IS NULL THEN
            IF r.type_pointage <> 'ENTREE' THEN v_seq_ok := FALSE; END IF;
        ELSE
            IF NOT ((v_prev = 'ENTREE' AND r.type_pointage IN ('DEBUT_PAUSE', 'SORTIE'))
                 OR (v_prev = 'DEBUT_PAUSE' AND r.type_pointage = 'FIN_PAUSE')
                 OR (v_prev = 'FIN_PAUSE' AND r.type_pointage IN ('DEBUT_PAUSE', 'SORTIE'))
                 OR (v_prev = 'SORTIE' AND r.type_pointage = 'ENTREE')) THEN
                v_seq_ok := FALSE;
            END IF;
            IF v_prev IN ('ENTREE', 'FIN_PAUSE') AND r.type_pointage IN ('DEBUT_PAUSE', 'SORTIE') THEN
                v_reel := v_reel + (EXTRACT(EPOCH FROM (r.date_heure - v_prev_ts)) / 60)::INTEGER;
            END IF;
        END IF;
        IF r.type_pointage = 'ENTREE' AND v_prem IS NULL THEN
            v_prem := (r.date_heure AT TIME ZONE c_tz)::TIME;
        END IF;
        v_prev := r.type_pointage; v_prev_ts := r.date_heure;
    END LOOP;

    IF v_prevu > 0 AND v_prem IS NOT NULL THEN
        v_retard := GREATEST(0, (EXTRACT(EPOCH FROM (v_prem - h.heure_debut)) / 60)::INTEGER
                                 - h.tolerance_retard_minutes);
    END IF;
    v_sup := GREATEST(0, v_reel - v_prevu);

    SELECT * INTO v_ancien FROM temps_travail_journalier WHERE id_employe = p_id_employe AND date_travail = p_date;
    IF v_sup = 0 THEN
        v_statut := 'AUCUNE';
    ELSIF v_ancien.id IS NOT NULL AND v_ancien.heures_supplementaires_minutes = v_sup
          AND v_ancien.statut_heures_sup IN ('VALIDEE', 'REFUSEE') THEN
        v_statut := v_ancien.statut_heures_sup;
        v_valid := v_ancien.id_utilisateur_validateur; v_date_valid := v_ancien.date_validation;
    ELSE
        v_statut := 'A_VALIDER';
    END IF;

    INSERT INTO temps_travail_journalier
        (id_employe, date_travail, temps_prevu_minutes, temps_reel_minutes, retard_minutes,
         heures_supplementaires_minutes, statut_heures_sup, id_utilisateur_validateur, date_validation)
    VALUES (p_id_employe, p_date, v_prevu, v_reel, v_retard, v_sup, v_statut, v_valid, v_date_valid)
    ON CONFLICT (id_employe, date_travail) DO UPDATE SET
        temps_prevu_minutes = EXCLUDED.temps_prevu_minutes, temps_reel_minutes = EXCLUDED.temps_reel_minutes,
        retard_minutes = EXCLUDED.retard_minutes,
        heures_supplementaires_minutes = EXCLUDED.heures_supplementaires_minutes,
        statut_heures_sup = EXCLUDED.statut_heures_sup,
        id_utilisateur_validateur = EXCLUDED.id_utilisateur_validateur,
        date_validation = EXCLUDED.date_validation;

    IF v_retard > 0 THEN
        INSERT INTO anomalie_pointage (id_employe, date_anomalie, type_anomalie, description)
        VALUES (p_id_employe, p_date, 'RETARD', format('Retard de %s minute(s).', v_retard))
        ON CONFLICT DO NOTHING;
    END IF;
    IF v_prevu > 0 AND v_nb = 0 AND p_date < v_aujourdhui THEN
        INSERT INTO anomalie_pointage (id_employe, date_anomalie, type_anomalie, description)
        VALUES (p_id_employe, p_date, 'JOUR_NON_POINTE', 'Aucun pointage sur un jour de travail sans absence ni mission.')
        ON CONFLICT DO NOTHING;
    END IF;
    IF v_nb > 0 AND v_prev IN ('ENTREE', 'DEBUT_PAUSE', 'FIN_PAUSE') AND p_date < v_aujourdhui THEN
        INSERT INTO anomalie_pointage (id_employe, date_anomalie, type_anomalie, description)
        VALUES (p_id_employe, p_date, 'SORTIE_MANQUANTE', 'La journée ne se termine pas par une sortie.')
        ON CONFLICT DO NOTHING;
    END IF;
    IF NOT v_seq_ok THEN
        INSERT INTO anomalie_pointage (id_employe, date_anomalie, type_anomalie, description)
        VALUES (p_id_employe, p_date, 'SEQUENCE_INVALIDE', 'Ordre des pointages incohérent.')
        ON CONFLICT DO NOTHING;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION calculer_temps_periode(p_debut DATE, p_fin DATE) RETURNS INTEGER AS $$
DECLARE r RECORD; v_nb INTEGER := 0;
BEGIN
    FOR r IN SELECT e.id AS id_emp, g::DATE AS jour
             FROM employe e
             CROSS JOIN generate_series(p_debut::TIMESTAMP, p_fin::TIMESTAMP, INTERVAL '1 day') g
             WHERE e.statut = 'ACTIF' AND g::DATE >= e.date_embauche
    LOOP
        PERFORM calculer_temps_journalier(r.id_emp, r.jour);
        v_nb := v_nb + 1;
    END LOOP;
    RETURN v_nb;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION valider_heures_supplementaires(p_id BIGINT, p_id_utilisateur INTEGER, p_decision VARCHAR)
RETURNS VOID AS $$
BEGIN
    IF p_decision NOT IN ('VALIDEE', 'REFUSEE') THEN
        RAISE EXCEPTION 'Décision invalide : VALIDEE ou REFUSEE attendu.';
    END IF;
    UPDATE temps_travail_journalier
    SET statut_heures_sup = p_decision, id_utilisateur_validateur = p_id_utilisateur,
        date_validation = CURRENT_TIMESTAMP
    WHERE id = p_id AND statut_heures_sup = 'A_VALIDER';
    IF NOT FOUND THEN RAISE EXCEPTION 'Aucune heure supplémentaire à valider pour cet enregistrement.'; END IF;
END;
$$ LANGUAGE plpgsql;

CREATE VIEW v_variables_paie_mensuelles AS
SELECT e.id AS id_employe, e.matricule, e.nom, e.prenom,
       EXTRACT(YEAR FROM t.date_travail)::INTEGER AS annee,
       EXTRACT(MONTH FROM t.date_travail)::INTEGER AS mois,
       SUM(t.temps_prevu_minutes) AS minutes_prevues,
       SUM(t.temps_reel_minutes) AS minutes_travaillees,
       SUM(t.retard_minutes) AS minutes_retard,
       COUNT(*) FILTER (WHERE t.retard_minutes > 0) AS nb_retards,
       COALESCE(SUM(t.heures_supplementaires_minutes) FILTER (WHERE t.statut_heures_sup = 'VALIDEE'), 0) AS minutes_sup_validees,
       COALESCE(SUM(t.heures_supplementaires_minutes) FILTER (WHERE t.statut_heures_sup = 'A_VALIDER'), 0) AS minutes_sup_a_valider
FROM temps_travail_journalier t JOIN employe e ON e.id = t.id_employe
GROUP BY e.id, e.matricule, e.nom, e.prenom, 5, 6;


-- ============================================================
-- 6. FONCTIONS DE GESTION DES CONGES
-- ============================================================

CREATE OR REPLACE FUNCTION fn_solde_conge(p_id_employe INTEGER, p_id_type_conge INTEGER)
RETURNS NUMERIC(6,2)
LANGUAGE SQL STABLE
AS $$
    SELECT COALESCE(SUM(nombre_jours), 0)::NUMERIC(6,2)
    FROM mouvement_conge
    WHERE id_employe = p_id_employe
      AND id_type_conge = p_id_type_conge;
$$;

-- Jours ouvres (lundi-vendredi) hors jours feries.
CREATE OR REPLACE FUNCTION fn_calculer_jours_conge(p_date_debut DATE, p_date_fin DATE)
RETURNS INTEGER
LANGUAGE SQL STABLE
AS $$
    SELECT COUNT(*)::INTEGER
    FROM generate_series(p_date_debut::TIMESTAMP, p_date_fin::TIMESTAMP, INTERVAL '1 day') AS calendrier(jour)
    WHERE EXTRACT(ISODOW FROM calendrier.jour) BETWEEN 1 AND 5
      AND NOT EXISTS (
          SELECT 1 FROM jour_ferie jf WHERE jf.date_jour = calendrier.jour::DATE
      );
$$;

CREATE OR REPLACE PROCEDURE sp_acquerir_conges_mensuels(p_periode DATE)
LANGUAGE plpgsql
AS $$
DECLARE
    v_employe RECORD;
    v_regle RECORD;
    v_periode DATE;
    v_solde NUMERIC(6,2);
    v_acquisition NUMERIC(6,2);
BEGIN
    IF p_periode IS NULL THEN
        RAISE EXCEPTION 'La période est obligatoire';
    END IF;

    v_periode := DATE_TRUNC('month', p_periode)::DATE;

    FOR v_employe IN
        SELECT id FROM employe
        WHERE statut = 'ACTIF' AND date_embauche <= v_periode
        ORDER BY id
        FOR UPDATE
    LOOP
        FOR v_regle IN
            SELECT DISTINCT ON (rc.id_type_conge)
                   rc.id AS id_regle, rc.id_type_conge, rc.jours_acquis_mois, rc.plafond_jours
            FROM regle_conge rc
            JOIN type_conge tc ON tc.id = rc.id_type_conge
            WHERE tc.actif = TRUE AND rc.actif = TRUE
              AND rc.acquisition_autorisee = TRUE
              AND rc.jours_acquis_mois > 0
              AND rc.date_debut_validite <= v_periode
              AND (rc.date_fin_validite IS NULL OR rc.date_fin_validite >= v_periode)
            ORDER BY rc.id_type_conge, rc.date_debut_validite DESC, rc.id DESC
        LOOP
            v_solde := fn_solde_conge(v_employe.id, v_regle.id_type_conge);

            IF v_regle.plafond_jours = 0 THEN
                v_acquisition := v_regle.jours_acquis_mois;
            ELSE
                v_acquisition := LEAST(v_regle.jours_acquis_mois,
                                       GREATEST(v_regle.plafond_jours - v_solde, 0));
            END IF;

            IF v_acquisition > 0 THEN
                INSERT INTO mouvement_conge (id_employe, id_type_conge, type_mouvement,
                                             nombre_jours, periode_debut, motif)
                VALUES (v_employe.id, v_regle.id_type_conge, 'ACQUISITION', v_acquisition, v_periode,
                        'Acquisition mensuelle automatique - règle ' || v_regle.id_regle)
                ON CONFLICT (id_employe, id_type_conge, periode_debut)
                WHERE type_mouvement = 'ACQUISITION'
                DO NOTHING;
            END IF;
        END LOOP;
    END LOOP;
END;
$$;


-- ============================================================
-- 7. INITIALISATION ET SUIVI DES EMPLOYES
-- ============================================================

CREATE OR REPLACE FUNCTION initialiser_dossier_employe(e employe) RETURNS VOID AS $$
DECLARE v_horaire INTEGER; v_regime INTEGER; v_fin DATE;
BEGIN
    v_fin := CASE WHEN e.statut = 'PARTI' THEN e.date_depart END;
    INSERT INTO historique_affectation
        (id_employe, id_departement, id_poste, id_superieur, date_debut, date_fin, motif)
    VALUES (e.id, e.id_departement, e.id_poste,
            CASE WHEN e.statut = 'PARTI' THEN NULL ELSE manager_employe_du_departement(e.id_departement, e.id) END,
            e.date_embauche, v_fin, 'Embauche');
    SELECT id INTO v_horaire FROM horaire_travail WHERE code = 'STANDARD';
    SELECT id INTO v_regime FROM regime_travail WHERE code = 'TEMPS_PLEIN';
    IF v_horaire IS NOT NULL AND NOT EXISTS (SELECT 1 FROM planning_travail WHERE id_employe = e.id) THEN
        INSERT INTO planning_travail (id_employe, id_horaire, id_regime, date_debut, date_fin)
        VALUES (e.id, v_horaire, v_regime, e.date_embauche, v_fin);
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION trg_employe_apres_insert() RETURNS trigger AS $$
BEGIN
    PERFORM initialiser_dossier_employe(NEW);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_employe_dossier_initial
AFTER INSERT ON employe FOR EACH ROW EXECUTE FUNCTION trg_employe_apres_insert();

CREATE OR REPLACE FUNCTION trg_employe_suivre_changements() RETURNS trigger AS $$
BEGIN
    IF current_setting('app.affectation_en_cours', TRUE) = '1' THEN RETURN NEW; END IF;
    IF NEW.statut = 'PARTI' AND OLD.statut <> 'PARTI' THEN
        UPDATE historique_affectation SET date_fin = GREATEST(NEW.date_depart, date_debut)
        WHERE id_employe = NEW.id AND date_fin IS NULL;
        UPDATE planning_travail SET date_fin = GREATEST(NEW.date_depart, date_debut)
        WHERE id_employe = NEW.id AND (date_fin IS NULL OR date_fin > NEW.date_depart);
    ELSIF NEW.statut <> 'PARTI'
          AND (NEW.id_departement IS DISTINCT FROM OLD.id_departement
               OR NEW.id_poste IS DISTINCT FROM OLD.id_poste) THEN
        PERFORM ouvrir_affectation(NEW.id, NEW.id_departement, NEW.id_poste,
            CASE WHEN NEW.id_departement IS DISTINCT FROM OLD.id_departement
                 THEN manager_employe_du_departement(NEW.id_departement, NEW.id)
                 ELSE (SELECT id_superieur FROM historique_affectation
                       WHERE id_employe = NEW.id AND date_fin IS NULL) END,
            CURRENT_DATE, 'Changement direct sur la fiche employé');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_employe_changements
AFTER UPDATE OF id_departement, id_poste, statut ON employe
FOR EACH ROW EXECUTE FUNCTION trg_employe_suivre_changements();


-- ============================================================
-- 8. INITIALISATION DES AFFECTATIONS MANQUANTES + VALIDATION DES CONTRAINTES
-- ============================================================

SELECT initialiser_dossier_employe(e) FROM employe e
WHERE NOT EXISTS (SELECT 1 FROM historique_affectation h WHERE h.id_employe = e.id);

ALTER TABLE employe VALIDATE CONSTRAINT chk_employe_statut_depart;
ALTER TABLE decision_recrutement VALIDATE CONSTRAINT chk_decision_decideur;
ALTER TABLE tache_onboarding VALIDATE CONSTRAINT chk_tache_realisation;
ALTER TABLE notification VALIDATE CONSTRAINT chk_notification_lecture;

COMMIT;