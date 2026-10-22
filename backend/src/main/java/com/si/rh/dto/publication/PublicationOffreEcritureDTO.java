package com.si.rh.dto.publication;

import com.si.rh.model.enums.StatutPublication;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record PublicationOffreEcritureDTO(@NotNull Integer offreId, @NotBlank @Size(max = 100) String plateforme,
        String urlPublication, LocalDate datePublication, LocalDate dateExpiration, StatutPublication statut) {
}
