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
@Table(name = "historique_contrat")
public class HistoriqueContrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrat", nullable = false)
    private Contrat contrat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ancien_statut")
    private StatutContract ancienStatut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_statut_contrat")
    private StatutContract StatutContract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur_auteur")
    private Utilisateur auteur;

    @Column(nullable = false, length = 100)
    private String evenement;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @CreationTimestamp
    @Column(name = "date_modification", nullable = false, updatable = false)
    private OffsetDateTime dateModification;

    @Column(name = "ancien_salaire", precision = 12, scale = 2)
    private BigDecimal ancienSalaire;

    @Column(name = "nouveau_salaire", precision = 12, scale = 2)
    private BigDecimal nouveauSalaire;

    @Column(name = "ancienne_date_fin")
    private LocalDate ancienneDateFin;

    @Column(name = "nouvelle_date_fin")
    private LocalDate nouvelleDateFin;

    //image signature
    @Column(name = "signature_image")
    private byte[] signatureImage;

    // ----- Constructeurs -----

    public HistoriqueContrat() {
    }

    public HistoriqueContrat(Integer id, Contrat contrat, StatutContract ancienStatut, StatutContract StatutContract, Utilisateur auteur, String evenement, String motif, OffsetDateTime dateModification, BigDecimal ancienSalaire, BigDecimal nouveauSalaire, LocalDate ancienneDateFin, LocalDate nouvelleDateFin) {
        this.id = id;
        this.contrat = contrat;
        this.ancienStatut = ancienStatut;
        this.StatutContract = StatutContract;
        this.auteur = auteur;
        this.evenement = evenement;
        this.motif = motif;
        this.dateModification = dateModification;
        this.ancienSalaire = ancienSalaire;
        this.nouveauSalaire = nouveauSalaire;
        this.ancienneDateFin = ancienneDateFin;
        this.nouvelleDateFin = nouvelleDateFin;
    }

    // ----- Getters / Setters -----

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Contrat getContrat() {
        return contrat;
    }

    public void setContrat(Contrat contrat) {
        this.contrat = contrat;
    }

    public StatutContract getAncienStatut() {
        return ancienStatut;
    }

    public void setAncienStatut(StatutContract ancienStatut) {
        this.ancienStatut = ancienStatut;
    }

    public StatutContract getStatutContract() {
        return StatutContract;
    }

    public void setStatutContract(StatutContract StatutContract) {
        this.StatutContract = StatutContract;
    }

    public Utilisateur getAuteur() {
        return auteur;
    }

    public void setAuteur(Utilisateur auteur) {
        this.auteur = auteur;
    }

    public String getEvenement() {
        return evenement;
    }

    public void setEvenement(String evenement) {
        this.evenement = evenement;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public OffsetDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(OffsetDateTime dateModification) {
        this.dateModification = dateModification;
    }

    public BigDecimal getAncienSalaire() {
        return ancienSalaire;
    }

    public void setAncienSalaire(BigDecimal ancienSalaire) {
        this.ancienSalaire = ancienSalaire;
    }

    public BigDecimal getNouveauSalaire() {
        return nouveauSalaire;
    }

    public void setNouveauSalaire(BigDecimal nouveauSalaire) {
        this.nouveauSalaire = nouveauSalaire;
    }

    public LocalDate getAncienneDateFin() {
        return ancienneDateFin;
    }

    public void setAncienneDateFin(LocalDate ancienneDateFin) {
        this.ancienneDateFin = ancienneDateFin;
    }

    public LocalDate getNouvelleDateFin() {
        return nouvelleDateFin;
    }

    public void setNouvelleDateFin(LocalDate nouvelleDateFin) {
        this.nouvelleDateFin = nouvelleDateFin;
    }


    public byte[] getSignatureImage() { return signatureImage; }
public void setSignatureImage(byte[] signatureImage) { this.signatureImage = signatureImage; }
}
