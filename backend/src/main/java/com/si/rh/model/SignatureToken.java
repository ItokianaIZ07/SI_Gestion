package com.si.rh.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "signature_token")
public class SignatureToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_contrat", nullable = false)
    private Integer idContrat;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "email_destinataire", nullable = false, length = 150)
    private String emailDestinataire;

    @Column(name = "date_creation", nullable = false)
    private OffsetDateTime dateCreation = OffsetDateTime.now();

    @Column(name = "date_expiration", nullable = false)
    private OffsetDateTime dateExpiration;

    @Column(nullable = false)
    private Boolean utilise = false;

    @Column(name = "date_utilisation")
    private OffsetDateTime dateUtilisation;

    // getters / setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getIdContrat() { return idContrat; }
    public void setIdContrat(Integer idContrat) { this.idContrat = idContrat; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public String getEmailDestinataire() { return emailDestinataire; }
    public void setEmailDestinataire(String emailDestinataire) { this.emailDestinataire = emailDestinataire; }
    public OffsetDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }
    public OffsetDateTime getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(OffsetDateTime dateExpiration) { this.dateExpiration = dateExpiration; }
    public Boolean getUtilise() { return utilise; }
    public void setUtilise(Boolean utilise) { this.utilise = utilise; }
    public OffsetDateTime getDateUtilisation() { return dateUtilisation; }
    public void setDateUtilisation(OffsetDateTime dateUtilisation) { this.dateUtilisation = dateUtilisation; }
}