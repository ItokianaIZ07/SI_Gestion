-- Pour supprimer toutes les tables et fonctions

-- ============================================================
-- SCRIPT DE NETTOYAGE COMPLET (SIG RH)
-- Supprime toutes les tables, fonctions et extensions créées
-- ============================================================

BEGIN;

-- 1. Suppression de toutes les tables avec l'option CASCADE 
-- pour éliminer automatiquement les index, contraintes et clés étrangères liés.
DROP TABLE IF EXISTS
    notification, document_employe, tache_onboarding, onboarding,
    modele_tache_onboarding, depense_departement, budget_departement,
    historique_contrat, contrat, statut_contract, type_contrat,
    decision_recrutement, entretien, candidature, cv, candidat,
    publication_offre, offre_emploi, recrutement, validation_besoin,
    besoin, type_besoin, etat_validation, decision, employe,
    grille_salaire, poste, departement, utilisateur, role
CASCADE;

-- 2. Suppression des fonctions personnalisées créées dans le script
DROP FUNCTION IF EXISTS verifier_validation_besoin() CASCADE;
DROP FUNCTION IF EXISTS verifier_publication_candidature() CASCADE;
DROP FUNCTION IF EXISTS verifier_contrat_actif() CASCADE;

-- 3. (Optionnel) Suppression de l'extension si elle n'est plus utile sur cette base
-- Décommentez la ligne ci-dessous si vous souhaitez aussi retirer l'extension btree_gist
-- DROP EXTENSION IF EXISTS btree_gist;

COMMIT;