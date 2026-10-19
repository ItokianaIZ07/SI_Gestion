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
@Table(name = "contrat")
public class Contrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_type_contrat", nullable = false)
    private TypeContrat typeContrat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_statut_contrat", nullable = false)
    private StatutContract statutContrat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrat_precedent")
    private Contrat contratPrecedent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur_createur")
    private Utilisateur createur;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut = LocalDate.now();

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "periode_essai_jours")
    private Integer periodeEssaiJours;

    @Column(name = "date_signature")
    private LocalDate dateSignature;

    @Column(precision = 12, scale = 2)
    private BigDecimal salaire;

    /** nom_fichier, mime_type et contenu sont tous null ou tous renseignés. */
    @Column(name = "nom_fichier")
    private String nomFichier;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "contenu")
    private byte[] contenu;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    // ----- Constructeurs -----

    public Contrat() {
    }

    public Contrat(Integer id, String reference, Employe employe, TypeContrat typeContrat, StatutContract statutContrat, Contrat contratPrecedent, Utilisateur createur, LocalDate dateDebut, LocalDate dateFin, Integer periodeEssaiJours, LocalDate dateSignature, BigDecimal salaire, String nomFichier, String mimeType, byte[] contenu, OffsetDateTime dateCreation) {
        this.id = id;
        this.reference = reference;
        this.employe = employe;
        this.typeContrat = typeContrat;
        this.statutContrat = statutContrat;
        this.contratPrecedent = contratPrecedent;
        this.createur = createur;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.periodeEssaiJours = periodeEssaiJours;
        this.dateSignature = dateSignature;
        this.salaire = salaire;
        this.nomFichier = nomFichier;
        this.mimeType = mimeType;
        this.contenu = contenu;
        this.dateCreation = dateCreation;
    }

    // ----- Getters / Setters -----

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Employe getEmploye() {
        return employe;
    }

    public void setEmploye(Employe employe) {
        this.employe = employe;
    }

    public TypeContrat getTypeContrat() {
        return typeContrat;
    }

    public void setTypeContrat(TypeContrat typeContrat) {
        this.typeContrat = typeContrat;
    }

    public StatutContract getStatutContrat() {
        return statutContrat;
    }

    public void setStatutContrat(StatutContract statutContrat) {
        this.statutContrat = statutContrat;
    }

    public Contrat getContratPrecedent() {
        return contratPrecedent;
    }

    public void setContratPrecedent(Contrat contratPrecedent) {
        this.contratPrecedent = contratPrecedent;
    }

    public Utilisateur getCreateur() {
        return createur;
    }

    public void setCreateur(Utilisateur createur) {
        this.createur = createur;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public Integer getPeriodeEssaiJours() {
        return periodeEssaiJours;
    }

    public void setPeriodeEssaiJours(Integer periodeEssaiJours) {
        this.periodeEssaiJours = periodeEssaiJours;
    }

    public LocalDate getDateSignature() {
        return dateSignature;
    }

    public void setDateSignature(LocalDate dateSignature) {
        this.dateSignature = dateSignature;
    }

    public BigDecimal getSalaire() {
        return salaire;
    }

    public void setSalaire(BigDecimal salaire) {
        this.salaire = salaire;
    }

    public String getNomFichier() {
        return nomFichier;
    }

    public void setNomFichier(String nomFichier) {
        this.nomFichier = nomFichier;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public byte[] getContenu() {
        return contenu;
    }

    public void setContenu(byte[] contenu) {
        this.contenu = contenu;
    }

    public OffsetDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(OffsetDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}
