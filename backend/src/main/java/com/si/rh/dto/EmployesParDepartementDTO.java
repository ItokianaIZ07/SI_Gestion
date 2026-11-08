package com.si.rh.dto;

import java.util.List;

public record EmployesParDepartementDTO(
        Integer idDepartement,
        String nomDepartement,
        List<EmployeResumeDTO> employes
) {
}
