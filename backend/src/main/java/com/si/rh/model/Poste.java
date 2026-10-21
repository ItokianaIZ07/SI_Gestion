package com.si.rh.model;

import com.si.rh.model.enums.*;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "poste",
       uniqueConstraints = @UniqueConstraint(name = "uq_poste_departement_nom",
                                             columnNames = {"id_departement", "nom"}))
public class Poste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_departement", nullable = false)
    private Departement departement;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false)
    private Boolean actif = true;

    // ----- Constructeurs -----

    public Poste() {
    }

    public Poste(Integer id, Departement departement, String nom, Boolean actif) {
        this.id = id;
        this.departement = departement;
        this.nom = nom;
        this.actif = actif;
    }

    // ----- Getters / Setters -----

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Departement getDepartement() {
        return departement;
    }

    public void setDepartement(Departement departement) {
        this.departement = departement;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }
}
