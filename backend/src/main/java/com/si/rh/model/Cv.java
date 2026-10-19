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
@Table(name = "cv")
public class Cv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_candidat", nullable = false)
    private Candidat candidat;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType = "application/pdf";

    /** Colonne BYTEA (ne pas utiliser @Lob, qui mapperait vers un OID). */
    @Column(name = "bytes", nullable = false)
    private byte[] bytes;

    @CreationTimestamp
    @Column(name = "date_depot", nullable = false, updatable = false)
    private OffsetDateTime dateDepot;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_lecture", nullable = false, length = 20)
    private StatutLecture statutLecture = StatutLecture.NON_LU;

    // ----- Constructeurs -----

    public Cv() {
    }

    public Cv(Integer id, Candidat candidat, String details, String nomFichier, String mimeType, byte[] bytes, OffsetDateTime dateDepot, StatutLecture statutLecture) {
        this.id = id;
        this.candidat = candidat;
        this.details = details;
        this.nomFichier = nomFichier;
        this.mimeType = mimeType;
        this.bytes = bytes;
        this.dateDepot = dateDepot;
        this.statutLecture = statutLecture;
    }

    // ----- Getters / Setters -----

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Candidat getCandidat() {
        return candidat;
    }

    public void setCandidat(Candidat candidat) {
        this.candidat = candidat;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
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

    public OffsetDateTime getDateDepot() {
        return dateDepot;
    }

    public void setDateDepot(OffsetDateTime dateDepot) {
        this.dateDepot = dateDepot;
    }

    public StatutLecture getStatutLecture() {
        return statutLecture;
    }

    public void setStatutLecture(StatutLecture statutLecture) {
        this.statutLecture = statutLecture;
    }
}
