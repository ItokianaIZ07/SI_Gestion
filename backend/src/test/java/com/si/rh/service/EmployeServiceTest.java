package com.si.rh.service;

import com.si.rh.dto.EmployeResumeDTO;
import com.si.rh.dto.EmployesParDepartementDTO;
import com.si.rh.model.Departement;
import com.si.rh.model.Employe;
import com.si.rh.model.enums.StatutEmploye;
import com.si.rh.repository.DepartementRepository;
import com.si.rh.repository.EmployeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmployeServiceTest {

    @Test
    void groupeLesEmployesActifsParDepartement() {
        Departement departement = new Departement();
        departement.setId(5);
        departement.setNom("Informatique");
        Employe employe = employe(10, departement);

        DepartementRepository departementRepository = mock(DepartementRepository.class);
        EmployeRepository employeRepository = mock(EmployeRepository.class);
        when(departementRepository.findAllByOrderByNomAsc())
                .thenReturn(List.of(departement));
        when(employeRepository.findByStatutOrderByNomAscPrenomAsc(StatutEmploye.ACTIF))
                .thenReturn(List.of(employe));

        List<EmployesParDepartementDTO> response =
                new EmployeService(employeRepository, departementRepository)
                        .getEmployesParDepartement();

        assertEquals(1, response.size());
        assertEquals(5, response.get(0).idDepartement());
        assertEquals("Informatique", response.get(0).nomDepartement());
        assertEquals(new EmployeResumeDTO(
                10, "EMP010", "Rakoto", "Jean", "jean@example.test"),
                response.get(0).employes().get(0));
    }

    @Test
    void retourneLesEmployesActifsDuDepartementDemande() {
        Employe employe = employe(10, null);
        DepartementRepository departementRepository = mock(DepartementRepository.class);
        EmployeRepository employeRepository = mock(EmployeRepository.class);
        when(departementRepository.existsById(5)).thenReturn(true);
        when(employeRepository.findByDepartement_IdAndStatutOrderByNomAscPrenomAsc(
                5, StatutEmploye.ACTIF))
                .thenReturn(List.of(employe));

        List<EmployeResumeDTO> response =
                new EmployeService(employeRepository, departementRepository)
                        .getEmployesActifsParDepartement(5);

        assertEquals(1, response.size());
        assertEquals(10, response.get(0).id());
        verify(employeRepository).findByDepartement_IdAndStatutOrderByNomAscPrenomAsc(
                5, StatutEmploye.ACTIF);
    }

    private Employe employe(Integer id, Departement departement) {
        Employe employe = new Employe();
        employe.setId(id);
        employe.setMatricule("EMP010");
        employe.setNom("Rakoto");
        employe.setPrenom("Jean");
        employe.setEmail("jean@example.test");
        employe.setDepartement(departement);
        employe.setStatut(StatutEmploye.ACTIF);
        return employe;
    }
}
