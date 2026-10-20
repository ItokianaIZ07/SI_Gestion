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
@Table(name = "tache_onboarding")
public class TacheOnboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_onboarding", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Onboarding onboarding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_modele")
    private ModeleTacheOnboarding modele;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur_responsable")
    private Utilisateur responsable;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategorieOnboarding categorie = CategorieOnboarding.AUTRE;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutTache statut = StatutTache.A_FAIRE;

    /** Obligatoire si statut = FAIT, null sinon. */
    @Column(name = "date_realisation")
    private OffsetDateTime dateRealisation;

    @Column(columnDefinition = "TEXT")
    private String remarque;

    // ----- Constructeurs -----

    public TacheOnboarding() {
    }

    public TacheOnboarding(Integer id, Onboarding onboarding, ModeleTacheOnboarding modele, Utilisateur responsable, String libelle, CategorieOnboarding categorie, LocalDate dateEcheance, StatutTache statut, OffsetDateTime dateRealisation, String remarque) {
        this.id = id;
        this.onboarding = onboarding;
        this.modele = modele;
        this.responsable = responsable;
        this.libelle = libelle;
        this.categorie = categorie;
        this.dateEcheance = dateEcheance;
        this.statut = statut;
        this.dateRealisation = dateRealisation;
        this.remarque = remarque;
    }

    // ----- Getters / Setters -----

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Onboarding getOnboarding() {
        return onboarding;
    }

    public void setOnboarding(Onboarding onboarding) {
        this.onboarding = onboarding;
    }

    public ModeleTacheOnboarding getModele() {
        return modele;
    }

    public void setModele(ModeleTacheOnboarding modele) {
        this.modele = modele;
    }

    public Utilisateur getResponsable() {
        return responsable;
    }

    public void setResponsable(Utilisateur responsable) {
        this.responsable = responsable;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public CategorieOnboarding getCategorie() {
        return categorie;
    }

    public void setCategorie(CategorieOnboarding categorie) {
        this.categorie = categorie;
    }

    public LocalDate getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(LocalDate dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public StatutTache getStatut() {
        return statut;
    }

    public void setStatut(StatutTache statut) {
        this.statut = statut;
    }

    public OffsetDateTime getDateRealisation() {
        return dateRealisation;
    }

    public void setDateRealisation(OffsetDateTime dateRealisation) {
        this.dateRealisation = dateRealisation;
    }

    public String getRemarque() {
        return remarque;
    }

    public void setRemarque(String remarque) {
        this.remarque = remarque;
    }
}
