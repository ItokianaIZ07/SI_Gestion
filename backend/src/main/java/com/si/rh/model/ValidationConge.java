package com.si.rh.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 *
 * @Author Itokiana
 * */

@Entity
@Table(name = "validation_conge")
public class ValidationConge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_demande_conge",nullable = false)
    private DemandeConge demandeConge;

    @ManyToOne
    @JoinColumn(name = "id_utilisateur")
    private Utilisateur utilisateur;

    @Column(name = "ordre")
    private int ordre;

    @ManyToOne
    @JoinColumn(name = "id_decision", nullable = false)
    private Decision decision;

    @Column(name = "commentaire")
    private String commentaire;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    public ValidationConge(){}

    public ValidationConge(Integer id, DemandeConge demandeConge, Utilisateur utilisateur, int ordre, Decision decision, String commentaire, LocalDateTime dateValidation) {
        this.id = id;
        this.demandeConge = demandeConge;
        this.utilisateur = utilisateur;
        this.ordre = ordre;
        this.decision = decision;
        this.commentaire = commentaire;
        this.dateValidation = dateValidation;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public DemandeConge getDemandeConge() {
        return demandeConge;
    }

    public void setDemandeConge(DemandeConge demandeConge) {
        this.demandeConge = demandeConge;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public LocalDateTime getDateValidation() {
        return dateValidation;
    }

    public void setDateValidation(LocalDateTime dateValidation) {
        this.dateValidation = dateValidation;
    }
}
