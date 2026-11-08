package com.si.rh.model;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Immutable
@Table(name = "journal_audit")
public class JournalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_utilisateur")
    private Integer idUtilisateur;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "table_concernee", nullable = false, length = 100)
    private String tableConcernee;

    @Column(name = "identifiant_enregistrement", length = 100)
    private String identifiantEnregistrement;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "anciennes_valeurs", columnDefinition = "jsonb")
    private Map<String, Object> anciennesValeurs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nouvelles_valeurs", columnDefinition = "jsonb")
    private Map<String, Object> nouvellesValeurs;

    @ColumnTransformer(read = "host(adresse_ip)")
    @Column(name = "adresse_ip", insertable = false, updatable = false)
    private String adresseIp;

    @Column(name = "date_action", nullable = false)
    private OffsetDateTime dateAction;

    public JournalAudit() {
    }

    public Long getId() {
        return id;
    }

    public Integer getIdUtilisateur() {
        return idUtilisateur;
    }

    public String getAction() {
        return action;
    }

    public String getTableConcernee() {
        return tableConcernee;
    }

    public String getIdentifiantEnregistrement() {
        return identifiantEnregistrement;
    }

    public Map<String, Object> getAnciennesValeurs() {
        return anciennesValeurs;
    }

    public Map<String, Object> getNouvellesValeurs() {
        return nouvellesValeurs;
    }

    public String getAdresseIp() {
        return adresseIp;
    }

    public OffsetDateTime getDateAction() {
        return dateAction;
    }
}
