package com.si.rh.service;

import com.si.rh.dto.fiche.*;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.model.Contrat;
import com.si.rh.model.Employe;
import com.si.rh.repository.ContratOnboardingRepository;
import com.si.rh.repository.ContratRepository;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.repository.HistoriqueAffectationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Fiche salarié centralisée (lecture seule). */
@Service
@Transactional(readOnly = true)
public class FicheEmployeService {

    private final EmployeRepository employeRepository;
    private final HistoriqueAffectationRepository historiqueRepository;
    private final ContratRepository contratRepository;
    private final ContratOnboardingRepository contratResumeRepository;
    private final DocumentAdministratifService documentService;

    public FicheEmployeService(EmployeRepository employeRepository,
            HistoriqueAffectationRepository historiqueRepository, ContratRepository contratRepository,
            ContratOnboardingRepository contratResumeRepository, DocumentAdministratifService documentService) {
        this.employeRepository = employeRepository;
        this.historiqueRepository = historiqueRepository;
        this.contratRepository = contratRepository;
        this.contratResumeRepository = contratResumeRepository;
        this.documentService = documentService;
    }

    public List<EmployeResumeDTO> liste() {
        return employeRepository.findAllAvecPosteEtDepartement().stream()
                .map(e -> new EmployeResumeDTO(e.getId(), e.getMatricule(), e.getNom(), e.getPrenom(), e.getEmail(),
                        e.getPoste().getNom(), e.getDepartement().getNom(), e.getStatut(), e.getDateEmbauche(),
                        e.getDateDepart()))
                .toList();
    }

    public FicheEmployeDTO fiche(Integer id) {
        Employe e = employeRepository.findByIdAvecDetails(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Employé introuvable : " + id));

        List<AffectationDTO> carriere = historiqueRepository.carriere(id).stream()
                .map(AffectationService::versDto).toList();
        AffectationDTO actuelle = carriere.stream().filter(AffectationDTO::courante).findFirst().orElse(null);
        // Sécurité : un employé sans historique (ne devrait pas arriver) retombe sur les valeurs de sa fiche.
        if (actuelle == null && carriere.isEmpty()) {
            actuelle = new AffectationDTO(null, e.getDepartement().getId(), e.getDepartement().getNom(),
                    e.getPoste().getId(), e.getPoste().getNom(), null, null, e.getDateEmbauche(), null, null, true);
        }

        ContratActifDTO contratActif = contratRepository.findActifParEmploye(id).map(FicheEmployeService::versDto)
                .orElse(null);

        return new FicheEmployeDTO(e.getId(), e.getMatricule(), e.getNom(), e.getPrenom(), e.getEmail(),
                e.getTelephone(), e.getNumeroCin(), e.getDateNaissance(), e.getAdresse(), e.getDateEmbauche(),
                e.getDateDepart(), e.getStatut(), e.getUtilisateur() != null, actuelle, contratActif,
                contratResumeRepository.parEmploye(id), documentService.liste(id), carriere);
    }

    private static ContratActifDTO versDto(Contrat c) {
        return new ContratActifDTO(c.getId(), c.getReference(), c.getTypeContrat().getLibelle(),
                c.getStatutContract().getCode(), c.getDateDebut(), c.getDateFin(), c.getDateSignature(),
                c.getSalaire(), c.getPeriodeEssaiJours());
    }
}
