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
@Table(name = "offre_emploi")
public class OffreEmploi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_recrutement", nullable = false, unique = true)
    private Recrutement recrutement;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(name = "description_poste", nullable = false, columnDefinition = "TEXT")
    private String descriptionPoste;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String missions;

    @Column(name = "competences_requises", columnDefinition = "TEXT")
    private String competencesRequises;

    @Column(name = "niveau_etude", length = 100)
    private String niveauEtude;

    @Column(name = "experience_min_annees", nullable = false)
    private Integer experienceMinAnnees = 0;

    @Column(name = "salaire_min", precision = 12, scale = 2)
    private BigDecimal salaireMin;

    @Column(name = "salaire_max", precision = 12, scale = 2)
    private BigDecimal salaireMax;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutOffre statut = StatutOffre.BROUILLON;

    // ----- Constructeurs -----

    public OffreEmploi() {
    }

    public OffreEmploi(Integer id, Recrutement recrutement, String titre, String descriptionPoste, String missions, String competencesRequises, String niveauEtude, Integer experienceMinAnnees, BigDecimal salaireMin, BigDecimal salaireMax, OffsetDateTime dateCreation, StatutOffre statut) {
        this.id = id;
        this.recrutement = recrutement;
        this.titre = titre;
        this.descriptionPoste = descriptionPoste;
        this.missions = missions;
        this.competencesRequises = competencesRequises;
        this.niveauEtude = niveauEtude;
        this.experienceMinAnnees = experienceMinAnnees;
        this.salaireMin = salaireMin;
        this.salaireMax = salaireMax;
        this.dateCreation = dateCreation;
        this.statut = statut;
    }

    // ----- Getters / Setters -----

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Recrutement getRecrutement() {
        return recrutement;
    }

    public void setRecrutement(Recrutement recrutement) {
        this.recrutement = recrutement;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescriptionPoste() {
        return descriptionPoste;
    }

    public void setDescriptionPoste(String descriptionPoste) {
        this.descriptionPoste = descriptionPoste;
    }

    public String getMissions() {
        return missions;
    }

    public void setMissions(String missions) {
        this.missions = missions;
    }

    public String getCompetencesRequises() {
        return competencesRequises;
    }

    public void setCompetencesRequises(String competencesRequises) {
        this.competencesRequises = competencesRequises;
    }

    public String getNiveauEtude() {
        return niveauEtude;
    }

    public void setNiveauEtude(String niveauEtude) {
        this.niveauEtude = niveauEtude;
    }

    public Integer getExperienceMinAnnees() {
        return experienceMinAnnees;
    }

    public void setExperienceMinAnnees(Integer experienceMinAnnees) {
        this.experienceMinAnnees = experienceMinAnnees;
    }

    public BigDecimal getSalaireMin() {
        return salaireMin;
    }

    public void setSalaireMin(BigDecimal salaireMin) {
        this.salaireMin = salaireMin;
    }

    public BigDecimal getSalaireMax() {
        return salaireMax;
    }

    public void setSalaireMax(BigDecimal salaireMax) {
        this.salaireMax = salaireMax;
    }

    public OffsetDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(OffsetDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public StatutOffre getStatut() {
        return statut;
    }

    public void setStatut(StatutOffre statut) {
        this.statut = statut;
    }
}
