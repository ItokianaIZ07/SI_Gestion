package com.si.rh.dto.recrutement;
import com.si.rh.model.enums.StatutRecrutement;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record RecrutementEcritureDTO(@NotNull Integer besoinId, @NotBlank @Size(max=50) String reference, @NotBlank @Size(max=150) String titre, String description, LocalDate dateOuverture, LocalDate dateCloture, StatutRecrutement statut, Integer responsableId) {}
