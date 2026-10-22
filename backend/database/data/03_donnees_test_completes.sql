-- ============================================================
-- SIG RH - JEU DE DONNEES DE TEST COMPLET (volume maximal)
-- ============================================================
-- A executer APRES init_tables.sql, sur une base de DEVELOPPEMENT.
--
-- ATTENTION : ce script VIDE d'abord toutes les tables de donnees
-- (utilisateurs, departements, besoins, candidats, contrats...) puis les
-- recree. Les tables de reference (role, type_besoin, etat_validation,
-- decision, statut_contract, type_contrat, modele_tache_onboarding) sont
-- conservees. Le script peut etre relance autant de fois que necessaire.
--
-- Principe : chaque bloc dispose de donnees "en amont" deja pretes, donc
-- aucun bloc ne depend du travail des autres (voir la requete finale
-- "points d'entree" qui compte ces donnees).
--
-- Comptes de connexion (mot de passe pour TOUS : Test1234!)
--   manager@exemple.mg   rh@exemple.mg   finance@exemple.mg   direction@exemple.mg
--   + manager02..manager12, rh02..rh05, finance02..finance04, direction02..direction03 @exemple.mg
--
-- Les dates sont calculees par rapport a CURRENT_DATE : les donnees restent
-- "recentes" quel que soit le jour d'execution.
-- Le fichier est volontairement en ASCII (sans accents).
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

BEGIN;

-- 0. Verification des donnees de reference ---------------------------------
DO $$
BEGIN
    IF (SELECT count(*) FROM role) < 4
       OR (SELECT count(*) FROM type_besoin) < 3
       OR (SELECT count(*) FROM etat_validation) < 3
       OR (SELECT count(*) FROM decision) < 3
       OR (SELECT count(*) FROM statut_contract) < 5
       OR (SELECT count(*) FROM type_contrat) < 3
       OR (SELECT count(*) FROM modele_tache_onboarding) < 7 THEN
        RAISE EXCEPTION 'Donnees de reference absentes : executez d abord init_tables.sql';
    END IF;
END $$;

-- Vidage des tables de donnees (les references ne sont pas touchees)
TRUNCATE notification, document_employe, tache_onboarding, onboarding,
         depense_departement, budget_departement, historique_contrat, contrat,
         decision_recrutement, entretien, candidature, cv, candidat,
         publication_offre, offre_emploi, recrutement, validation_besoin,
         besoin, employe, grille_salaire, poste, departement, utilisateur
RESTART IDENTITY CASCADE;

-- Fonctions temporaires (disparaissent a la fin de la session) ---------------
CREATE OR REPLACE FUNCTION pg_temp.nom(n int) RETURNS text AS $f$
    SELECT (ARRAY['Rakoto','Rasoa','Randria','Rabe','Ravelo','Andriamihaja','Rakotomalala',
                  'Razafy','Ratsimba','Rajaonarison','Andrianarisoa','Rasolofo','Ramanana',
                  'Raharison','Rakotondrabe','Randrianasolo','Rabemananjara','Ratovo',
                  'Rasamoely','Razanakolona'])[1 + abs(n) % 20]
$f$ LANGUAGE sql IMMUTABLE;

CREATE OR REPLACE FUNCTION pg_temp.prenom(n int) RETURNS text AS $f$
    SELECT (ARRAY['Hery','Mialy','Tojo','Lanto','Hanta','Rivo','Fanja','Niry','Toky','Faly',
                  'Miora','Voahangy','Andry','Sitraka','Zo','Rado','Lova','Tiana','Soa','Rina',
                  'Nirina','Koloina','Mahefa','Haja','Ando','Fidy','Aina','Nomena','Tahina','Mamy'])[1 + abs(n) % 30]
$f$ LANGUAGE sql IMMUTABLE;

-- Petit PDF valide contenant un texte (utilise pour les CV, contrats, documents)
CREATE OR REPLACE FUNCTION pg_temp.pdf_minimal(titre text) RETURNS bytea AS $f$
DECLARE
    contenu text := 'BT /F1 16 Tf 30 70 Td (' ||
                    replace(replace(replace(titre, '\', '\\'), '(', '\('), ')', '\)') || ') Tj ET';
BEGIN
    RETURN convert_to(
        E'%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n' ||
        E'2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n' ||
        E'3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 400 140]/Contents 4 0 R/Resources<</Font<</F1 5 0 R>>>>>>endobj\n' ||
        E'4 0 obj<</Length ' || length(contenu) || E'>>stream\n' || contenu || E'\nendstream\nendobj\n' ||
        E'5 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj\n' ||
        E'trailer<</Root 1 0 R>>\n%%EOF', 'UTF8');
END;
$f$ LANGUAGE plpgsql;

-- Image PNG 1x1 (photo d'identite factice)
CREATE OR REPLACE FUNCTION pg_temp.png_minimal() RETURNS bytea AS $f$
    SELECT decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==', 'base64')
$f$ LANGUAGE sql IMMUTABLE;

-- 1. UTILISATEURS (24 comptes : 12 managers, 5 RH, 4 finance, 3 direction) -----
INSERT INTO utilisateur (id_role, nom, prenom, email, mdp, date_creation)
SELECT r.id, v.nom, v.prenom, v.email, h.hash, now() - interval '400 days'
FROM (VALUES
    ('MANAGER',   'Rakoto', 'Jean',  'manager@exemple.mg'),
    ('RH',        'Lanto',  'Mialy', 'rh@exemple.mg'),
    ('FINANCE',   'Rasoa',  'Hanta', 'finance@exemple.mg'),
    ('DIRECTION', 'Andry',  'Tojo',  'direction@exemple.mg')
) AS v(role_code, nom, prenom, email)
JOIN role r ON r.code = v.role_code
CROSS JOIN (SELECT crypt('Test1234!', gen_salt('bf', 10)) AS hash) h;

INSERT INTO utilisateur (id_role, nom, prenom, email, mdp, date_creation)
SELECT r.id,
       pg_temp.nom(g.n * 7 + r.id), pg_temp.prenom(g.n * 11 + r.id),
       lower(r.code) || lpad(g.n::text, 2, '0') || '@exemple.mg',
       h.hash,
       now() - make_interval(days => 20 + g.n * 20)
FROM (VALUES ('MANAGER', 11), ('RH', 4), ('FINANCE', 3), ('DIRECTION', 2)) AS x(code, nb)
JOIN role r ON r.code = x.code
CROSS JOIN LATERAL generate_series(2, x.nb + 1) AS g(n)
CROSS JOIN (SELECT crypt('Test1234!', gen_salt('bf', 10)) AS hash) h
ORDER BY r.id, g.n;

-- Table de correspondance role -> liste d'identifiants (sert aux affectations)
CREATE TEMP TABLE tmp_val ON COMMIT DROP AS
SELECT r.code, array_agg(u.id ORDER BY u.id) AS ids
FROM utilisateur u JOIN role r ON r.id = u.id_role
GROUP BY r.code;

-- 2. DEPARTEMENTS (10) -----------------------------------------------------
INSERT INTO departement (id_utilisateur_manager, nom, effectif_max)
SELECT m.id, d.nom, 100
FROM (VALUES
    (1, 'Informatique'), (2, 'Commercial'), (3, 'Ressources humaines'), (4, 'Finance'),
    (5, 'Marketing'), (6, 'Logistique'), (7, 'Production'), (8, 'Juridique'),
    (9, 'Service client'), (10, 'Qualite')
) AS d(rang, nom)
JOIN (SELECT u.id, row_number() OVER (ORDER BY u.id) AS rang
      FROM utilisateur u JOIN role r ON r.id = u.id_role
      WHERE r.code = 'MANAGER') m ON m.rang = d.rang
ORDER BY d.rang;

-- 3. POSTES (60, dont 3 inactifs) -------------------------------------------
INSERT INTO poste (id_departement, nom)
SELECT d.id, v.nom
FROM (VALUES
    ('Informatique', 'Developpeur Java'), ('Informatique', 'Developpeur Front-end'),
    ('Informatique', 'Administrateur systemes'), ('Informatique', 'Technicien reseau'),
    ('Informatique', 'Ingenieur DevOps'), ('Informatique', 'Analyste QA'),
    ('Commercial', 'Charge de clientele'), ('Commercial', 'Commercial terrain'),
    ('Commercial', 'Responsable de zone'), ('Commercial', 'Assistant commercial'),
    ('Commercial', 'Key account manager'), ('Commercial', 'Telephoniste'),
    ('Ressources humaines', 'Assistant RH'), ('Ressources humaines', 'Charge de recrutement'),
    ('Ressources humaines', 'Gestionnaire de paie'), ('Ressources humaines', 'Responsable formation'),
    ('Ressources humaines', 'Charge de relations sociales'), ('Ressources humaines', 'Juriste social'),
    ('Finance', 'Comptable'), ('Finance', 'Analyste financier'),
    ('Finance', 'Controleur de gestion'), ('Finance', 'Tresorier'),
    ('Finance', 'Assistant comptable'), ('Finance', 'Auditeur interne'),
    ('Marketing', 'Responsable marketing'), ('Marketing', 'Community manager'),
    ('Marketing', 'Graphiste'), ('Marketing', 'Charge de communication'),
    ('Marketing', 'Chef de produit'), ('Marketing', 'Redacteur web'),
    ('Logistique', 'Magasinier'), ('Logistique', 'Responsable logistique'),
    ('Logistique', 'Chauffeur livreur'), ('Logistique', 'Gestionnaire de stock'),
    ('Logistique', 'Approvisionneur'), ('Logistique', 'Agent de transit'),
    ('Production', 'Operateur de production'), ('Production', 'Chef equipe'),
    ('Production', 'Technicien de maintenance'), ('Production', 'Responsable production'),
    ('Production', 'Controleur qualite'), ('Production', 'Planificateur'),
    ('Juridique', 'Juriste d entreprise'), ('Juridique', 'Assistant juridique'),
    ('Juridique', 'Responsable conformite'), ('Juridique', 'Charge de contrats'),
    ('Juridique', 'Paralegal'), ('Juridique', 'Secretaire juridique'),
    ('Service client', 'Conseiller clientele'), ('Service client', 'Superviseur centre appels'),
    ('Service client', 'Charge de reclamations'), ('Service client', 'Agent accueil'),
    ('Service client', 'Responsable service client'), ('Service client', 'Formateur client'),
    ('Qualite', 'Responsable qualite'), ('Qualite', 'Auditeur qualite'),
    ('Qualite', 'Technicien laboratoire'), ('Qualite', 'Charge hygiene securite'),
    ('Qualite', 'Animateur qualite'), ('Qualite', 'Metrologue')
) AS v(dep, nom)
JOIN departement d ON d.nom = v.dep
ORDER BY d.id, v.nom;

UPDATE poste SET actif = FALSE WHERE nom IN ('Paralegal', 'Metrologue', 'Agent de transit');

CREATE TEMP TABLE tmp_poste_actif ON COMMIT DROP AS
SELECT id, id_departement, row_number() OVER (ORDER BY id) AS rn
FROM poste WHERE actif;

-- 4. GRILLES SALARIALES (2 periodes par poste : historique + en cours) --------
INSERT INTO grille_salaire (id_poste, valeur_min, valeur_max, date_debut_validite, date_fin_validite)
SELECT p.id, round(b.base * f.fmin, -3), round(b.base * f.fmax, -3), f.debut, f.fin
FROM poste p
CROSS JOIN LATERAL (SELECT (1000000 + ((p.id * 137) % 20) * 150000)::numeric AS base) b
CROSS JOIN (VALUES
    (0.90, 1.45, date_trunc('year', current_date - interval '3 years')::date,
                 (date_trunc('year', current_date)::date - 1)),
    (1.00, 1.60, date_trunc('year', current_date)::date, NULL::date)
) AS f(fmin, fmax, debut, fin)
ORDER BY p.id, f.debut;

-- 5. EMPLOYES (320) -----------------------------------------------------------
INSERT INTO employe (matricule, nom, prenom, email, telephone, numero_cin, date_naissance,
                     adresse, id_departement, id_poste, date_embauche, date_depart, statut, date_creation)
SELECT 'MAT-' || lpad(x.n::text, 5, '0'),
       x.nom, x.prenom,
       lower(x.prenom) || '.' || lower(x.nom) || '.' || x.n || '@entreprise.mg',
       '03' || (2 + x.n % 4)::text || lpad(((x.n * 7919) % 10000000)::text, 7, '0'),
       '1' || lpad(x.n::text, 11, '0'),
       DATE '1965-01-01' + ((x.n * 3571) % 11000),
       'Lot ' || (1 + x.n % 150) || ' ' ||
       (ARRAY['Analakely','Ankorondrano','Ivandry','Andraharo','Tsaralalana','Isoraka',
              'Ambohijatovo','67 Ha','Ampefiloha','Behoririka'])[1 + x.n % 10] || ', Antananarivo',
       p.id_departement, p.id,
       x.embauche,
       CASE WHEN x.statut = 'PARTI' THEN x.embauche + ((x.n * 29) % 600 + 90) END,
       x.statut,
       x.embauche::timestamptz - interval '10 days'
FROM (
    SELECT g.n, pg_temp.nom(g.n * 7) AS nom, pg_temp.prenom(g.n * 11) AS prenom,
           CASE WHEN g.n % 12 = 0 THEN 'PARTI' WHEN g.n % 17 = 0 THEN 'SUSPENDU' ELSE 'ACTIF' END AS statut,
           CASE WHEN g.n % 12 = 0 THEN current_date - ((g.n * 53) % 2500 + 800)
                WHEN g.n % 9 = 0 THEN current_date - (g.n % 60 + 5)
                ELSE current_date - ((g.n * 53) % 3650 + 30) END AS embauche
    FROM generate_series(1, 320) AS g(n)
) x
JOIN tmp_poste_actif p ON p.rn = 1 + (x.n - 1) % (SELECT count(*) FROM tmp_poste_actif)
ORDER BY x.n;

CREATE TEMP TABLE tmp_emp_poste ON COMMIT DROP AS
SELECT id_poste, array_agg(id ORDER BY id) AS ids FROM employe GROUP BY id_poste;

-- 6. BLOC 1 : BESOINS (200) ET VALIDATIONS ---------------------------------------
-- Les besoins anciens (n <= 100) sont deja traites ; les 100 derniers contiennent
-- les demandes en attente de validation RH, Finance et Direction.
INSERT INTO besoin (id_type_besoin, id_poste, id_utilisateur_demandeur, id_employe_remplace,
                    id_type_contrat_souhaite, effectif, priorite, date_arrivee_souhaitee,
                    date_demande, lieu_travail, motif, experience_requise, niveau_etude,
                    competences_requises, salaire_estime, statut_global, date_modification)
SELECT tb.id, p.id, d.id_utilisateur_manager,
       CASE WHEN x.type_code = 'REMPLACEMENT'
            THEN tep.ids[1 + x.n % array_length(tep.ids, 1)] END,
       tc.id,
       CASE WHEN x.n % 10 = 0 THEN 3 WHEN x.n % 5 = 0 THEN 2 ELSE 1 END,
       (ARRAY['BASSE','NORMALE','NORMALE','HAUTE','URGENTE'])[1 + x.n % 5],
       x.dd::date + 30 + x.n % 60,
       x.dd,
       (ARRAY['Antananarivo','Antsirabe','Toamasina','Mahajanga','Fianarantsoa'])[1 + x.n % 5],
       CASE x.type_code
            WHEN 'REMPLACEMENT' THEN 'Remplacement suite au depart d un collaborateur'
            WHEN 'RENFORT' THEN 'Augmentation de la charge de travail du departement'
            ELSE 'Creation d un nouveau poste pour le developpement de l activite' END,
       (ARRAY['Debutant accepte','1 an minimum','2 ans minimum','3 ans minimum','5 ans minimum'])[1 + x.n % 5],
       (ARRAY['Bac','Bac+2','Licence','Master','Ingenieur'])[1 + (x.n * 3) % 5],
       (ARRAY['Rigueur, sens de l organisation, travail en equipe',
              'Maitrise des outils bureautiques, communication',
              'Autonomie, esprit d analyse, gestion du temps',
              'Relation client, negociation, reporting'])[1 + x.n % 4],
       round(g.valeur_min + ((x.n * 13) % 100) / 100.0 * (g.valeur_max - g.valeur_min), -3),
       x.statut,
       LEAST(now(), x.dd + make_interval(days => 1 + x.n % 6))
FROM (
    SELECT n, n % 20 AS k,
           (ARRAY['REMPLACEMENT','RENFORT','CREATION'])[1 + n % 3] AS type_code,
           (ARRAY['CDI','CDI','CDI','CDD','STAGE'])[1 + n % 5] AS contrat_code,
           now() - make_interval(days => (201 - n) * 2, hours => n % 24) AS dd,
           CASE
               WHEN n <= 100 AND n % 20 < 8 THEN CASE WHEN n % 2 = 0 THEN 'ACCEPTE' ELSE 'REFUSE' END
               WHEN n % 20 < 4 THEN 'EN_ATTENTE_RH'
               WHEN n % 20 < 6 THEN 'EN_ATTENTE_FINANCE'
               WHEN n % 20 < 8 THEN 'EN_ATTENTE_DIRECTION'
               WHEN n % 20 < 16 THEN 'ACCEPTE'
               WHEN n % 20 < 19 THEN 'REFUSE'
               ELSE 'ANNULE' END AS statut
    FROM generate_series(1, 200) AS n
) x
JOIN type_besoin tb ON tb.code = x.type_code
JOIN type_contrat tc ON tc.code = x.contrat_code
JOIN tmp_poste_actif pa ON pa.rn = 1 + (x.n * 7) % (SELECT count(*) FROM tmp_poste_actif)
JOIN poste p ON p.id = pa.id
JOIN departement d ON d.id = p.id_departement
JOIN grille_salaire g ON g.id_poste = p.id AND g.date_fin_validite IS NULL
LEFT JOIN tmp_emp_poste tep ON tep.id_poste = p.id
ORDER BY x.n;

-- Les 3 etapes de validation de chaque besoin, coherentes avec son statut
INSERT INTO validation_besoin (id_besoin, id_utilisateur_validateur, id_etat_validation,
                               id_decision, date_decision, remarque)
SELECT f.id,
       CASE WHEN dc.code = 'EN_ATTENTE' THEN NULL ELSE tv.ids[1 + f.id % array_length(tv.ids, 1)] END,
       ev.id, d.id,
       CASE WHEN dc.code = 'EN_ATTENTE' THEN NULL
            ELSE LEAST(now(), f.date_demande + make_interval(days => ev.ordre * 2, hours => f.id % 7)) END,
       CASE dc.code
            WHEN 'ACCEPTE' THEN 'Valide : ' || (ARRAY['justification conforme','budget disponible','priorite confirmee'])[ev.ordre]
            WHEN 'REFUSE' THEN 'Refuse : ' || (ARRAY['besoin non justifie','budget insuffisant','recrutement reporte'])[ev.ordre]
            END
FROM (
    SELECT b.id, b.date_demande,
           CASE b.statut_global
                WHEN 'EN_ATTENTE_RH' THEN 0
                WHEN 'EN_ATTENTE_FINANCE' THEN 1
                WHEN 'EN_ATTENTE_DIRECTION' THEN 2
                WHEN 'ACCEPTE' THEN 3
                WHEN 'REFUSE' THEN b.id % 3
                ELSE b.id % 3 END AS acc,
           CASE WHEN b.statut_global = 'REFUSE' THEN (b.id % 3) + 1 END AS ref_step
    FROM besoin b
) f
CROSS JOIN etat_validation ev
CROSS JOIN LATERAL (SELECT CASE WHEN ev.ordre <= f.acc THEN 'ACCEPTE'
                                WHEN ev.ordre = f.ref_step THEN 'REFUSE'
                                ELSE 'EN_ATTENTE' END AS code) dc
JOIN decision d ON d.code = dc.code
JOIN tmp_val tv ON tv.code = ev.code
ORDER BY f.id, ev.ordre;

-- 7. BLOC 2 : RECRUTEMENTS, OFFRES, PUBLICATIONS -----------------------------------
-- 12 besoins ACCEPTE (les plus recents) restent SANS recrutement : point d'entree du bloc 2.
INSERT INTO recrutement (id_besoin, reference, titre, description, date_ouverture, date_cloture,
                         statut, id_utilisateur_responsable, date_creation, date_modification)
SELECT a.bid,
       'REC-' || to_char(s.dope, 'YYYY') || '-' || lpad(a.rn::text, 4, '0'),
       p.nom || ' - ' || d.nom,
       'Recrutement ouvert suite au besoin numero ' || a.bid || ' (' || a.effectif || ' poste(s)).',
       s.dope,
       CASE WHEN s.statut IN ('TERMINE', 'ANNULE') THEN LEAST(current_date, s.dope + 25 + (a.rn % 30)::int) END,
       s.statut,
       v.ids[1 + (a.rn % array_length(v.ids, 1))::int],
       s.dope::timestamptz,
       LEAST(now(), s.dope::timestamptz + interval '2 days')
FROM (
    SELECT b.id AS bid, b.id_poste, b.effectif, b.date_modification,
           row_number() OVER (ORDER BY b.id) AS rn,
           count(*) OVER () AS total
    FROM besoin b
    WHERE b.statut_global = 'ACCEPTE'
) a
JOIN poste p ON p.id = a.id_poste
JOIN departement d ON d.id = p.id_departement
JOIN tmp_val v ON v.code = 'RH'
CROSS JOIN LATERAL (
    SELECT LEAST(current_date, a.date_modification::date + 3 + (a.rn % 4)::int) AS dope,
           CASE WHEN a.rn::numeric / a.total <= 0.35 THEN CASE WHEN a.rn % 8 = 0 THEN 'ANNULE' ELSE 'TERMINE' END
                WHEN a.rn::numeric / a.total <= 0.40 THEN 'SUSPENDU'
                WHEN a.rn::numeric / a.total <= 0.72 THEN 'OUVERT'
                ELSE 'BROUILLON' END AS statut
) s
WHERE a.rn <= a.total - 12
ORDER BY a.rn;

-- Offres : les recrutements BROUILLON d'identifiant impair n'ont pas encore d'offre.
INSERT INTO offre_emploi (id_recrutement, titre, description_poste, missions, competences_requises,
                          niveau_etude, experience_min_annees, salaire_min, salaire_max,
                          date_creation, statut)
SELECT r.id, r.titre,
       'Nous recherchons un(e) ' || p.nom || ' pour renforcer le departement ' || d.nom || '.',
       'Participer aux activites du departement, assurer le suivi des dossiers, collaborer avec les equipes internes et rendre compte a la hierarchie.',
       b.competences_requises, b.niveau_etude, (b.id % 5)::int,
       g.valeur_min, g.valeur_max,
       LEAST(now(), r.date_creation + interval '1 day'),
       CASE r.statut WHEN 'BROUILLON' THEN 'BROUILLON'
                     WHEN 'OUVERT' THEN 'VALIDEE'
                     WHEN 'SUSPENDU' THEN 'VALIDEE'
                     ELSE 'ARCHIVEE' END
FROM recrutement r
JOIN besoin b ON b.id = r.id_besoin
JOIN poste p ON p.id = b.id_poste
JOIN departement d ON d.id = p.id_departement
JOIN grille_salaire g ON g.id_poste = p.id AND g.date_fin_validite IS NULL
WHERE NOT (r.statut = 'BROUILLON' AND r.id % 2 = 1)
ORDER BY r.id;

-- Publications (1 a 3 par offre ; certaines offres VALIDEE n'en ont pas encore)
INSERT INTO publication_offre (id_offre, plateforme, url_publication, date_publication,
                               date_expiration, statut)
SELECT o.id,
       (ARRAY['Site carriere','LinkedIn','Facebook','Emploi.mg','Journal local'])[1 + (o.id + gp.k) % 5],
       'https://exemple.mg/offres/' || o.id || '-' || gp.k,
       dt.dpub, dt.dexp,
       CASE WHEN dt.dexp < current_date THEN 'EXPIREE'
            WHEN gp.k = 3 AND o.id % 2 = 0 THEN 'BROUILLON'
            WHEN gp.k = 3 THEN 'RETIREE'
            ELSE 'PUBLIEE' END
FROM offre_emploi o
JOIN recrutement r ON r.id = o.id_recrutement
CROSS JOIN LATERAL generate_series(1, 1 + o.id % 3) AS gp(k)
CROSS JOIN LATERAL (
    SELECT LEAST(current_date, r.date_ouverture + gp.k - 1) AS dpub,
           CASE WHEN r.statut = 'OUVERT'
                THEN GREATEST(LEAST(current_date, r.date_ouverture + gp.k - 1) + 30, current_date + 5 + gp.k * 3)
                ELSE LEAST(current_date, r.date_ouverture + gp.k - 1) + 30 END AS dexp
) dt
WHERE o.statut IN ('VALIDEE', 'ARCHIVEE')
  AND NOT (o.statut = 'VALIDEE' AND r.statut = 'OUVERT' AND o.id % 7 = 0)
ORDER BY o.id, gp.k;

CREATE TEMP TABLE tmp_pub ON COMMIT DROP AS
SELECT o.id_recrutement, array_agg(p.id ORDER BY p.id) AS ids
FROM publication_offre p JOIN offre_emploi o ON o.id = p.id_offre
GROUP BY o.id_recrutement;

-- 8. BLOC 3 : CANDIDATS (600) ET CV (guides ou importes) ---------------------------
INSERT INTO candidat (nom, prenom, email, telephone, numero_cin, experience, date_creation)
SELECT pg_temp.nom(g.n * 13), pg_temp.prenom(g.n * 17),
       lower(pg_temp.prenom(g.n * 17)) || '.' || lower(pg_temp.nom(g.n * 13)) || '.c' || g.n || '@mail.mg',
       '03' || (2 + g.n % 4)::text || lpad(((g.n * 6173) % 10000000)::text, 7, '0'),
       '2' || lpad(g.n::text, 11, '0'),
       g.n % 12,
       now() - make_interval(days => 2 + (g.n * 11) % 330, hours => g.n % 24)
FROM generate_series(1, 600) AS g(n)
ORDER BY g.n;

-- 1er CV de chaque candidat : un sur deux est un CV "guide" (details JSON renseigne)
INSERT INTO cv (id_candidat, details, nom_fichier, mime_type, bytes, date_depot, statut_lecture)
SELECT c.id,
       CASE WHEN c.id % 2 = 0 THEN jsonb_build_object(
            'identite', jsonb_build_object('nom', c.nom, 'prenom', c.prenom, 'email', c.email, 'telephone', c.telephone),
            'titre', (ARRAY['Developpeur','Comptable','Charge de clientele','Technicien','Assistant administratif','Responsable logistique'])[1 + c.id % 6],
            'formation', jsonb_build_array(jsonb_build_object(
                'diplome', (ARRAY['Licence','Master','BTS','DUT','Ingenieur'])[1 + c.id % 5],
                'etablissement', (ARRAY['Universite Antananarivo','ISPM','ESTI','IT University','Universite Toamasina'])[1 + c.id % 5],
                'annee', 2005 + c.id % 18)),
            'experiences', jsonb_build_array(jsonb_build_object(
                'poste', (ARRAY['Assistant','Agent','Technicien','Charge de mission'])[1 + c.id % 4],
                'entreprise', (ARRAY['Telma','Jirama','Orange','Star','Socota'])[1 + c.id % 5],
                'periode', (2012 + c.id % 10) || ' a ' || (2016 + c.id % 10),
                'missions', 'Suivi des dossiers, relation avec les clients, reporting mensuel')),
            'competences', jsonb_build_array('Communication', 'Travail en equipe', 'Excel', 'Gestion du temps'),
            'langues', jsonb_build_array('Malgache', 'Francais', 'Anglais'),
            'resume', 'Professionnel motive avec ' || c.experience || ' an(s) d experience, a la recherche de nouveaux defis.'
       )::text END,
       CASE WHEN c.id % 2 = 0 THEN 'cv_guide_' || lower(c.nom) || '_' || c.id || '.pdf'
            ELSE 'CV_' || c.nom || '_' || c.prenom || '.pdf' END,
       'application/pdf',
       pg_temp.pdf_minimal('CV - ' || c.prenom || ' ' || c.nom),
       c.date_creation + interval '1 hour',
       CASE WHEN c.id % 4 = 0 THEN 'NON_LU' ELSE 'LU' END
FROM candidat c
ORDER BY c.id;

-- 2e CV (version importee) pour un candidat sur trois
INSERT INTO cv (id_candidat, details, nom_fichier, mime_type, bytes, date_depot, statut_lecture)
SELECT c.id, NULL,
       'CV_' || c.nom || '_' || c.prenom || '_v2.pdf',
       'application/pdf',
       pg_temp.pdf_minimal('CV mis a jour - ' || c.prenom || ' ' || c.nom),
       LEAST(now(), c.date_creation + interval '5 days'),
       'NON_LU'
FROM candidat c
WHERE c.id % 3 = 0
ORDER BY c.id;

CREATE TEMP TABLE tmp_cv ON COMMIT DROP AS
SELECT id_candidat, array_agg(id ORDER BY id) AS ids FROM cv GROUP BY id_candidat;

-- 9. CANDIDATURES (6 a 14 par recrutement non brouillon) --------------------------
-- TERMINE : exactement "effectif" candidats RETENU (embauches creees plus bas).
-- (la formule du candidat est injective pour les recrutements TERMINE : aucun candidat n'est embauche deux fois)
-- OUVERT/SUSPENDU : cas varies. Un recrutement sur 4 a tous ses postes pourvus
-- (RETENU = effectif) et un sur 4 a un poste restant : pour tester la regle d'effectif.
INSERT INTO candidature (id_candidat, id_recrutement, id_cv, id_publication,
                         date_candidature, statut, score, remarque)
SELECT x.cid, x.rid,
       tc.ids[1 + x.j % array_length(tc.ids, 1)],
       CASE WHEN tp.ids IS NULL OR x.j % 4 = 0 THEN NULL
            ELSE tp.ids[1 + x.j % array_length(tp.ids, 1)] END,
       x.dcand, x.statut,
       CASE x.statut
            WHEN 'RETENU' THEN (80 + x.h % 19)::numeric + (x.h % 4) * 0.25
            WHEN 'PRESELECTIONNE' THEN (65 + x.h % 21)::numeric + (x.h % 4) * 0.25
            WHEN 'REJETE' THEN (20 + x.h % 36)::numeric + (x.h % 4) * 0.25
            WHEN 'EN_ATTENTE' THEN CASE WHEN x.j % 3 = 0 THEN NULL ELSE (40 + x.h % 36)::numeric END
            ELSE NULL END,
       CASE x.statut
            WHEN 'RETENU' THEN 'Excellent profil, correspond au poste'
            WHEN 'REJETE' THEN 'Profil non retenu pour ce poste'
            WHEN 'RETIRE' THEN 'Candidature retiree par le candidat'
            ELSE NULL END
FROM (
    SELECT r.id AS rid, gs.j, hh.h,
           (((r.id * 14 + gs.j) * 7) % 600) + 1 AS cid,
           st.statut,
           LEAST(now(), COALESCE(r.date_cloture::timestamptz, now()),
                 r.date_ouverture::timestamptz + make_interval(days => 1 + (gs.j * 3) % 20, hours => gs.j % 12)) AS dcand
    FROM recrutement r
    JOIN besoin b ON b.id = r.id_besoin
    CROSS JOIN LATERAL generate_series(1, 6 + (r.id % 9)) AS gs(j)
    CROSS JOIN LATERAL (SELECT r.id * 7 + gs.j * 13 AS h) hh
    CROSS JOIN LATERAL (SELECT CASE r.id % 4 WHEN 0 THEN b.effectif
                                             WHEN 1 THEN GREATEST(b.effectif - 1, 0)
                                             ELSE 0 END AS nret) k
    CROSS JOIN LATERAL (SELECT CASE
            WHEN r.statut = 'TERMINE' THEN
                 CASE WHEN gs.j <= b.effectif THEN 'RETENU' WHEN gs.j % 5 = 0 THEN 'RETIRE' ELSE 'REJETE' END
            WHEN r.statut = 'ANNULE' THEN
                 CASE WHEN gs.j % 4 = 0 THEN 'RETIRE' ELSE 'REJETE' END
            ELSE
                 CASE WHEN gs.j <= k.nret THEN 'RETENU'
                      WHEN gs.j <= k.nret + 3 THEN 'PRESELECTIONNE'
                      WHEN gs.j <= k.nret + 5 THEN 'REJETE'
                      WHEN gs.j % 6 = 0 THEN 'RETIRE'
                      ELSE 'EN_ATTENTE' END
            END AS statut) st
    WHERE r.statut <> 'BROUILLON'
) x
JOIN tmp_cv tc ON tc.id_candidat = x.cid
LEFT JOIN tmp_pub tp ON tp.id_recrutement = x.rid
ORDER BY x.rid, x.j;

-- 10. ENTRETIENS --------------------------------------------------------------------
-- (a) Entretien telephonique realise
INSERT INTO entretien (id_candidature, id_utilisateur_intervieweur, type_entretien, date_entretien,
                       lieu, statut, note, commentaire)
SELECT ca.id, v.ids[1 + ca.id % array_length(v.ids, 1)], 'TELEPHONE',
       LEAST(now() - interval '2 hours', ca.date_candidature + interval '2 days'),
       'Telephone', 'REALISE',
       CASE ca.statut WHEN 'RETENU' THEN 14 + ca.id % 5
                      WHEN 'PRESELECTIONNE' THEN 12 + ca.id % 4
                      ELSE 5 + ca.id % 5 END + (ca.id % 4) * 0.25,
       CASE ca.statut WHEN 'REJETE' THEN 'Motivation et competences insuffisantes'
                      ELSE 'Bon contact, profil coherent avec le poste' END
FROM candidature ca
JOIN tmp_val v ON v.code = 'RH'
WHERE ca.statut IN ('RETENU', 'PRESELECTIONNE') OR (ca.statut = 'REJETE' AND ca.id % 2 = 0);

-- (b) Entretien technique realise
INSERT INTO entretien (id_candidature, id_utilisateur_intervieweur, type_entretien, date_entretien,
                       lieu, statut, note, commentaire)
SELECT ca.id, v.ids[1 + ca.id % array_length(v.ids, 1)], 'TECHNIQUE',
       LEAST(now() - interval '2 hours', ca.date_candidature + interval '6 days'),
       'Salle de reunion A', 'REALISE',
       CASE ca.statut WHEN 'RETENU' THEN 15 + ca.id % 5 ELSE 11 + ca.id % 6 END + (ca.id % 4) * 0.25,
       'Competences techniques evaluees, resultats ' ||
       CASE ca.statut WHEN 'RETENU' THEN 'tres satisfaisants' ELSE 'satisfaisants' END
FROM candidature ca
JOIN tmp_val v ON v.code = 'MANAGER'
WHERE ca.statut IN ('RETENU', 'PRESELECTIONNE') AND ca.id % 3 <> 0;

-- (c) Entretien avec la direction (candidats retenus)
INSERT INTO entretien (id_candidature, id_utilisateur_intervieweur, type_entretien, date_entretien,
                       lieu, statut, note, commentaire)
SELECT ca.id, v.ids[1 + ca.id % array_length(v.ids, 1)], 'DIRECTION',
       LEAST(now() - interval '2 hours', ca.date_candidature + interval '9 days'),
       'Bureau de la direction', 'REALISE',
       14 + ca.id % 6 + (ca.id % 4) * 0.25,
       'Entretien final concluant'
FROM candidature ca
JOIN tmp_val v ON v.code = 'DIRECTION'
WHERE ca.statut = 'RETENU' AND ca.id % 2 = 0;

-- (d) Entretiens planifies dans le futur (pour tester le calendrier)
INSERT INTO entretien (id_candidature, id_utilisateur_intervieweur, type_entretien, date_entretien,
                       lieu, statut, note, commentaire)
SELECT ca.id,
       CASE WHEN ca.id % 2 = 0 THEN vr.ids[1 + ca.id % array_length(vr.ids, 1)]
            ELSE vm.ids[1 + ca.id % array_length(vm.ids, 1)] END,
       CASE WHEN ca.id % 2 = 0 THEN 'RH' ELSE 'TEST_TECHNIQUE' END,
       now() + make_interval(days => 1 + ca.id % 10, hours => 1 + ca.id % 8),
       (ARRAY['Salle de reunion B','Visioconference','Siege - 2e etage'])[1 + ca.id % 3],
       'PLANIFIE', NULL::numeric, NULL::text
FROM candidature ca
JOIN tmp_val vr ON vr.code = 'RH'
JOIN tmp_val vm ON vm.code = 'MANAGER'
WHERE ca.statut = 'PRESELECTIONNE' AND ca.id % 3 = 0;

-- (e) Entretiens annules
INSERT INTO entretien (id_candidature, id_utilisateur_intervieweur, type_entretien, date_entretien,
                       lieu, statut, note, commentaire)
SELECT ca.id, v.ids[1 + ca.id % array_length(v.ids, 1)], 'TELEPHONE',
       now() - interval '3 days', 'Telephone', 'ANNULE', NULL::numeric,
       'Entretien annule a la demande du candidat'
FROM candidature ca
JOIN tmp_val v ON v.code = 'RH'
WHERE (ca.statut = 'PRESELECTIONNE' AND ca.id % 7 = 0) OR (ca.statut = 'RETIRE' AND ca.id % 2 = 0);

-- 11. DECISIONS DE RECRUTEMENT --------------------------------------------------------
INSERT INTO decision_recrutement (id_candidature, id_utilisateur_decideur, decision, motif,
                                  date_decision, date_creation)
SELECT y.cid,
       CASE WHEN y.decision = 'EN_ATTENTE' THEN NULL ELSE y.decideur END,
       y.decision,
       CASE y.decision
            WHEN 'RETENU' THEN 'Profil le plus adapte apres les entretiens'
            WHEN 'REJETE' THEN 'Competences insuffisantes pour le poste'
            WHEN 'LISTE_ATTENTE' THEN 'Bon profil, a recontacter si un poste se libere'
            ELSE NULL END,
       CASE WHEN y.decision = 'EN_ATTENTE' THEN NULL
            ELSE LEAST(now() - interval '1 hour', y.dcand + make_interval(days => 10 + y.cid % 5)) END,
       LEAST(now(), y.dcand + interval '8 days')
FROM (
    SELECT ca.id AS cid, ca.date_candidature AS dcand,
           CASE WHEN ca.id % 2 = 0 THEN vr.ids[1 + ca.id % array_length(vr.ids, 1)]
                ELSE vd.ids[1 + ca.id % array_length(vd.ids, 1)] END AS decideur,
           CASE ca.statut WHEN 'RETENU' THEN 'RETENU'
                          WHEN 'REJETE' THEN 'REJETE'
                          ELSE CASE ca.id % 3 WHEN 1 THEN 'LISTE_ATTENTE' ELSE 'EN_ATTENTE' END END AS decision
    FROM candidature ca
    JOIN tmp_val vr ON vr.code = 'RH'
    JOIN tmp_val vd ON vd.code = 'DIRECTION'
    WHERE ca.statut IN ('RETENU', 'REJETE')
       OR (ca.statut = 'PRESELECTIONNE' AND ca.id % 3 <> 0)
) y
ORDER BY y.cid;

-- 12. EMBAUCHES : employes crees a partir des candidats RETENU des recrutements TERMINE ----
-- Les candidats RETENU des recrutements OUVERT n'ont PAS encore d'employe :
-- c'est le point d'entree du bloc 4 (embauche).
INSERT INTO employe (matricule, nom, prenom, email, telephone, numero_cin, date_naissance, adresse,
                     id_candidature, id_departement, id_poste, date_embauche, statut, date_creation)
SELECT 'MAT-' || lpad(((SELECT count(*) FROM employe) + row_number() OVER (ORDER BY ca.id))::text, 5, '0'),
       cd.nom, cd.prenom, cd.email, cd.telephone, cd.numero_cin,
       DATE '1975-01-01' + ((cd.id * 131) % 9000),
       'Lot ' || (1 + cd.id % 150) || ', Antananarivo',
       ca.id, p.id_departement, p.id,
       LEAST(current_date, dr.date_decision::date + 15 + ca.id % 20),
       'ACTIF',
       dr.date_decision
FROM candidature ca
JOIN candidat cd ON cd.id = ca.id_candidat
JOIN recrutement r ON r.id = ca.id_recrutement
JOIN besoin b ON b.id = r.id_besoin
JOIN poste p ON p.id = b.id_poste
JOIN decision_recrutement dr ON dr.id_candidature = ca.id
WHERE ca.statut = 'RETENU' AND r.statut = 'TERMINE'
ORDER BY ca.id;

-- Effectif maximum : departements 4 et 8 (Finance, Logistique) deja au maximum,
-- les autres disposent de 2 a 8 places libres.
UPDATE departement d
SET effectif_max = x.actifs + CASE WHEN d.id % 4 = 0 THEN 0 ELSE 2 + (d.id % 7) END
FROM (SELECT id_departement,
             count(*) FILTER (WHERE statut IN ('ACTIF', 'SUSPENDU')) AS actifs
      FROM employe GROUP BY id_departement) x
WHERE x.id_departement = d.id;

-- 13. BLOC 4 : CONTRATS ---------------------------------------------------------------
-- Les contrats a signer ou en brouillon, les employes sans contrat (1 sur 40) et
-- les contrats expires sont des points d'entree du bloc 4.
CREATE TEMP TABLE tmp_sc ON COMMIT DROP AS SELECT code, id FROM statut_contract;

-- (a) Premier contrat de chaque employe present dans la base initiale
INSERT INTO contrat (reference, id_employe, id_type_contrat, id_statut_contrat, id_utilisateur_createur,
                     date_debut, date_fin, periode_essai_jours, date_signature, salaire,
                     nom_fichier, mime_type, contenu, date_creation)
SELECT 'CTR-' || to_char(x.debut, 'YYYY') || '-' || lpad(x.eid::text, 5, '0'),
       x.eid, t.id, s.id, x.createur, x.debut, x.fin, x.essai,
       CASE WHEN x.signe THEN x.debut END,
       x.salaire,
       CASE WHEN x.fichier THEN 'contrat_' || lpad(x.eid::text, 5, '0') || '.pdf' END,
       CASE WHEN x.fichier THEN 'application/pdf' END,
       CASE WHEN x.fichier THEN pg_temp.pdf_minimal('Contrat ' || x.type_code || ' - employe ' || x.eid) END,
       x.debut::timestamptz - interval '7 days'
FROM (
    SELECT e.id AS eid, e.date_embauche AS debut, c.fin, c.type_code, c.statut_code,
           CASE c.type_code WHEN 'CDI' THEN 90 WHEN 'CDD' THEN 60 ELSE 15 END AS essai,
           (c.statut_code IN ('ACTIF', 'TERMINE', 'RESILIE')) AS signe,
           (c.statut_code IN ('ACTIF', 'TERMINE', 'RESILIE') AND e.id % 3 <> 0) AS fichier,
           round(g.valeur_min + ((e.id * 29) % 100) / 100.0 * (g.valeur_max - g.valeur_min)
                 * CASE WHEN c.statut_code IN ('TERMINE', 'RESILIE') THEN 0.9 ELSE 1 END, -3) AS salaire,
           (SELECT v.ids[1 + e.id % array_length(v.ids, 1)] FROM tmp_val v WHERE v.code = 'RH') AS createur
    FROM employe e
    JOIN grille_salaire g ON g.id_poste = e.id_poste AND g.date_fin_validite IS NULL
    CROSS JOIN LATERAL (
        SELECT CASE WHEN e.statut = 'PARTI' THEN CASE WHEN e.id % 2 = 0 THEN 'CDI' ELSE 'CDD' END
                    WHEN e.date_embauche > current_date - 70 THEN CASE WHEN e.id % 2 = 0 THEN 'STAGE' ELSE 'CDD' END
                    WHEN e.date_embauche <= current_date - 400 AND e.id % 6 = 1 THEN 'CDD'
                    ELSE 'CDI' END AS type_code,
               CASE WHEN e.statut = 'PARTI' THEN CASE WHEN e.id % 2 = 0 THEN 'RESILIE' ELSE 'TERMINE' END
                    WHEN e.date_embauche > current_date - 70 THEN 'ACTIF'
                    WHEN e.date_embauche <= current_date - 400 AND e.id % 6 = 1 THEN 'TERMINE'
                    ELSE 'ACTIF' END AS statut_code,
               CASE WHEN e.statut = 'PARTI' THEN e.date_depart
                    WHEN e.date_embauche > current_date - 70
                         THEN e.date_embauche + CASE WHEN e.id % 2 = 0 THEN 90 ELSE 365 END
                    WHEN e.date_embauche <= current_date - 400 AND e.id % 6 = 1 THEN e.date_embauche + 364
                    ELSE NULL END AS fin
    ) c
    WHERE e.id_candidature IS NULL
      AND NOT (e.id % 40 = 0 AND e.statut = 'ACTIF')
) x
JOIN type_contrat t ON t.code = x.type_code
JOIN statut_contract s ON s.code = x.statut_code
ORDER BY x.eid;

-- (b) Renouvellement en CDI pour les employes dont le CDD est termine
INSERT INTO contrat (reference, id_employe, id_type_contrat, id_statut_contrat, id_contrat_precedent,
                     id_utilisateur_createur, date_debut, date_fin, periode_essai_jours, date_signature,
                     salaire, nom_fichier, mime_type, contenu, date_creation)
SELECT 'CTR-' || to_char(c1.date_fin + 1, 'YYYY') || '-' || lpad(e.id::text, 5, '0') || 'R',
       e.id, t.id, s.id, c1.id, c1.id_utilisateur_createur,
       c1.date_fin + 1, NULL::date, 0, c1.date_fin + 1,
       round(g.valeur_min + ((e.id * 29) % 100) / 100.0 * (g.valeur_max - g.valeur_min), -3),
       'contrat_' || lpad(e.id::text, 5, '0') || '_renouvellement.pdf',
       'application/pdf',
       pg_temp.pdf_minimal('Contrat CDI renouvellement - employe ' || e.id),
       (c1.date_fin + 1)::timestamptz - interval '7 days'
FROM contrat c1
JOIN employe e ON e.id = c1.id_employe
JOIN grille_salaire g ON g.id_poste = e.id_poste AND g.date_fin_validite IS NULL
JOIN type_contrat t ON t.code = 'CDI'
JOIN statut_contract s ON s.code = 'ACTIF'
WHERE c1.id_type_contrat = (SELECT id FROM type_contrat WHERE code = 'CDD')
  AND c1.id_statut_contrat = (SELECT id FROM statut_contract WHERE code = 'TERMINE')
  AND e.statut <> 'PARTI'
  AND e.id_candidature IS NULL
ORDER BY e.id;

-- (c) Contrats des employes embauches via le recrutement (brouillon, a signer, actifs, expires)
INSERT INTO contrat (reference, id_employe, id_type_contrat, id_statut_contrat, id_utilisateur_createur,
                     date_debut, date_fin, periode_essai_jours, date_signature, salaire,
                     nom_fichier, mime_type, contenu, date_creation)
SELECT 'CTR-' || to_char(x.debut, 'YYYY') || '-' || lpad(x.eid::text, 5, '0'),
       x.eid, t.id, s.id, x.createur, x.debut, x.fin, x.essai,
       CASE WHEN x.signe THEN x.debut END,
       x.salaire,
       CASE WHEN x.fichier THEN 'contrat_' || lpad(x.eid::text, 5, '0') || '.pdf' END,
       CASE WHEN x.fichier THEN 'application/pdf' END,
       CASE WHEN x.fichier THEN pg_temp.pdf_minimal('Contrat ' || x.type_code || ' - employe ' || x.eid) END,
       x.debut::timestamptz - interval '7 days'
FROM (
    SELECT e.id AS eid, e.date_embauche AS debut, f.fin,
           COALESCE(tc.code, 'CDI') AS type_code, st.statut_code,
           CASE COALESCE(tc.code, 'CDI') WHEN 'CDI' THEN 90 WHEN 'CDD' THEN 60 ELSE 15 END AS essai,
           (st.statut_code IN ('ACTIF', 'TERMINE', 'RESILIE')) AS signe,
           (st.statut_code <> 'BROUILLON' AND e.id % 4 <> 0) AS fichier,
           round(g.valeur_min + ((e.id * 29) % 100) / 100.0 * (g.valeur_max - g.valeur_min), -3) AS salaire,
           (SELECT v.ids[1 + e.id % array_length(v.ids, 1)] FROM tmp_val v WHERE v.code = 'RH') AS createur
    FROM employe e
    JOIN candidature ca ON ca.id = e.id_candidature
    JOIN recrutement r ON r.id = ca.id_recrutement
    JOIN besoin b ON b.id = r.id_besoin
    LEFT JOIN type_contrat tc ON tc.id = b.id_type_contrat_souhaite
    JOIN grille_salaire g ON g.id_poste = e.id_poste AND g.date_fin_validite IS NULL
    CROSS JOIN LATERAL (SELECT CASE WHEN tc.duree IS NULL THEN NULL::date
                                    ELSE e.date_embauche + tc.duree END AS fin) f
    CROSS JOIN LATERAL (SELECT CASE WHEN e.id % 7 = 0 THEN 'BROUILLON'
                                    WHEN e.id % 7 = 1 THEN 'EN_ATTENTE_SIGNATURE'
                                    WHEN f.fin IS NOT NULL AND f.fin < current_date THEN 'TERMINE'
                                    ELSE 'ACTIF' END AS statut_code) st
) x
JOIN type_contrat t ON t.code = x.type_code
JOIN statut_contract s ON s.code = x.statut_code
ORDER BY x.eid;

-- 14. HISTORIQUE DES CONTRATS ------------------------------------------------------------
-- Creation de chaque contrat
INSERT INTO historique_contrat (id_contrat, id_ancien_statut, id_statut_contrat, id_utilisateur_auteur,
                                evenement, motif, date_modification)
SELECT c.id, NULL::int, sb.id, c.id_utilisateur_createur, 'CREATION', 'Creation du contrat', c.date_creation
FROM contrat c
JOIN tmp_sc sb ON sb.code = 'BROUILLON';

-- Envoi pour signature
INSERT INTO historique_contrat (id_contrat, id_ancien_statut, id_statut_contrat, id_utilisateur_auteur,
                                evenement, motif, date_modification)
SELECT c.id, sb.id, se.id, c.id_utilisateur_createur, 'ENVOI_SIGNATURE',
       'Contrat envoye au collaborateur pour signature', c.date_creation + interval '1 day'
FROM contrat c
JOIN statut_contract cs ON cs.id = c.id_statut_contrat
JOIN tmp_sc sb ON sb.code = 'BROUILLON'
JOIN tmp_sc se ON se.code = 'EN_ATTENTE_SIGNATURE'
WHERE cs.code IN ('EN_ATTENTE_SIGNATURE', 'ACTIF', 'TERMINE', 'RESILIE');

-- Signature (contrat actif)
INSERT INTO historique_contrat (id_contrat, id_ancien_statut, id_statut_contrat, id_utilisateur_auteur,
                                evenement, motif, date_modification)
SELECT c.id, se.id, sa.id, c.id_utilisateur_createur, 'SIGNATURE',
       'Contrat signe par les deux parties', c.date_debut::timestamptz
FROM contrat c
JOIN statut_contract cs ON cs.id = c.id_statut_contrat
JOIN tmp_sc se ON se.code = 'EN_ATTENTE_SIGNATURE'
JOIN tmp_sc sa ON sa.code = 'ACTIF'
WHERE cs.code IN ('ACTIF', 'TERMINE', 'RESILIE');

-- Fin de contrat
INSERT INTO historique_contrat (id_contrat, id_ancien_statut, id_statut_contrat, id_utilisateur_auteur,
                                evenement, motif, date_modification, ancienne_date_fin, nouvelle_date_fin)
SELECT c.id, sa.id, st.id, c.id_utilisateur_createur, 'FIN_CONTRAT',
       'Arrivee du terme du contrat', c.date_fin::timestamptz, c.date_fin, c.date_fin
FROM contrat c
JOIN statut_contract cs ON cs.id = c.id_statut_contrat
JOIN tmp_sc sa ON sa.code = 'ACTIF'
JOIN tmp_sc st ON st.code = 'TERMINE'
WHERE cs.code = 'TERMINE' AND c.date_fin IS NOT NULL;

-- Resiliation
INSERT INTO historique_contrat (id_contrat, id_ancien_statut, id_statut_contrat, id_utilisateur_auteur,
                                evenement, motif, date_modification)
SELECT c.id, sa.id, sr.id, c.id_utilisateur_createur, 'RESILIATION',
       'Depart du collaborateur', c.date_fin::timestamptz
FROM contrat c
JOIN statut_contract cs ON cs.id = c.id_statut_contrat
JOIN tmp_sc sa ON sa.code = 'ACTIF'
JOIN tmp_sc sr ON sr.code = 'RESILIE'
WHERE cs.code = 'RESILIE' AND c.date_fin IS NOT NULL;

-- Augmentation de salaire (contrats CDI actifs de plus de 200 jours, 1 employe sur 4)
INSERT INTO historique_contrat (id_contrat, id_ancien_statut, id_statut_contrat, id_utilisateur_auteur,
                                evenement, motif, date_modification, ancien_salaire, nouveau_salaire)
SELECT c.id, sa.id, sa.id, c.id_utilisateur_createur, 'AUGMENTATION',
       'Revision annuelle du salaire', (c.date_debut + 200)::timestamptz,
       round(c.salaire * 0.92, -3), c.salaire
FROM contrat c
JOIN statut_contract cs ON cs.id = c.id_statut_contrat
JOIN type_contrat t ON t.id = c.id_type_contrat
JOIN tmp_sc sa ON sa.code = 'ACTIF'
WHERE cs.code = 'ACTIF' AND t.code = 'CDI' AND c.id_employe % 4 = 0
  AND c.salaire IS NOT NULL AND c.date_debut + 200 <= current_date;

-- Prolongation (CDD actifs, 1 sur 3)
INSERT INTO historique_contrat (id_contrat, id_ancien_statut, id_statut_contrat, id_utilisateur_auteur,
                                evenement, motif, date_modification, ancienne_date_fin, nouvelle_date_fin)
SELECT c.id, sa.id, sa.id, c.id_utilisateur_createur, 'PROLONGATION',
       'Prolongation de la duree du contrat', (c.date_debut + 60)::timestamptz,
       c.date_fin - 90, c.date_fin
FROM contrat c
JOIN statut_contract cs ON cs.id = c.id_statut_contrat
JOIN type_contrat t ON t.id = c.id_type_contrat
JOIN tmp_sc sa ON sa.code = 'ACTIF'
WHERE cs.code = 'ACTIF' AND t.code = 'CDD' AND c.id % 3 = 0
  AND c.date_fin IS NOT NULL AND c.date_debut + 60 <= current_date;

-- 15. ONBOARDING ET TACHES ------------------------------------------------------------------
-- 1 employe actif sur 27 (base initiale) n'a PAS d'onboarding : point d'entree du bloc 4.
INSERT INTO onboarding (id_employe, id_utilisateur_responsable, date_debut, date_fin_prevue,
                        statut, date_creation)
SELECT e.id,
       (SELECT v.ids[1 + e.id % array_length(v.ids, 1)] FROM tmp_val v WHERE v.code = 'RH'),
       e.date_embauche, e.date_embauche + 30,
       CASE WHEN e.date_embauche < current_date - 60 THEN 'TERMINE'
            WHEN e.date_embauche < current_date - 20 THEN CASE WHEN e.id % 2 = 0 THEN 'TERMINE' ELSE 'EN_COURS' END
            WHEN e.date_embauche < current_date - 5 THEN 'EN_COURS'
            ELSE 'A_FAIRE' END,
       e.date_embauche::timestamptz - interval '3 days'
FROM employe e
WHERE NOT (e.id % 27 = 0 AND e.statut = 'ACTIF' AND e.id_candidature IS NULL)
ORDER BY e.id;

INSERT INTO tache_onboarding (id_onboarding, id_modele, id_utilisateur_responsable, libelle, categorie,
                              date_echeance, statut, date_realisation, remarque)
SELECT x.oid, x.mid, x.resp, x.libelle, x.categorie, x.echeance, x.st,
       CASE WHEN x.st = 'FAIT' THEN LEAST(now(), x.echeance::timestamptz + interval '15 hours') END,
       CASE WHEN x.st = 'EN_COURS' AND x.echeance < current_date
            THEN 'En retard : action en cours de traitement' END
FROM (
    SELECT o.id AS oid, m.id AS mid, o.id_utilisateur_responsable AS resp,
           m.libelle, m.categorie,
           o.date_debut + m.delai_jours AS echeance,
           CASE WHEN o.statut = 'TERMINE' THEN 'FAIT'
                WHEN o.statut = 'A_FAIRE' THEN 'A_FAIRE'
                WHEN m.delai_jours <= current_date - o.date_debut THEN 'FAIT'
                WHEN m.delai_jours <= current_date - o.date_debut + 7 THEN 'EN_COURS'
                ELSE 'A_FAIRE' END AS st
    FROM onboarding o
    CROSS JOIN modele_tache_onboarding m
    WHERE m.actif
) x
ORDER BY x.oid, x.mid;

-- 16. DOCUMENTS DES EMPLOYES (2 a 5 par employe) -----------------------------------------------
INSERT INTO document_employe (id_employe, type_document, nom_fichier, mime_type, bytes, statut, date_depot)
SELECT x.eid, x.typ,
       lower(x.typ) || '_' || x.matricule || CASE WHEN x.typ = 'PHOTO' THEN '.png' ELSE '.pdf' END,
       CASE WHEN x.typ = 'PHOTO' THEN 'image/png' ELSE 'application/pdf' END,
       CASE WHEN x.typ = 'PHOTO' THEN pg_temp.png_minimal()
            ELSE pg_temp.pdf_minimal(x.typ || ' - ' || x.prenom || ' ' || x.nom) END,
       CASE WHEN x.debut > current_date - 30 THEN 'FOURNI'
            WHEN x.eid % 13 = 0 AND x.k = 2 THEN 'REFUSE'
            WHEN x.eid % 5 = 0 AND x.k = 1 THEN 'FOURNI'
            ELSE 'VALIDE' END,
       LEAST(now(), (x.debut + x.k)::timestamptz)
FROM (
    SELECT e.id AS eid, e.matricule, e.nom, e.prenom, e.date_embauche AS debut, gk.k,
           (ARRAY['CIN','DIPLOME','RIB','CV','CERTIFICAT_MEDICAL','PHOTO','AUTRE'])[1 + (e.id + gk.k) % 7] AS typ
    FROM employe e
    CROSS JOIN LATERAL generate_series(1, 2 + e.id % 4) AS gk(k)
) x
ORDER BY x.eid, x.k;

-- 17. BUDGETS ET DEPENSES -------------------------------------------------------------------------
-- Budgets de N-2 a N+1. Les departements d'identifiant multiple de 5 ont consomme
-- environ 97 % de leur budget de l'annee en cours : pour tester le controle budgetaire.
INSERT INTO budget_departement (id_departement, annee, montant, devise)
SELECT d.id, y.annee,
       round((300000000 + ((d.id * 37) % 10) * 60000000)::numeric
             * (1 + 0.05 * (y.annee - (extract(year from current_date)::int - 2))), -3),
       'MGA'
FROM departement d
CROSS JOIN LATERAL generate_series(extract(year from current_date)::int - 2,
                                   extract(year from current_date)::int + 1) AS y(annee)
ORDER BY d.id, y.annee;

-- Masse salariale mensuelle
INSERT INTO depense_departement (id_departement, libelle, categorie, montant, date_depense)
SELECT b.id_departement,
       'Masse salariale ' || to_char(make_date(b.annee, gm.mois, 28), 'MM/YYYY'),
       'Salaires',
       round(b.montant * r.ratio * 0.7 / nm.nb, 2),
       LEAST(current_date, make_date(b.annee, gm.mois, 28))
FROM budget_departement b
CROSS JOIN LATERAL (SELECT CASE WHEN b.annee = extract(year from current_date)::int
                                THEN extract(month from current_date)::int ELSE 12 END AS nb) nm
CROSS JOIN LATERAL generate_series(1, nm.nb) AS gm(mois)
CROSS JOIN LATERAL (SELECT CASE WHEN b.annee = extract(year from current_date)::int
                                THEN CASE WHEN b.id_departement % 5 = 0 THEN 0.97
                                          ELSE 0.35 + ((b.id_departement * 7) % 40) / 100.0 END
                                ELSE 0.80 + ((b.id_departement * 3 + b.annee) % 20) / 100.0 END AS ratio) r
WHERE b.annee <= extract(year from current_date)::int
ORDER BY b.id_departement, b.annee, gm.mois;

-- Autres depenses (8 categories par departement et par annee)
INSERT INTO depense_departement (id_departement, libelle, categorie, montant, date_depense)
SELECT b.id_departement,
       cat.nom || ' ' || b.annee,
       cat.nom,
       round(b.montant * r.ratio * 0.3 * cat.k / 36.0, 2),
       LEAST(current_date,
             make_date(b.annee,
                       1 + (cat.k * 3 + b.id_departement) % (CASE WHEN b.annee = extract(year from current_date)::int
                                                                  THEN extract(month from current_date)::int ELSE 12 END),
                       1 + (cat.k * 5) % 27))
FROM budget_departement b
CROSS JOIN (VALUES (1, 'Formation'), (2, 'Materiel informatique'), (3, 'Publication des offres'),
                   (4, 'Deplacements'), (5, 'Equipement bureau'), (6, 'Primes'),
                   (7, 'Frais de mission'), (8, 'Frais divers')) AS cat(k, nom)
CROSS JOIN LATERAL (SELECT CASE WHEN b.annee = extract(year from current_date)::int
                                THEN CASE WHEN b.id_departement % 5 = 0 THEN 0.97
                                          ELSE 0.35 + ((b.id_departement * 7) % 40) / 100.0 END
                                ELSE 0.80 + ((b.id_departement * 3 + b.annee) % 20) / 100.0 END AS ratio) r
WHERE b.annee <= extract(year from current_date)::int
ORDER BY b.id_departement, b.annee, cat.k;

-- Frais de recrutement lies a chaque recrutement (via le besoin)
INSERT INTO depense_departement (id_departement, id_besoin, libelle, categorie, montant, date_depense)
SELECT p.id_departement, r.id_besoin, 'Frais de recrutement ' || r.reference, 'Recrutement',
       round((150000 + ((r.id * 7919) % 750000))::numeric, -3),
       LEAST(current_date, r.date_ouverture + 5)
FROM recrutement r
JOIN besoin b ON b.id = r.id_besoin
JOIN poste p ON p.id = b.id_poste
ORDER BY r.id;

-- Equipement des nouveaux employes (lie au contrat)
INSERT INTO depense_departement (id_departement, id_contrat, libelle, categorie, montant, date_depense)
SELECT e.id_departement, c.id, 'Equipement et installation ' || c.reference, 'Equipement',
       round((200000 + ((c.id * 977) % 400000))::numeric, -3),
       LEAST(current_date, c.date_debut + 3)
FROM contrat c
JOIN employe e ON e.id = c.id_employe
WHERE e.id_candidature IS NOT NULL OR c.id % 3 = 0
ORDER BY c.id;

-- 18. NOTIFICATIONS ---------------------------------------------------------------------------------
-- Besoins en attente : notification au validateur de l'etape en cours
INSERT INTO notification (id_utilisateur, id_besoin, type_notification, titre, message, date_creation, date_lecture)
SELECT x.uid, x.bid, 'BESOIN_A_VALIDER', 'Besoin a valider', x.msg, x.dc,
       CASE WHEN x.bid % 5 < 2 THEN NULL ELSE LEAST(now(), x.dc + make_interval(hours => 1 + x.bid % 48)) END
FROM (
    SELECT b.id AS bid, b.date_modification AS dc,
           tv.ids[1 + b.id % array_length(tv.ids, 1)] AS uid,
           'Le besoin numero ' || b.id || ' (' || p.nom || ') attend votre validation.' AS msg
    FROM besoin b
    JOIN poste p ON p.id = b.id_poste
    JOIN tmp_val tv ON tv.code = CASE b.statut_global
                                      WHEN 'EN_ATTENTE_RH' THEN 'RH'
                                      WHEN 'EN_ATTENTE_FINANCE' THEN 'FINANCE'
                                      WHEN 'EN_ATTENTE_DIRECTION' THEN 'DIRECTION' END
) x;

-- Reponse au demandeur (besoin accepte ou refuse)
INSERT INTO notification (id_utilisateur, id_besoin, type_notification, titre, message, date_creation, date_lecture)
SELECT x.uid, x.bid, x.typ, x.titre, x.msg, x.dc,
       CASE WHEN x.bid % 5 < 2 THEN NULL ELSE LEAST(now(), x.dc + make_interval(hours => 1 + x.bid % 48)) END
FROM (
    SELECT b.id AS bid, b.id_utilisateur_demandeur AS uid, b.date_modification AS dc,
           CASE WHEN b.statut_global = 'ACCEPTE' THEN 'BESOIN_ACCEPTE' ELSE 'BESOIN_REFUSE' END AS typ,
           CASE WHEN b.statut_global = 'ACCEPTE' THEN 'Besoin accepte' ELSE 'Besoin refuse' END AS titre,
           'Votre demande numero ' || b.id || ' (' || p.nom || ') est ' ||
           CASE WHEN b.statut_global = 'ACCEPTE' THEN 'acceptee.' ELSE 'refusee.' END AS msg
    FROM besoin b
    JOIN poste p ON p.id = b.id_poste
    WHERE b.statut_global IN ('ACCEPTE', 'REFUSE')
) x;

-- Nouvelles candidatures recues (responsable du recrutement)
INSERT INTO notification (id_utilisateur, id_candidature, type_notification, titre, message, date_creation, date_lecture)
SELECT r.id_utilisateur_responsable, ca.id, 'CANDIDATURE_RECUE', 'Nouvelle candidature',
       'Une candidature a ete recue pour le recrutement ' || r.reference || '.',
       ca.date_candidature,
       CASE WHEN ca.id % 5 < 2 THEN NULL ELSE LEAST(now(), ca.date_candidature + make_interval(hours => 1 + ca.id % 48)) END
FROM candidature ca
JOIN recrutement r ON r.id = ca.id_recrutement
WHERE ca.statut = 'EN_ATTENTE' AND r.statut = 'OUVERT' AND r.id_utilisateur_responsable IS NOT NULL;

-- Entretiens planifies (intervieweur)
INSERT INTO notification (id_utilisateur, id_candidature, type_notification, titre, message, date_creation, date_lecture)
SELECT en.id_utilisateur_intervieweur, en.id_candidature, 'ENTRETIEN_PLANIFIE', 'Entretien planifie',
       'Un entretien est prevu le ' || to_char(en.date_entretien, 'DD/MM/YYYY "a" HH24:MI') || '.',
       now() - make_interval(days => en.id % 5, hours => 1),
       NULL::timestamptz
FROM entretien en
WHERE en.statut = 'PLANIFIE' AND en.id_utilisateur_intervieweur IS NOT NULL;

-- Contrats a signer (createur du contrat)
INSERT INTO notification (id_utilisateur, type_notification, titre, message, date_creation, date_lecture)
SELECT c.id_utilisateur_createur, 'CONTRAT_A_SIGNER', 'Contrat en attente de signature',
       'Le contrat ' || c.reference || ' attend la signature du collaborateur.',
       c.date_creation + interval '1 day', NULL::timestamptz
FROM contrat c
JOIN statut_contract s ON s.id = c.id_statut_contrat
WHERE s.code = 'EN_ATTENTE_SIGNATURE' AND c.id_utilisateur_createur IS NOT NULL;

-- Taches d'onboarding en cours (responsable)
INSERT INTO notification (id_utilisateur, type_notification, titre, message, date_creation, date_lecture)
SELECT t.id_utilisateur_responsable, 'TACHE_ONBOARDING', 'Tache d integration a traiter',
       'La tache "' || t.libelle || '" est en cours.',
       now() - make_interval(days => t.id % 6), NULL::timestamptz
FROM tache_onboarding t
WHERE t.statut = 'EN_COURS' AND t.id_utilisateur_responsable IS NOT NULL;

COMMIT;

-- ============================================================
-- RAPPORT 1 : volume de donnees par table
-- ============================================================
SELECT 'utilisateur' AS table_nom, count(*) AS nombre FROM utilisateur
UNION ALL SELECT 'departement', count(*) FROM departement
UNION ALL SELECT 'poste', count(*) FROM poste
UNION ALL SELECT 'grille_salaire', count(*) FROM grille_salaire
UNION ALL SELECT 'employe', count(*) FROM employe
UNION ALL SELECT 'besoin', count(*) FROM besoin
UNION ALL SELECT 'validation_besoin', count(*) FROM validation_besoin
UNION ALL SELECT 'recrutement', count(*) FROM recrutement
UNION ALL SELECT 'offre_emploi', count(*) FROM offre_emploi
UNION ALL SELECT 'publication_offre', count(*) FROM publication_offre
UNION ALL SELECT 'candidat', count(*) FROM candidat
UNION ALL SELECT 'cv', count(*) FROM cv
UNION ALL SELECT 'candidature', count(*) FROM candidature
UNION ALL SELECT 'entretien', count(*) FROM entretien
UNION ALL SELECT 'decision_recrutement', count(*) FROM decision_recrutement
UNION ALL SELECT 'contrat', count(*) FROM contrat
UNION ALL SELECT 'historique_contrat', count(*) FROM historique_contrat
UNION ALL SELECT 'onboarding', count(*) FROM onboarding
UNION ALL SELECT 'tache_onboarding', count(*) FROM tache_onboarding
UNION ALL SELECT 'document_employe', count(*) FROM document_employe
UNION ALL SELECT 'budget_departement', count(*) FROM budget_departement
UNION ALL SELECT 'depense_departement', count(*) FROM depense_departement
UNION ALL SELECT 'notification', count(*) FROM notification;

-- ============================================================
-- RAPPORT 2 : points d'entree de chaque bloc (donnees pretes a l'emploi)
-- ============================================================
SELECT 'BLOC 1 - besoins EN_ATTENTE_RH' AS point_entree, count(*) AS nombre
  FROM besoin WHERE statut_global = 'EN_ATTENTE_RH'
UNION ALL SELECT 'BLOC 1 - besoins EN_ATTENTE_FINANCE', count(*) FROM besoin WHERE statut_global = 'EN_ATTENTE_FINANCE'
UNION ALL SELECT 'BLOC 1 - besoins EN_ATTENTE_DIRECTION', count(*) FROM besoin WHERE statut_global = 'EN_ATTENTE_DIRECTION'
UNION ALL SELECT 'BLOC 1 - besoins ACCEPTE / REFUSE / ANNULE',
       count(*) FROM besoin WHERE statut_global IN ('ACCEPTE', 'REFUSE', 'ANNULE')
UNION ALL SELECT 'BLOC 2 - besoins ACCEPTE sans recrutement',
       count(*) FROM besoin b WHERE b.statut_global = 'ACCEPTE'
         AND NOT EXISTS (SELECT 1 FROM recrutement r WHERE r.id_besoin = b.id)
UNION ALL SELECT 'BLOC 2 - recrutements BROUILLON sans offre',
       count(*) FROM recrutement r WHERE r.statut = 'BROUILLON'
         AND NOT EXISTS (SELECT 1 FROM offre_emploi o WHERE o.id_recrutement = r.id)
UNION ALL SELECT 'BLOC 2 - offres VALIDEE sans publication',
       count(*) FROM offre_emploi o WHERE o.statut = 'VALIDEE'
         AND NOT EXISTS (SELECT 1 FROM publication_offre p WHERE p.id_offre = o.id)
UNION ALL SELECT 'BLOC 3 - recrutements OUVERT', count(*) FROM recrutement WHERE statut = 'OUVERT'
UNION ALL SELECT 'BLOC 3 - candidatures EN_ATTENTE (a trier)', count(*) FROM candidature WHERE statut = 'EN_ATTENTE'
UNION ALL SELECT 'BLOC 3 - candidatures PRESELECTIONNE', count(*) FROM candidature WHERE statut = 'PRESELECTIONNE'
UNION ALL SELECT 'BLOC 3 - entretiens PLANIFIE', count(*) FROM entretien WHERE statut = 'PLANIFIE'
UNION ALL SELECT 'BLOC 3 - recrutements OUVERT avec tous les postes pourvus',
       count(*) FROM recrutement r JOIN besoin b ON b.id = r.id_besoin
       WHERE r.statut = 'OUVERT'
         AND (SELECT count(*) FROM candidature c WHERE c.id_recrutement = r.id AND c.statut = 'RETENU') >= b.effectif
UNION ALL SELECT 'BLOC 3 - recrutements OUVERT avec au moins un poste restant',
       count(*) FROM recrutement r JOIN besoin b ON b.id = r.id_besoin
       WHERE r.statut = 'OUVERT'
         AND (SELECT count(*) FROM candidature c WHERE c.id_recrutement = r.id AND c.statut = 'RETENU') < b.effectif
UNION ALL SELECT 'BLOC 4 - candidats RETENU sans employe (a embaucher)',
       count(*) FROM candidature ca WHERE ca.statut = 'RETENU'
         AND NOT EXISTS (SELECT 1 FROM employe e WHERE e.id_candidature = ca.id)
UNION ALL SELECT 'BLOC 4 - employes ACTIF sans contrat',
       count(*) FROM employe e WHERE e.statut = 'ACTIF'
         AND NOT EXISTS (SELECT 1 FROM contrat c WHERE c.id_employe = e.id)
UNION ALL SELECT 'BLOC 4 - contrats BROUILLON',
       count(*) FROM contrat c JOIN statut_contract s ON s.id = c.id_statut_contrat WHERE s.code = 'BROUILLON'
UNION ALL SELECT 'BLOC 4 - contrats EN_ATTENTE_SIGNATURE',
       count(*) FROM contrat c JOIN statut_contract s ON s.id = c.id_statut_contrat WHERE s.code = 'EN_ATTENTE_SIGNATURE'
UNION ALL SELECT 'BLOC 4 - employes sans onboarding',
       count(*) FROM employe e WHERE NOT EXISTS (SELECT 1 FROM onboarding o WHERE o.id_employe = e.id)
UNION ALL SELECT 'BLOC 4 - onboardings EN_COURS', count(*) FROM onboarding WHERE statut = 'EN_COURS'
UNION ALL SELECT 'BUDGET - departements a plus de 95 % du budget de l annee',
       count(*) FROM budget_departement b
       WHERE b.annee = extract(year from current_date)::int
         AND (SELECT COALESCE(sum(d.montant), 0) FROM depense_departement d
              WHERE d.id_departement = b.id_departement AND d.annee = b.annee) > b.montant * 0.95
UNION ALL SELECT 'CONTROLE - recrutements avec plus de RETENU que d effectif (doit etre 0)',
       count(*) FROM recrutement r JOIN besoin b ON b.id = r.id_besoin
       WHERE (SELECT count(*) FROM candidature c WHERE c.id_recrutement = r.id AND c.statut = 'RETENU') > b.effectif;
