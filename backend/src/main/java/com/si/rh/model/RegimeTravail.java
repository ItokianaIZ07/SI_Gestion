package com.si.rh.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "regime_travail")
public class RegimeTravail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String libelle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "heures_hebdomadaires", precision = 5, scale = 2)
    private BigDecimal heuresHebdomadaires;

    public RegimeTravail() {
    }

    public RegimeTravail(Integer id, String code, String libelle, String description, BigDecimal heuresHebdomadaires) {
        this.id = id;
        this.code = code;
        this.libelle = libelle;
        this.description = description;
        this.heuresHebdomadaires = heuresHebdomadaires;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getHeuresHebdomadaires() {
        return heuresHebdomadaires;
    }

    public void setHeuresHebdomadaires(BigDecimal heuresHebdomadaires) {
        this.heuresHebdomadaires = heuresHebdomadaires;
    }
}
