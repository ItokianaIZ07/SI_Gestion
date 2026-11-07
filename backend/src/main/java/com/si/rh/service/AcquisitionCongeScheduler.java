package com.si.rh.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class AcquisitionCongeScheduler {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");
    private static final Logger logger =
            LoggerFactory.getLogger(AcquisitionCongeScheduler.class);

    private final JdbcTemplate jdbcTemplate;

    public AcquisitionCongeScheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "0 0 0 1 * *", zone = "Indian/Antananarivo")
    @Transactional
    public void acquerirCongesMensuels() {
        logger.info("Démarrage de l'acquisition mensuelle des congés.");
        LocalDate periode = LocalDate.now(BUSINESS_ZONE).withDayOfMonth(1);
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (CallableStatement statement =
                         connection.prepareCall("CALL sp_acquerir_conges_mensuels(?)")) {
                statement.setObject(1, periode, Types.DATE);
                statement.execute();
            }
            return null;
        });
        logger.info("Acquisition mensuelle des congés terminée.");
    }
}
