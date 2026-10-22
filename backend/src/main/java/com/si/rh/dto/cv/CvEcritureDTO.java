package com.si.rh.dto.cv;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CvEcritureDTO(@NotNull Integer candidatId, String details, @NotBlank @Size(max = 255) String nomFichier,
        @NotBlank @Size(max = 100) String mimeType) {
}
