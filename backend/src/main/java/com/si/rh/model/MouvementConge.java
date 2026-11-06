package com.si.rh.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 *
 * @Author Itokiana
 * */

@Entity
@Table(
        name = "mouvement_conge",
        indexes = {
                @Index(name = "idx_mouvement_employe", columnList = "id_employe"),
                @Index(name = "idx_mouvement_type", columnList = "id_type_conge"),
                @Index(name = "idx_mouvement_date", columnList = "date_mouvement"),
                @Index(name = "idx_mouvement_demande", columnList = "id_demande_conge")
        }
)
public class MouvementConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_type_conge", nullable = false)
    private TypeConge typeConge;

    @ManyToOne // Optionnel car pas de NOT NULL en BDD
    @JoinColumn(name = "id_demande_conge")
    private DemandeConge demandeConge;

    @ManyToOne // Optionnel
    @JoinColumn(name = "id_utilisateur")
    private Utilisateur utilisateur;

    @Column(name = "date_mouvement", nullable = false, insertable = false, updatable = false)
    private LocalDateTime dateMouvement;

    @Column(name = "type_mouvement", nullable = false, length = 30)
    private String typeMouvement;

    @Column(name = "nombre_jours", nullable = false)
    private double nombreJours;

    @Column(name = "periode_debut")
    private LocalDate periodeDebut;

    @Column(columnDefinition = "TEXT")
    private String motif;

    public MouvementConge() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employe getEmploye() { return employe; }
    public void setEmploye(Employe employe) { this.employe = employe; }

    public TypeConge getTypeConge() { return typeConge; }
    public void setTypeConge(TypeConge typeConge) { this.typeConge = typeConge; }

    public DemandeConge getDemandeConge() { return demandeConge; }
    public void setDemandeConge(DemandeConge demandeConge) { this.demandeConge = demandeConge; }

    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }

    public LocalDateTime getDateMouvement() { return dateMouvement; }
    public void setDateMouvement(LocalDateTime dateMouvement) { this.dateMouvement = dateMouvement; }

    public String getTypeMouvement() { return typeMouvement; }
    public void setTypeMouvement(String typeMouvement) { this.typeMouvement = typeMouvement; }

    public double getNombreJours() { return nombreJours; }
    public void setNombreJours(double nombreJours) { this.nombreJours = nombreJours; }

    public LocalDate getPeriodeDebut() { return periodeDebut; }
    public void setPeriodeDebut(LocalDate periodeDebut) { this.periodeDebut = periodeDebut; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
}

