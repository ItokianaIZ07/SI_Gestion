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
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_besoin")
    private Besoin besoin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_candidature")
    private Candidature candidature;

    @Column(name = "type_notification", nullable = false, length = 50)
    private String typeNotification = "INFORMATION";

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_lecture")
    private OffsetDateTime dateLecture;

    // ----- Constructeurs -----

    public Notification() {
    }

    public Notification(Integer id, Utilisateur utilisateur, Besoin besoin, Candidature candidature, String typeNotification, String titre, String message, OffsetDateTime dateCreation, OffsetDateTime dateLecture) {
        this.id = id;
        this.utilisateur = utilisateur;
        this.besoin = besoin;
        this.candidature = candidature;
        this.typeNotification = typeNotification;
        this.titre = titre;
        this.message = message;
        this.dateCreation = dateCreation;
        this.dateLecture = dateLecture;
    }

    // ----- Getters / Setters -----

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Besoin getBesoin() {
        return besoin;
    }

    public void setBesoin(Besoin besoin) {
        this.besoin = besoin;
    }

    public Candidature getCandidature() {
        return candidature;
    }

    public void setCandidature(Candidature candidature) {
        this.candidature = candidature;
    }

    public String getTypeNotification() {
        return typeNotification;
    }

    public void setTypeNotification(String typeNotification) {
        this.typeNotification = typeNotification;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(OffsetDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public OffsetDateTime getDateLecture() {
        return dateLecture;
    }

    public void setDateLecture(OffsetDateTime dateLecture) {
        this.dateLecture = dateLecture;
    }
}
