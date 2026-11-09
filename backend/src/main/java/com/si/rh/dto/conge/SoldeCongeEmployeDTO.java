package com.si.rh.dto.conge;


/**
 *
 * @Author Itokiana
 * */

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
