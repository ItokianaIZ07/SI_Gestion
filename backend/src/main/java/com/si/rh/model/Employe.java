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

/**
 * La clé étrangère composite (id_poste, id_departement) -> poste(id, id_departement)
 * est garantie par la base : on mappe simplement les deux relations séparément.
 * Le service doit toujours affecter un poste appartenant au département choisi.
 */
@Entity
@Table(name = "employe")
public class Employe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 30)
    private String matricule;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 30)
    private String telephone;

    @Column(name = "numero_cin", unique = true, length = 30)
    private String numeroCin;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_candidature", unique = true)
    private Candidature candidature;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", unique = true)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_departement", nullable = false)
    private Departement departement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_poste", nullable = false)
    private Poste poste;

    @Column(name = "date_embauche", nullable = false)
    private LocalDate dateEmbauche = LocalDate.now();

    @Column(name = "date_depart")
    private LocalDate dateDepart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutEmploye statut = StatutEmploye.ACTIF;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    // ----- Constructeurs -----

    public Employe() {
    }

    public Employe(Integer id, String matricule, String nom, String prenom, String email, String telephone, String numeroCin, LocalDate dateNaissance, String adresse, Candidature candidature, Utilisateur utilisateur, Departement departement, Poste poste, LocalDate dateEmbauche, LocalDate dateDepart, StatutEmploye statut, OffsetDateTime dateCreation) {
        this.id = id;
        this.matricule = matricule;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.numeroCin = numeroCin;
        this.dateNaissance = dateNaissance;
        this.adresse = adresse;
        this.candidature = candidature;
        this.utilisateur = utilisateur;
        this.departement = departement;
        this.poste = poste;
        this.dateEmbauche = dateEmbauche;
        this.dateDepart = dateDepart;
        this.statut = statut;
        this.dateCreation = dateCreation;
    }

    // ----- Getters / Setters -----

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getNumeroCin() {
        return numeroCin;
    }

    public void setNumeroCin(String numeroCin) {
        this.numeroCin = numeroCin;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public Candidature getCandidature() {
        return candidature;
    }

    public void setCandidature(Candidature candidature) {
        this.candidature = candidature;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Departement getDepartement() {
        return departement;
    }

    public void setDepartement(Departement departement) {
        this.departement = departement;
    }

    public Poste getPoste() {
        return poste;
    }

    public void setPoste(Poste poste) {
        this.poste = poste;
    }

    public LocalDate getDateEmbauche() {
        return dateEmbauche;
    }

    public void setDateEmbauche(LocalDate dateEmbauche) {
        this.dateEmbauche = dateEmbauche;
    }

    public LocalDate getDateDepart() {
        return dateDepart;
    }

    public void setDateDepart(LocalDate dateDepart) {
        this.dateDepart = dateDepart;
    }

    public StatutEmploye getStatut() {
        return statut;
    }

    public void setStatut(StatutEmploye statut) {
        this.statut = statut;
    }

    public OffsetDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(OffsetDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}
