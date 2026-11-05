package com.si.rh.dto.conge;

import com.si.rh.model.Employe;
import com.si.rh.model.TypeConge;

public record SoldeCongeEmployeDTO(
        Integer idEmploye,
        String nomEmploye,
        String prenomEmploye,
        String matriculeEmploye,
        String emailEmploye,
        String typeConge,
        double solde
) {
}
