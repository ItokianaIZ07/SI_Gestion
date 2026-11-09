package com.si.rh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;

@Entity
@Table(name = "horaire_travail")
public class HoraireTravail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String libelle;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "heure_fin", nullable = false)
    private LocalTime heureFin;

    @Column(name = "debut_pause")
    private LocalTime debutPause;

    @Column(name = "fin_pause")
    private LocalTime finPause;

    @Column(name = "tolerance_retard_minutes", nullable = false)
    private Integer toleranceRetardMinutes = 0;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "jours_travail", nullable = false, columnDefinition = "smallint[]")
    private Short[] joursTravail = {1, 2, 3, 4, 5};

    @Column(nullable = false)
    private Boolean actif = true;

    public HoraireTravail() {
    }

    public HoraireTravail(Integer id, String code, String libelle, LocalTime heureDebut, LocalTime heureFin,
                          LocalTime debutPause, LocalTime finPause, Integer toleranceRetardMinutes,
                          Short[] joursTravail, Boolean actif) {
        this.id = id;
        this.code = code;
        this.libelle = libelle;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.debutPause = debutPause;
        this.finPause = finPause;
        this.toleranceRetardMinutes = toleranceRetardMinutes;
        this.joursTravail = joursTravail;
        this.actif = actif;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public LocalTime getHeureDebut() {
        return heureDebut;
    }

    public void setHeureDebut(LocalTime heureDebut) {
        this.heureDebut = heureDebut;
    }

    public LocalTime getHeureFin() {
        return heureFin;
    }

    public void setHeureFin(LocalTime heureFin) {
        this.heureFin = heureFin;
    }

    public LocalTime getDebutPause() {
        return debutPause;
    }

    public void setDebutPause(LocalTime debutPause) {
        this.debutPause = debutPause;
    }

    public LocalTime getFinPause() {
        return finPause;
    }

    public void setFinPause(LocalTime finPause) {
        this.finPause = finPause;
    }

    public Integer getToleranceRetardMinutes() {
        return toleranceRetardMinutes;
    }

    public void setToleranceRetardMinutes(Integer toleranceRetardMinutes) {
        this.toleranceRetardMinutes = toleranceRetardMinutes;
    }

    public Short[] getJoursTravail() {
        return joursTravail;
    }

    public void setJoursTravail(Short[] joursTravail) {
        this.joursTravail = joursTravail;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }
}
