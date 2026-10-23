package com.si.rh.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EmbaucheRequest {
    private Integer idCandidature;
    private String typeContrat;
    private LocalDate dateDebut;
    private BigDecimal salaire;
    private Integer periodeEssaiJours;

    public Integer getIdCandidature() { return idCandidature; }
    public void setIdCandidature(Integer idCandidature) { this.idCandidature = idCandidature; }
    public String getTypeContrat() { return typeContrat; }
    public void setTypeContrat(String typeContrat) { this.typeContrat = typeContrat; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public BigDecimal getSalaire() { return salaire; }
    public void setSalaire(BigDecimal salaire) { this.salaire = salaire; }
    public Integer getPeriodeEssaiJours() { return periodeEssaiJours; }
    public void setPeriodeEssaiJours(Integer periodeEssaiJours) { this.periodeEssaiJours = periodeEssaiJours; }
}