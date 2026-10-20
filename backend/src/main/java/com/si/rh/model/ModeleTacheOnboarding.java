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
@Table(name = "modele_tache_onboarding")
public class ModeleTacheOnboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategorieOnboarding categorie;

    @Column(name = "delai_jours", nullable = false)
    private Integer delaiJours = 0;

    @Column(nullable = false)
    private Boolean actif = true;

    // ----- Constructeurs -----

    public ModeleTacheOnboarding() {
    }

    public ModeleTacheOnboarding(Integer id, String code, String libelle, CategorieOnboarding categorie, Integer delaiJours, Boolean actif) {
        this.id = id;
        this.code = code;
        this.libelle = libelle;
        this.categorie = categorie;
        this.delaiJours = delaiJours;
        this.actif = actif;
    }

    // ----- Getters / Setters -----

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

    public CategorieOnboarding getCategorie() {
        return categorie;
    }

    public void setCategorie(CategorieOnboarding categorie) {
        this.categorie = categorie;
    }

    public Integer getDelaiJours() {
        return delaiJours;
    }

    public void setDelaiJours(Integer delaiJours) {
        this.delaiJours = delaiJours;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }
}
