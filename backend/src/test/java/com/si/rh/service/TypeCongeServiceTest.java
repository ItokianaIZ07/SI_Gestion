package com.si.rh.service;

import com.si.rh.dto.conge.TypeCongeResponse;
import com.si.rh.model.RegleConge;
import com.si.rh.model.TypeConge;
import com.si.rh.repository.RegleCongeRepository;
import com.si.rh.repository.TypeCongeRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TypeCongeServiceTest {

    @Test
    void retourneLesTypesActifsAvecLaRegleApplicableLaPlusRecente() {
        TypeConge typeAnnuel = new TypeConge(
                1, "ANNUEL", "Congé annuel", "Congé acquis mensuellement", true);
        TypeConge typeSansRegle = new TypeConge(
                2, "SPECIAL", "Congé spécial", null, true);
        TypeConge typeInactif = new TypeConge(
                3, "INACTIF", "Type inactif", null, false);

        RegleConge regleRecente = regle(12, typeAnnuel, 2.5, null);
        RegleConge regleAncienne = regle(
                11, typeAnnuel, 2.0, 60.0);

        RegleCongeRepository regleRepository = mock(RegleCongeRepository.class);
        TypeCongeRepository typeRepository = mock(TypeCongeRepository.class);
        when(regleRepository.findAllApplicableRules(LocalDate.now()))
                .thenReturn(List.of(regleRecente, regleAncienne));
        when(typeRepository.findByActifTrueOrderByLibelleAsc())
                .thenReturn(List.of(typeAnnuel, typeSansRegle, typeInactif));

        List<TypeCongeResponse> response =
                new TypeCongeService(typeRepository, regleRepository).getAllType();

        assertEquals(1, response.size());
        TypeCongeResponse typeResponse = response.get(0);
        assertEquals(1, typeResponse.idType());
        assertEquals("ANNUEL", typeResponse.code());
        assertEquals("Congé annuel", typeResponse.libelleTypeConge());
        assertEquals("Congé acquis mensuellement", typeResponse.description());
        assertEquals(12, typeResponse.regle().idRegle());
        assertEquals(2.5, typeResponse.regle().joursAcquisMois());
        assertNull(typeResponse.regle().plafondJours());
        verify(regleRepository).findAllApplicableRules(LocalDate.now());
        verify(typeRepository).findByActifTrueOrderByLibelleAsc();
    }

    @Test
    void retourneUneListeVideQuandAucunTypeActifNePossedeDeRegleApplicable() {
        TypeCongeRepository typeRepository = mock(TypeCongeRepository.class);
        RegleCongeRepository regleRepository = mock(RegleCongeRepository.class);
        when(regleRepository.findAllApplicableRules(LocalDate.now())).thenReturn(List.of());
        when(typeRepository.findByActifTrueOrderByLibelleAsc())
                .thenReturn(List.of(new TypeConge(
                        1, "ANNUEL", "Congé annuel", null, true)));

        List<TypeCongeResponse> response =
                new TypeCongeService(typeRepository, regleRepository).getAllType();

        assertEquals(List.of(), response);
    }

    private RegleConge regle(
            Integer id,
            TypeConge typeConge,
            double joursAcquis,
            Double plafond) {
        RegleConge regle = new RegleConge();
        regle.setId(id);
        regle.setTypeConge(typeConge);
        regle.setJoursAcquisMois(joursAcquis);
        regle.setPlafondJours(plafond);
        regle.setDateDebutValidite(LocalDate.now().minusMonths(id == 11 ? 1 : 0));
        regle.setActif(true);
        return regle;
    }
}
