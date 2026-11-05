# ============================================================
# BLOC 7 : GESTION DES CONGES ET ABSENCES
#
# IMPORTANT :
# Les tables existantes suivantes sont réutilisées :
# - employe(id)
# - utilisateur(id)
# - decision(id, code)
#
# Les tables existantes ne sont pas recréées ici.
#
# Les décisions attendues dans la table decision :
# EN_ATTENTE, ACCEPTE, REFUSE
#
# ============================================================


# ============================================================
# 1. CONFIGURATION DES TYPES DE CONGE
# ============================================================

# ------------------------------------------------------------
# TABLE : type_conge
#
# Rôle :
# Définit les différents types de congés proposés par
# l'entreprise.
#
# Exemples :
# ANNUEL       : congé annuel
# MALADIE      : congé maladie
# EXCEPTIONNEL : congé exceptionnel
# SANS_SOLDE   : congé sans solde
# ------------------------------------------------------------

CREATE TABLE type_conge (
    id SERIAL PRIMARY KEY,

    code VARCHAR(30) NOT NULL UNIQUE,

    libelle VARCHAR(100) NOT NULL,

    description TEXT,

    actif BOOLEAN NOT NULL DEFAULT TRUE
);


# ------------------------------------------------------------
# TABLE : regle_conge
#
# Rôle :
# Définit le comportement de chaque type de congé.
#
# Exemple :
# Congé annuel :
# - Déduit du solde : oui
# - Justificatif obligatoire : non
# - Remplaçant obligatoire : oui
# - Acquisition mensuelle : 2,5 jours
# - Plafond : 90 jours
#
# Un type peut avoir plusieurs versions de règles
# si les règles changent dans le temps.
# ------------------------------------------------------------

CREATE TABLE regle_conge (
    id SERIAL PRIMARY KEY,

    id_type_conge INTEGER NOT NULL
        REFERENCES type_conge(id),

    deduction_solde BOOLEAN NOT NULL DEFAULT FALSE,

    justificatif_obligatoire BOOLEAN NOT NULL DEFAULT FALSE,

    remplacant_obligatoire BOOLEAN NOT NULL DEFAULT FALSE,

    acquisition_autorisee BOOLEAN NOT NULL DEFAULT FALSE,

    jours_acquis_mois NUMERIC(6,2) NOT NULL DEFAULT 0,

    plafond_jours NUMERIC(6,2),

    date_debut_validite DATE NOT NULL DEFAULT CURRENT_DATE,

    date_fin_validite DATE,

    actif BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT ck_regle_jours_acquis
        CHECK (jours_acquis_mois >= 0),

    CONSTRAINT ck_regle_plafond
        CHECK (plafond_jours IS NULL OR plafond_jours >= 0),

    CONSTRAINT ck_regle_dates
        CHECK (
            date_fin_validite IS NULL
            OR date_fin_validite >= date_debut_validite
        )
);

CREATE INDEX idx_regle_conge_type
    ON regle_conge(id_type_conge);


# ------------------------------------------------------------
# DONNEES INITIALES : TYPES DE CONGE
# ------------------------------------------------------------

INSERT INTO type_conge(code, libelle, description)
VALUES
(
    'ANNUEL',
    'Congé annuel',
    'Congé annuel acquis progressivement par le salarié.'
),
(
    'MALADIE',
    'Congé maladie',
    'Congé accordé en raison d un problème de santé.'
),
(
    'EXCEPTIONNEL',
    'Congé exceptionnel',
    'Congé accordé pour un événement particulier.'
),
(
    'SANS_SOLDE',
    'Congé sans solde',
    'Congé dont les solde de congé ne sont pas compté'
)
ON CONFLICT (code) DO NOTHING;


# ------------------------------------------------------------
# DONNEES INITIALES : REGLES
#
# Les valeurs proposées sont des exemples de configuration.
# Elles doivent être adaptées aux règles de ton entreprise.
# ------------------------------------------------------------

INSERT INTO regle_conge (
    id_type_conge,
    deduction_solde,
    justificatif_obligatoire,
    remplacant_obligatoire,
    acquisition_autorisee,
    jours_acquis_mois,
    plafond_jours
)
SELECT
    t.id,
    r.deduction_solde,
    r.justificatif_obligatoire,
    r.remplacant_obligatoire,
    r.acquisition_autorisee,
    r.jours_acquis_mois,
    r.plafond_jours
FROM (
    VALUES
    ('ANNUEL', TRUE, FALSE, TRUE, TRUE, 2.5, 90),
    ('MALADIE', FALSE, TRUE, FALSE, FALSE, 0, NULL),
    ('EXCEPTIONNEL', FALSE, FALSE, FALSE, FALSE, 0, NULL),
    ('SANS_SOLDE', FALSE, FALSE, FALSE, FALSE, 0, NULL)
) AS r(
    code,
    deduction_solde,
    justificatif_obligatoire,
    remplacant_obligatoire,
    acquisition_autorisee,
    jours_acquis_mois,
    plafond_jours
)
JOIN type_conge t ON t.code = r.code
WHERE NOT EXISTS (
    SELECT 1
    FROM regle_conge rc
    WHERE rc.id_type_conge = t.id
      AND rc.actif = TRUE
);


# ============================================================
# 2. CALENDRIER DES JOURS NON TRAVAILLES
# ============================================================

# ------------------------------------------------------------
# TABLE : jour_ferie
#
# Rôle :
# Enregistre les jours fériés et autres jours non travaillés.
#
# Cette table permet de calculer le nombre de jours ouvrés
# dans une demande de congé.
#
# Exemple :
# 2027-01-01 : Nouvel an
# ------------------------------------------------------------

CREATE TABLE jour_ferie (
    id SERIAL PRIMARY KEY,

    date_jour DATE NOT NULL UNIQUE,

    libelle VARCHAR(150) NOT NULL,

    description TEXT
);


# ============================================================
# 3. DEMANDES DE CONGE
# ============================================================

# ------------------------------------------------------------
# TABLE : demande_conge
#
# Rôle :
# Représente une demande de congé effectuée par un employé.
#
# Le nombre de jours est calculé par le backend ou la fonction
# SQL, et non saisi librement par l'utilisateur.
#
# statut_global représente l'état global du circuit :
# EN_ATTENTE_MANAGER
# EN_ATTENTE_RH
# VALIDEE
# REFUSEE
# ANNULEE
# ------------------------------------------------------------

CREATE TABLE demande_conge (
    id SERIAL PRIMARY KEY,

    id_employe INTEGER NOT NULL
        REFERENCES employe(id),

    id_type_conge INTEGER NOT NULL
        REFERENCES type_conge(id),

    date_debut DATE NOT NULL,

    date_fin DATE NOT NULL,

    nombre_jours NUMERIC(6,2) NOT NULL,

    motif TEXT,

    date_demande TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    statut_global VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE_MANAGER',

    date_decision_finale TIMESTAMP,

    CONSTRAINT ck_demande_dates
        CHECK (date_fin >= date_debut),

    CONSTRAINT ck_demande_nombre_jours
        CHECK (nombre_jours > 0),

    CONSTRAINT ck_demande_statut
        CHECK (
            statut_global IN (
                'EN_ATTENTE_MANAGER',
                'EN_ATTENTE_RH',
                'VALIDEE',
                'REFUSEE',
                'ANNULEE'
            )
        )
);

CREATE INDEX idx_demande_conge_employe
    ON demande_conge(id_employe);

CREATE INDEX idx_demande_conge_type
    ON demande_conge(id_type_conge);

CREATE INDEX idx_demande_conge_statut
    ON demande_conge(statut_global);

CREATE INDEX idx_demande_conge_dates
    ON demande_conge(date_debut, date_fin);


# ============================================================
# 4. VALIDATION DES DEMANDES
# ============================================================

# ------------------------------------------------------------
# TABLE : validation_conge
#
# Rôle :
# Conserve chaque étape de validation.
#
# Circuit (Ordre):
# 1. MANAGER
# 2. RH
#
# id_decision référence la table decision déjà existante.
#
# Exemples :
# MANAGER / ACCEPTE
# RH / REFUSE
#
# Un commentaire est conservé pour chaque décision.
# ------------------------------------------------------------

CREATE TABLE validation_conge (
    id SERIAL PRIMARY KEY,

    id_demande_conge INTEGER NOT NULL
        REFERENCES demande_conge(id),

    id_utilisateur INTEGER
        REFERENCES utilisateur(id),


    ordre SMALLINT NOT NULL,

    id_decision INTEGER NOT NULL
        REFERENCES decision(id),

    commentaire TEXT,

    date_validation TIMESTAMP,

    CONSTRAINT ck_validation_conge_ordre
        CHECK (ordre IN (1, 2)),

    CONSTRAINT uq_validation_conge_ordre
        UNIQUE (id_demande_conge, ordre)
);

CREATE INDEX idx_validation_conge_demande
    ON validation_conge(id_demande_conge);

CREATE INDEX idx_validation_conge_utilisateur
    ON validation_conge(id_utilisateur);


# ============================================================
# 5. GESTION DES REMPLACANTS
# ============================================================

# ------------------------------------------------------------
# TABLE : remplacement_conge
#
# Rôle :
# Enregistre le remplaçant proposé pour une demande de congé.
#
# Le remplaçant peut :
# - ACCEPTER
# - REFUSER
# - Rester EN_ATTENTE
#
# La décision du remplaçant est indépendante de la décision
# de validation du congé.
# ------------------------------------------------------------

CREATE TABLE remplacement_conge (
    id SERIAL PRIMARY KEY,

    id_demande_conge INTEGER NOT NULL UNIQUE
        REFERENCES demande_conge(id),

    id_employe_remplacant INTEGER NOT NULL
        REFERENCES employe(id),

    date_proposition TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    decision VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',

    commentaire TEXT,

    date_decision TIMESTAMP,

    CONSTRAINT ck_remplacement_decision
        CHECK (
            decision IN (
                'EN_ATTENTE',
                'ACCEPTE',
                'REFUSE'
            )
        )
);

CREATE INDEX idx_remplacement_employe
    ON remplacement_conge(id_employe_remplacant);


# ============================================================
# 6. HISTORIQUE DES MOUVEMENTS DE CONGE
# ============================================================

# ------------------------------------------------------------
# TABLE : mouvement_conge
#
# Rôle :
# Conserve tous les mouvements qui modifient le solde.
#
# IMPORTANT :
# Le nombre de jours est signé.
#
# +2.5 : acquisition
# -5   : consommation
# +5   : restitution
# +3   : ajustement positif
# -2   : ajustement négatif
#
# Le solde est obtenu par SUM(nombre_jours).
#
# periode_debut permet de garantir qu'une acquisition
# mensuelle ne soit pas exécutée deux fois.
# ------------------------------------------------------------

CREATE TABLE mouvement_conge (
    id SERIAL PRIMARY KEY,

    id_employe INTEGER NOT NULL
        REFERENCES employe(id),

    id_type_conge INTEGER NOT NULL
        REFERENCES type_conge(id),

    id_demande_conge INTEGER
        REFERENCES demande_conge(id),

    id_utilisateur INTEGER
        REFERENCES utilisateur(id),

    date_mouvement TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    type_mouvement VARCHAR(30) NOT NULL,

    nombre_jours NUMERIC(6,2) NOT NULL,

    periode_debut DATE,

    motif TEXT,

    CONSTRAINT ck_mouvement_type
        CHECK (
            type_mouvement IN (
                'ACQUISITION',
                'CONSOMMATION',
                'AJUSTEMENT',
                'RESTITUTION',
                'EXPIRATION'
            )
        ),

    CONSTRAINT ck_mouvement_nombre
        CHECK (nombre_jours <> 0),

    CONSTRAINT ck_mouvement_signe
        CHECK (
            (type_mouvement = 'ACQUISITION' AND nombre_jours > 0)
            OR
            (type_mouvement = 'CONSOMMATION' AND nombre_jours < 0)
            OR
            (type_mouvement = 'RESTITUTION' AND nombre_jours > 0)
            OR
            (type_mouvement = 'AJUSTEMENT')
            OR
            (type_mouvement = 'EXPIRATION' AND nombre_jours < 0)
        )
);

CREATE INDEX idx_mouvement_employe
    ON mouvement_conge(id_employe);

CREATE INDEX idx_mouvement_type
    ON mouvement_conge(id_type_conge);

CREATE INDEX idx_mouvement_date
    ON mouvement_conge(date_mouvement);

CREATE INDEX idx_mouvement_demande
    ON mouvement_conge(id_demande_conge);

# Empêche une double acquisition pour un même employé,
# un même type de congé et une même période.

CREATE UNIQUE INDEX uq_acquisition_mensuelle
    ON mouvement_conge(
        id_employe,
        id_type_conge,
        periode_debut
    )
    WHERE type_mouvement = 'ACQUISITION';


# ============================================================
# 7. ABSENCES
# ============================================================

# ------------------------------------------------------------
# TABLE : type_absence
#
# Rôle :
# Définit les types d'absence.
#
# Exemples :
# MALADIE
# ABSENCE_INJUSTIFIEE
# RETARD
# ABSENCE_AUTORISEE
# ACCIDENT
# ------------------------------------------------------------

CREATE TABLE type_absence (
    id SERIAL PRIMARY KEY,

    code VARCHAR(30) NOT NULL UNIQUE,

    libelle VARCHAR(100) NOT NULL,

    justificatif_obligatoire BOOLEAN NOT NULL DEFAULT FALSE,

    actif BOOLEAN NOT NULL DEFAULT TRUE
);


# ------------------------------------------------------------
# TABLE : absence
#
# Rôle :
# Enregistre une absence réellement constatée.
#
# Une absence peut être déclarée après l'événement.
# Elle n'est pas nécessairement issue d'une demande de congé.
# ------------------------------------------------------------

CREATE TABLE absence (
    id SERIAL PRIMARY KEY,

    id_employe INTEGER NOT NULL
        REFERENCES employe(id),

    id_type_absence INTEGER NOT NULL
        REFERENCES type_absence(id),

    date_debut TIMESTAMP NOT NULL,

    date_fin TIMESTAMP NOT NULL,

    motif TEXT,

    statut VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',

    date_declaration TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    id_utilisateur_verificateur INTEGER
        REFERENCES utilisateur(id),

    commentaire_rh TEXT,

    CONSTRAINT ck_absence_dates
        CHECK (date_fin >= date_debut),

    CONSTRAINT ck_absence_statut
        CHECK (
            statut IN (
                'EN_ATTENTE',
                'JUSTIFIEE',
                'NON_JUSTIFIEE',
                'REFUSEE'
            )
        )
);

CREATE INDEX idx_absence_employe
    ON absence(id_employe);

CREATE INDEX idx_absence_dates
    ON absence(date_debut, date_fin);

CREATE INDEX idx_absence_statut
    ON absence(statut);


# ------------------------------------------------------------
# TABLE : justificatif_absence
#
# Rôle :
# Stocke les justificatifs associés à une absence.
#
# BYTEA permet de stocker directement le fichier.
# Une absence peut posséder plusieurs justificatifs.
# ------------------------------------------------------------

CREATE TABLE justificatif_absence (
    id SERIAL PRIMARY KEY,

    id_absence INTEGER NOT NULL
        REFERENCES absence(id),

    nom_fichier VARCHAR(255) NOT NULL,

    type_mime VARCHAR(100),

    contenu BYTEA NOT NULL,

    date_depot TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    id_utilisateur_verificateur INTEGER
        REFERENCES utilisateur(id),

    date_verification TIMESTAMP,

    statut_verification VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',

    commentaire TEXT,

    CONSTRAINT ck_justificatif_statut
        CHECK (
            statut_verification IN (
                'EN_ATTENTE',
                'ACCEPTE',
                'REFUSE'
            )
        )
);

CREATE INDEX idx_justificatif_absence
    ON justificatif_absence(id_absence);


# ============================================================
# 8. MISSIONS PROFESSIONNELLES
# ============================================================

# ------------------------------------------------------------
# TABLE : mission
#
# Rôle :
# Enregistre les missions professionnelles.
#
# Une mission ne modifie jamais le solde de congé.
# ------------------------------------------------------------

CREATE TABLE mission (
    id SERIAL PRIMARY KEY,

    reference VARCHAR(100) NOT NULL UNIQUE,

    objet VARCHAR(255) NOT NULL,

    description TEXT,

    lieu VARCHAR(255),

    date_debut DATE NOT NULL,

    date_fin DATE NOT NULL,

    statut VARCHAR(30) NOT NULL DEFAULT 'PLANIFIEE',

    id_utilisateur_createur INTEGER
        REFERENCES utilisateur(id),

    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_mission_dates
        CHECK (date_fin >= date_debut),

    CONSTRAINT ck_mission_statut
        CHECK (
            statut IN (
                'PLANIFIEE',
                'EN_COURS',
                'TERMINEE',
                'ANNULEE'
            )
        )
);


# ------------------------------------------------------------
# TABLE : mission_employe
#
# Rôle :
# Table de liaison entre les missions et les employés.
#
# Une mission peut concerner plusieurs employés.
# Un employé peut participer à plusieurs missions.
# ------------------------------------------------------------

CREATE TABLE mission_employe (
    id SERIAL PRIMARY KEY,

    id_mission INTEGER NOT NULL
        REFERENCES mission(id),

    id_employe INTEGER NOT NULL
        REFERENCES employe(id),

    role_mission VARCHAR(100),

    commentaire TEXT,

    CONSTRAINT uq_mission_employe
        UNIQUE (id_mission, id_employe)
);

CREATE INDEX idx_mission_employe_employe
    ON mission_employe(id_employe);


# ============================================================
# 9. INDEMNITE DE CONGE NON PRIS
# ============================================================

# ------------------------------------------------------------
# TABLE : indemnite_conge
#
# Rôle :
# Conserve les calculs d'indemnité des congés non pris.
#
# On conserve les valeurs utilisées au moment du calcul
# pour garantir l'historique même si le salaire évolue.
# ------------------------------------------------------------

CREATE TABLE indemnite_conge (
    id SERIAL PRIMARY KEY,

    id_employe INTEGER NOT NULL
        REFERENCES employe(id),

    date_calcul TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    solde_jours NUMERIC(6,2) NOT NULL,

    montant_journalier NUMERIC(14,2) NOT NULL,

    montant_total NUMERIC(14,2) NOT NULL,

    methode_calcul VARCHAR(100),

    statut VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',

    id_utilisateur INTEGER
        REFERENCES utilisateur(id),

    commentaire TEXT,

    CONSTRAINT ck_indemnite_jours
        CHECK (solde_jours >= 0),

    CONSTRAINT ck_indemnite_montants
        CHECK (
            montant_journalier >= 0
            AND montant_total >= 0
        ),

    CONSTRAINT ck_indemnite_statut
        CHECK (
            statut IN (
                'EN_ATTENTE',
                'VALIDEE',
                'PAYEE',
                'ANNULEE'
            )
        )
);

CREATE INDEX idx_indemnite_employe
    ON indemnite_conge(id_employe);


# ============================================================
# 10. FONCTION DE CALCUL DU SOLDE
# ============================================================

# ------------------------------------------------------------
# FONCTION : fn_solde_conge
#
# Rôle :
# Calcule le solde d'un employé pour un type de congé.
#
# Exemple :
# SELECT fn_solde_conge(1, 1);
#
# Résultat :
# Solde de l'employé 1 pour le type de congé 1.
#
# Le calcul repose uniquement sur l'historique des mouvements.
# ------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_solde_conge(
    p_id_employe INTEGER,
    p_id_type_conge INTEGER
)
RETURNS NUMERIC(6,2)
LANGUAGE SQL
STABLE
AS $$
    SELECT COALESCE(SUM(nombre_jours), 0)::NUMERIC(6,2)
    FROM mouvement_conge
    WHERE id_employe = p_id_employe
      AND id_type_conge = p_id_type_conge;
$$;


# ============================================================
# 11. FONCTION DE CALCUL DES JOURS OUVRES
# ============================================================

# ------------------------------------------------------------
# FONCTION : fn_calculer_jours_conge
#
# Rôle :
# Calcule le nombre de jours ouvrés entre deux dates.
#
# Sont exclus :
# - Samedi
# - Dimanche
# - Dates enregistrées dans jour_ferie
#
# Cette version suppose une semaine de travail du lundi
# au vendredi. Elle pourra être adaptée à ton calendrier RH.
#
# Exemple :
# SELECT fn_calculer_jours_conge('2026-11-02', '2026-11-06');
# Résultat : 5
# ------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_calculer_jours_conge(
    p_date_debut DATE,
    p_date_fin DATE
)
RETURNS INTEGER
LANGUAGE SQL
STABLE
AS $$
    SELECT COUNT(*)::INTEGER
    FROM generate_series(
        p_date_debut,
        p_date_fin,
        INTERVAL '1 day'
    ) AS calendrier(jour)
    WHERE EXTRACT(ISODOW FROM calendrier.jour) BETWEEN 1 AND 5
      AND NOT EXISTS (
          SELECT 1
          FROM jour_ferie jf
          WHERE jf.date_jour = calendrier.jour::DATE
      );
$$;


# ============================================================
# 12. PROCEDURE D'ACQUISITION MENSUELLE
# ============================================================

# ------------------------------------------------------------
# PROCEDURE : sp_acquerir_conges_mensuels
#
# Rôle :
# Ajoute automatiquement les jours acquis par chaque employé.
#
# Exemple :
# CALL sp_acquerir_conges_mensuels('2026-11-01');
#
# Pour chaque employé :
# 1. Récupère les règles actives du congé annuel.
# 2. Calcule le solde actuel.
# 3. Vérifie le plafond.
# 4. Ajoute le mouvement d'acquisition.
#
# La procédure est idempotente pour une même période :
# elle ne crée pas deux acquisitions identiques.
#
# IMPORTANT :
# Cette procédure ne détermine pas à elle seule les employés
# ayant droit à l'acquisition. Il faut compléter la condition
# d'éligibilité selon les dates de contrat, suspensions et
# règles de présence de ton entreprise.
# ------------------------------------------------------------

CREATE OR REPLACE PROCEDURE sp_acquerir_conges_mensuels(
    p_periode DATE
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_employe RECORD;
    v_regle RECORD;

    v_solde NUMERIC(6,2);
    v_acquisition NUMERIC(6,2);
BEGIN

    IF p_periode IS NULL THEN
        RAISE EXCEPTION 'La période est obligatoire';
    END IF;

    FOR v_employe IN
        SELECT id
        FROM employe
    LOOP

        # Verrouille la ligne employé pendant le traitement.
        PERFORM 1
        FROM employe
        WHERE id = v_employe.id
        FOR UPDATE;

        FOR v_regle IN
            SELECT
                rc.id_type_conge,
                rc.jours_acquis_mois,
                rc.plafond_jours
            FROM regle_conge rc
            JOIN type_conge tc
                ON tc.id = rc.id_type_conge
            WHERE tc.code = 'ANNUEL'
              AND rc.actif = TRUE
              AND rc.acquisition_autorisee = TRUE
              AND rc.date_debut_validite <= p_periode
              AND (
                  rc.date_fin_validite IS NULL
                  OR rc.date_fin_validite >= p_periode
              )
        LOOP

            v_solde := fn_solde_conge(
                v_employe.id,
                v_regle.id_type_conge
            );

            IF v_regle.plafond_jours IS NULL THEN
                v_acquisition := v_regle.jours_acquis_mois;
            ELSE
                v_acquisition := LEAST(
                    v_regle.jours_acquis_mois,
                    GREATEST(
                        v_regle.plafond_jours - v_solde,
                        0
                    )
                );
            END IF;

            IF v_acquisition > 0 THEN

                INSERT INTO mouvement_conge (
                    id_employe,
                    id_type_conge,
                    type_mouvement,
                    nombre_jours,
                    periode_debut,
                    motif
                )
                VALUES (
                    v_employe.id,
                    v_regle.id_type_conge,
                    'ACQUISITION',
                    v_acquisition,
                    DATE_TRUNC('month', p_periode)::DATE,
                    'Acquisition mensuelle automatique'
                )
                ON CONFLICT (
                    id_employe,
                    id_type_conge,
                    periode_debut
                )
                WHERE type_mouvement = 'ACQUISITION'
                DO NOTHING;

            END IF;

        END LOOP;

    END LOOP;

END;
$$;


# ============================================================
# 13. EXEMPLES D'UTILISATION
# ============================================================

# ------------------------------------------------------------
# EXEMPLE 1 : CONSULTER LE SOLDE D'UN EMPLOYE
#
# Remplacer 1 par un identifiant existant.
# ------------------------------------------------------------

# SELECT fn_solde_conge(1, (
#     SELECT id FROM type_conge WHERE code = 'ANNUEL'
# ));


# ------------------------------------------------------------
# EXEMPLE 2 : CALCULER LES JOURS OUVRES
# ------------------------------------------------------------

# SELECT fn_calculer_jours_conge(
#     '2026-11-02',
#     '2026-11-06'
# );


# ------------------------------------------------------------
# EXEMPLE 3 : LANCER L'ACQUISITION MENSUELLE
# ------------------------------------------------------------

# CALL sp_acquerir_conges_mensuels('2026-11-01');


# ------------------------------------------------------------
# EXEMPLE 4 : AJOUTER UN AJUSTEMENT POSITIF
#
# Remplacer les identifiants par des valeurs existantes.
# ------------------------------------------------------------

# INSERT INTO mouvement_conge (
#     id_employe,
#     id_type_conge,
#     type_mouvement,
#     nombre_jours,
#     id_utilisateur,
#     motif
# )
# VALUES (
#     1,
#     (SELECT id FROM type_conge WHERE code = 'ANNUEL'),
#     'AJUSTEMENT',
#     3,
#     1,
#     'Correction administrative'
# );