package com.sigrh.model;

import com.sigrh.model.enums.*;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "departement")
public class Departement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_utilisateur_manager", nullable = false)
    private Utilisateur manager;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    @Column(name = "effectif_max", nullable = false)
    private Integer effectifMax = 0;

    // ----- Constructeurs -----

    public Departement() {
    }

    public Departement(Integer id, Utilisateur manager, String nom, Integer effectifMax) {
        this.id = id;
        this.manager = manager;
        this.nom = nom;
        this.effectifMax = effectifMax;
    }

    // ----- Getters / Setters -----

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Utilisateur getManager() {
        return manager;
    }

    public void setManager(Utilisateur manager) {
        this.manager = manager;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Integer getEffectifMax() {
        return effectifMax;
    }

    public void setEffectifMax(Integer effectifMax) {
        this.effectifMax = effectifMax;
    }
}
