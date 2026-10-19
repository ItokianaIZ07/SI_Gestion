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
@Table(name = "besoin")
public class Besoin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_type_besoin", nullable = false)
    private TypeBesoin typeBesoin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_poste", nullable = false)
    private Poste poste;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_utilisateur_demandeur", nullable = false)
    private Utilisateur demandeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_employe_remplace")
    private Employe employeRemplace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_type_contrat_souhaite")
    private TypeContrat typeContratSouhaite;

    @Column(nullable = false)
    private Integer effectif = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priorite priorite = Priorite.NORMALE;

    @Column(name = "date_arrivee_souhaitee")
    private LocalDate dateArriveeSouhaitee;

    @CreationTimestamp
    @Column(name = "date_demande", nullable = false, updatable = false)
    private OffsetDateTime dateDemande;

    @Column(name = "lieu_travail", length = 150)
    private String lieuTravail;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motif;

    @Column(name = "experience_requise")
    private String experienceRequise;

    @Column(name = "niveau_etude", length = 100)
    private String niveauEtude;

    @Column(name = "competences_requises", columnDefinition = "TEXT")
    private String competencesRequises;

    @Column(name = "salaire_estime", precision = 12, scale = 2)
    private BigDecimal salaireEstime;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_global", nullable = false, length = 30)
    private StatutGlobalBesoin statutGlobal = StatutGlobalBesoin.EN_ATTENTE_RH;

    @UpdateTimestamp
    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    // ----- Constructeurs -----

    public Besoin() {
    }

    public Besoin(Integer id, TypeBesoin typeBesoin, Poste poste, Utilisateur demandeur, Employe employeRemplace, TypeContrat typeContratSouhaite, Integer effectif, Priorite priorite, LocalDate dateArriveeSouhaitee, OffsetDateTime dateDemande, String lieuTravail, String motif, String experienceRequise, String niveauEtude, String competencesRequises, BigDecimal salaireEstime, StatutGlobalBesoin statutGlobal, OffsetDateTime dateModification) {
        this.id = id;
        this.typeBesoin = typeBesoin;
        this.poste = poste;
        this.demandeur = demandeur;
        this.employeRemplace = employeRemplace;
        this.typeContratSouhaite = typeContratSouhaite;
        this.effectif = effectif;
        this.priorite = priorite;
        this.dateArriveeSouhaitee = dateArriveeSouhaitee;
        this.dateDemande = dateDemande;
        this.lieuTravail = lieuTravail;
        this.motif = motif;
        this.experienceRequise = experienceRequise;
        this.niveauEtude = niveauEtude;
        this.competencesRequises = competencesRequises;
        this.salaireEstime = salaireEstime;
        this.statutGlobal = statutGlobal;
        this.dateModification = dateModification;
    }

    // ----- Getters / Setters -----

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public TypeBesoin getTypeBesoin() {
        return typeBesoin;
    }

    public void setTypeBesoin(TypeBesoin typeBesoin) {
        this.typeBesoin = typeBesoin;
    }

    public Poste getPoste() {
        return poste;
    }

    public void setPoste(Poste poste) {
        this.poste = poste;
    }

    public Utilisateur getDemandeur() {
        return demandeur;
    }

    public void setDemandeur(Utilisateur demandeur) {
        this.demandeur = demandeur;
    }

    public Employe getEmployeRemplace() {
        return employeRemplace;
    }

    public void setEmployeRemplace(Employe employeRemplace) {
        this.employeRemplace = employeRemplace;
    }

    public TypeContrat getTypeContratSouhaite() {
        return typeContratSouhaite;
    }

    public void setTypeContratSouhaite(TypeContrat typeContratSouhaite) {
        this.typeContratSouhaite = typeContratSouhaite;
    }

    public Integer getEffectif() {
        return effectif;
    }

    public void setEffectif(Integer effectif) {
        this.effectif = effectif;
    }

    public Priorite getPriorite() {
        return priorite;
    }

    public void setPriorite(Priorite priorite) {
        this.priorite = priorite;
    }

    public LocalDate getDateArriveeSouhaitee() {
        return dateArriveeSouhaitee;
    }

    public void setDateArriveeSouhaitee(LocalDate dateArriveeSouhaitee) {
        this.dateArriveeSouhaitee = dateArriveeSouhaitee;
    }

    public OffsetDateTime getDateDemande() {
        return dateDemande;
    }

    public void setDateDemande(OffsetDateTime dateDemande) {
        this.dateDemande = dateDemande;
    }

    public String getLieuTravail() {
        return lieuTravail;
    }

    public void setLieuTravail(String lieuTravail) {
        this.lieuTravail = lieuTravail;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public String getExperienceRequise() {
        return experienceRequise;
    }

    public void setExperienceRequise(String experienceRequise) {
        this.experienceRequise = experienceRequise;
    }

    public String getNiveauEtude() {
        return niveauEtude;
    }

    public void setNiveauEtude(String niveauEtude) {
        this.niveauEtude = niveauEtude;
    }

    public String getCompetencesRequises() {
        return competencesRequises;
    }

    public void setCompetencesRequises(String competencesRequises) {
        this.competencesRequises = competencesRequises;
    }

    public BigDecimal getSalaireEstime() {
        return salaireEstime;
    }

    public void setSalaireEstime(BigDecimal salaireEstime) {
        this.salaireEstime = salaireEstime;
    }

    public StatutGlobalBesoin getStatutGlobal() {
        return statutGlobal;
    }

    public void setStatutGlobal(StatutGlobalBesoin statutGlobal) {
        this.statutGlobal = statutGlobal;
    }

    public OffsetDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(OffsetDateTime dateModification) {
        this.dateModification = dateModification;
    }
}
