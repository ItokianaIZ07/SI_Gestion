-- Automatise l'acquisition mensuelle des congés à partir des règles actives.
-- Ne crée aucune table.

BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM mouvement_conge
        WHERE type_mouvement = 'ACQUISITION'
          AND periode_debut IS NOT NULL
        GROUP BY id_employe, id_type_conge, periode_debut
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Migration annulee : plusieurs acquisitions existent pour un employe, un type et une periode.';
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_acquisition_mensuelle
    ON mouvement_conge (
        id_employe,
        id_type_conge,
        periode_debut
    )
    WHERE type_mouvement = 'ACQUISITION';

CREATE OR REPLACE PROCEDURE sp_acquerir_conges_mensuels(
    p_periode DATE
)
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
        RAISE EXCEPTION 'La periode est obligatoire';
    END IF;

    v_periode := DATE_TRUNC('month', p_periode)::DATE;

    FOR v_employe IN
        SELECT id
        FROM employe
        WHERE statut = 'ACTIF'
          AND date_embauche <= v_periode
        ORDER BY id
        FOR UPDATE
    LOOP
        FOR v_regle IN
            SELECT DISTINCT ON (rc.id_type_conge)
                rc.id AS id_regle,
                rc.id_type_conge,
                rc.jours_acquis_mois,
                rc.plafond_jours
            FROM regle_conge rc
            JOIN type_conge tc ON tc.id = rc.id_type_conge
            WHERE tc.actif = TRUE
              AND rc.actif = TRUE
              AND rc.acquisition_autorisee = TRUE
              AND rc.jours_acquis_mois > 0
              AND rc.date_debut_validite <= v_periode
              AND (
                  rc.date_fin_validite IS NULL
                  OR rc.date_fin_validite >= v_periode
              )
            ORDER BY
                rc.id_type_conge,
                rc.date_debut_validite DESC,
                rc.id DESC
        LOOP
            v_solde := fn_solde_conge(v_employe.id, v_regle.id_type_conge);

            IF v_regle.plafond_jours IS NULL THEN
                v_acquisition := v_regle.jours_acquis_mois;
            ELSE
                v_acquisition := LEAST(
                    v_regle.jours_acquis_mois,
                    GREATEST(v_regle.plafond_jours - v_solde, 0)
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
                    v_periode,
                    'Acquisition mensuelle automatique - regle '
                        || v_regle.id_regle
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

DO $$
BEGIN
    IF to_regprocedure('sp_acquerir_conges_mensuels(date)') IS NULL THEN
        RAISE EXCEPTION
            'Validation echouee : procedure sp_acquerir_conges_mensuels(date) absente.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_index i
        JOIN pg_class idx ON idx.oid = i.indexrelid
        JOIN pg_class tbl ON tbl.oid = i.indrelid
        WHERE idx.relname = 'uq_acquisition_mensuelle'
          AND tbl.relname = 'mouvement_conge'
          AND i.indisunique
          AND i.indisvalid
    ) THEN
        RAISE EXCEPTION
            'Validation echouee : index unique uq_acquisition_mensuelle absent ou invalide.';
    END IF;
END
$$;

COMMIT;


BEGIN;

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

COMMIT;