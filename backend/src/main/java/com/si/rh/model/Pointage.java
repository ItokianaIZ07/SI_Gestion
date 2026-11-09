// BY : Brandon
package com.si.rh.model;

import com.si.rh.model.enums.TypePointage;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "pointage",
       uniqueConstraints = @UniqueConstraint(name = "pointage_id_employe_date_heure_type_pointage_key",
                                             columnNames = {"id_employe", "date_heure", "type_pointage"}))
public class Pointage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;

    @Column(name = "date_heure", nullable = false)
    private OffsetDateTime dateHeure = OffsetDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "type_pointage", nullable = false, length = 20)
    private TypePointage typePointage;

    @Column(nullable = false, length = 30)
    private String source = "MANUEL";

    @Column(columnDefinition = "TEXT")
    private String remarque;

    // ----- Constructeurs -----

    public Pointage() {
    }

    public Pointage(Integer id, Employe employe, OffsetDateTime dateHeure, TypePointage typePointage, String source, String remarque) {
        this.id = id;
        this.employe = employe;
        this.dateHeure = dateHeure;
        this.typePointage = typePointage;
        this.source = source;
        this.remarque = remarque;
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

    public OffsetDateTime getDateHeure() {
        return dateHeure;
    }

    public void setDateHeure(OffsetDateTime dateHeure) {
        this.dateHeure = dateHeure;
    }

    public TypePointage getTypePointage() {
        return typePointage;
    }

    public void setTypePointage(TypePointage typePointage) {
        this.typePointage = typePointage;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRemarque() {
        return remarque;
    }

    public void setRemarque(String remarque) {
        this.remarque = remarque;
    }
}