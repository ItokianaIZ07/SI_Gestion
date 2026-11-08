package com.si.rh.service;

import com.si.rh.dto.document.DocumentAlerteDTO;
import com.si.rh.dto.document.DocumentAlerteLigne;
import com.si.rh.repository.DocumentEmployeRepository;
import com.si.rh.utils.Fuseau;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class AlerteDocumentService {

    private static final Logger LOG = LoggerFactory.getLogger(AlerteDocumentService.class);
    private static final int FREQUENCE_JOURS = 7;

    private final JdbcTemplate jdbc;
    private final DocumentEmployeRepository documentRepository;

    public AlerteDocumentService(JdbcTemplate jdbc, DocumentEmployeRepository documentRepository) {
        this.jdbc = jdbc;
        this.documentRepository = documentRepository;
    }

    @Scheduled(cron = "0 0 7 * * *", zone = "Indian/Antananarivo")
    public void generationQuotidienne() {
        LOG.info("Alertes de documents générées : {}", generer());
    }

    public int generer() {
        Integer nb = jdbc.queryForObject("SELECT generer_alertes_documents(?)", Integer.class, FREQUENCE_JOURS);
        return nb == null ? 0 : nb;
    }

    @Transactional(readOnly = true)
    public List<DocumentAlerteDTO> aRenouveler() {
        LocalDate aujourdhui = Fuseau.aujourdhui();
        return documentRepository.findCandidatsAlerte().stream()
                .map(l -> versDto(l, aujourdhui))
                .filter(a -> DocumentAdministratifService.EXPIRE.equals(a.etatValidite())
                        || DocumentAdministratifService.BIENTOT_EXPIRE.equals(a.etatValidite()))
                .toList();
    }

    private static DocumentAlerteDTO versDto(DocumentAlerteLigne l, LocalDate aujourdhui) {
        return new DocumentAlerteDTO(l.idDocument(), l.idEmploye(), l.matricule(), l.prenom() + " " + l.nom(),
                l.typeDocument(), l.dateExpiration(), DocumentAdministratifService.joursRestants(l.dateExpiration(), aujourdhui),
                DocumentAdministratifService.etatValidite(l.dateExpiration(), null, l.delaiAlerteJours(), aujourdhui));
    }
}
