package com.si.rh.repository;

import com.si.rh.model.Onboarding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OnboardingRepository extends JpaRepository<Onboarding, Integer> {

    @Query("""
            SELECT o FROM Onboarding o
            JOIN FETCH o.employe e
            JOIN FETCH e.poste
            JOIN FETCH e.departement
            ORDER BY o.dateDebut DESC, o.id DESC
            """)
    List<Onboarding> findAllAvecEmploye();

    @Query("""
            SELECT o FROM Onboarding o
            JOIN FETCH o.employe e
            JOIN FETCH e.poste
            JOIN FETCH e.departement
            LEFT JOIN FETCH o.responsable
            WHERE o.id = :id
            """)
    Optional<Onboarding> findByIdAvecDetails(@Param("id") Integer id);
}
