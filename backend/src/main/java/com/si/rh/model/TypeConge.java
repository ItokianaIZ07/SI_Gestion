package com.si.rh.model;

import jakarta.persistence.*;

@Entity
@Table(name = "type_conge")
public class TypeConge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name="libelle", length = 30, nullable = false)
    private String libelle;

    @Column(name = "description")
    private String description;

    @Column(name = "actif", nullable = false)
    private boolean actif;

    public TypeConge(){}

    public TypeConge(Integer id, String code, String libelle, String description, boolean actif) {
        this.id = id;
        this.code = code;
        this.libelle = libelle;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }
}
