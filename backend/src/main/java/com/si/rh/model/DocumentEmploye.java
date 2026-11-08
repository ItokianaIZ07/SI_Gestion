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
@Table(name = "document_employe")
public class DocumentEmploye {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_employe", nullable = false)
    private Employe employe;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_document", nullable = false, length = 30)
    private TypeDocument typeDocument;

    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "bytes", nullable = false)
    private byte[] bytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutDocument statut = StatutDocument.FOURNI;

    @CreationTimestamp
    @Column(name = "date_depot", nullable = false, updatable = false)
    private OffsetDateTime dateDepot;

    /** Fin de validité du document (null = n'expire pas). */
    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    /** Date à laquelle le document a été renouvelé (non null = plus d'alerte). */
    @Column(name = "date_renouvellement")
    private LocalDate dateRenouvellement;

    /** Nombre de jours AVANT l'expiration où l'alerte est envoyée. */
    @Column(name = "delai_alerte_jours", nullable = false)
    private Integer delaiAlerteJours = 30;

    // ----- Constructeurs -----

    public DocumentEmploye() {
    }

    public DocumentEmploye(Integer id, Employe employe, TypeDocument typeDocument, String nomFichier, String mimeType, byte[] bytes, StatutDocument statut, OffsetDateTime dateDepot) {
        this.id = id;
        this.employe = employe;
        this.typeDocument = typeDocument;
        this.nomFichier = nomFichier;
        this.mimeType = mimeType;
        this.bytes = bytes;
        this.statut = statut;
        this.dateDepot = dateDepot;
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

    public TypeDocument getTypeDocument() {
        return typeDocument;
    }

    public void setTypeDocument(TypeDocument typeDocument) {
        this.typeDocument = typeDocument;
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

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public StatutDocument getStatut() {
        return statut;
    }

    public void setStatut(StatutDocument statut) {
        this.statut = statut;
    }

    public OffsetDateTime getDateDepot() {
        return dateDepot;
    }

    public void setDateDepot(OffsetDateTime dateDepot) {
        this.dateDepot = dateDepot;
    }

    public LocalDate getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDate dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public LocalDate getDateRenouvellement() {
        return dateRenouvellement;
    }

    public void setDateRenouvellement(LocalDate dateRenouvellement) {
        this.dateRenouvellement = dateRenouvellement;
    }

    public Integer getDelaiAlerteJours() {
        return delaiAlerteJours;
    }

    public void setDelaiAlerteJours(Integer delaiAlerteJours) {
        this.delaiAlerteJours = delaiAlerteJours;
    }
}
