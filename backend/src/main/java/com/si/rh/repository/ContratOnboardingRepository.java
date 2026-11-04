package com.si.rh.repository;

import com.si.rh.dto.onboarding.ContratResumeDTO;
import com.si.rh.model.Contrat;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.util.List;

/** Lecture des contrats d'un employé pour l'écran d'onboarding (sans charger le fichier du contrat). */
public interface ContratOnboardingRepository extends Repository<Contrat, Integer> {

    @Query("""
            SELECT new com.si.rh.dto.onboarding.ContratResumeDTO(
                c.id, c.reference, t.code, s.code, c.dateDebut, c.dateFin, c.dateSignature)
            FROM Contrat c
            JOIN c.typeContrat t
            JOIN c.StatutContract s
            WHERE c.employe.id = :idEmploye
            ORDER BY c.dateDebut DESC, c.id DESC
            """)
    List<ContratResumeDTO> parEmploye(@Param("idEmploye") Integer idEmploye);
}
