package com.si.rh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "planning_travail")
public class PlanningTravail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_horaire", nullable = false)
    private HoraireTravail horaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_regime")
    private RegimeTravail regime;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    public PlanningTravail() {
    }

    public PlanningTravail(Integer id, Employe employe, HoraireTravail horaire, RegimeTravail regime,
                           LocalDate dateDebut, LocalDate dateFin) {
        this.id = id;
        this.employe = employe;
        this.horaire = horaire;
        this.regime = regime;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
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

    public HoraireTravail getHoraire() {
        return horaire;
    }

    public void setHoraire(HoraireTravail horaire) {
        this.horaire = horaire;
    }

    public RegimeTravail getRegime() {
        return regime;
    }

    public void setRegime(RegimeTravail regime) {
        this.regime = regime;
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
}
