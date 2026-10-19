package com.sigrh.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Un seul des deux liens (besoin ou contrat) peut être renseigné. */
@Entity
@Table(name = "depense_departement")
public class DepenseDepartement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_departement", nullable = false)
    private Departement departement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_besoin")
    private Besoin besoin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrat")
    private Contrat contrat;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Column(length = 100)
    private String categorie;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal montant = BigDecimal.ZERO;

    @Column(name = "date_depense", nullable = false)
    private LocalDate dateDepense = LocalDate.now();

    /** Colonne générée par PostgreSQL (STORED) : lecture seule. */
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "annee", insertable = false, updatable = false)
    private Integer annee;

    // ----- Constructeurs -----

    public DepenseDepartement() {
    }

    public DepenseDepartement(Integer id, Departement departement, Besoin besoin, Contrat contrat, String libelle, String categorie, BigDecimal montant, LocalDate dateDepense, Integer annee) {
        this.id = id;
        this.departement = departement;
        this.besoin = besoin;
        this.contrat = contrat;
        this.libelle = libelle;
        this.categorie = categorie;
        this.montant = montant;
        this.dateDepense = dateDepense;
        this.annee = annee;
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

    public Besoin getBesoin() {
        return besoin;
    }

    public void setBesoin(Besoin besoin) {
        this.besoin = besoin;
    }

    public Contrat getContrat() {
        return contrat;
    }

    public void setContrat(Contrat contrat) {
        this.contrat = contrat;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public LocalDate getDateDepense() {
        return dateDepense;
    }

    public void setDateDepense(LocalDate dateDepense) {
        this.dateDepense = dateDepense;
    }

    public Integer getAnnee() {
        return annee;
    }

    public void setAnnee(Integer annee) {
        this.annee = annee;
    }
}
