// BY : Brandon
package com.si.rh.model;

import com.si.rh.model.enums.StatutHeuresSup;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "temps_travail_journalier",
       uniqueConstraints = @UniqueConstraint(name = "temps_travail_journalier_id_employe_date_travail_key",
                                             columnNames = {"id_employe", "date_travail"}))
public class TempsTravailJournalier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;

    @Column(name = "date_travail", nullable = false)
    private LocalDate dateTravail;

    @Column(name = "temps_prevu_minutes", nullable = false)
    private Integer tempsPrevuMinutes = 0;

    @Column(name = "temps_reel_minutes", nullable = false)
    private Integer tempsReelMinutes = 0;

    @Column(name = "retard_minutes", nullable = false)
    private Integer retardMinutes = 0;

    @Column(name = "heures_supplementaires_minutes", nullable = false)
    private Integer heuresSupplementairesMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_heures_sup", nullable = false, length = 10)
    private StatutHeuresSup statutHeuresSup = StatutHeuresSup.AUCUNE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur_validateur")
    private Utilisateur validateur;

    @Column(name = "date_validation")
    private OffsetDateTime dateValidation;

    // ----- Constructeurs -----

    public TempsTravailJournalier() {
    }

    public TempsTravailJournalier(Integer id, Employe employe, LocalDate dateTravail, Integer tempsPrevuMinutes, Integer tempsReelMinutes, Integer retardMinutes, Integer heuresSupplementairesMinutes, StatutHeuresSup statutHeuresSup, Utilisateur validateur, OffsetDateTime dateValidation) {
        this.id = id;
        this.employe = employe;
        this.dateTravail = dateTravail;
        this.tempsPrevuMinutes = tempsPrevuMinutes;
        this.tempsReelMinutes = tempsReelMinutes;
        this.retardMinutes = retardMinutes;
        this.heuresSupplementairesMinutes = heuresSupplementairesMinutes;
        this.statutHeuresSup = statutHeuresSup;
        this.validateur = validateur;
        this.dateValidation = dateValidation;
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

    public LocalDate getDateTravail() {
        return dateTravail;
    }

    public void setDateTravail(LocalDate dateTravail) {
        this.dateTravail = dateTravail;
    }

    public Integer getTempsPrevuMinutes() {
        return tempsPrevuMinutes;
    }

    public void setTempsPrevuMinutes(Integer tempsPrevuMinutes) {
        this.tempsPrevuMinutes = tempsPrevuMinutes;
    }

    public Integer getTempsReelMinutes() {
        return tempsReelMinutes;
    }

    public void setTempsReelMinutes(Integer tempsReelMinutes) {
        this.tempsReelMinutes = tempsReelMinutes;
    }

    public Integer getRetardMinutes() {
        return retardMinutes;
    }

    public void setRetardMinutes(Integer retardMinutes) {
        this.retardMinutes = retardMinutes;
    }

    public Integer getHeuresSupplementairesMinutes() {
        return heuresSupplementairesMinutes;
    }

    public void setHeuresSupplementairesMinutes(Integer heuresSupplementairesMinutes) {
        this.heuresSupplementairesMinutes = heuresSupplementairesMinutes;
    }

    public StatutHeuresSup getStatutHeuresSup() {
        return statutHeuresSup;
    }

    public void setStatutHeuresSup(StatutHeuresSup statutHeuresSup) {
        this.statutHeuresSup = statutHeuresSup;
    }

    public Utilisateur getValidateur() {
        return validateur;
    }

    public void setValidateur(Utilisateur validateur) {
        this.validateur = validateur;
    }

    public OffsetDateTime getDateValidation() {
        return dateValidation;
    }

    public void setDateValidation(OffsetDateTime dateValidation) {
        this.dateValidation = dateValidation;
    }
}