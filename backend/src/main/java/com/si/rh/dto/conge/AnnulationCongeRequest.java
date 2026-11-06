package com.si.rh.dto.conge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnulationCongeRequest(
        @NotBlank @Size(max = 2000) String motif
) {
}
