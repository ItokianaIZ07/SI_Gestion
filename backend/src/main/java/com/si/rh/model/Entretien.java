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
@Table(name = "entretien")
public class Entretien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_candidature", nullable = false)
    private Candidature candidature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur_intervieweur")
    private Utilisateur intervieweur;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_entretien", nullable = false, length = 20)
    private TypeEntretien typeEntretien;

    @Column(name = "date_entretien", nullable = false)
    private OffsetDateTime dateEntretien;

    @Column(length = 150)
    private String lieu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutEntretien statut = StatutEntretien.PLANIFIE;

    /** Note sur 20. */
    @Column(precision = 4, scale = 2)
    private BigDecimal note;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    // ----- Constructeurs -----

    public Entretien() {
    }

    public Entretien(Integer id, Candidature candidature, Utilisateur intervieweur, TypeEntretien typeEntretien, OffsetDateTime dateEntretien, String lieu, StatutEntretien statut, BigDecimal note, String commentaire) {
        this.id = id;
        this.candidature = candidature;
        this.intervieweur = intervieweur;
        this.typeEntretien = typeEntretien;
        this.dateEntretien = dateEntretien;
        this.lieu = lieu;
        this.statut = statut;
        this.note = note;
        this.commentaire = commentaire;
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

    public Utilisateur getIntervieweur() {
        return intervieweur;
    }

    public void setIntervieweur(Utilisateur intervieweur) {
        this.intervieweur = intervieweur;
    }

    public TypeEntretien getTypeEntretien() {
        return typeEntretien;
    }

    public void setTypeEntretien(TypeEntretien typeEntretien) {
        this.typeEntretien = typeEntretien;
    }

    public OffsetDateTime getDateEntretien() {
        return dateEntretien;
    }

    public void setDateEntretien(OffsetDateTime dateEntretien) {
        this.dateEntretien = dateEntretien;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public StatutEntretien getStatut() {
        return statut;
    }

    public void setStatut(StatutEntretien statut) {
        this.statut = statut;
    }

    public BigDecimal getNote() {
        return note;
    }

    public void setNote(BigDecimal note) {
        this.note = note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
}
