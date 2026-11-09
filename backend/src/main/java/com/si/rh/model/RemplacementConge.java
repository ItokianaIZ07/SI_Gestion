package com.si.rh.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 *
 * @Author Itokiana
 * */

@Entity
@Table(name = "remplacement_conge")
public class RemplacementConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relation 1:1 avec DemandeConge (id_demande_conge NOT NULL UNIQUE)
    @OneToOne(optional = false)
    @JoinColumn(name = "id_demande_conge", nullable = false, unique = true)
    private DemandeConge demandeConge;

    // Relation N:1 (id_employe_remplacant NOT NULL)
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_employe_remplacant", nullable = false)
    private Employe employeRemplacant;

    @Column(name = "date_proposition", nullable = false, insertable = false, updatable = false)
    private LocalDateTime dateProposition; // Géré par le DEFAULT CURRENT_TIMESTAMP de la BDD

    @Column(nullable = false, length = 20)
    private String decision = "EN_ATTENTE";

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    // Constructeurs
    public RemplacementConge() {}

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DemandeConge getDemandeConge() { return demandeConge; }
    public void setDemandeConge(DemandeConge demandeConge) { this.demandeConge = demandeConge; }

    public Employe getEmployeRemplacant() { return employeRemplacant; }
    public void setEmployeRemplacant(Employe employeRemplacant) { this.employeRemplacant = employeRemplacant; }

    public LocalDateTime getDateProposition() { return dateProposition; }
    public void setDateProposition(LocalDateTime dateProposition) { this.dateProposition = dateProposition; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public LocalDateTime getDateDecision() { return dateDecision; }
    public void setDateDecision(LocalDateTime dateDecision) { this.dateDecision = dateDecision; }
}
