package com.si.rh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "jour_ferie")
public class JourFerie {

    @Id
    @Column(name = "date_jour", nullable = false)
    private LocalDate dateJour;

    @Column(nullable = false, length = 100)
    private String libelle;

    public JourFerie() {
    }

    public JourFerie(LocalDate dateJour, String libelle) {
        this.dateJour = dateJour;
        this.libelle = libelle;
    }

    public LocalDate getDateJour() {
        return dateJour;
    }

    public void setDateJour(LocalDate dateJour) {
        this.dateJour = dateJour;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }
}
