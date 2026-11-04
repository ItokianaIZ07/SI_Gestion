BEGIN;

CREATE EXTENSION IF NOT EXISTS btree_gist;

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
    ADD COLUMN date_expiration DATE,
    ADD COLUMN date_renouvellement DATE,
    ADD COLUMN delai_alerte_jours INTEGER NOT NULL DEFAULT 30
        CONSTRAINT chk_document_delai_alerte CHECK (delai_alerte_jours >= 0);

CREATE INDEX idx_document_expiration ON document_employe(date_expiration)
    WHERE date_expiration IS NOT NULL AND date_renouvellement IS NULL;

ALTER TABLE notification
    ADD COLUMN id_document_employe INTEGER REFERENCES document_employe(id) ON DELETE SET NULL;
CREATE INDEX idx_notification_document ON notification(id_document_employe)
    WHERE id_document_employe IS NOT NULL;

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

CREATE FUNCTION verifier_publication_candidature() RETURNS trigger AS $$
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

CREATE FUNCTION verifier_contrat_actif() RETURNS trigger AS $$
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

CREATE TABLE historique_tache_onboarding (
    id BIGSERIAL PRIMARY KEY,
    id_tache INTEGER NOT NULL REFERENCES tache_onboarding(id) ON DELETE CASCADE,
    id_utilisateur_auteur INTEGER REFERENCES utilisateur(id),
    ancien_statut VARCHAR(20), nouveau_statut VARCHAR(20),
    ancienne_echeance DATE, nouvelle_echeance DATE,
    ancienne_remarque TEXT, nouvelle_remarque TEXT,
    date_action TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE FUNCTION tracer_modification_tache_onboarding() RETURNS trigger AS $$
BEGIN
    IF OLD.statut IS DISTINCT FROM NEW.statut
       OR OLD.date_echeance IS DISTINCT FROM NEW.date_echeance
       OR OLD.remarque IS DISTINCT FROM NEW.remarque THEN
        INSERT INTO historique_tache_onboarding
            (id_tache, id_utilisateur_auteur, ancien_statut, nouveau_statut,
             ancienne_echeance, nouvelle_echeance, ancienne_remarque, nouvelle_remarque)
        VALUES (OLD.id, NULLIF(current_setting('app.user_id', TRUE), '')::INTEGER,
                OLD.statut, NEW.statut, OLD.date_echeance, NEW.date_echeance,
                OLD.remarque, NEW.remarque);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_historique_tache_onboarding
AFTER UPDATE ON tache_onboarding
FOR EACH ROW EXECUTE FUNCTION tracer_modification_tache_onboarding();

CREATE INDEX idx_historique_tache_date ON historique_tache_onboarding(id_tache, date_action DESC);

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
CREATE UNIQUE INDEX uq_affectation_courante ON historique_affectation(id_employe)
    WHERE date_fin IS NULL;
CREATE INDEX idx_affectation_employe_dates ON historique_affectation(id_employe, date_debut DESC);
CREATE INDEX idx_affectation_superieur ON historique_affectation(id_superieur)
    WHERE date_fin IS NULL;

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
CREATE INDEX idx_audit_utilisateur_date ON journal_audit(id_utilisateur, date_action DESC);
CREATE INDEX idx_audit_table_enregistrement ON journal_audit(table_concernee, identifiant_enregistrement);

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

CREATE TABLE jour_ferie (
    date_jour DATE PRIMARY KEY,
    libelle VARCHAR(100) NOT NULL
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

CREATE INDEX idx_planning_employe_dates ON planning_travail(id_employe, date_debut, date_fin);
CREATE INDEX idx_pointage_employe_date ON pointage(id_employe, date_heure DESC);
CREATE INDEX idx_temps_journalier_date ON temps_travail_journalier(date_travail, id_employe);
CREATE INDEX idx_anomalie_employe_statut ON anomalie_pointage(id_employe, statut, date_anomalie DESC);

CREATE TABLE type_conge (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL,
    description TEXT,
    deduit_solde BOOLEAN NOT NULL DEFAULT TRUE,
    justificatif_obligatoire BOOLEAN NOT NULL DEFAULT FALSE,
    remplacant_obligatoire BOOLEAN NOT NULL DEFAULT TRUE,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE droit_conge (
    id SERIAL PRIMARY KEY,
    id_employe INTEGER NOT NULL REFERENCES employe(id),
    id_type_conge INTEGER NOT NULL REFERENCES type_conge(id),
    acquisition_mensuelle NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK (acquisition_mensuelle >= 0),
    plafond_jours NUMERIC(6,2) NOT NULL DEFAULT 90 CHECK (plafond_jours > 0 AND plafond_jours <= 90),
    date_debut DATE NOT NULL DEFAULT CURRENT_DATE,
    date_fin DATE,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (date_fin IS NULL OR date_fin >= date_debut),
    UNIQUE (id_employe, id_type_conge, date_debut)
);

CREATE TABLE solde_conge (
    id BIGSERIAL PRIMARY KEY,
    id_employe INTEGER NOT NULL REFERENCES employe(id),
    id_type_conge INTEGER NOT NULL REFERENCES type_conge(id),
    annee INTEGER NOT NULL CHECK (annee >= 2000),
    jours_acquis NUMERIC(6,2) NOT NULL DEFAULT 0 CHECK (jours_acquis >= 0),
    jours_pris NUMERIC(6,2) NOT NULL DEFAULT 0 CHECK (jours_pris >= 0),
    ajustement_jours NUMERIC(6,2) NOT NULL DEFAULT 0,
    jours_disponibles NUMERIC(7,2) GENERATED ALWAYS AS (jours_acquis + ajustement_jours - jours_pris) STORED,
    date_modification TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id_employe, id_type_conge, annee),
    CHECK (jours_acquis + ajustement_jours <= 90),
    CHECK (jours_acquis + ajustement_jours - jours_pris >= 0)
);

CREATE TABLE demande_conge (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(50) NOT NULL UNIQUE,
    id_employe INTEGER NOT NULL REFERENCES employe(id),
    id_type_conge INTEGER NOT NULL REFERENCES type_conge(id),
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    jours_demandes NUMERIC(6,2) NOT NULL CHECK (jours_demandes > 0),
    motif TEXT,
    statut VARCHAR(25) NOT NULL DEFAULT 'EN_ATTENTE_MANAGER'
        CHECK (statut IN ('EN_ATTENTE_MANAGER', 'EN_ATTENTE_RH', 'ACCEPTEE', 'REFUSEE', 'ANNULEE')),
    date_demande TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_decision TIMESTAMPTZ,
    CHECK (date_fin >= date_debut),
    CONSTRAINT ex_demande_periode EXCLUDE USING gist (
        id_employe WITH =, daterange(date_debut, date_fin, '[]') WITH &&)
        WHERE (statut IN ('EN_ATTENTE_MANAGER', 'EN_ATTENTE_RH', 'ACCEPTEE'))
);

CREATE TABLE validation_conge (
    id BIGSERIAL PRIMARY KEY,
    id_demande_conge BIGINT NOT NULL REFERENCES demande_conge(id) ON DELETE CASCADE,
    etape VARCHAR(15) NOT NULL CHECK (etape IN ('MANAGER', 'RH')),
    id_validateur INTEGER REFERENCES utilisateur(id),
    decision VARCHAR(15) NOT NULL DEFAULT 'EN_ATTENTE'
        CHECK (decision IN ('EN_ATTENTE', 'ACCEPTE', 'REFUSE')),
    commentaire TEXT,
    date_decision TIMESTAMPTZ,
    UNIQUE (id_demande_conge, etape),
    CHECK ((decision = 'EN_ATTENTE' AND id_validateur IS NULL AND date_decision IS NULL)
        OR (decision IN ('ACCEPTE', 'REFUSE') AND id_validateur IS NOT NULL AND date_decision IS NOT NULL))
);

CREATE TABLE absence (
    id BIGSERIAL PRIMARY KEY,
    id_employe INTEGER NOT NULL REFERENCES employe(id),
    id_demande_conge BIGINT UNIQUE REFERENCES demande_conge(id),
    type_absence VARCHAR(25) NOT NULL
        CHECK (type_absence IN ('CONGE', 'MALADIE', 'AUTORISEE', 'INJUSTIFIEE', 'AUTRE')),
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    jours_absence NUMERIC(6,2) NOT NULL CHECK (jours_absence > 0),
    statut VARCHAR(20) NOT NULL DEFAULT 'PLANIFIEE'
        CHECK (statut IN ('PLANIFIEE', 'EN_COURS', 'A_JUSTIFIER', 'JUSTIFIEE', 'NON_JUSTIFIEE', 'ANNULEE')),
    remarque TEXT,
    CHECK (date_fin >= date_debut),
    CHECK ((type_absence = 'CONGE') = (id_demande_conge IS NOT NULL)),
    CONSTRAINT ex_absence_periode EXCLUDE USING gist (
        id_employe WITH =, daterange(date_debut, date_fin, '[]') WITH &&)
        WHERE (statut <> 'ANNULEE')
);

CREATE TABLE justificatif_absence (
    id BIGSERIAL PRIMARY KEY,
    id_absence BIGINT NOT NULL REFERENCES absence(id) ON DELETE CASCADE,
    nom_fichier VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    contenu BYTEA NOT NULL,
    date_depot TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut VARCHAR(20) NOT NULL DEFAULT 'A_VERIFIER'
        CHECK (statut IN ('A_VERIFIER', 'VALIDE', 'REFUSE'))
);

CREATE TABLE mission (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(50) NOT NULL UNIQUE,
    id_employe INTEGER NOT NULL REFERENCES employe(id),
    lieu VARCHAR(200) NOT NULL,
    objet TEXT NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'PLANIFIEE'
        CHECK (statut IN ('BROUILLON', 'PLANIFIEE', 'EN_COURS', 'TERMINEE', 'ANNULEE')),
    id_utilisateur_createur INTEGER REFERENCES utilisateur(id),
    CHECK (date_fin >= date_debut),
    CONSTRAINT ex_mission_periode EXCLUDE USING gist (
        id_employe WITH =, daterange(date_debut, date_fin, '[]') WITH &&)
        WHERE (statut IN ('PLANIFIEE', 'EN_COURS'))
);

CREATE TABLE remplacement_conge (
    id BIGSERIAL PRIMARY KEY,
    id_demande_conge BIGINT NOT NULL REFERENCES demande_conge(id) ON DELETE CASCADE,
    id_employe_remplacant INTEGER NOT NULL REFERENCES employe(id),
    responsabilites TEXT,
    statut VARCHAR(20) NOT NULL DEFAULT 'PROPOSE'
        CHECK (statut IN ('PROPOSE', 'ACCEPTE', 'REFUSE', 'ANNULE')),
    date_creation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id_demande_conge, id_employe_remplacant)
);

CREATE TABLE mouvement_solde_conge (
    id BIGSERIAL PRIMARY KEY,
    id_solde_conge BIGINT NOT NULL REFERENCES solde_conge(id),
    type_mouvement VARCHAR(20) NOT NULL
        CHECK (type_mouvement IN ('ACQUISITION', 'PRISE', 'AJUSTEMENT', 'ANNULATION')),
    jours NUMERIC(6,2) NOT NULL CHECK (jours > 0),
    reference_externe VARCHAR(100) NOT NULL,
    commentaire TEXT,
    date_mouvement TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (id_solde_conge, type_mouvement, reference_externe)
);

CREATE TABLE indemnite_conge (
    id SERIAL PRIMARY KEY,
    id_employe INTEGER NOT NULL REFERENCES employe(id),
    id_contrat INTEGER NOT NULL REFERENCES contrat(id),
    date_reference DATE NOT NULL,
    salaire_brut NUMERIC(12,2) NOT NULL CHECK (salaire_brut >= 0),
    jours_non_pris NUMERIC(7,2) NOT NULL CHECK (jours_non_pris >= 0),
    montant NUMERIC(14,2) NOT NULL CHECK (montant >= 0),
    id_utilisateur_auteur INTEGER REFERENCES utilisateur(id),
    date_calcul TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_droit_conge_employe ON droit_conge(id_employe, id_type_conge, actif);
CREATE INDEX idx_solde_conge_employe_annee ON solde_conge(id_employe, annee);
CREATE INDEX idx_demande_conge_employe_statut ON demande_conge(id_employe, statut, date_debut);
CREATE INDEX idx_validation_conge_demande ON validation_conge(id_demande_conge, etape, decision);
CREATE INDEX idx_absence_employe_dates ON absence(id_employe, date_debut, date_fin);
CREATE INDEX idx_mission_employe_dates ON mission(id_employe, date_debut, date_fin);
CREATE INDEX idx_remplacement_demande ON remplacement_conge(id_demande_conge);
CREATE INDEX idx_mouvement_solde_date ON mouvement_solde_conge(id_solde_conge, date_mouvement DESC);
CREATE INDEX idx_indemnite_employe ON indemnite_conge(id_employe);

INSERT INTO type_conge (code, libelle, description, deduit_solde, justificatif_obligatoire, remplacant_obligatoire) VALUES
    ('ANNUEL', 'Congé annuel', 'Congé acquis au fil des mois', TRUE, FALSE, TRUE),
    ('MALADIE', 'Congé maladie', 'Absence pour raison médicale', FALSE, TRUE, FALSE),
    ('EXCEPTIONNEL', 'Congé exceptionnel', 'Événement familial', FALSE, TRUE, TRUE),
    ('SANS_SOLDE', 'Congé sans solde', 'Congé non rémunéré', FALSE, FALSE, TRUE);

INSERT INTO regime_travail (code, libelle, heures_hebdomadaires) VALUES
    ('TEMPS_PLEIN', 'Temps plein', 40),
    ('TEMPS_PARTIEL', 'Temps partiel', 20);

INSERT INTO horaire_travail (code, libelle, heure_debut, heure_fin, debut_pause, fin_pause,
                             tolerance_retard_minutes, jours_travail) VALUES
    ('STANDARD', 'Standard 08h-17h (pause 12h-13h)', '08:00', '17:00', '12:00', '13:00',
     10, ARRAY[1,2,3,4,5]::SMALLINT[]);

INSERT INTO jour_ferie (date_jour, libelle)
SELECT make_date(g.y, v.m, v.d), v.l
FROM generate_series(2026, 2030) AS g(y),
     (VALUES (1, 1, 'Jour de l''An'), (3, 29, 'Commémoration de 1947'),
             (5, 1, 'Fête du Travail'), (6, 26, 'Fête de l''Indépendance'),
             (8, 15, 'Assomption'), (11, 1, 'Toussaint'), (12, 25, 'Noël')) AS v(m, d, l)
ON CONFLICT DO NOTHING;

CREATE FUNCTION verifier_hierarchie() RETURNS trigger AS $$
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

CREATE FUNCTION manager_employe_du_departement(p_id_departement INTEGER, p_exclure INTEGER)
RETURNS INTEGER AS $$
    SELECT e.id FROM departement d JOIN employe e ON e.id_utilisateur = d.id_utilisateur_manager
    WHERE d.id = p_id_departement AND e.id <> COALESCE(p_exclure, -1) AND e.statut <> 'PARTI'
    LIMIT 1;
$$ LANGUAGE sql STABLE;

CREATE FUNCTION ouvrir_affectation(p_id_employe INTEGER, p_id_departement INTEGER, p_id_poste INTEGER,
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

CREATE FUNCTION changer_affectation(p_id_employe INTEGER, p_id_departement INTEGER, p_id_poste INTEGER,
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

CREATE FUNCTION lister_subordonnes(p_id_employe INTEGER, p_directs_seulement BOOLEAN DEFAULT FALSE)
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

CREATE FUNCTION lister_superieurs(p_id_employe INTEGER)
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

CREATE FUNCTION generer_alertes_documents(p_frequence_jours INTEGER DEFAULT 7) RETURNS INTEGER AS $$
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

CREATE FUNCTION auditer_modification() RETURNS trigger AS $$
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

CREATE FUNCTION interdire_modification_journal() RETURNS trigger AS $$
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
        'candidature', 'decision_recrutement', 'demande_conge', 'validation_conge',
        'solde_conge', 'absence', 'mission', 'remplacement_conge',
        'temps_travail_journalier', 'indemnite_conge']
    LOOP
        EXECUTE format('CREATE TRIGGER trg_audit_%1$s AFTER INSERT OR UPDATE OR DELETE ON %1$I
                        FOR EACH ROW EXECUTE FUNCTION auditer_modification()', t);
    END LOOP;
END $$;

CREATE FUNCTION calculer_temps_journalier(p_id_employe INTEGER, p_date DATE) RETURNS VOID AS $$
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

    v_hors_service :=
        EXISTS (SELECT 1 FROM absence a WHERE a.id_employe = p_id_employe AND a.statut <> 'ANNULEE'
                AND p_date BETWEEN a.date_debut AND a.date_fin)
        OR EXISTS (SELECT 1 FROM mission m WHERE m.id_employe = p_id_employe
                   AND m.statut IN ('PLANIFIEE', 'EN_COURS', 'TERMINEE') AND p_date BETWEEN m.date_debut AND m.date_fin)
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

CREATE FUNCTION calculer_temps_periode(p_debut DATE, p_fin DATE) RETURNS INTEGER AS $$
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

CREATE FUNCTION valider_heures_supplementaires(p_id BIGINT, p_id_utilisateur INTEGER, p_decision VARCHAR)
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

CREATE FUNCTION calculer_jours_conge(p_id_employe INTEGER, p_debut DATE, p_fin DATE) RETURNS NUMERIC AS $$
    SELECT COUNT(*)::NUMERIC
    FROM generate_series(p_debut::TIMESTAMP, p_fin::TIMESTAMP, INTERVAL '1 day') g
    WHERE EXTRACT(ISODOW FROM g)::SMALLINT = ANY (COALESCE(
            (SELECT ht.jours_travail FROM planning_travail pl JOIN horaire_travail ht ON ht.id = pl.id_horaire
             WHERE pl.id_employe = p_id_employe AND g::DATE BETWEEN pl.date_debut AND COALESCE(pl.date_fin, DATE 'infinity')),
            ARRAY[1,2,3,4,5]::SMALLINT[]))
      AND NOT EXISTS (SELECT 1 FROM jour_ferie f WHERE f.date_jour = g::DATE);
$$ LANGUAGE sql STABLE;

CREATE FUNCTION acquerir_conges_mensuels(p_annee INTEGER, p_mois INTEGER) RETURNS INTEGER AS $$
DECLARE r RECORD; v_id BIGINT; v_jours NUMERIC(6,2); v_ref VARCHAR(100); v_nb INTEGER := 0;
BEGIN
    IF p_mois NOT BETWEEN 1 AND 12 OR p_annee < 2000 THEN RAISE EXCEPTION 'Mois/année invalide.'; END IF;
    v_ref := 'ACQUISITION:' || p_annee || '-' || LPAD(p_mois::TEXT, 2, '0');
    FOR r IN
        SELECT dc.id_employe, dc.id_type_conge, dc.acquisition_mensuelle, dc.plafond_jours
        FROM droit_conge dc JOIN employe e ON e.id = dc.id_employe JOIN type_conge tc ON tc.id = dc.id_type_conge
        WHERE dc.actif AND tc.actif AND e.statut = 'ACTIF' AND dc.acquisition_mensuelle > 0
          AND make_date(p_annee, p_mois, 1) >= dc.date_debut
          AND (dc.date_fin IS NULL OR make_date(p_annee, p_mois, 1) <= dc.date_fin)
    LOOP
        INSERT INTO solde_conge (id_employe, id_type_conge, annee) VALUES (r.id_employe, r.id_type_conge, p_annee)
        ON CONFLICT (id_employe, id_type_conge, annee) DO NOTHING;
        SELECT id INTO v_id FROM solde_conge
        WHERE id_employe = r.id_employe AND id_type_conge = r.id_type_conge AND annee = p_annee FOR UPDATE;
        SELECT LEAST(r.acquisition_mensuelle,
                     GREATEST(0, LEAST(90, r.plafond_jours) - COALESCE(SUM(jours_disponibles), 0)))
        INTO v_jours FROM solde_conge WHERE id_employe = r.id_employe AND id_type_conge = r.id_type_conge;
        IF v_jours > 0 THEN
            INSERT INTO mouvement_solde_conge (id_solde_conge, type_mouvement, jours, reference_externe, commentaire)
            VALUES (v_id, 'ACQUISITION', v_jours, v_ref, 'Acquisition mensuelle automatique')
            ON CONFLICT (id_solde_conge, type_mouvement, reference_externe) DO NOTHING;
            IF FOUND THEN
                UPDATE solde_conge SET jours_acquis = jours_acquis + v_jours, date_modification = CURRENT_TIMESTAMP
                WHERE id = v_id;
                v_nb := v_nb + 1;
            END IF;
        END IF;
    END LOOP;
    RETURN v_nb;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION preparer_demande_conge() RETURNS trigger AS $$
DECLARE v_deduit BOOLEAN; v_dispo NUMERIC; v_reserve NUMERIC;
BEGIN
    PERFORM 1 FROM employe WHERE id = NEW.id_employe FOR UPDATE;
    NEW.jours_demandes := calculer_jours_conge(NEW.id_employe, NEW.date_debut, NEW.date_fin);
    IF NEW.jours_demandes <= 0 THEN
        RAISE EXCEPTION 'La période demandée ne contient aucun jour de travail.';
    END IF;
    SELECT deduit_solde INTO v_deduit FROM type_conge WHERE id = NEW.id_type_conge AND actif;
    IF v_deduit IS NULL THEN RAISE EXCEPTION 'Type de congé inconnu ou inactif.'; END IF;
    IF v_deduit THEN
        SELECT COALESCE(SUM(jours_disponibles), 0) INTO v_dispo FROM solde_conge
        WHERE id_employe = NEW.id_employe AND id_type_conge = NEW.id_type_conge;
        SELECT COALESCE(SUM(jours_demandes), 0) INTO v_reserve FROM demande_conge
        WHERE id_employe = NEW.id_employe AND id_type_conge = NEW.id_type_conge
          AND statut IN ('EN_ATTENTE_MANAGER', 'EN_ATTENTE_RH');
        IF v_dispo - v_reserve < NEW.jours_demandes THEN
            RAISE EXCEPTION 'Solde insuffisant : disponible % (dont % déjà réservés), demandé %.',
                v_dispo, v_reserve, NEW.jours_demandes;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_preparer_demande_conge
BEFORE INSERT ON demande_conge FOR EACH ROW EXECUTE FUNCTION preparer_demande_conge();

CREATE FUNCTION initialiser_validations_conge() RETURNS trigger AS $$
BEGIN
    INSERT INTO validation_conge (id_demande_conge, etape) VALUES (NEW.id, 'MANAGER'), (NEW.id, 'RH');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_initialiser_validations_conge
AFTER INSERT ON demande_conge FOR EACH ROW EXECUTE FUNCTION initialiser_validations_conge();

CREATE FUNCTION utilisateur_est_manager_de(p_id_utilisateur INTEGER, p_id_employe INTEGER) RETURNS BOOLEAN AS $$
    SELECT NOT EXISTS (SELECT 1 FROM employe WHERE id = p_id_employe AND id_utilisateur = p_id_utilisateur)
       AND (
        EXISTS (SELECT 1 FROM historique_affectation h JOIN employe s ON s.id = h.id_superieur
                WHERE h.id_employe = p_id_employe AND h.date_fin IS NULL AND s.id_utilisateur = p_id_utilisateur)
        OR EXISTS (SELECT 1 FROM employe e JOIN departement d ON d.id = e.id_departement
                   WHERE e.id = p_id_employe AND d.id_utilisateur_manager = p_id_utilisateur)
        OR (EXISTS (SELECT 1 FROM utilisateur u JOIN role r ON r.id = u.id_role
                    WHERE u.id = p_id_utilisateur AND r.code = 'DIRECTION')
            AND NOT EXISTS (SELECT 1 FROM historique_affectation h
                            WHERE h.id_employe = p_id_employe AND h.date_fin IS NULL AND h.id_superieur IS NOT NULL))
       );
$$ LANGUAGE sql STABLE;

CREATE FUNCTION confirmer_demande_conge(p_id_demande BIGINT) RETURNS VOID AS $$
DECLARE d demande_conge%ROWTYPE; s solde_conge%ROWTYPE; t type_conge%ROWTYPE;
        v_reste NUMERIC; v_pris NUMERIC; v_dispo NUMERIC;
BEGIN
    SELECT * INTO d FROM demande_conge WHERE id = p_id_demande FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Demande introuvable.'; END IF;
    IF d.statut <> 'EN_ATTENTE_RH' THEN RAISE EXCEPTION 'Demande non prête pour la validation RH.'; END IF;
    IF (SELECT COUNT(*) FROM validation_conge WHERE id_demande_conge = d.id AND decision = 'ACCEPTE') <> 2 THEN
        RAISE EXCEPTION 'Les validations Manager et RH doivent être acceptées.';
    END IF;
    SELECT * INTO t FROM type_conge WHERE id = d.id_type_conge;
    IF NOT FOUND THEN RAISE EXCEPTION 'Type de congé introuvable.'; END IF;

    IF t.remplacant_obligatoire AND NOT EXISTS (
        SELECT 1 FROM remplacement_conge WHERE id_demande_conge = d.id AND statut = 'ACCEPTE') THEN
        RAISE EXCEPTION 'Un remplaçant ayant accepté est obligatoire pour ce type de congé.';
    END IF;
    IF EXISTS (SELECT 1 FROM absence a WHERE a.id_employe = d.id_employe AND a.statut <> 'ANNULEE'
               AND daterange(a.date_debut, a.date_fin, '[]') && daterange(d.date_debut, d.date_fin, '[]')) THEN
        RAISE EXCEPTION 'Une absence chevauche cette période.';
    END IF;
    IF EXISTS (SELECT 1 FROM mission m WHERE m.id_employe = d.id_employe AND m.statut IN ('PLANIFIEE', 'EN_COURS')
               AND daterange(m.date_debut, m.date_fin, '[]') && daterange(d.date_debut, d.date_fin, '[]')) THEN
        RAISE EXCEPTION 'Une mission chevauche cette période.';
    END IF;

    IF t.deduit_solde THEN
        SELECT COALESCE(SUM(jours_disponibles), 0) INTO v_dispo FROM solde_conge
        WHERE id_employe = d.id_employe AND id_type_conge = d.id_type_conge;
        IF v_dispo < d.jours_demandes THEN
            RAISE EXCEPTION 'Solde insuffisant : disponible %, demandé %.', v_dispo, d.jours_demandes;
        END IF;
        v_reste := d.jours_demandes;
        FOR s IN SELECT * FROM solde_conge
                 WHERE id_employe = d.id_employe AND id_type_conge = d.id_type_conge AND jours_disponibles > 0
                 ORDER BY annee FOR UPDATE
        LOOP
            v_pris := LEAST(v_reste, s.jours_disponibles);
            UPDATE solde_conge SET jours_pris = jours_pris + v_pris, date_modification = CURRENT_TIMESTAMP
            WHERE id = s.id;
            INSERT INTO mouvement_solde_conge (id_solde_conge, type_mouvement, jours, reference_externe, commentaire)
            VALUES (s.id, 'PRISE', v_pris, 'DEMANDE:' || d.id, 'Prise de congé après validation');
            v_reste := v_reste - v_pris;
            EXIT WHEN v_reste <= 0;
        END LOOP;
    END IF;

    INSERT INTO absence (id_employe, id_demande_conge, type_absence, date_debut, date_fin, jours_absence, statut)
    VALUES (d.id_employe, d.id, 'CONGE', d.date_debut, d.date_fin, d.jours_demandes, 'PLANIFIEE');
    UPDATE demande_conge SET statut = 'ACCEPTEE', date_decision = CURRENT_TIMESTAMP WHERE id = d.id;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION decider_validation_conge(p_id_demande BIGINT, p_etape VARCHAR, p_id_validateur INTEGER,
                                         p_decision VARCHAR, p_commentaire TEXT DEFAULT NULL)
RETURNS VOID AS $$
DECLARE d demande_conge%ROWTYPE;
BEGIN
    IF p_decision NOT IN ('ACCEPTE', 'REFUSE') THEN RAISE EXCEPTION 'Décision invalide : ACCEPTE ou REFUSE.'; END IF;
    SELECT * INTO d FROM demande_conge WHERE id = p_id_demande FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Demande introuvable.'; END IF;

    IF p_etape = 'MANAGER' THEN
        IF d.statut <> 'EN_ATTENTE_MANAGER' THEN RAISE EXCEPTION 'La demande n''attend pas la validation du manager.'; END IF;
        IF NOT utilisateur_est_manager_de(p_id_validateur, d.id_employe) THEN
            RAISE EXCEPTION 'Cet utilisateur n''est pas habilité à valider cette demande (étape manager).';
        END IF;
    ELSIF p_etape = 'RH' THEN
        IF d.statut <> 'EN_ATTENTE_RH' THEN RAISE EXCEPTION 'La demande n''attend pas la validation RH.'; END IF;
        IF NOT EXISTS (SELECT 1 FROM utilisateur u JOIN role r ON r.id = u.id_role
                       WHERE u.id = p_id_validateur AND r.code = 'RH' AND u.actif)
           OR EXISTS (SELECT 1 FROM employe WHERE id = d.id_employe AND id_utilisateur = p_id_validateur) THEN
            RAISE EXCEPTION 'Cet utilisateur n''est pas habilité à valider cette demande (étape RH).';
        END IF;
    ELSE
        RAISE EXCEPTION 'Étape invalide : MANAGER ou RH.';
    END IF;

    UPDATE validation_conge SET id_validateur = p_id_validateur, decision = p_decision,
           commentaire = p_commentaire, date_decision = CURRENT_TIMESTAMP
    WHERE id_demande_conge = p_id_demande AND etape = p_etape AND decision = 'EN_ATTENTE';
    IF NOT FOUND THEN RAISE EXCEPTION 'Étape déjà traitée.'; END IF;

    IF p_decision = 'REFUSE' THEN
        UPDATE demande_conge SET statut = 'REFUSEE', date_decision = CURRENT_TIMESTAMP WHERE id = p_id_demande;
    ELSIF p_etape = 'MANAGER' THEN
        UPDATE demande_conge SET statut = 'EN_ATTENTE_RH' WHERE id = p_id_demande;
    ELSE
        PERFORM confirmer_demande_conge(p_id_demande);
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION annuler_demande_conge(p_id_demande BIGINT, p_id_utilisateur INTEGER) RETURNS VOID AS $$
DECLARE d demande_conge%ROWTYPE; m mouvement_solde_conge%ROWTYPE;
BEGIN
    SELECT * INTO d FROM demande_conge WHERE id = p_id_demande FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Demande introuvable.'; END IF;
    IF d.statut IN ('REFUSEE', 'ANNULEE') THEN RAISE EXCEPTION 'Cette demande est déjà clôturée.'; END IF;
    IF NOT EXISTS (SELECT 1 FROM employe WHERE id = d.id_employe AND id_utilisateur = p_id_utilisateur)
       AND NOT EXISTS (SELECT 1 FROM utilisateur u JOIN role r ON r.id = u.id_role
                       WHERE u.id = p_id_utilisateur AND r.code = 'RH') THEN
        RAISE EXCEPTION 'Seul l''employé concerné ou le RH peut annuler cette demande.';
    END IF;
    IF d.statut = 'ACCEPTEE' THEN
        IF d.date_debut <= CURRENT_DATE THEN RAISE EXCEPTION 'Un congé déjà commencé ne peut plus être annulé.'; END IF;
        FOR m IN SELECT * FROM mouvement_solde_conge
                 WHERE reference_externe = 'DEMANDE:' || d.id AND type_mouvement = 'PRISE'
        LOOP
            UPDATE solde_conge SET jours_pris = jours_pris - m.jours, date_modification = CURRENT_TIMESTAMP
            WHERE id = m.id_solde_conge;
            INSERT INTO mouvement_solde_conge (id_solde_conge, type_mouvement, jours, reference_externe, commentaire)
            VALUES (m.id_solde_conge, 'ANNULATION', m.jours, 'DEMANDE:' || d.id, 'Annulation du congé');
        END LOOP;
        UPDATE absence SET statut = 'ANNULEE' WHERE id_demande_conge = d.id;
    END IF;
    UPDATE demande_conge SET statut = 'ANNULEE', date_decision = CURRENT_TIMESTAMP WHERE id = d.id;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION verifier_remplacement() RETURNS trigger AS $$
DECLARE d demande_conge%ROWTYPE;
BEGIN
    IF NEW.statut NOT IN ('PROPOSE', 'ACCEPTE') THEN RETURN NEW; END IF;
    SELECT * INTO d FROM demande_conge WHERE id = NEW.id_demande_conge;
    IF NEW.id_employe_remplacant = d.id_employe THEN
        RAISE EXCEPTION 'Un employé ne peut pas se remplacer lui-même.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM employe WHERE id = NEW.id_employe_remplacant AND statut = 'ACTIF') THEN
        RAISE EXCEPTION 'Le remplaçant doit être un employé actif.';
    END IF;
    IF EXISTS (SELECT 1 FROM absence a WHERE a.id_employe = NEW.id_employe_remplacant AND a.statut <> 'ANNULEE'
               AND daterange(a.date_debut, a.date_fin, '[]') && daterange(d.date_debut, d.date_fin, '[]')) THEN
        RAISE EXCEPTION 'Le remplaçant est lui-même absent sur cette période.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_verifier_remplacement
BEFORE INSERT OR UPDATE ON remplacement_conge FOR EACH ROW EXECUTE FUNCTION verifier_remplacement();

CREATE FUNCTION verifier_mission_absence() RETURNS trigger AS $$
BEGIN
    IF NEW.statut IN ('PLANIFIEE', 'EN_COURS') AND EXISTS (
        SELECT 1 FROM absence a WHERE a.id_employe = NEW.id_employe AND a.statut <> 'ANNULEE'
        AND daterange(a.date_debut, a.date_fin, '[]') && daterange(NEW.date_debut, NEW.date_fin, '[]')) THEN
        RAISE EXCEPTION 'Cette mission chevauche une absence de l''employé.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_verifier_mission_absence
BEFORE INSERT OR UPDATE ON mission FOR EACH ROW EXECUTE FUNCTION verifier_mission_absence();

CREATE FUNCTION calculer_indemnite_conge(p_id_employe INTEGER, p_date_reference DATE DEFAULT NULL,
                                         p_enregistrer BOOLEAN DEFAULT FALSE, p_id_utilisateur INTEGER DEFAULT NULL)
RETURNS TABLE (out_salaire_brut NUMERIC, out_jours_non_pris NUMERIC,
               out_montant NUMERIC, out_id_contrat INTEGER) AS $$
DECLARE v_date DATE; v_contrat INTEGER; v_salaire NUMERIC; v_jours NUMERIC; v_montant NUMERIC;
BEGIN
    v_date := COALESCE(p_date_reference, (SELECT date_depart FROM employe WHERE id = p_id_employe), CURRENT_DATE);
    SELECT c.id, c.salaire INTO v_contrat, v_salaire
    FROM contrat c JOIN statut_contract sc ON sc.id = c.id_statut_contrat
    WHERE c.id_employe = p_id_employe AND c.salaire IS NOT NULL AND c.date_debut <= v_date
      AND sc.code IN ('ACTIF', 'TERMINE', 'RESILIE')
    ORDER BY (c.date_fin IS NULL OR c.date_fin >= v_date) DESC, c.date_debut DESC
    LIMIT 1;
    IF v_contrat IS NULL THEN RAISE EXCEPTION 'Aucun contrat avec salaire trouvé pour cet employé.'; END IF;
    SELECT COALESCE(SUM(s.jours_disponibles), 0) INTO v_jours
    FROM solde_conge s JOIN type_conge t ON t.id = s.id_type_conge AND t.deduit_solde
    WHERE s.id_employe = p_id_employe;
    v_montant := ROUND(v_salaire * v_jours / 30, 2);
    IF p_enregistrer THEN
        INSERT INTO indemnite_conge (id_employe, id_contrat, date_reference, salaire_brut, jours_non_pris, montant,
                                     id_utilisateur_auteur)
        VALUES (p_id_employe, v_contrat, v_date, v_salaire, v_jours, v_montant, p_id_utilisateur);
    END IF;
    RETURN QUERY SELECT v_salaire, v_jours, v_montant, v_contrat;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION initialiser_dossier_employe(e employe) RETURNS VOID AS $$
DECLARE v_horaire INTEGER; v_regime INTEGER; v_fin DATE;
BEGIN
    v_fin := CASE WHEN e.statut = 'PARTI' THEN e.date_depart END;
    INSERT INTO historique_affectation
        (id_employe, id_departement, id_poste, id_superieur, date_debut, date_fin, motif)
    VALUES (e.id, e.id_departement, e.id_poste,
            CASE WHEN e.statut = 'PARTI' THEN NULL ELSE manager_employe_du_departement(e.id_departement, e.id) END,
            e.date_embauche, v_fin, 'Embauche');
    INSERT INTO droit_conge (id_employe, id_type_conge, acquisition_mensuelle, plafond_jours,
                             date_debut, date_fin, actif)
    SELECT e.id, tc.id, 2.5, 90, e.date_embauche, v_fin, (e.statut <> 'PARTI')
    FROM type_conge tc WHERE tc.code = 'ANNUEL'
    ON CONFLICT DO NOTHING;
    SELECT id INTO v_horaire FROM horaire_travail WHERE code = 'STANDARD';
    SELECT id INTO v_regime FROM regime_travail WHERE code = 'TEMPS_PLEIN';
    IF v_horaire IS NOT NULL AND NOT EXISTS (SELECT 1 FROM planning_travail WHERE id_employe = e.id) THEN
        INSERT INTO planning_travail (id_employe, id_horaire, id_regime, date_debut, date_fin)
        VALUES (e.id, v_horaire, v_regime, e.date_embauche, v_fin);
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION trg_employe_apres_insert() RETURNS trigger AS $$
BEGIN
    PERFORM initialiser_dossier_employe(NEW);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_employe_dossier_initial
AFTER INSERT ON employe FOR EACH ROW EXECUTE FUNCTION trg_employe_apres_insert();

CREATE FUNCTION trg_employe_suivre_changements() RETURNS trigger AS $$
BEGIN
    IF current_setting('app.affectation_en_cours', TRUE) = '1' THEN RETURN NEW; END IF;
    IF NEW.statut = 'PARTI' AND OLD.statut <> 'PARTI' THEN
        UPDATE historique_affectation SET date_fin = GREATEST(NEW.date_depart, date_debut)
        WHERE id_employe = NEW.id AND date_fin IS NULL;
        UPDATE droit_conge SET date_fin = GREATEST(NEW.date_depart, date_debut), actif = FALSE
        WHERE id_employe = NEW.id AND (date_fin IS NULL OR date_fin > NEW.date_depart);
        UPDATE planning_travail SET date_fin = GREATEST(NEW.date_depart, date_debut)
        WHERE id_employe = NEW.id AND (date_fin IS NULL OR date_fin > NEW.date_depart);
    ELSIF NEW.statut <> 'PARTI'
          AND (NEW.id_departement <> OLD.id_departement OR NEW.id_poste <> OLD.id_poste) THEN
        PERFORM ouvrir_affectation(NEW.id, NEW.id_departement, NEW.id_poste,
            CASE WHEN NEW.id_departement <> OLD.id_departement
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

SELECT initialiser_dossier_employe(e) FROM employe e
WHERE NOT EXISTS (SELECT 1 FROM historique_affectation h WHERE h.id_employe = e.id);

ALTER TABLE employe VALIDATE CONSTRAINT chk_employe_statut_depart;
ALTER TABLE decision_recrutement VALIDATE CONSTRAINT chk_decision_decideur;
ALTER TABLE tache_onboarding VALIDATE CONSTRAINT chk_tache_realisation;
ALTER TABLE notification VALIDATE CONSTRAINT chk_notification_lecture;

COMMIT;

