package com.si.rh.dto.recrutement;
import com.si.rh.model.enums.StatutRecrutement;
import java.time.LocalDate;
import java.time.OffsetDateTime;
public record RecrutementDTO(Integer id, Integer besoinId, String reference, String titre, String description, LocalDate dateOuverture, LocalDate dateCloture, StatutRecrutement statut, Integer responsableId, OffsetDateTime dateCreation, OffsetDateTime dateModification) {}
