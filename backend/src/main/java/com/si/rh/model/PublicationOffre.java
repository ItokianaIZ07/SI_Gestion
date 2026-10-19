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
@Table(name = "publication_offre")
public class PublicationOffre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_offre", nullable = false)
    private OffreEmploi offre;

    @Column(nullable = false, length = 100)
    private String plateforme;

    @Column(name = "url_publication", columnDefinition = "TEXT")
    private String urlPublication;

    @Column(name = "date_publication", nullable = false)
    private LocalDate datePublication = LocalDate.now();

    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutPublication statut = StatutPublication.PUBLIEE;

    // ----- Constructeurs -----

    public PublicationOffre() {
    }

    public PublicationOffre(Integer id, OffreEmploi offre, String plateforme, String urlPublication, LocalDate datePublication, LocalDate dateExpiration, StatutPublication statut) {
        this.id = id;
        this.offre = offre;
        this.plateforme = plateforme;
        this.urlPublication = urlPublication;
        this.datePublication = datePublication;
        this.dateExpiration = dateExpiration;
        this.statut = statut;
    }

    // ----- Getters / Setters -----

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public OffreEmploi getOffre() {
        return offre;
    }

    public void setOffre(OffreEmploi offre) {
        this.offre = offre;
    }

    public String getPlateforme() {
        return plateforme;
    }

    public void setPlateforme(String plateforme) {
        this.plateforme = plateforme;
    }

    public String getUrlPublication() {
        return urlPublication;
    }

    public void setUrlPublication(String urlPublication) {
        this.urlPublication = urlPublication;
    }

    public LocalDate getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(LocalDate datePublication) {
        this.datePublication = datePublication;
    }

    public LocalDate getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDate dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public StatutPublication getStatut() {
        return statut;
    }

    public void setStatut(StatutPublication statut) {
        this.statut = statut;
    }
}
