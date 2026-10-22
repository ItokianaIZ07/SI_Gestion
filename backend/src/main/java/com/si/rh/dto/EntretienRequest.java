//Brnadon
package com.si.rh.dto;

import java.time.LocalDateTime;

public class EntretienRequest {
    private Integer id;
    private Integer idCandidature;
    private String typeEntretien;
    private LocalDateTime dateEntretien;
    private String lieu;
    private Integer idUtilisateurIntervieweur;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getIdCandidature() { return idCandidature; }
    public void setIdCandidature(Integer idCandidature) { this.idCandidature = idCandidature; }
    public String getTypeEntretien() { return typeEntretien; }
    public void setTypeEntretien(String typeEntretien) { this.typeEntretien = typeEntretien; }
    public LocalDateTime getDateEntretien() { return dateEntretien; }
    public void setDateEntretien(LocalDateTime dateEntretien) { this.dateEntretien = dateEntretien; }
    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }
    public Integer getIdUtilisateurIntervieweur() { return idUtilisateurIntervieweur; }
    public void setIdUtilisateurIntervieweur(Integer idUtilisateurIntervieweur) { this.idUtilisateurIntervieweur = idUtilisateurIntervieweur; }
}