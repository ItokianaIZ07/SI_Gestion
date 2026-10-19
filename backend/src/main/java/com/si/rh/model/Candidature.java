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

/**
 * La FK composite (id_cv, id_candidat) -> cv(id, id_candidat) est garantie par la base :
 * le service doit vérifier que le CV appartient bien au candidat.
 * Le trigger SQL vérifie aussi que la publication correspond au recrutement.
 */
@Entity
@Table(name = "candidature",
       uniqueConstraints = @UniqueConstraint(name = "uq_candidature_candidat_recrutement",
                                             columnNames = {"id_candidat", "id_recrutement"}))
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_candidat", nullable = false)
    private Candidat candidat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_recrutement", nullable = false)
    private Recrutement recrutement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cv", nullable = false)
    private Cv cv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_publication")
    private PublicationOffre publication;

    @CreationTimestamp
    @Column(name = "date_candidature", nullable = false, updatable = false)
    private OffsetDateTime dateCandidature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutCandidature statut = StatutCandidature.EN_ATTENTE;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(columnDefinition = "TEXT")
    private String remarque;

    // ----- Constructeurs -----

    public Candidature() {
    }

    public Candidature(Integer id, Candidat candidat, Recrutement recrutement, Cv cv, PublicationOffre publication, OffsetDateTime dateCandidature, StatutCandidature statut, BigDecimal score, String remarque) {
        this.id = id;
        this.candidat = candidat;
        this.recrutement = recrutement;
        this.cv = cv;
        this.publication = publication;
        this.dateCandidature = dateCandidature;
        this.statut = statut;
        this.score = score;
        this.remarque = remarque;
    }

    // ----- Getters / Setters -----

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Candidat getCandidat() {
        return candidat;
    }

    public void setCandidat(Candidat candidat) {
        this.candidat = candidat;
    }

    public Recrutement getRecrutement() {
        return recrutement;
    }

    public void setRecrutement(Recrutement recrutement) {
        this.recrutement = recrutement;
    }

    public Cv getCv() {
        return cv;
    }

    public void setCv(Cv cv) {
        this.cv = cv;
    }

    public PublicationOffre getPublication() {
        return publication;
    }

    public void setPublication(PublicationOffre publication) {
        this.publication = publication;
    }

    public OffsetDateTime getDateCandidature() {
        return dateCandidature;
    }

    public void setDateCandidature(OffsetDateTime dateCandidature) {
        this.dateCandidature = dateCandidature;
    }

    public StatutCandidature getStatut() {
        return statut;
    }

    public void setStatut(StatutCandidature statut) {
        this.statut = statut;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getRemarque() {
        return remarque;
    }

    public void setRemarque(String remarque) {
        this.remarque = remarque;
    }
}
