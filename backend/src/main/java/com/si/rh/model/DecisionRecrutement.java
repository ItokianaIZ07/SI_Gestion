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
@Table(name = "decision_recrutement")
public class DecisionRecrutement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_candidature", nullable = false, unique = true)
    private Candidature candidature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur_decideur")
    private Utilisateur decideur;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private ValeurDecision decision = ValeurDecision.EN_ATTENTE;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(name = "date_decision")
    private OffsetDateTime dateDecision;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    // ----- Constructeurs -----

    public DecisionRecrutement() {
    }

    public DecisionRecrutement(Integer id, Candidature candidature, Utilisateur decideur, ValeurDecision decision, String motif, OffsetDateTime dateDecision, OffsetDateTime dateCreation) {
        this.id = id;
        this.candidature = candidature;
        this.decideur = decideur;
        this.decision = decision;
        this.motif = motif;
        this.dateDecision = dateDecision;
        this.dateCreation = dateCreation;
    }

    // ----- Getters / Setters -----

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Candidature getCandidature() {
        return candidature;
    }

    public void setCandidature(Candidature candidature) {
        this.candidature = candidature;
    }

    public Utilisateur getDecideur() {
        return decideur;
    }

    public void setDecideur(Utilisateur decideur) {
        this.decideur = decideur;
    }

    public ValeurDecision getDecision() {
        return decision;
    }

    public void setDecision(ValeurDecision decision) {
        this.decision = decision;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public OffsetDateTime getDateDecision() {
        return dateDecision;
    }

    public void setDateDecision(OffsetDateTime dateDecision) {
        this.dateDecision = dateDecision;
    }

    public OffsetDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(OffsetDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}
