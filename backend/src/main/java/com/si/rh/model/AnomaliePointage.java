// BY : Brandon
package com.si.rh.model;

import com.si.rh.model.enums.StatutAnomalie;
import com.si.rh.model.enums.TypeAnomalie;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "anomalie_pointage",
       uniqueConstraints = @UniqueConstraint(name = "anomalie_pointage_id_employe_date_anomalie_type_anomalie_key",
                                             columnNames = {"id_employe", "date_anomalie", "type_anomalie"}))
public class AnomaliePointage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;

    @Column(name = "date_anomalie", nullable = false)
    private LocalDate dateAnomalie;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_anomalie", nullable = false, length = 50)
    private TypeAnomalie typeAnomalie;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutAnomalie statut = StatutAnomalie.A_TRAITER;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Column(name = "date_resolution")
    private OffsetDateTime dateResolution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur_responsable")
    private Utilisateur responsable;

    // ----- Constructeurs -----

    public AnomaliePointage() {
    }

    public AnomaliePointage(Integer id, Employe employe, LocalDate dateAnomalie, TypeAnomalie typeAnomalie, String description, StatutAnomalie statut, String justification, OffsetDateTime dateResolution, Utilisateur responsable) {
        this.id = id;
        this.employe = employe;
        this.dateAnomalie = dateAnomalie;
        this.typeAnomalie = typeAnomalie;
        this.description = description;
        this.statut = statut;
        this.justification = justification;
        this.dateResolution = dateResolution;
        this.responsable = responsable;
    }

    // ----- Getters / Setters -----

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Employe getEmploye() {
        return employe;
    }

    public void setEmploye(Employe employe) {
        this.employe = employe;
    }

    public LocalDate getDateAnomalie() {
        return dateAnomalie;
    }

    public void setDateAnomalie(LocalDate dateAnomalie) {
        this.dateAnomalie = dateAnomalie;
    }

    public TypeAnomalie getTypeAnomalie() {
        return typeAnomalie;
    }

    public void setTypeAnomalie(TypeAnomalie typeAnomalie) {
        this.typeAnomalie = typeAnomalie;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public StatutAnomalie getStatut() {
        return statut;
    }

    public void setStatut(StatutAnomalie statut) {
        this.statut = statut;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public OffsetDateTime getDateResolution() {
        return dateResolution;
    }

    public void setDateResolution(OffsetDateTime dateResolution) {
        this.dateResolution = dateResolution;
    }

    public Utilisateur getResponsable() {
        return responsable;
    }

    public void setResponsable(Utilisateur responsable) {
        this.responsable = responsable;
    }
}