package com.si.rh.model;

import com.si.rh.model.enums.StatutCongeGlobal;
import jakarta.persistence.*;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "demande_conge")
public class DemandeConge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;

    @ManyToOne
    @JoinColumn(name = "id_type_conge", nullable = false)
    private TypeConge typeConge;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "motif")
    private String motif;

    @Column(name = "date_demande")
    private LocalDateTime dateDemande;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_global", nullable = false)
    private StatutCongeGlobal statutGlobal;

    @Column(name = "date_decision_finale")
    private LocalDateTime dateDecisionFinale;

    @Column(name = "nombre_jours")
    private double nombreJours;

    public DemandeConge(){}

    public DemandeConge(Integer id, Employe employe, TypeConge typeConge, LocalDate dateDebut, LocalDate dateFin, String motif, LocalDateTime dateDemande, StatutCongeGlobal statutGlobal, LocalDateTime dateDecisionFinale, double nombreJours) {
        this.id = id;
        this.employe = employe;
        this.typeConge = typeConge;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.motif = motif;
        this.dateDemande = dateDemande;
        this.statutGlobal = statutGlobal;
        this.dateDecisionFinale = dateDecisionFinale;
        this.nombreJours = nombreJours;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Employe getEmploye() {
        return employe;
    }

    public void setEmploye(Employe employe) {
        this.employe = employe;
    }

    public TypeConge getTypeConge() {
        return typeConge;
    }

    public void setTypeConge(TypeConge typeConge) {
        this.typeConge = typeConge;
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

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public LocalDateTime getDateDemande() {
        return dateDemande;
    }

    public void setDateDemande(LocalDateTime dateDemande) {
        this.dateDemande = dateDemande;
    }

    public StatutCongeGlobal getStatutGlobal() {
        return statutGlobal;
    }

    public void setStatutGlobal(StatutCongeGlobal statutGlobal) {
        this.statutGlobal = statutGlobal;
    }

    public LocalDateTime getDateDecisionFinale() {
        return dateDecisionFinale;
    }

    public void setDateDecisionFinale(LocalDateTime dateDecisionFinale) {
        this.dateDecisionFinale = dateDecisionFinale;
    }

    public double getNombreJours() {
        return nombreJours;
    }

    public void setNombreJours(double nombreJours) {
        this.nombreJours = nombreJours;
    }
}
