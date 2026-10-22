package com.si.rh.repository;

import com.si.rh.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;   // ← AJOUT

import java.util.List;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {
    Optional<Utilisateur> findByEmail(String email);

    @Query("""
        SELECT u FROM Utilisateur u
        JOIN FETCH u.role r
        WHERE r.code IN ('RH', 'MANAGER')
        ORDER BY u.nom, u.prenom
    """)
    List<Utilisateur> findIntervieweurs();
}
