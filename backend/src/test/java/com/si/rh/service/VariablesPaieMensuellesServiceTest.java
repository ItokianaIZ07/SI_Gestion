package com.si.rh.service;

import com.si.rh.dto.rh.variablesPaie.VariablesPaieBrutes;
import com.si.rh.dto.rh.variablesPaie.VariablesPaieRecapitulatif;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.model.Role;
import com.si.rh.model.Utilisateur;
import com.si.rh.repository.UtilisateurRepository;
import com.si.rh.repository.VariablesPaieMensuellesRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VariablesPaieMensuellesServiceTest {

    @Mock
    private VariablesPaieMensuellesRepository variablesPaieRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private VariablesPaieMensuellesService service;

    @Test
    void convertitLesMinutesEnHeuresEtMinutesSansLimiteDe24Heures() {
        assertEquals("25:05", service.minutesVersHeuresMinutes(1505L));
        assertEquals("1.50", service.minutesVersHeuresDecimales(90L).toPlainString());
    }

    @Test
    void retourneDesTotauxNulsPourUnMoisSansJourneeCalculee() {
        when(variablesPaieRepository.findRecapitulatif(2026, 11, null)).thenReturn(List.of());

        VariablesPaieRecapitulatif recapitulatif = service.getRecapitulatif(2026, 11, null);

        assertTrue(recapitulatif.lignes().isEmpty());
        assertEquals("00:00", recapitulatif.totaux().tempsPrevu());
        assertEquals(0L, recapitulatif.totaux().nbRetards());
    }

    @Test
    void rejetteUnMoisInvalideAvantDeConsulterLaBase() {
        assertThrows(RegleMetierException.class, () -> service.getRecapitulatif(2026, 13, null));
        verifyNoInteractions(variablesPaieRepository);
    }

    @Test
    void exporteUnCsvAvecBomUtf8EtLigneDeTotaux() {
        when(variablesPaieRepository.findRecapitulatif(2026, 11, null))
                .thenReturn(List.of(new VariablesPaieBrutes(
                        3, "MAT-003", "Ranaivo", "Zoé", "Finances", 2026, 11,
                        480L, 450L, 10L, 1L, 60L, 30L)));

        byte[] export = service.exporterCsv(2026, 11, null);
        String contenu = new String(export, StandardCharsets.UTF_8);

        assertEquals("\uFEFF", contenu.substring(0, 1));
        assertTrue(contenu.contains("Zoé"));
        assertTrue(contenu.contains("TOTAL;;;;2026;11;08:00;8.00;07:30;7.50"));
        assertEquals("variables_paie_2026-11.csv", service.nomFichierCsv(2026, 11));
    }

    @Test
    void utiliseLeFiltreEquipePourUnManagerEtSeulementPourUnManager() {
        Utilisateur manager = new Utilisateur();
        manager.setRole(new Role(1, "MANAGER", "Manager"));
        when(utilisateurRepository.findById(18)).thenReturn(Optional.of(manager));
        when(variablesPaieRepository.findRecapitulatifManager(2026, 11, 4, 18))
                .thenReturn(List.of());

        service.getRecapitulatifManager(2026, 11, 4, 18);

        verify(variablesPaieRepository).findRecapitulatifManager(2026, 11, 4, 18);
    }
}
