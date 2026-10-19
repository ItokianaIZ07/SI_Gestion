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
@Table(name = "recrutement")
public class Recrutement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_besoin", nullable = false, unique = true)
    private Besoin besoin;

    @Column(nullable = false, unique = true, length = 50)
    private String reference;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_ouverture", nullable = false)
    private LocalDate dateOuverture = LocalDate.now();

    @Column(name = "date_cloture")
    private LocalDate dateCloture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutRecrutement statut = StatutRecrutement.BROUILLON;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur_responsable")
    private Utilisateur responsable;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    // ----- Constructeurs -----

    public Recrutement() {
    }

    public Recrutement(Integer id, Besoin besoin, String reference, String titre, String description, LocalDate dateOuverture, LocalDate dateCloture, StatutRecrutement statut, Utilisateur responsable, OffsetDateTime dateCreation, OffsetDateTime dateModification) {
        this.id = id;
        this.besoin = besoin;
        this.reference = reference;
        this.titre = titre;
        this.description = description;
        this.dateOuverture = dateOuverture;
        this.dateCloture = dateCloture;
        this.statut = statut;
        this.responsable = responsable;
        this.dateCreation = dateCreation;
        this.dateModification = dateModification;
    }

    // ----- Getters / Setters -----

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Besoin getBesoin() {
        return besoin;
    }

    public void setBesoin(Besoin besoin) {
        this.besoin = besoin;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDateOuverture() {
        return dateOuverture;
    }

    public void setDateOuverture(LocalDate dateOuverture) {
        this.dateOuverture = dateOuverture;
    }

    public LocalDate getDateCloture() {
        return dateCloture;
    }

    public void setDateCloture(LocalDate dateCloture) {
        this.dateCloture = dateCloture;
    }

    public StatutRecrutement getStatut() {
        return statut;
    }

    public void setStatut(StatutRecrutement statut) {
        this.statut = statut;
    }

    public Utilisateur getResponsable() {
        return responsable;
    }

    public void setResponsable(Utilisateur responsable) {
        this.responsable = responsable;
    }

    public OffsetDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(OffsetDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public OffsetDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(OffsetDateTime dateModification) {
        this.dateModification = dateModification;
    }
}
