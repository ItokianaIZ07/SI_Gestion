package com.si.rh.service;

import com.si.rh.dto.conge.AnnulationCongeRequest;
import com.si.rh.dto.conge.AnnulationCongeResponse;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.model.DemandeConge;
import com.si.rh.model.Employe;
import com.si.rh.model.MouvementConge;
import com.si.rh.model.RegleConge;
import com.si.rh.model.Role;
import com.si.rh.model.TypeConge;
import com.si.rh.model.Utilisateur;
import com.si.rh.model.enums.StatutCongeGlobal;
import com.si.rh.repository.DemandeCongeRepository;
import com.si.rh.repository.EmployeRepository;
import com.si.rh.repository.MouvementCongeRepository;
import com.si.rh.repository.RegleCongeRepository;
import com.si.rh.repository.UtilisateurRepository;
import com.si.rh.security.EmployeePrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AnnulationCongeServiceTest {

    private static final Integer DEMANDE_ID = 31;
    private static final String RH_EMAIL = "rh@example.test";

    @Mock
    private DemandeCongeRepository demandeCongeRepository;
    @Mock
    private MouvementCongeRepository mouvementCongeRepository;
    @Mock
    private RegleCongeRepository regleCongeRepository;
    @Mock
    private EmployeRepository employeRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private Authentication authentication;

    private AnnulationCongeService service;

    @BeforeEach
    void setUp() {
        service = new AnnulationCongeService(
                demandeCongeRepository,
                mouvementCongeRepository,
                regleCongeRepository,
                employeRepository,
                utilisateurRepository);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn(RH_EMAIL);
        lenient().doReturn(List.of(new SimpleGrantedAuthority("ROLE_RH")))
                .when(authentication).getAuthorities();
    }

    @Test
    void annuleAvantLeDebutEtRestitueTousLesJours() {
        LocalDate debut = LocalDate.now().plusDays(1);
        DemandeConge demande = demande(debut, debut.plusDays(4));
        Utilisateur rh = utilisateur("RH", RH_EMAIL);
        preparer(demande, rh, true);
        when(mouvementCongeRepository.findAllByDemandeConge_IdAndTypeMouvement(
                DEMANDE_ID, "CONSOMMATION"))
                .thenReturn(List.of(consommation(demande, -5)));
        when(mouvementCongeRepository.findAllByDemandeConge_IdAndTypeMouvement(
                DEMANDE_ID, "RESTITUTION"))
                .thenReturn(List.of());
        when(employeRepository.findByIdForUpdate(demande.getEmploye().getId()))
                .thenReturn(Optional.of(demande.getEmploye()));

        AnnulationCongeResponse response = service.annuler(
                DEMANDE_ID, new AnnulationCongeRequest("Changement de planning"), authentication);

        assertEquals(5, response.joursRestitues());
        assertEquals(StatutCongeGlobal.ANNULEE, demande.getStatutGlobal());
        verify(demandeCongeRepository, never()).compterJoursOuvres(any(), any());
        verifierMouvements(demande, rh, "Changement de planning", 5);
    }

    @Test
    void annulePendantLaPeriodeEtRestitueLesJoursNonUtilises() {
        LocalDate debut = LocalDate.now().minusDays(2);
        DemandeConge demande = demande(debut, LocalDate.now().plusDays(3));
        Utilisateur rh = utilisateur("RH", RH_EMAIL);
        preparer(demande, rh, true);
        when(mouvementCongeRepository.findAllByDemandeConge_IdAndTypeMouvement(
                DEMANDE_ID, "CONSOMMATION"))
                .thenReturn(List.of(consommation(demande, -5)));
        when(mouvementCongeRepository.findAllByDemandeConge_IdAndTypeMouvement(
                DEMANDE_ID, "RESTITUTION"))
                .thenReturn(List.of());
        when(demandeCongeRepository.compterJoursOuvres(debut, LocalDate.now()))
                .thenReturn(2);
        when(employeRepository.findByIdForUpdate(demande.getEmploye().getId()))
                .thenReturn(Optional.of(demande.getEmploye()));

        AnnulationCongeResponse response = service.annuler(
                DEMANDE_ID, new AnnulationCongeRequest("Retour anticipé"), authentication);

        assertEquals(3, response.joursRestitues());
        verifierMouvements(demande, rh, "Retour anticipé", 3);
    }

    @Test
    void annuleApresLaFinSansRestitution() {
        LocalDate fin = LocalDate.now().minusDays(1);
        DemandeConge demande = demande(fin.minusDays(4), fin);
        Utilisateur rh = utilisateur("RH", RH_EMAIL);
        preparer(demande, rh, true);
        when(mouvementCongeRepository.findAllByDemandeConge_IdAndTypeMouvement(
                DEMANDE_ID, "CONSOMMATION"))
                .thenReturn(List.of(consommation(demande, -5)));

        AnnulationCongeResponse response = service.annuler(
                DEMANDE_ID, new AnnulationCongeRequest("Correction administrative"), authentication);

        assertEquals(0, response.joursRestitues());
        assertEquals(StatutCongeGlobal.ANNULEE, demande.getStatutGlobal());
        verifierMouvements(demande, rh, "Correction administrative", 0);
    }

    @Test
    void annulePendantLaPeriodeSansRestitutionSiTousLesJoursSontUtilises() {
        LocalDate debut = LocalDate.now().minusDays(2);
        DemandeConge demande = demande(debut, LocalDate.now().plusDays(3));
        Utilisateur rh = utilisateur("RH", RH_EMAIL);
        preparer(demande, rh, true);
        when(mouvementCongeRepository.findAllByDemandeConge_IdAndTypeMouvement(
                DEMANDE_ID, "CONSOMMATION"))
                .thenReturn(List.of(consommation(demande, -2)));
        when(demandeCongeRepository.compterJoursOuvres(debut, LocalDate.now()))
                .thenReturn(3);

        AnnulationCongeResponse response = service.annuler(
                DEMANDE_ID, new AnnulationCongeRequest("Jours consommés"), authentication);

        assertEquals(0, response.joursRestitues());
        assertEquals(StatutCongeGlobal.ANNULEE, demande.getStatutGlobal());
        verifierMouvements(demande, rh, "Jours consommés", 0);
    }

    @Test
    void regleSansDeductionCreeUneTraceSansModifierLeSolde() {
        LocalDate debut = LocalDate.now().plusDays(1);
        DemandeConge demande = demande(debut, debut.plusDays(4));
        Utilisateur rh = utilisateur("RH", RH_EMAIL);
        preparer(demande, rh, false);

        AnnulationCongeResponse response = service.annuler(
                DEMANDE_ID, new AnnulationCongeRequest("Congé non déduit"), authentication);

        assertEquals(0, response.joursRestitues());
        verify(mouvementCongeRepository, never())
                .findAllByDemandeConge_IdAndTypeMouvement(any(), any());
        verifierMouvements(demande, rh, "Congé non déduit", 0);
    }

    @Test
    void consommationAbsenteNeCreePasDeRestitution() {
        LocalDate debut = LocalDate.now().plusDays(1);
        DemandeConge demande = demande(debut, debut.plusDays(4));
        Utilisateur rh = utilisateur("RH", RH_EMAIL);
        preparer(demande, rh, true);
        when(mouvementCongeRepository.findAllByDemandeConge_IdAndTypeMouvement(
                DEMANDE_ID, "CONSOMMATION"))
                .thenReturn(List.of());

        AnnulationCongeResponse response = service.annuler(
                DEMANDE_ID, new AnnulationCongeRequest("Aucun débit historique"), authentication);

        assertEquals(0, response.joursRestitues());
        verifierMouvements(demande, rh, "Aucun débit historique", 0);
    }

    @Test
    void refuseLesDemandesDejaAnnulees() {
        DemandeConge demande = demande(
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));
        demande.setStatutGlobal(StatutCongeGlobal.ANNULEE);
        when(demandeCongeRepository.findByIdForUpdate(DEMANDE_ID))
                .thenReturn(Optional.of(demande));

        assertThrows(RegleMetierException.class, () -> service.annuler(
                DEMANDE_ID, new AnnulationCongeRequest("Deuxième tentative"), authentication));
        verify(mouvementCongeRepository, never()).save(any(MouvementConge.class));
    }

    @Test
    void refuseUneRestitutionDejaPresente() {
        LocalDate debut = LocalDate.now().plusDays(1);
        DemandeConge demande = demande(debut, debut.plusDays(4));
        preparer(demande, utilisateur("RH", RH_EMAIL), true);
        when(mouvementCongeRepository.findAllByDemandeConge_IdAndTypeMouvement(
                DEMANDE_ID, "CONSOMMATION"))
                .thenReturn(List.of(consommation(demande, -5)));
        when(mouvementCongeRepository.findAllByDemandeConge_IdAndTypeMouvement(
                DEMANDE_ID, "RESTITUTION"))
                .thenReturn(List.of(new MouvementConge()));

        assertThrows(RegleMetierException.class, () -> service.annuler(
                DEMANDE_ID, new AnnulationCongeRequest("Restitution existante"), authentication));
        assertEquals(StatutCongeGlobal.VALIDEE, demande.getStatutGlobal());
        verify(mouvementCongeRepository, never()).save(any(MouvementConge.class));
    }

    @Test
    void refuseUnMouvementAnnulationDejaPresent() {
        LocalDate debut = LocalDate.now().plusDays(1);
        DemandeConge demande = demande(debut, debut.plusDays(4));
        preparer(demande, utilisateur("RH", RH_EMAIL), false);
        when(mouvementCongeRepository.existsByDemandeConge_IdAndTypeMouvement(
                DEMANDE_ID, "ANNULATION"))
                .thenReturn(true);

        assertThrows(RegleMetierException.class, () -> service.annuler(
                DEMANDE_ID,
                new AnnulationCongeRequest("Annulation déjà enregistrée"),
                authentication));
        verify(mouvementCongeRepository, never()).save(any(MouvementConge.class));
        verify(demandeCongeRepository, never()).save(demande);
    }

    @Test
    void employeNePeutAnnulerQueSaPropreDemande() {
        LocalDate debut = LocalDate.now().plusDays(1);
        DemandeConge demande = demande(debut, debut.plusDays(4));
        when(demandeCongeRepository.findByIdForUpdate(DEMANDE_ID))
                .thenReturn(Optional.of(demande));
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_EMPLOYE")))
                .when(authentication).getAuthorities();
        when(authentication.getPrincipal()).thenReturn(new EmployeePrincipal(
                999, "EMP999", "Autre", "Employé", "other@example.test"));

        assertThrows(AccessDeniedException.class, () -> service.annuler(
                DEMANDE_ID, new AnnulationCongeRequest("Tentative étrangère"), authentication));
        verify(regleCongeRepository, never()).findApplicableRules(any(), any());
    }

    @Test
    void employePeutAnnulerSaPropreDemandeSansRestitution() {
        LocalDate debut = LocalDate.now().plusDays(1);
        DemandeConge demande = demande(debut, debut.plusDays(4));
        Utilisateur employeUtilisateur = utilisateur("EMPLOYE", "jean@example.test");
        demande.getEmploye().setUtilisateur(employeUtilisateur);
        when(demandeCongeRepository.findByIdForUpdate(DEMANDE_ID))
                .thenReturn(Optional.of(demande));
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_EMPLOYE")))
                .when(authentication).getAuthorities();
        when(authentication.getPrincipal()).thenReturn(new EmployeePrincipal(
                demande.getEmploye().getId(),
                demande.getEmploye().getMatricule(),
                "Rakoto",
                "Jean",
                "jean@example.test"));
        RegleConge regle = new RegleConge();
        regle.setDeductionSolde(false);
        when(regleCongeRepository.findApplicableRules(
                demande.getTypeConge().getId(), demande.getDateDebut()))
                .thenReturn(List.of(regle));
        when(employeRepository.findByIdForUpdate(demande.getEmploye().getId()))
                .thenReturn(Optional.of(demande.getEmploye()));

        AnnulationCongeResponse response = service.annuler(
                DEMANDE_ID, new AnnulationCongeRequest("Annulation par le salarié"), authentication);

        assertEquals("ANNULEE", response.statutGlobal());
        assertEquals(StatutCongeGlobal.ANNULEE, demande.getStatutGlobal());
        verify(demandeCongeRepository).save(demande);
        verify(mouvementCongeRepository).save(any(MouvementConge.class));
    }

    private void preparer(DemandeConge demande, Utilisateur rh, boolean deduction) {
        when(demandeCongeRepository.findByIdForUpdate(DEMANDE_ID))
                .thenReturn(Optional.of(demande));
        when(utilisateurRepository.findByEmail(RH_EMAIL)).thenReturn(Optional.of(rh));
        RegleConge regle = new RegleConge();
        regle.setDeductionSolde(deduction);
        when(regleCongeRepository.findApplicableRules(
                demande.getTypeConge().getId(), demande.getDateDebut()))
                .thenReturn(List.of(regle));
        when(employeRepository.findByIdForUpdate(demande.getEmploye().getId()))
                .thenReturn(Optional.of(demande.getEmploye()));
    }

    private DemandeConge demande(LocalDate debut, LocalDate fin) {
        Employe employe = new Employe();
        employe.setId(8);
        employe.setMatricule("EMP008");

        TypeConge typeConge = new TypeConge();
        typeConge.setId(4);

        DemandeConge demande = new DemandeConge();
        demande.setId(DEMANDE_ID);
        demande.setEmploye(employe);
        demande.setTypeConge(typeConge);
        demande.setDateDebut(debut);
        demande.setDateFin(fin);
        demande.setNombreJours(5);
        demande.setStatutGlobal(StatutCongeGlobal.VALIDEE);
        return demande;
    }

    private Utilisateur utilisateur(String roleCode, String email) {
        Role role = new Role();
        role.setCode(roleCode);
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(18);
        utilisateur.setEmail(email);
        utilisateur.setActif(true);
        utilisateur.setRole(role);
        return utilisateur;
    }

    private MouvementConge consommation(DemandeConge demande, double nombreJours) {
        MouvementConge mouvement = new MouvementConge();
        mouvement.setDemandeConge(demande);
        mouvement.setNombreJours(nombreJours);
        mouvement.setTypeMouvement("CONSOMMATION");
        return mouvement;
    }

    private void verifierMouvements(
            DemandeConge demande,
            Utilisateur utilisateur,
            String motif,
            double joursRestitues) {
        ArgumentCaptor<MouvementConge> captor =
                ArgumentCaptor.forClass(MouvementConge.class);
        int nombreMouvements = joursRestitues > 0 ? 2 : 1;
        verify(mouvementCongeRepository, times(nombreMouvements)).save(captor.capture());
        List<MouvementConge> mouvements = captor.getAllValues();
        MouvementConge annulation = mouvements.stream()
                .filter(mouvement -> "ANNULATION".equals(mouvement.getTypeMouvement()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, annulation.getNombreJours());
        assertEquals(motif, annulation.getMotif());
        assertEquals(demande, annulation.getDemandeConge());
        assertEquals(demande.getEmploye(), annulation.getEmploye());
        assertEquals(demande.getTypeConge(), annulation.getTypeConge());
        assertEquals(utilisateur, annulation.getUtilisateur());
        assertEquals(demande.getDateDebut(), annulation.getPeriodeDebut());

        List<MouvementConge> restitutions = mouvements.stream()
                .filter(mouvement -> "RESTITUTION".equals(mouvement.getTypeMouvement()))
                .toList();
        assertEquals(joursRestitues > 0 ? 1 : 0, restitutions.size());
        if (joursRestitues > 0) {
            MouvementConge restitution = restitutions.get(0);
            assertEquals(joursRestitues, restitution.getNombreJours());
            assertEquals(demande, restitution.getDemandeConge());
            assertEquals(demande.getEmploye(), restitution.getEmploye());
            assertEquals(demande.getTypeConge(), restitution.getTypeConge());
            assertEquals(utilisateur, restitution.getUtilisateur());
            assertEquals(demande.getDateDebut(), restitution.getPeriodeDebut());
        }
    }
}
