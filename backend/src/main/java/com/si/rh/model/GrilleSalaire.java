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
@Table(name = "grille_salaire")
public class GrilleSalaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_poste", nullable = false)
    private Poste poste;

    @Column(name = "valeur_min", nullable = false, precision = 12, scale = 2)
    private BigDecimal valeurMin = BigDecimal.ZERO;

    @Column(name = "valeur_max", nullable = false, precision = 12, scale = 2)
    private BigDecimal valeurMax = BigDecimal.ZERO;

    @Column(name = "date_debut_validite", nullable = false)
    private LocalDate dateDebutValidite = LocalDate.now();

    @Column(name = "date_fin_validite")
    private LocalDate dateFinValidite;

    // ----- Constructeurs -----

    public GrilleSalaire() {
    }

    public GrilleSalaire(Integer id, Poste poste, BigDecimal valeurMin, BigDecimal valeurMax, LocalDate dateDebutValidite, LocalDate dateFinValidite) {
        this.id = id;
        this.poste = poste;
        this.valeurMin = valeurMin;
        this.valeurMax = valeurMax;
        this.dateDebutValidite = dateDebutValidite;
        this.dateFinValidite = dateFinValidite;
    }

    // ----- Getters / Setters -----

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Poste getPoste() {
        return poste;
    }

    public void setPoste(Poste poste) {
        this.poste = poste;
    }

    public BigDecimal getValeurMin() {
        return valeurMin;
    }

    public void setValeurMin(BigDecimal valeurMin) {
        this.valeurMin = valeurMin;
    }

    public BigDecimal getValeurMax() {
        return valeurMax;
    }

    public void setValeurMax(BigDecimal valeurMax) {
        this.valeurMax = valeurMax;
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
}
