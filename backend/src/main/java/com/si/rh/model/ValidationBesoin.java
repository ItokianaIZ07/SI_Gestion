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
@Table(name = "validation_besoin",
       uniqueConstraints = @UniqueConstraint(name = "uq_validation_besoin_etape",
                                             columnNames = {"id_besoin", "id_etat_validation"}))
public class ValidationBesoin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_besoin", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Besoin besoin;

    /** Null tant que la décision est EN_ATTENTE. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur_validateur")
    private Utilisateur validateur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_etat_validation", nullable = false)
    private EtatValidation etatValidation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_decision", nullable = false)
    private Decision decision;

    @Column(name = "date_decision")
    private OffsetDateTime dateDecision;

    @Column(columnDefinition = "TEXT")
    private String remarque;

    // ----- Constructeurs -----

    public ValidationBesoin() {
    }

    public ValidationBesoin(Integer id, Besoin besoin, Utilisateur validateur, EtatValidation etatValidation, Decision decision, OffsetDateTime dateDecision, String remarque) {
        this.id = id;
        this.besoin = besoin;
        this.validateur = validateur;
        this.etatValidation = etatValidation;
        this.decision = decision;
        this.dateDecision = dateDecision;
        this.remarque = remarque;
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

    public Utilisateur getValidateur() {
        return validateur;
    }

    public void setValidateur(Utilisateur validateur) {
        this.validateur = validateur;
    }

    public EtatValidation getEtatValidation() {
        return etatValidation;
    }

    public void setEtatValidation(EtatValidation etatValidation) {
        this.etatValidation = etatValidation;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public OffsetDateTime getDateDecision() {
        return dateDecision;
    }

    public void setDateDecision(OffsetDateTime dateDecision) {
        this.dateDecision = dateDecision;
    }

    public String getRemarque() {
        return remarque;
    }

    public void setRemarque(String remarque) {
        this.remarque = remarque;
    }
}
