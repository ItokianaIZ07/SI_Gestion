package com.si.rh.dto.conge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DecisionCongeRequest(
        @NotBlank @Pattern(regexp = "ACCEPTE|REFUSE") String decision,
        @Size(max = 2000) String commentaire
) {
}
