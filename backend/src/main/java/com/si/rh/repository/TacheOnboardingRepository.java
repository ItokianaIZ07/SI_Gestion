package com.si.rh.repository;

import com.si.rh.model.TacheOnboarding;
import com.si.rh.model.enums.StatutTache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TacheOnboardingRepository extends JpaRepository<TacheOnboarding, Integer> {

    List<TacheOnboarding> findByOnboarding_IdOrderByDateEcheanceAscIdAsc(Integer onboardingId);

    @Query("SELECT t FROM TacheOnboarding t JOIN FETCH t.onboarding WHERE t.id = :id")
    Optional<TacheOnboarding> findByIdAvecOnboarding(@Param("id") Integer id);

    /**
     * Tâches « contrat signé » encore ouvertes alors que l'employé a désormais un contrat ACTIF
     * (c'est-à-dire signé par les deux parties).
     */
    @Query("""
            SELECT t FROM TacheOnboarding t JOIN FETCH t.onboarding o
            WHERE t.modele.code = :codeModele AND t.statut IN :statuts
              AND EXISTS (SELECT 1 FROM Contrat c
                          WHERE c.employe = o.employe AND c.StatutContract.code = 'ACTIF')
            """)
    List<TacheOnboarding> findContratSigneEnAttente(@Param("codeModele") String codeModele,
            @Param("statuts") Collection<StatutTache> statuts);

    /** [idOnboarding, statut, nombre] */
    @Query("SELECT t.onboarding.id, t.statut, COUNT(t) FROM TacheOnboarding t GROUP BY t.onboarding.id, t.statut")
    List<Object[]> compterParOnboardingEtStatut();

    /** [idOnboarding, nombre] de tâches dont l'échéance est dépassée et qui ne sont pas terminées. */
    @Query("""
            SELECT t.onboarding.id, COUNT(t) FROM TacheOnboarding t
            WHERE t.dateEcheance < :jour AND t.statut IN :statuts
            GROUP BY t.onboarding.id
            """)
    List<Object[]> compterEnRetard(@Param("jour") LocalDate jour, @Param("statuts") Collection<StatutTache> statuts);
}
