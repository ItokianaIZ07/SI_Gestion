package com.si.rh.model;

import jakarta.persistence.*;

import java.sql.Date;
import java.time.LocalDate;

@Entity
@Table(name="regle_conge")
public class RegleConge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_type_conge", nullable = false)
    private TypeConge typeConge;

    @Column(name = "deduction_solde", nullable = false)
    private boolean deductionSolde;

    @Column(name = "justificatif_obligatoire", nullable = false)
    private boolean justificatifObligatoire;

    @Column(name = "remplacant_obligatoire", nullable = false)
    private boolean remplacantObligatoire;

    @Column(name = "acquisition_autorisee", nullable = false)
    private boolean acquisitionAutorisee; // raha true d zay vo mise zavatra le jours_acquis

    @Column(name = "jours_acquis_mois", nullable = false)
    private double joursAcquisMois;

    @Column(name = "plafond_jours")
    private double plafondJours;

    @Column(name = "date_debut_validite", nullable = false)
    private LocalDate dateDebutValidite;

    @Column(name = "date_fin_validite")
    private LocalDate dateFinValidite;

    @Column(name = "actif", nullable = false)
    private boolean actif;

    public RegleConge(){}

    public RegleConge(Integer id, TypeConge typeConge, boolean deductionSolde, boolean justificatifObligatoire, boolean remplacantObligatoire, boolean acquisitionAutorisee, double joursAcquisMois, double plafondJours, LocalDate dateDebutValidite, LocalDate dateFinValidite, boolean actif) {
        this.id = id;
        this.typeConge = typeConge;
        this.deductionSolde = deductionSolde;
        this.justificatifObligatoire = justificatifObligatoire;
        this.remplacantObligatoire = remplacantObligatoire;
        this.acquisitionAutorisee = acquisitionAutorisee;
        this.joursAcquisMois = joursAcquisMois;
        this.plafondJours = plafondJours;
        this.dateDebutValidite = dateDebutValidite;
        this.dateFinValidite = dateFinValidite;
        this.actif = actif;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public TypeConge getTypeConge() {
        return typeConge;
    }

    public void setTypeConge(TypeConge typeConge) {
        this.typeConge = typeConge;
    }

    public boolean isDeductionSolde() {
        return deductionSolde;
    }

    public void setDeductionSolde(boolean deductionSolde) {
        this.deductionSolde = deductionSolde;
    }

    public boolean isJustificatifObligatoire() {
        return justificatifObligatoire;
    }

    public void setJustificatifObligatoire(boolean justificatifObligatoire) {
        this.justificatifObligatoire = justificatifObligatoire;
    }

    public boolean isRemplacantObligatoire() {
        return remplacantObligatoire;
    }

    public void setRemplacantObligatoire(boolean remplacantObligatoire) {
        this.remplacantObligatoire = remplacantObligatoire;
    }

    public boolean isAcquisitionAutorisee() {
        return acquisitionAutorisee;
    }

    public void setAcquisitionAutorisee(boolean acquisitionAutorisee) {
        this.acquisitionAutorisee = acquisitionAutorisee;
    }

    public double getJoursAcquisMois() {
        return joursAcquisMois;
    }

    public void setJoursAcquisMois(double joursAcquisMois) {
        this.joursAcquisMois = joursAcquisMois;
    }

    public double getPlafondJours() {
        return plafondJours;
    }

    public void setPlafondJours(double plafondJours) {
        this.plafondJours = plafondJours;
    }

    public LocalDate getDateDebutValidite() {
        return dateDebutValidite;
    }

    public void setDateDebutValidite(LocalDate dateDebutValidite) {
        this.dateDebutValidite = dateDebutValidite;
    }

    public LocalDate getDateFinValidite() {
        return dateFinValidite;
    }

    public void setDateFinValidite(LocalDate dateFinValidite) {
        this.dateFinValidite = dateFinValidite;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }
}
