// BY : Brandon
package com.si.rh.model;

import com.si.rh.model.enums.StatutAbsence;
import com.si.rh.model.enums.TypeAbsence;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "absence")
public class Absence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;

    // TODO : à activer quand l'entité DemandeConge sera créée (Bloc 7 - congés)
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "id_demande_conge")
    // private DemandeConge demandeConge;
    
    // Pour l'instant, on garde juste la colonne brute si besoin plus tard
    @Column(name = "id_demande_conge")
    private Long idDemandeConge;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_absence", nullable = false, length = 25)
    private TypeAbsence typeAbsence;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "jours_absence", nullable = false, precision = 6, scale = 2)
    private BigDecimal joursAbsence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutAbsence statut = StatutAbsence.PLANIFIEE;

    @Column(columnDefinition = "TEXT")
    private String remarque;

    // Getters / Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Employe getEmploye() { return employe; }
    public void setEmploye(Employe employe) { this.employe = employe; }
    public Long getIdDemandeConge() { return idDemandeConge; }
    public void setIdDemandeConge(Long idDemandeConge) { this.idDemandeConge = idDemandeConge; }
    public TypeAbsence getTypeAbsence() { return typeAbsence; }
    public void setTypeAbsence(TypeAbsence typeAbsence) { this.typeAbsence = typeAbsence; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    public BigDecimal getJoursAbsence() { return joursAbsence; }
    public void setJoursAbsence(BigDecimal joursAbsence) { this.joursAbsence = joursAbsence; }
    public StatutAbsence getStatut() { return statut; }
    public void setStatut(StatutAbsence statut) { this.statut = statut; }
    public String getRemarque() { return remarque; }
    public void setRemarque(String remarque) { this.remarque = remarque; }
}