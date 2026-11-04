-- ============================================================
-- SIG RH - DONNEES DE TEST (PostgreSQL 12+)
-- A executer APRES init_tables.sql, sur une base de DEVELOPPEMENT.
-- Le script est rejouable : il vide toutes les tables avant d'inserer.
-- Mot de passe de tous les comptes : Test1234!
-- Date de reference du jeu de donnees : 26/10/2026
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

BEGIN;

-- ------------------------------------------------------------
-- A. Modifications de schema demandees
-- ------------------------------------------------------------
ALTER TABLE validation_besoin DROP CONSTRAINT IF EXISTS uq_validation_besoin_etape;

DROP TABLE IF EXISTS signature_token;
CREATE TABLE signature_token (
    id SERIAL PRIMARY KEY,
    id_contrat INTEGER NOT NULL REFERENCES contrat(id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,   -- SHA-256 hex du token
    email_destinataire VARCHAR(150) NOT NULL,
    date_creation TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    date_expiration TIMESTAMPTZ NOT NULL,
    utilise BOOLEAN NOT NULL DEFAULT FALSE,
    date_utilisation TIMESTAMPTZ
);
CREATE INDEX idx_signature_token_hash ON signature_token(token_hash);
CREATE INDEX idx_signature_token_contrat ON signature_token(id_contrat);

ALTER TABLE historique_contrat ADD COLUMN IF NOT EXISTS signature_image BYTEA;

-- Un manager = un seul departement
ALTER TABLE departement DROP CONSTRAINT IF EXISTS uq_departement_manager;
ALTER TABLE departement ADD CONSTRAINT uq_departement_manager UNIQUE (id_utilisateur_manager);

-- OPTIONNEL (voir remarque) : permettre de signer un contrat AVANT sa date de debut
-- ALTER TABLE contrat DROP CONSTRAINT chk_contrat_signature;

-- ------------------------------------------------------------
-- B. Nettoyage (rend le script rejouable)
-- ------------------------------------------------------------
TRUNCATE TABLE
    signature_token, notification, document_employe, tache_onboarding, onboarding,
    modele_tache_onboarding, depense_departement, budget_departement,
    historique_contrat, contrat, statut_contract, type_contrat,
    decision_recrutement, entretien, candidature, cv, candidat,
    publication_offre, offre_emploi, recrutement, validation_besoin,
    besoin, type_besoin, etat_validation, decision, employe,
    grille_salaire, poste, departement, utilisateur, role
RESTART IDENTITY CASCADE;

-- ------------------------------------------------------------
-- C. Referentiels
-- ------------------------------------------------------------
INSERT INTO role (id, code, libelle) VALUES
    (1, 'MANAGER',   'Manager de departement'),
    (2, 'RH',        'Ressources humaines'),
    (3, 'FINANCE',   'Finance'),
    (4, 'DIRECTION', 'Direction generale');

INSERT INTO type_besoin (id, code, libelle) VALUES
    (1, 'NOUVEAU_POSTE',      'Creation de poste'),
    (2, 'REMPLACEMENT',       'Remplacement d''un employe'),
    (3, 'RENFORT_TEMPORAIRE', 'Renfort temporaire');

-- Circuit de validation : RH -> FINANCE -> DIRECTION
INSERT INTO etat_validation (id, code, libelle, ordre) VALUES
    (1, 'RH',        'Validation RH',        1),
    (2, 'FINANCE',   'Validation Finance',   2),
    (3, 'DIRECTION', 'Validation Direction', 3);

INSERT INTO decision (id, code, libelle) VALUES
    (1, 'EN_ATTENTE', 'En attente'),
    (2, 'ACCEPTE',    'Accepte'),
    (3, 'REFUSE',     'Refuse');

INSERT INTO statut_contract (id, code, libelle) VALUES
    (1, 'BROUILLON',           'Brouillon'),
    (2, 'EN_ATTENTE_SIGNATURE','En attente de signature'),
    (3, 'ACTIF',               'Actif'),
    (4, 'TERMINE',             'Termine'),
    (5, 'RESILIE',             'Resilie');

INSERT INTO type_contrat (id, code, libelle, duree) VALUES
    (1, 'CDI',     'Contrat a duree indeterminee', NULL),
    (2, 'CDD',     'Contrat a duree determinee (mois)', 12),
    (3, 'STAGE',   'Convention de stage (mois)', 6),
    (4, 'INTERIM', 'Mission interimaire (mois)', 3);

-- ------------------------------------------------------------
-- D. Utilisateurs (4 roles, 5 managers = 5 departements)
-- ------------------------------------------------------------
INSERT INTO utilisateur (id, id_role, nom, prenom, email, mdp, actif, date_creation)
SELECT v.id, r.id, v.nom, v.prenom, v.email,
       crypt('Test1234!', gen_salt('bf', 10)), v.actif, v.dc::timestamptz
FROM (VALUES
    (1,  'DIRECTION', 'Andry',        'Tojo',    'direction@exemple.mg', TRUE,  '2016-01-04'),
    (2,  'RH',        'Lanto',        'Mialy',   'rh@exemple.mg',        TRUE,  '2021-05-03'),
    (3,  'RH',        'Razafy',       'Voahangy','rh2@exemple.mg',       TRUE,  '2023-02-01'),
    (4,  'FINANCE',   'Rasoa',        'Hanta',   'finance@exemple.mg',   TRUE,  '2019-10-01'),
    (5,  'MANAGER',   'Rakoto',       'Jean',    'manager@exemple.mg',   TRUE,  '2019-03-04'),
    (6,  'MANAGER',   'Rabe',         'Soa',     'manager2@exemple.mg',  TRUE,  '2018-06-01'),
    (7,  'MANAGER',   'Randria',      'Faniry',  'manager3@exemple.mg',  TRUE,  '2017-09-11'),
    (8,  'MANAGER',   'Ravelo',       'Hery',    'manager4@exemple.mg',  TRUE,  '2016-02-15'),
    (9,  'MANAGER',   'Rakotomalala', 'Njaka',   'manager5@exemple.mg',  TRUE,  '2020-01-13'),
    (10, 'RH',        'Inactif',      'Compte',  'inactif@exemple.mg',   FALSE, '2022-01-10')
) AS v(id, role_code, nom, prenom, email, actif, dc)
JOIN role r ON r.code = v.role_code;

-- ------------------------------------------------------------
-- E. Organisation : departements, postes, grilles salariales (MGA / mois)
-- ------------------------------------------------------------
INSERT INTO departement (id, id_utilisateur_manager, nom, effectif_max) VALUES
    (1, 5, 'Informatique',            12),
    (2, 6, 'Commercial',               6),
    (3, 7, 'Finance et Comptabilite',  3),   -- effectif max atteint si B4 est accepte
    (4, 8, 'Production',              10),
    (5, 9, 'Ressources Humaines',      4);

INSERT INTO poste (id, id_departement, nom, actif) VALUES
    (1,  1, 'Developpeur Full Stack',      TRUE),
    (2,  1, 'Administrateur Systemes',     TRUE),
    (3,  1, 'Chef de projet IT',           TRUE),
    (4,  2, 'Commercial terrain',          TRUE),
    (5,  2, 'Responsable marketing',       TRUE),
    (6,  3, 'Comptable',                   TRUE),
    (7,  3, 'Controleur de gestion',       TRUE),
    (8,  4, 'Operateur de production',     TRUE),
    (9,  4, 'Technicien de maintenance',   TRUE),
    (10, 4, 'Chef d''equipe',              TRUE),
    (11, 5, 'Charge de recrutement',       TRUE),
    (12, 5, 'Gestionnaire de paie',        TRUE),
    (13, 1, 'Analyste junior (supprime)',  FALSE);   -- poste inactif pour tester les filtres

INSERT INTO grille_salaire (id_poste, valeur_min, valeur_max, date_debut_validite, date_fin_validite) VALUES
    (1, 1500000, 3000000, '2024-01-01', '2025-12-31'),   -- ancienne grille
    (1, 1800000, 3500000, '2026-01-01', NULL),           -- grille en vigueur
    (2, 1700000, 3200000, '2024-01-01', NULL),
    (3, 3000000, 5500000, '2024-01-01', NULL),
    (4,  800000, 1800000, '2024-01-01', NULL),
    (5, 2000000, 4000000, '2024-01-01', NULL),
    (6, 1200000, 2500000, '2024-01-01', NULL),
    (7, 2000000, 4000000, '2024-01-01', NULL),
    (8,  500000,  900000, '2024-01-01', NULL),
    (9,  900000, 1800000, '2024-01-01', NULL),
    (10,1500000, 2800000, '2024-01-01', NULL),
    (11,1300000, 2800000, '2024-01-01', NULL),
    (12,1400000, 2800000, '2024-01-01', NULL);

-- ------------------------------------------------------------
-- F. Employes existants (1 a 15). Les 16 et 17 sont crees plus bas,
--    apres les candidatures dont ils sont issus.
-- ------------------------------------------------------------
INSERT INTO employe (id, matricule, nom, prenom, email, telephone, numero_cin, date_naissance, adresse,
                     id_candidature, id_utilisateur, id_departement, id_poste, date_embauche, date_depart, statut) VALUES
 (1,  'EMP-0001','Rakoto',       'Jean',   'jean.rakoto@entreprise.mg',      '034 11 000 01','101234560001','1984-05-12','Lot II A 12 Ivato, Antananarivo',     NULL,5,   1,3, '2019-03-04',NULL,'ACTIF'),
 (2,  'EMP-0002','Rabe',         'Soa',    'soa.rabe@entreprise.mg',         '034 11 000 02','101234560002','1986-09-03','Lot IV B 8 Analakely, Antananarivo',  NULL,6,   2,5, '2018-06-01',NULL,'ACTIF'),
 (3,  'EMP-0003','Randria',      'Faniry', 'faniry.randria@entreprise.mg',   '034 11 000 03','101234560003','1981-11-27','Lot VG 45 Ambohimanarina',            NULL,7,   3,7, '2017-09-11',NULL,'ACTIF'),
 (4,  'EMP-0004','Ravelo',       'Hery',   'hery.ravelo@entreprise.mg',      '034 11 000 04','101234560004','1979-02-18','Lot 22 Ankadimbahoaka',               NULL,8,   4,10,'2016-02-15',NULL,'ACTIF'),
 (5,  'EMP-0005','Rakotomalala', 'Njaka',  'njaka.rakotomalala@entreprise.mg','034 11 000 05','101234560005','1988-07-30','Lot II M 3 Tsiadana',               NULL,9,   5,12,'2020-01-13',NULL,'ACTIF'),
 (6,  'EMP-0006','Lanto',        'Mialy',  'mialy.lanto@entreprise.mg',      '034 11 000 06','101234560006','1991-04-09','Lot 17 Ambatobe',                     NULL,2,   5,11,'2021-05-03',NULL,'ACTIF'),
 (7,  'EMP-0007','Rasoa',        'Hanta',  'hanta.rasoa@entreprise.mg',      '034 11 000 07','101234560007','1989-12-21','Lot III F 9 Ankorondrano',            NULL,4,   3,6, '2019-10-01',NULL,'ACTIF'),
 (8,  'EMP-0008','Andrianina',   'Mamy',   'mamy.andrianina@entreprise.mg',  '034 11 000 08','101234560008','1993-01-15','Lot 5 Ivandry',                       NULL,NULL,1,1, '2022-02-01',NULL,'ACTIF'),
 (9,  'EMP-0009','Rasolofo',     'Tiana',  'tiana.rasolofo@entreprise.mg',   '034 11 000 09','101234560009','1996-06-25','Lot 31 Andoharanofotsy',              NULL,NULL,1,1, '2023-09-18',NULL,'ACTIF'),
 (10, 'EMP-0010','Ramanana',     'Lova',   'lova.ramanana@entreprise.mg',    '034 11 000 10','101234560010','1990-03-02','Lot 8 Itaosy',                        NULL,NULL,1,2, '2021-04-12','2026-06-30','PARTI'),
 (11, 'EMP-0011','Rakotonirina', 'Fidy',   'fidy.rakotonirina@entreprise.mg','034 11 000 11','101234560011','1992-08-14','Lot 12 Ampefiloha',                   NULL,NULL,2,4, '2022-07-04',NULL,'ACTIF'),
 (12, 'EMP-0012','Ravao',        'Fara',   'fara.ravao@entreprise.mg',       '034 11 000 12','101234560012','1995-10-05','Lot 66 Ankadifotsy',                  NULL,NULL,2,4, '2024-01-08',NULL,'SUSPENDU'),
 (13, 'EMP-0013','Randrianarisoa','Toky',  'toky.randrianarisoa@entreprise.mg','034 11 000 13','101234560013','1987-05-19','Lot 4 Ankadimbahoaka',             NULL,NULL,4,8, '2020-08-24',NULL,'ACTIF'),
 (14, 'EMP-0014','Razanamalala', 'Noro',   'noro.razanamalala@entreprise.mg','034 11 000 14','101234560014','1994-02-11','Lot 27 Anosizato',                   NULL,NULL,4,9, '2023-03-06',NULL,'ACTIF'),
 (15, 'EMP-0015','Rasamoelina',  'Bodo',   'bodo.rasamoelina@entreprise.mg', '034 11 000 15','101234560015','1985-09-29','Lot 9 Ambohimangakely',              NULL,NULL,4,8, '2019-11-04','2025-12-31','PARTI');

-- ------------------------------------------------------------
-- G. Besoins et circuit de validation
--    B1 ACCEPTE -> recrutement termine (embauche)
--    B2 ACCEPTE -> remplacement, recrutement ouvert
--    B3 ACCEPTE -> 2 postes, recrutement ouvert
--    B4 EN_ATTENTE_RH      B5 EN_ATTENTE_FINANCE   B6 EN_ATTENTE_DIRECTION
--    B7 REFUSE (finance)   B8 REFUSE (direction)   B9 ANNULE
--    B10 ACCEPTE sans recrutement (pour tester la creation de recrutement)
--    B11 ACCEPTE -> recrutement suspendu
-- ------------------------------------------------------------
INSERT INTO besoin (id, id_type_besoin, id_poste, id_utilisateur_demandeur, id_employe_remplace, id_type_contrat_souhaite,
                    effectif, priorite, date_arrivee_souhaitee, date_demande, lieu_travail, motif,
                    experience_requise, niveau_etude, competences_requises, salaire_estime, statut_global, date_modification) VALUES
 (1, 1, 1, 5, NULL, 1, 1, 'HAUTE',    '2026-07-01','2026-03-02 09:15:00+03','Antananarivo - Siege',
  'Nouveau projet de plateforme client necessitant un developpeur supplementaire',
  '3 ans','Bac+3 Informatique','Java, Spring Boot, React, PostgreSQL', 2600000,'ACCEPTE','2026-03-09 15:00:00+03'),
 (2, 2, 2, 5, 10,   1, 1, 'HAUTE',    '2026-08-15','2026-06-10 10:00:00+03','Antananarivo - Siege',
  'Remplacement de Ramanana Lova (demission, depart le 30/06/2026)',
  '4 ans','Bac+3 Systemes et reseaux','Linux, Docker, supervision, sauvegardes', 2500000,'ACCEPTE','2026-06-15 11:00:00+03'),
 (3, 1, 4, 6, NULL, 2, 2, 'NORMALE',  '2026-11-02','2026-07-20 08:45:00+03','Antananarivo - Agence Analakely',
  'Ouverture de la zone Nord : deux commerciaux terrain necessaires',
  '1 an','Bac+2 Commerce','Prospection, negociation, permis B', 1100000,'ACCEPTE','2026-07-27 16:30:00+03'),
 (4, 1, 6, 7, NULL, 1, 1, 'NORMALE',  '2027-01-04','2026-10-20 14:00:00+03','Antananarivo - Siege',
  'Charge de travail accrue due a la cloture annuelle',
  '2 ans','Bac+3 Comptabilite','Comptabilite generale, Sage, fiscalite', 1800000,'EN_ATTENTE_RH','2026-10-20 14:00:00+03'),
 (5, 2, 8, 8, 15,   1, 1, 'HAUTE',    '2026-12-01','2026-10-12 09:00:00+03','Antananarivo - Usine Ankadimbahoaka',
  'Remplacement de Rasamoelina Bodo (parti le 31/12/2025), poste reste vacant',
  '1 an','BEPC','Conduite de ligne, securite industrielle', 700000,'EN_ATTENTE_FINANCE','2026-10-14 10:30:00+03'),
 (6, 3, 9, 8, NULL, 4, 2, 'URGENTE',  '2026-11-15','2026-10-05 08:00:00+03','Antananarivo - Usine Ankadimbahoaka',
  'Renfort temporaire pour la periode de forte production de fin d''annee',
  '2 ans','Bac technique','Maintenance preventive, electromecanique', 1400000,'EN_ATTENTE_DIRECTION','2026-10-08 17:00:00+03'),
 (7, 1, 5, 6, NULL, 1, 1, 'BASSE',    '2027-02-01','2026-09-01 10:20:00+03','Antananarivo - Siege',
  'Creation d''un poste de responsable marketing',
  '5 ans','Bac+5 Marketing','Marketing digital, strategie de marque', 3500000,'REFUSE','2026-09-04 12:00:00+03'),
 (8, 1, 11,9, NULL, 1, 1, 'NORMALE',  '2026-11-01','2026-08-10 11:00:00+03','Antananarivo - Siege',
  'Second charge de recrutement pour absorber le volume de recrutements',
  '2 ans','Bac+3 GRH','Sourcing, entretien, ATS', 2000000,'REFUSE','2026-08-18 09:30:00+03'),
 (9, 1, 3, 5, NULL, 1, 1, 'NORMALE',  '2027-01-15','2026-09-15 15:00:00+03','Antananarivo - Siege',
  'Chef de projet pour la refonte ERP (demande annulee, projet reporte)',
  '6 ans','Bac+5 Informatique','Gestion de projet agile, ERP', 4500000,'ANNULE','2026-09-22 10:00:00+03'),
 (10,1, 1, 5, NULL, 3, 1, 'BASSE',    '2027-02-01','2026-09-20 09:40:00+03','Antananarivo - Siege',
  'Stagiaire developpeur pour la maintenance applicative',
  'Aucune','Bac+3 en cours','Java, SQL', 400000,'ACCEPTE','2026-09-28 14:00:00+03'),
 (11,1, 7, 7, NULL, 1, 1, 'NORMALE',  '2026-12-01','2026-08-25 13:10:00+03','Antananarivo - Siege',
  'Besoin de suivi budgetaire renforce',
  '3 ans','Bac+5 Gestion/Finance','Controle budgetaire, Power BI, Excel avance', 3200000,'ACCEPTE','2026-09-02 11:00:00+03');

-- Les 3 etapes sont creees des le depot du besoin (EN_ATTENTE tant que non traitees).
-- Colonnes : besoin, validateur, etat (1 RH, 2 FINANCE, 3 DIRECTION), decision (1 EN_ATTENTE, 2 ACCEPTE, 3 REFUSE), date, remarque
INSERT INTO validation_besoin (id_besoin, id_utilisateur_validateur, id_etat_validation, id_decision, date_decision, remarque) VALUES
 -- B1
 (1, 2, 1, 2, '2026-03-03 10:00:00+03', 'Besoin conforme au plan de recrutement'),
 (1, 4, 2, 2, '2026-03-05 14:00:00+03', 'Budget disponible'),
 (1, 1, 3, 2, '2026-03-09 15:00:00+03', 'Valide'),
 -- B2
 (2, 2, 1, 2, '2026-06-11 09:00:00+03', 'Remplacement a l''identique'),
 (2, 4, 2, 2, '2026-06-12 16:00:00+03', 'Budget libere par le depart'),
 (2, 1, 3, 2, '2026-06-15 11:00:00+03', 'Valide'),
 -- B3
 (3, 3, 1, 2, '2026-07-21 10:00:00+03', NULL),
 (3, 4, 2, 2, '2026-07-23 15:00:00+03', 'Budget commercial 2026 suffisant'),
 (3, 1, 3, 2, '2026-07-27 16:30:00+03', 'Valide pour 2 postes'),
 -- B4 : tout en attente
 (4, NULL, 1, 1, NULL, NULL),
 (4, NULL, 2, 1, NULL, NULL),
 (4, NULL, 3, 1, NULL, NULL),
 -- B5 : RH ok, finance en attente
 (5, 2, 1, 2, '2026-10-14 10:30:00+03', 'Poste vacant depuis 10 mois, a pourvoir'),
 (5, NULL, 2, 1, NULL, NULL),
 (5, NULL, 3, 1, NULL, NULL),
 -- B6 : RH et finance ok, direction en attente
 (6, 2, 1, 2, '2026-10-06 09:00:00+03', 'Urgence confirmee'),
 (6, 4, 2, 2, '2026-10-08 17:00:00+03', 'Budget interim disponible'),
 (6, NULL, 3, 1, NULL, NULL),
 -- B7 : refuse par la finance
 (7, 3, 1, 2, '2026-09-02 09:30:00+03', NULL),
 (7, 4, 2, 3, '2026-09-04 12:00:00+03', 'Budget Commercial 2026 insuffisant pour ce poste'),
 (7, NULL, 3, 1, NULL, NULL),
 -- B8 : refuse par la direction
 (8, 3, 1, 2, '2026-08-11 09:00:00+03', NULL),
 (8, 4, 2, 2, '2026-08-13 11:00:00+03', NULL),
 (8, 1, 3, 3, '2026-08-18 09:30:00+03', 'Pas de creation de poste RH cette annee'),
 -- B9 : annule par le demandeur avant traitement
 (9, NULL, 1, 1, NULL, NULL),
 (9, NULL, 2, 1, NULL, NULL),
 (9, NULL, 3, 1, NULL, NULL),
 -- B10
 (10, 2, 1, 2, '2026-09-21 10:00:00+03', NULL),
 (10, 4, 2, 2, '2026-09-23 10:00:00+03', NULL),
 (10, 1, 3, 2, '2026-09-28 14:00:00+03', NULL),
 -- B11
 (11, 3, 1, 2, '2026-08-26 10:00:00+03', NULL),
 (11, 4, 2, 2, '2026-08-28 10:00:00+03', NULL),
 (11, 1, 3, 2, '2026-09-02 11:00:00+03', NULL);

-- ------------------------------------------------------------
-- H. Recrutements, offres, publications
-- ------------------------------------------------------------
INSERT INTO recrutement (id, id_besoin, reference, titre, description, date_ouverture, date_cloture, statut, id_utilisateur_responsable, date_creation, date_modification) VALUES
 (1, 1,  'REC-2026-001', 'Developpeur Full Stack',   'Recrutement CDI pour le projet plateforme client', '2026-03-16','2026-06-12','TERMINE', 2,'2026-03-12 09:00:00+03','2026-06-12 17:00:00+03'),
 (2, 2,  'REC-2026-002', 'Administrateur Systemes',  'Remplacement de Ramanana Lova',                    '2026-07-01',NULL,        'OUVERT',  2,'2026-06-25 09:00:00+03','2026-09-22 10:00:00+03'),
 (3, 3,  'REC-2026-003', 'Commerciaux terrain (x2)', 'Ouverture de la zone Nord, 2 postes en CDD',      '2026-08-03',NULL,        'OUVERT',  3,'2026-07-30 09:00:00+03','2026-09-23 10:00:00+03'),
 (4, 11, 'REC-2026-004', 'Controleur de gestion',    'Recrutement suspendu en attente d''arbitrage',     '2026-09-14',NULL,        'SUSPENDU',3,'2026-09-10 09:00:00+03','2026-10-02 10:00:00+03');

INSERT INTO offre_emploi (id, id_recrutement, titre, description_poste, missions, competences_requises, niveau_etude, experience_min_annees, salaire_min, salaire_max, date_creation, statut) VALUES
 (1, 1, 'Developpeur Full Stack H/F', 'Rejoignez l''equipe Informatique pour construire notre plateforme client.',
  'Developper les API et interfaces, assurer la qualite du code, participer aux revues.', 'Java, Spring Boot, React, PostgreSQL', 'Bac+3', 3, 2200000, 3000000, '2026-03-14 10:00:00+03','ARCHIVEE'),
 (2, 2, 'Administrateur Systemes H/F', 'Garantir la disponibilite de l''infrastructure du siege.',
  'Administrer les serveurs Linux, superviser, gerer les sauvegardes et la securite.', 'Linux, Docker, supervision', 'Bac+3', 4, 2200000, 2800000, '2026-06-27 10:00:00+03','VALIDEE'),
 (3, 3, 'Commercial terrain H/F (2 postes)', 'Developpez notre portefeuille clients dans la zone Nord.',
  'Prospecter, negocier, assurer le suivi client et le reporting hebdomadaire.', 'Negociation, permis B', 'Bac+2', 1, 900000, 1300000, '2026-07-31 10:00:00+03','VALIDEE'),
 (4, 4, 'Controleur de gestion H/F', 'Pilotage budgetaire de l''entreprise.',
  'Elaborer les budgets, suivre les ecarts, produire le reporting mensuel.', 'Power BI, Excel avance', 'Bac+5', 3, NULL, NULL, '2026-09-12 10:00:00+03','BROUILLON');

INSERT INTO publication_offre (id, id_offre, plateforme, url_publication, date_publication, date_expiration, statut) VALUES
 (1, 1, 'LinkedIn',      'https://exemple.mg/offres/rec-2026-001/linkedin', '2026-03-16','2026-05-31','EXPIREE'),
 (2, 1, 'Emploi.mg',     'https://exemple.mg/offres/rec-2026-001/emploi',   '2026-03-16','2026-05-31','EXPIREE'),
 (3, 2, 'LinkedIn',      'https://exemple.mg/offres/rec-2026-002/linkedin', '2026-07-01','2026-12-31','PUBLIEE'),
 (4, 2, 'Site carriere', 'https://exemple.mg/offres/rec-2026-002/site',     '2026-07-01',NULL,        'PUBLIEE'),
 (5, 3, 'Emploi.mg',     'https://exemple.mg/offres/rec-2026-003/emploi',   '2026-08-03','2026-12-31','PUBLIEE'),
 (6, 3, 'Facebook',      'https://exemple.mg/offres/rec-2026-003/facebook', '2026-08-03','2026-09-30','RETIREE');

-- ------------------------------------------------------------
-- I. Candidats, CV, candidatures
-- ------------------------------------------------------------
INSERT INTO candidat (id, nom, prenom, email, telephone, numero_cin, experience, date_creation) VALUES
 (1,  'Rakotoarisoa',      'Hasina',     'hasina.rakotoarisoa@gmail.com',      '032 20 000 01','201234560001', 4, '2026-04-01 18:00:00+03'),
 (2,  'Andriamihaja',      'Tsiory',     'tsiory.andriamihaja@gmail.com',      '032 20 000 02','201234560002', 3, '2026-04-04 12:00:00+03'),
 (3,  'Ratsimbazafy',      'Lalaina',    'lalaina.ratsimbazafy@gmail.com',     '032 20 000 03','201234560003', 2, '2026-04-07 09:00:00+03'),
 (4,  'Randriamampionona', 'Sitraka',    'sitraka.randriamampionona@gmail.com','032 20 000 04','201234560004', 5, '2026-07-11 20:00:00+03'),
 (5,  'Rakotoson',         'Mirana',     'mirana.rakotoson@gmail.com',         '032 20 000 05','201234560005', 6, '2026-07-09 08:30:00+03'),
 (6,  'Razafindrakoto',    'Hery',       'hery.razafindrakoto@gmail.com',      '032 20 000 06','201234560006', 4, '2026-08-31 19:00:00+03'),
 (7,  'Rasolonjatovo',     'Aina',       'aina.rasolonjatovo@gmail.com',       '032 20 000 07','201234560007', 1, '2026-07-14 10:00:00+03'),
 (8,  'Andrianarivelo',    'Mamitiana',  'mamitiana.andrianarivelo@gmail.com', '032 20 000 08','201234560008', 2, '2026-08-09 15:00:00+03'),
 (9,  'Raharimalala',      'Zo',         'zo.raharimalala@gmail.com',          '032 20 000 09','201234560009', 3, '2026-08-11 11:00:00+03'),
 (10, 'Ramarosaona',       'Feno',       'feno.ramarosaona@gmail.com',         '032 20 000 10','201234560010', 1, '2026-08-19 16:00:00+03'),
 (11, 'Rajaonarison',      'Lanja',      'lanja.rajaonarison@gmail.com',       '032 20 000 11','201234560011', 2, '2026-08-21 13:00:00+03'),
 (12, 'Rabarison',         'Tahiry',     'tahiry.rabarison@gmail.com',         '032 20 000 12','201234560012', 5, '2026-09-19 17:00:00+03'),
 (13, 'Ranaivo',           'Kanto',      'kanto.ranaivo@gmail.com',            '032 20 000 13','201234560013', 0, '2026-10-15 09:00:00+03');  -- sans candidature

INSERT INTO cv (id, id_candidat, details, nom_fichier, mime_type, bytes, date_depot, statut_lecture)
SELECT c.id, c.id,
       'CV de ' || c.prenom || ' ' || c.nom || ' (' || c.experience || ' an(s) d''experience)',
       'cv_' || lower(c.nom) || '.pdf',
       'application/pdf',
       convert_to('%PDF-1.4 CV fictif de ' || c.prenom || ' ' || c.nom, 'UTF8'),
       c.date_creation,
       CASE WHEN c.id IN (6, 10, 11, 12, 13) THEN 'NON_LU' ELSE 'LU' END
FROM candidat c;

-- La publication choisie doit appartenir au recrutement (verifie par trigger).
INSERT INTO candidature (id, id_candidat, id_recrutement, id_cv, id_publication, date_candidature, statut, score, remarque) VALUES
 -- REC-001 (termine)
 (1,  1, 1, 1, 1,    '2026-04-02 09:00:00+03','RETENU',        88.50,'Excellent profil technique'),
 (2,  2, 1, 2, 2,    '2026-04-05 10:00:00+03','REJETE',        61.00,NULL),
 (3,  3, 1, 3, 1,    '2026-04-08 11:00:00+03','REJETE',        55.00,'Experience insuffisante'),
 -- REC-002 (ouvert, admin systemes)
 (4,  4, 2, 4, 3,    '2026-07-12 09:30:00+03','PRESELECTIONNE',78.00,NULL),
 (5,  5, 2, 5, 3,    '2026-07-10 08:00:00+03','RETENU',        91.00,'Candidate retenue, contrat a preparer'),
 (6,  6, 2, 6, 4,    '2026-09-01 08:15:00+03','EN_ATTENTE',    NULL, NULL),
 (7,  7, 2, 7, 4,    '2026-07-15 14:00:00+03','REJETE',        48.00,NULL),
 (8,  2, 2, 2, 4,    '2026-09-02 09:00:00+03','EN_ATTENTE',    NULL, 'Candidat deja rejete sur REC-001, nouvelle candidature'),
 -- REC-003 (ouvert, 2 commerciaux)
 (9,  8, 3, 8, 5,    '2026-08-10 09:00:00+03','RETENU',        85.00,NULL),
 (10, 9, 3, 9, 5,    '2026-08-12 10:00:00+03','PRESELECTIONNE',74.00,'En liste d''attente pour le 2e poste'),
 (11,10, 3,10, 5,    '2026-08-20 09:00:00+03','EN_ATTENTE',    NULL, NULL),
 (12,11, 3,11, NULL, '2026-08-22 16:00:00+03','RETIRE',        NULL, 'Le candidat a retire sa candidature (autre offre)'),
 -- REC-004 (suspendu)
 (13,12, 4,12, NULL, '2026-09-20 18:00:00+03','EN_ATTENTE',    NULL, NULL);

INSERT INTO entretien (id_candidature, id_utilisateur_intervieweur, type_entretien, date_entretien, lieu, statut, note, commentaire) VALUES
 -- Candidature 1 (retenue, parcours complet)
 (1, 2, 'TELEPHONE', '2026-04-10 10:00:00+03', 'Telephone',        'REALISE', 15.00, 'Motive, bon contact'),
 (1, 2, 'RH',        '2026-04-17 14:00:00+03', 'Siege - Salle 1',  'REALISE', 16.00, NULL),
 (1, 5, 'TECHNIQUE', '2026-04-24 09:00:00+03', 'Siege - Salle 2',  'REALISE', 17.00, 'Tres bon niveau Java'),
 (1, 1, 'DIRECTION', '2026-05-12 15:00:00+03', 'Siege - Direction','REALISE', 16.50, NULL),
 -- Candidature 2
 (2, 2, 'TELEPHONE', '2026-04-12 11:00:00+03', 'Telephone',        'REALISE', 11.00, NULL),
 (2, 2, 'RH',        '2026-04-20 10:00:00+03', 'Siege - Salle 1',  'REALISE', 10.00, 'Pretentions salariales elevees'),
 -- Candidature 3
 (3, 2, 'TELEPHONE', '2026-04-14 16:00:00+03', 'Telephone',        'REALISE',  9.00, NULL),
 -- Candidature 4 (en cours : un entretien technique planifie)
 (4, 3, 'TELEPHONE', '2026-07-20 10:00:00+03', 'Telephone',        'REALISE', 14.00, NULL),
 (4, 3, 'RH',        '2026-08-05 14:00:00+03', 'Siege - Salle 1',  'REALISE', 15.00, NULL),
 (4, 5, 'TECHNIQUE', '2026-10-29 10:00:00+03', 'Siege - Salle 2',  'PLANIFIE',NULL, NULL),
 -- Candidature 5 (retenue)
 (5, 3, 'TELEPHONE', '2026-07-18 09:30:00+03', 'Telephone',        'REALISE', 16.00, NULL),
 (5, 3, 'RH',        '2026-07-30 11:00:00+03', 'Siege - Salle 1',  'REALISE', 16.00, NULL),
 (5, 5, 'TECHNIQUE', '2026-08-20 09:00:00+03', 'Siege - Salle 2',  'REALISE', 18.00, 'Excellente maitrise de Linux'),
 (5, 1, 'DIRECTION', '2026-09-10 15:00:00+03', 'Siege - Direction','REALISE', 17.00, NULL),
 -- Candidature 6 (entretien annule)
 (6, 3, 'TELEPHONE', '2026-09-10 10:00:00+03', 'Telephone',        'ANNULE',  NULL, 'Candidat injoignable'),
 -- Candidature 7
 (7, 3, 'TELEPHONE', '2026-07-22 10:00:00+03', 'Telephone',        'REALISE',  8.00, 'Profil trop junior'),
 -- Candidature 9 (retenue, REC-003)
 (9, 3, 'TELEPHONE', '2026-08-18 10:00:00+03', 'Telephone',        'REALISE', 15.00, NULL),
 (9, 3, 'RH',        '2026-08-27 14:00:00+03', 'Siege - Salle 1',  'REALISE', 16.00, NULL),
 (9, 6, 'TECHNIQUE', '2026-09-03 09:00:00+03', 'Agence Analakely', 'REALISE', 15.00, 'Bon sens commercial'),
 (9, 1, 'DIRECTION', '2026-09-17 15:00:00+03', 'Siege - Direction','REALISE', 16.00, NULL),
 -- Candidature 10 (liste d'attente)
 (10,3, 'TELEPHONE', '2026-08-19 10:00:00+03', 'Telephone',        'REALISE', 14.00, NULL),
 (10,3, 'RH',        '2026-08-28 14:00:00+03', 'Siege - Salle 1',  'REALISE', 14.50, NULL),
 (10,6, 'TECHNIQUE', '2026-09-04 09:00:00+03', 'Agence Analakely', 'REALISE', 13.00, NULL),
 -- Candidature 11 (entretien telephonique a venir)
 (11,3, 'TELEPHONE', '2026-10-27 09:30:00+03', 'Telephone',        'PLANIFIE',NULL, NULL);

INSERT INTO decision_recrutement (id_candidature, id_utilisateur_decideur, decision, motif, date_decision, date_creation) VALUES
 (1, 2,    'RETENU',        'Meilleur profil sur l''ensemble des entretiens',  '2026-05-18 10:00:00+03','2026-04-02 09:00:00+03'),
 (2, 2,    'REJETE',        'Niveau technique et pretentions inadaptes',       '2026-05-18 10:05:00+03','2026-04-05 10:00:00+03'),
 (3, 2,    'REJETE',        'Experience insuffisante',                         '2026-04-20 10:00:00+03','2026-04-08 11:00:00+03'),
 (4, NULL, 'EN_ATTENTE',    NULL,                                              NULL,                    '2026-07-12 09:30:00+03'),
 (5, 3,    'RETENU',        'Profil le plus complet, validee par la direction','2026-09-15 10:00:00+03','2026-07-10 08:00:00+03'),
 (7, 3,    'REJETE',        'Profil trop junior pour le poste',                '2026-07-25 10:00:00+03','2026-07-15 14:00:00+03'),
 (9, 3,    'RETENU',        'Premier poste pourvu',                            '2026-09-22 10:00:00+03','2026-08-10 09:00:00+03'),
 (10,3,    'LISTE_ATTENTE', 'Retenu pour le 2e poste si un desistement survient','2026-09-23 10:00:00+03','2026-08-12 10:00:00+03');

-- ------------------------------------------------------------
-- J. Employes issus du recrutement (lies a leur candidature)
-- ------------------------------------------------------------
INSERT INTO employe (id, matricule, nom, prenom, email, telephone, numero_cin, date_naissance, adresse,
                     id_candidature, id_utilisateur, id_departement, id_poste, date_embauche, date_depart, statut) VALUES
 (16,'EMP-0016','Rakotoarisoa','Hasina',   'hasina.rakotoarisoa@gmail.com',     '032 20 000 01','201234560001','1995-03-22','Lot 14 Ambohijatovo',   1,NULL,1,1,'2026-07-01',NULL,'ACTIF'),
 (17,'EMP-0017','Andrianarivelo','Mamitiana','mamitiana.andrianarivelo@gmail.com','032 20 000 08','201234560008','1998-12-02','Lot 3 Ankorondrano',    9,NULL,2,4,'2026-11-02',NULL,'ACTIF');

-- ------------------------------------------------------------
-- K. Contrats (un seul ACTIF par employe) et historique
-- ------------------------------------------------------------
INSERT INTO contrat (id, reference, id_employe, id_type_contrat, id_statut_contrat, id_contrat_precedent, id_utilisateur_createur,
                     date_debut, date_fin, periode_essai_jours, date_signature, salaire, nom_fichier, mime_type, contenu, date_creation) VALUES
 (1,  'CTR-2019-001', 1, 1, 3, NULL, NULL,'2019-03-04',NULL,        90,'2019-03-04', 4200000, NULL,NULL,NULL,'2019-02-25 09:00:00+03'),
 (2,  'CTR-2018-001', 2, 1, 3, NULL, NULL,'2018-06-01',NULL,        90,'2018-06-01', 3400000, NULL,NULL,NULL,'2018-05-20 09:00:00+03'),
 (3,  'CTR-2017-001', 3, 1, 3, NULL, NULL,'2017-09-11',NULL,        90,'2017-09-11', 3100000, NULL,NULL,NULL,'2017-09-01 09:00:00+03'),
 (4,  'CTR-2016-001', 4, 1, 3, NULL, NULL,'2016-02-15',NULL,        90,'2016-02-15', 2300000, NULL,NULL,NULL,'2016-02-01 09:00:00+03'),
 (5,  'CTR-2020-001', 5, 1, 3, NULL, NULL,'2020-01-13',NULL,        90,'2020-01-13', 2200000, NULL,NULL,NULL,'2020-01-02 09:00:00+03'),
 (6,  'CTR-2021-001', 6, 2, 4, NULL, 2,   '2021-05-03','2022-05-02',30,'2021-05-03', 1500000, NULL,NULL,NULL,'2021-04-26 09:00:00+03'),
 (7,  'CTR-2022-001', 6, 1, 3, 6,    2,   '2022-05-03',NULL,        0, '2022-05-03', 1900000, NULL,NULL,NULL,'2022-04-25 09:00:00+03'),
 (8,  'CTR-2019-002', 7, 1, 3, NULL, NULL,'2019-10-01',NULL,        90,'2019-10-01', 1800000, NULL,NULL,NULL,'2019-09-20 09:00:00+03'),
 (9,  'CTR-2022-002', 8, 1, 3, NULL, 2,   '2022-02-01',NULL,        90,'2022-02-01', 2200000, NULL,NULL,NULL,'2022-01-20 09:00:00+03'),
 (10, 'CTR-2023-001', 9, 1, 3, NULL, 2,   '2023-09-18',NULL,        90,'2023-09-18', 2000000, NULL,NULL,NULL,'2023-09-05 09:00:00+03'),
 (11, 'CTR-2021-002', 10,1, 5, NULL, 2,   '2021-04-12','2026-06-30',90,'2021-04-12', 2400000, NULL,NULL,NULL,'2021-04-01 09:00:00+03'),
 (12, 'CTR-2022-003', 11,1, 3, NULL, 2,   '2022-07-04',NULL,        90,'2022-07-04', 1200000, NULL,NULL,NULL,'2022-06-22 09:00:00+03'),
 (13, 'CTR-2024-001', 12,1, 3, NULL, 2,   '2024-01-08',NULL,        90,'2024-01-08', 1000000, NULL,NULL,NULL,'2023-12-27 09:00:00+03'),
 (14, 'CTR-2020-002', 13,1, 3, NULL, 2,   '2020-08-24',NULL,        90,'2020-08-24',  700000, NULL,NULL,NULL,'2020-08-10 09:00:00+03'),
 (15, 'CTR-2023-002', 14,1, 3, NULL, 2,   '2023-03-06',NULL,        90,'2023-03-06', 1300000, NULL,NULL,NULL,'2023-02-20 09:00:00+03'),
 (16, 'CTR-2019-003', 15,1, 4, NULL, NULL,'2019-11-04','2025-12-31',90,'2019-11-04',  650000, NULL,NULL,NULL,'2019-10-25 09:00:00+03'),
 -- Embauche REC-001 : contrat signe, actif
 (17, 'CTR-2026-001', 16,1, 3, NULL, 2,   '2026-07-01',NULL,        90,'2026-07-01', 2600000,
      'contrat_CTR-2026-001.pdf','application/pdf', convert_to('%PDF-1.4 Contrat CDI fictif CTR-2026-001','UTF8'),'2026-06-20 10:00:00+03'),
 -- Embauche REC-003 : CDD en attente de signature (token disponible ci-dessous)
 (18, 'CTR-2026-002', 17,2, 2, NULL, 3,   '2026-11-02','2027-11-01',30,NULL,         1100000,
      'contrat_CTR-2026-002.pdf','application/pdf', convert_to('%PDF-1.4 Contrat CDD fictif CTR-2026-002','UTF8'),'2026-10-20 10:00:00+03'),
 -- Brouillon de revalorisation (le contrat 15 reste ACTIF)
 (19, 'CTR-2026-003', 14,1, 1, 15,   3,   '2027-01-01',NULL,        0, NULL,         1450000, NULL,NULL,NULL,'2026-10-24 11:00:00+03');

-- signature_image : PNG 1x1 pixel fictif
INSERT INTO historique_contrat (id_contrat, id_ancien_statut, id_statut_contrat, id_utilisateur_auteur, evenement, motif,
                                date_modification, ancien_salaire, nouveau_salaire, ancienne_date_fin, nouvelle_date_fin, signature_image) VALUES
 (1,  NULL,3, 2,    'CREATION',        'Contrat initial',                      '2019-03-04 09:00:00+03', NULL,    4200000-600000, NULL, NULL, NULL),
 (1,  3,   3, 2,    'AUGMENTATION',    'Revalorisation annuelle',              '2024-01-01 09:00:00+03', 3600000, 4200000,        NULL, NULL, NULL),
 (6,  NULL,3, 2,    'CREATION',        'CDD initial',                          '2021-05-03 09:00:00+03', NULL,    1500000,        NULL, '2022-05-02', NULL),
 (6,  3,   4, 2,    'FIN_CONTRAT',     'Passage en CDI',                       '2022-05-02 17:00:00+03', NULL,    NULL,           NULL, NULL, NULL),
 (7,  NULL,3, 2,    'CREATION',        'CDI faisant suite au CDD CTR-2021-001','2022-05-03 09:00:00+03', NULL,    1900000,        NULL, NULL, NULL),
 (11, 3,   5, 2,    'RESILIATION',     'Demission',                            '2026-05-28 11:00:00+03', NULL,    NULL,           NULL, '2026-06-30', NULL),
 (14, 3,   3, 2,    'AUGMENTATION',    'Revalorisation annuelle',              '2025-01-01 09:00:00+03', 650000,  700000,         NULL, NULL, NULL),
 (16, 3,   4, 2,    'FIN_CONTRAT',     'Demission',                            '2025-12-31 17:00:00+03', NULL,    NULL,           NULL, '2025-12-31', NULL),
 (17, NULL,1, 2,    'CREATION',        'Contrat genere apres decision RETENU', '2026-06-20 10:00:00+03', NULL,    2600000,        NULL, NULL, NULL),
 (17, 1,   2, 2,    'ENVOI_SIGNATURE', 'Lien de signature envoye au candidat', '2026-06-26 09:00:00+03', NULL,    NULL,           NULL, NULL, NULL),
 (17, 2,   3, NULL, 'SIGNATURE',       'Signature electronique du salarie',    '2026-07-01 08:30:00+03', NULL,    NULL,           NULL, NULL,
      decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==','base64')),
 (18, NULL,1, 3,    'CREATION',        'Contrat genere apres decision RETENU', '2026-10-20 10:00:00+03', NULL,    1100000,        NULL, '2027-11-01', NULL),
 (18, 1,   2, 3,    'ENVOI_SIGNATURE', 'Lien de signature envoye au candidat', '2026-10-22 09:00:00+03', NULL,    NULL,           NULL, NULL, NULL),
 (19, NULL,1, 3,    'CREATION',        'Brouillon de revalorisation',          '2026-10-24 11:00:00+03', NULL,    1450000,        NULL, NULL, NULL);

-- Tokens de signature. Tokens EN CLAIR (a utiliser dans l'URL de test) :
--   tok_c17_utilise_2026  -> deja utilise (contrat 17)
--   tok_c18_expire_2026   -> expire, non utilise (contrat 18)
--   tok_c18_valide_2026   -> valide ~2 jours (contrat 18)
INSERT INTO signature_token (id_contrat, token_hash, email_destinataire, date_creation, date_expiration, utilise, date_utilisation) VALUES
 (17, encode(digest('tok_c17_utilise_2026','sha256'),'hex'), 'hasina.rakotoarisoa@gmail.com',
      '2026-06-26 09:00:00+03','2026-07-03 09:00:00+03', TRUE, '2026-07-01 08:30:00+03'),
 (18, encode(digest('tok_c18_expire_2026','sha256'),'hex'),  'mamitiana.andrianarivelo@gmail.com',
      NOW() - INTERVAL '5 days', NOW() - INTERVAL '2 days', FALSE, NULL),
 (18, encode(digest('tok_c18_valide_2026','sha256'),'hex'),  'mamitiana.andrianarivelo@gmail.com',
      NOW() - INTERVAL '1 day',  NOW() + INTERVAL '2 days', FALSE, NULL);

-- ------------------------------------------------------------
-- L. Onboarding et documents
-- ------------------------------------------------------------
INSERT INTO modele_tache_onboarding (id, code, libelle, categorie, delai_jours, actif) VALUES
 (1, 'CONTRAT_SIGNE',      'Faire signer le contrat de travail',                  'ADMINISTRATIF', 0, TRUE),
 (2, 'COMPTE_EMAIL',       'Creer le compte email et les acces',                  'INFORMATIQUE',  0, TRUE),
 (3, 'POSTE_TRAVAIL',      'Preparer l''ordinateur et le poste de travail',       'INFORMATIQUE',  1, TRUE),
 (4, 'ACCUEIL_VISITE',     'Accueil et visite des locaux',                        'ACCUEIL',       0, TRUE),
 (5, 'PRESENTATION_EQUIPE','Presentation a l''equipe',                            'ACCUEIL',       1, TRUE),
 (6, 'FORMATION_SECURITE', 'Formation securite et reglement interieur',           'FORMATION',     7, TRUE),
 (7, 'DOSSIER_ADMIN',      'Completer le dossier administratif (CIN, RIB, diplomes)','ADMINISTRATIF',3, TRUE),
 (8, 'BADGE_ACCES',        'Remettre le badge d''acces (ancien processus)',       'AUTRE',         1, FALSE);

INSERT INTO onboarding (id, id_employe, id_utilisateur_responsable, date_debut, date_fin_prevue, statut, date_creation) VALUES
 (1, 16, 2, '2026-07-01', '2026-07-31', 'TERMINE',  '2026-06-26 10:00:00+03'),
 (2, 17, 3, '2026-11-02', '2026-11-30', 'EN_COURS', '2026-10-22 10:00:00+03');

INSERT INTO tache_onboarding (id_onboarding, id_modele, id_utilisateur_responsable, libelle, categorie, date_echeance, statut, date_realisation, remarque) VALUES
 -- Onboarding 1 (termine)
 (1, 1, 2, 'Faire signer le contrat de travail',                       'ADMINISTRATIF','2026-07-01','FAIT',  '2026-07-01 08:30:00+03', NULL),
 (1, 2, 5, 'Creer le compte email et les acces',                       'INFORMATIQUE', '2026-07-01','FAIT',  '2026-06-30 16:00:00+03', NULL),
 (1, 3, 5, 'Preparer l''ordinateur et le poste de travail',            'INFORMATIQUE', '2026-07-02','FAIT',  '2026-07-01 10:00:00+03', NULL),
 (1, 4, 5, 'Accueil et visite des locaux',                             'ACCUEIL',      '2026-07-01','FAIT',  '2026-07-01 09:00:00+03', NULL),
 (1, 5, 5, 'Presentation a l''equipe',                                 'ACCUEIL',      '2026-07-02','FAIT',  '2026-07-02 11:00:00+03', NULL),
 (1, 6, 3, 'Formation securite et reglement interieur',                'FORMATION',    '2026-07-08','FAIT',  '2026-07-07 15:00:00+03', NULL),
 (1, 7, 2, 'Completer le dossier administratif (CIN, RIB, diplomes)',  'ADMINISTRATIF','2026-07-04','FAIT',  '2026-07-03 12:00:00+03', NULL),
 (1, 8, 2, 'Remettre le badge d''acces (ancien processus)',            'AUTRE',        '2026-07-02','ANNULE',NULL,                     'Processus supprime'),
 -- Onboarding 2 (en preparation, arrivee le 02/11/2026)
 (2, 1, 3, 'Faire signer le contrat de travail',                       'ADMINISTRATIF','2026-11-02','A_FAIRE',NULL, 'En attente de signature electronique'),
 (2, 2, 5, 'Creer le compte email et les acces',                       'INFORMATIQUE', '2026-11-02','EN_COURS',NULL,NULL),
 (2, 3, 5, 'Preparer l''ordinateur et le poste de travail',            'INFORMATIQUE', '2026-11-03','A_FAIRE',NULL, NULL),
 (2, 4, 6, 'Accueil et visite des locaux',                             'ACCUEIL',      '2026-11-02','A_FAIRE',NULL, NULL),
 (2, 5, 6, 'Presentation a l''equipe',                                 'ACCUEIL',      '2026-11-03','A_FAIRE',NULL, NULL),
 (2, 6, 3, 'Formation securite et reglement interieur',                'FORMATION',    '2026-11-09','A_FAIRE',NULL, NULL),
 (2, 7, 3, 'Completer le dossier administratif (CIN, RIB, diplomes)',  'ADMINISTRATIF','2026-11-05','EN_COURS',NULL,'Dossier partiellement depose');

INSERT INTO document_employe (id_employe, type_document, nom_fichier, mime_type, bytes, statut, date_depot) VALUES
 (1,  'CIN',     'cin_rakoto.pdf',        'application/pdf', convert_to('Scan fictif CIN Rakoto Jean','UTF8'),               'VALIDE', '2019-03-05 10:00:00+03'),
 (16, 'CIN',     'cin_rakotoarisoa.pdf',  'application/pdf', convert_to('Scan fictif CIN Rakotoarisoa Hasina','UTF8'),       'VALIDE', '2026-07-02 09:00:00+03'),
 (16, 'DIPLOME', 'diplome_rakotoarisoa.pdf','application/pdf',convert_to('Scan fictif diplome Rakotoarisoa Hasina','UTF8'),  'VALIDE', '2026-07-02 09:05:00+03'),
 (16, 'RIB',     'rib_rakotoarisoa.pdf',  'application/pdf', convert_to('Scan fictif RIB Rakotoarisoa Hasina','UTF8'),       'VALIDE', '2026-07-02 09:10:00+03'),
 (16, 'PHOTO',   'photo_rakotoarisoa.jpg','image/jpeg',      convert_to('Photo fictive Rakotoarisoa Hasina','UTF8'),         'FOURNI', '2026-07-02 09:15:00+03'),
 (17, 'CIN',     'cin_andrianarivelo.pdf','application/pdf', convert_to('Scan fictif CIN Andrianarivelo Mamitiana','UTF8'),  'FOURNI', '2026-10-23 10:00:00+03'),
 (17, 'DIPLOME', 'diplome_andrianarivelo.pdf','application/pdf',convert_to('Scan fictif diplome Andrianarivelo','UTF8'),      'FOURNI', '2026-10-23 10:05:00+03'),
 (17, 'RIB',     'rib_andrianarivelo.pdf','application/pdf', convert_to('Scan RIB illisible','UTF8'),                        'REFUSE', '2026-10-23 10:10:00+03'),
 (17, 'CERTIFICAT_MEDICAL','certificat_andrianarivelo.pdf','application/pdf', convert_to('Scan fictif certificat medical','UTF8'),'FOURNI','2026-10-24 08:00:00+03');

-- ------------------------------------------------------------
-- M. Budgets et depenses (MGA)
--    Finance : budget 2026 = 30 M, depenses = 24 M -> reste 6 M,
--    insuffisant pour le besoin B4 (comptable ~21,6 M/an) : cas de test "budget insuffisant".
-- ------------------------------------------------------------
INSERT INTO budget_departement (id_departement, annee, montant, devise) VALUES
 (1, 2025, 150000000, 'MGA'), (1, 2026, 180000000, 'MGA'),
 (2, 2025,  90000000, 'MGA'), (2, 2026, 120000000, 'MGA'),
 (3, 2025,  28000000, 'MGA'), (3, 2026,  30000000, 'MGA'),
 (4, 2025, 180000000, 'MGA'), (4, 2026, 200000000, 'MGA'),
 (5, 2025,  45000000, 'MGA'), (5, 2026,  50000000, 'MGA');

-- Un depense est liee soit a un besoin, soit a un contrat, soit a aucun.
INSERT INTO depense_departement (id_departement, id_besoin, id_contrat, libelle, categorie, montant, date_depense) VALUES
 (1, NULL, NULL, 'Licences logicielles 2025',                 'INFRASTRUCTURE', 10000000, '2025-02-12'),
 (1, NULL, NULL, 'Licences logicielles 2026',                 'INFRASTRUCTURE', 12000000, '2026-02-10'),
 (1, 1,    NULL, 'Annonces recrutement Developpeur Full Stack','RECRUTEMENT',       450000, '2026-03-20'),
 (1, 2,    NULL, 'Annonces recrutement Administrateur Systemes','RECRUTEMENT',      600000, '2026-07-02'),
 (1, NULL, 17,   'Salaires juillet a septembre 2026 (EMP-0016)','SALAIRE',         7800000, '2026-09-30'),
 (2, NULL, NULL, 'Campagne marketing printemps',              'MARKETING',      25000000, '2026-05-15'),
 (2, 3,    NULL, 'Annonces recrutement Commerciaux terrain',  'RECRUTEMENT',       350000, '2026-08-04'),
 (3, NULL, NULL, 'Audit externe',                             'PRESTATION',     12000000, '2026-03-10'),
 (3, NULL, NULL, 'Logiciel comptable',                        'INFRASTRUCTURE',  9000000, '2026-05-02'),
 (3, NULL, NULL, 'Formation fiscalite',                       'FORMATION',       3000000, '2026-09-12'),
 (4, NULL, NULL, 'Maintenance equipements 2025',              'MAINTENANCE',    35000000, '2025-06-10'),
 (4, NULL, NULL, 'Maintenance equipements 2026',              'MAINTENANCE',    40000000, '2026-06-01'),
 (5, NULL, NULL, 'Abonnement plateforme de recrutement',      'RECRUTEMENT',     4800000, '2026-01-15');

-- ------------------------------------------------------------
-- N. Notifications
-- ------------------------------------------------------------
INSERT INTO notification (id_utilisateur, id_besoin, id_candidature, type_notification, titre, message, date_creation, date_lecture) VALUES
 (2, 4,    NULL,'VALIDATION_REQUISE','Nouveau besoin a valider',          'Le besoin #4 (Comptable) attend votre validation RH.',            '2026-10-20 14:00:00+03', NULL),
 (4, 5,    NULL,'VALIDATION_REQUISE','Besoin a valider (Finance)',        'Le besoin #5 (Operateur de production) attend votre validation.', '2026-10-14 10:30:00+03', NULL),
 (1, 6,    NULL,'VALIDATION_REQUISE','Besoin a valider (Direction)',      'Le besoin #6 (Technicien de maintenance) attend votre decision.', '2026-10-08 17:00:00+03', NULL),
 (5, 1,    NULL,'BESOIN_ACCEPTE',    'Besoin accepte',                    'Votre besoin #1 (Developpeur Full Stack) a ete accepte.',         '2026-03-09 15:00:00+03', '2026-03-09 16:00:00+03'),
 (5, 2,    NULL,'BESOIN_ACCEPTE',    'Besoin accepte',                    'Votre besoin #2 (Administrateur Systemes) a ete accepte.',        '2026-06-15 11:00:00+03', '2026-06-15 11:30:00+03'),
 (6, 7,    NULL,'BESOIN_REFUSE',     'Besoin refuse',                     'Votre besoin #7 a ete refuse par la Finance : budget insuffisant.','2026-09-04 12:00:00+03','2026-09-04 14:00:00+03'),
 (9, 8,    NULL,'BESOIN_REFUSE',     'Besoin refuse',                     'Votre besoin #8 a ete refuse par la Direction.',                  '2026-08-18 09:30:00+03', NULL),
 (2, NULL, 11,  'NOUVELLE_CANDIDATURE','Nouvelle candidature',            'Feno Ramarosaona a postule a REC-2026-003.',                      '2026-08-20 09:00:00+03', NULL),
 (5, NULL, 4,   'ENTRETIEN_PLANIFIE','Entretien technique planifie',     'Entretien technique de Sitraka Randriamampionona le 29/10/2026 a 10h.','2026-10-20 09:00:00+03', NULL),
 (6, NULL, 9,   'CANDIDAT_RETENU',   'Candidat retenu',                   'Mamitiana Andrianarivelo est retenu pour REC-2026-003.',          '2026-09-22 10:00:00+03', '2026-09-22 11:00:00+03'),
 (3, NULL, 5,   'CANDIDAT_RETENU',   'Contrat a preparer',                'Mirana Rakotoson est retenue : creez l''employe et le contrat.',  '2026-09-15 10:00:00+03', NULL);

-- ------------------------------------------------------------
-- O. Remise a niveau des sequences (ids explicites utilises ci-dessus)
-- ------------------------------------------------------------
SELECT setval(pg_get_serial_sequence('role','id'),                      (SELECT MAX(id) FROM role));
SELECT setval(pg_get_serial_sequence('type_besoin','id'),               (SELECT MAX(id) FROM type_besoin));
SELECT setval(pg_get_serial_sequence('etat_validation','id'),           (SELECT MAX(id) FROM etat_validation));
SELECT setval(pg_get_serial_sequence('decision','id'),                  (SELECT MAX(id) FROM decision));
SELECT setval(pg_get_serial_sequence('statut_contract','id'),           (SELECT MAX(id) FROM statut_contract));
SELECT setval(pg_get_serial_sequence('type_contrat','id'),              (SELECT MAX(id) FROM type_contrat));
SELECT setval(pg_get_serial_sequence('utilisateur','id'),               (SELECT MAX(id) FROM utilisateur));
SELECT setval(pg_get_serial_sequence('departement','id'),               (SELECT MAX(id) FROM departement));
SELECT setval(pg_get_serial_sequence('poste','id'),                     (SELECT MAX(id) FROM poste));
SELECT setval(pg_get_serial_sequence('employe','id'),                   (SELECT MAX(id) FROM employe));
SELECT setval(pg_get_serial_sequence('besoin','id'),                    (SELECT MAX(id) FROM besoin));
SELECT setval(pg_get_serial_sequence('recrutement','id'),               (SELECT MAX(id) FROM recrutement));
SELECT setval(pg_get_serial_sequence('offre_emploi','id'),              (SELECT MAX(id) FROM offre_emploi));
SELECT setval(pg_get_serial_sequence('publication_offre','id'),         (SELECT MAX(id) FROM publication_offre));
SELECT setval(pg_get_serial_sequence('candidat','id'),                  (SELECT MAX(id) FROM candidat));
SELECT setval(pg_get_serial_sequence('cv','id'),                        (SELECT MAX(id) FROM cv));
SELECT setval(pg_get_serial_sequence('candidature','id'),               (SELECT MAX(id) FROM candidature));
SELECT setval(pg_get_serial_sequence('contrat','id'),                   (SELECT MAX(id) FROM contrat));
SELECT setval(pg_get_serial_sequence('modele_tache_onboarding','id'),   (SELECT MAX(id) FROM modele_tache_onboarding));
SELECT setval(pg_get_serial_sequence('onboarding','id'),                (SELECT MAX(id) FROM onboarding));

COMMIT;

-- ------------------------------------------------------------
-- Verifications rapides
-- ------------------------------------------------------------
-- Comptes : 10 lignes, hash commencant par $2a$
SELECT u.id, u.email, r.code AS role, u.actif, left(u.mdp, 4) AS debut_hash
FROM utilisateur u JOIN role r ON r.id = u.id_role ORDER BY u.id;

-- Un manager par departement
SELECT d.nom AS departement, u.email AS manager, d.effectif_max,
       (SELECT COUNT(*) FROM employe e WHERE e.id_departement = d.id AND e.statut = 'ACTIF') AS effectif_actif
FROM departement d JOIN utilisateur u ON u.id = d.id_utilisateur_manager ORDER BY d.id;

-- Etat des besoins
SELECT b.id, p.nom AS poste, b.statut_global FROM besoin b JOIN poste p ON p.id = b.id_poste ORDER BY b.id;
