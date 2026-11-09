package com.si.rh.dto.fiche;

import com.si.rh.dto.document.DocumentAdminDTO;
import com.si.rh.dto.onboarding.ContratResumeDTO;
import com.si.rh.model.enums.StatutEmploye;

import java.time.LocalDate;
import java.util.List;

public record FicheEmployeDTO(Integer id, String matricule, String nom, String prenom, String email,
        String telephone, String numeroCin, LocalDate dateNaissance, String adresse, LocalDate dateEmbauche,
        LocalDate dateDepart, StatutEmploye statut, boolean aCompteUtilisateur, AffectationDTO affectationActuelle,
        ContratActifDTO contratActif, List<ContratResumeDTO> contrats, List<DocumentAdminDTO> documents,
        List<AffectationDTO> carriere) {
}
