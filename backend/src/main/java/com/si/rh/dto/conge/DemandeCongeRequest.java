package com.si.rh.dto.conge;

import java.time.LocalDate;

/**
 *
 * @Author Itokiana
 * */

public class DemandeCongeRequest {
    private int idEmploye;
    private int idTypeConge;
    private Integer idEmployeRemplacant;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private double nombreJours;
    private String motif;

    public DemandeCongeRequest(){}

    public DemandeCongeRequest(int idEmploye, int idTypeConge, LocalDate dateDebut, LocalDate dateFin, double nombreJours, String motif) {
        this(idEmploye, idTypeConge, null, dateDebut, dateFin, nombreJours, motif);
    }

    public DemandeCongeRequest(
            int idEmploye,
            int idTypeConge,
            Integer idEmployeRemplacant,
            LocalDate dateDebut,
            LocalDate dateFin,
            double nombreJours,
            String motif) {
        this.idEmploye = idEmploye;
        this.idTypeConge = idTypeConge;
        this.idEmployeRemplacant = idEmployeRemplacant;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.nombreJours = nombreJours;
        this.motif = motif;
    }

    public int getIdEmploye() {
        return idEmploye;
    }

    public void setIdEmploye(int idEmploye) {
        this.idEmploye = idEmploye;
    }

    public int getIdTypeConge() {
        return idTypeConge;
    }

    public void setIdTypeConge(int idTypeConge) {
        this.idTypeConge = idTypeConge;
    }

    public Integer getIdEmployeRemplacant() {
        return idEmployeRemplacant;
    }

    public void setIdEmployeRemplacant(Integer idEmployeRemplacant) {
        this.idEmployeRemplacant = idEmployeRemplacant;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public double getNombreJours() {
        return nombreJours;
    }

    public void setNombreJours(double nombreJours) {
        this.nombreJours = nombreJours;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }
}
