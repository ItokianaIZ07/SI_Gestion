package com.si.rh.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AcquisitionCongeSchedulerTest {

    @Test
    void appelleLaProcedureAvecLePremierJourDuMoisAlaDateDeMadagascar() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        CallableStatement statement = mock(CallableStatement.class);
        when(connection.prepareCall("CALL sp_acquerir_conges_mensuels(?)"))
                .thenReturn(statement);
        doAnswer(invocation -> {
            ConnectionCallback<?> callback = invocation.getArgument(0);
            return callback.doInConnection(connection);
        }).when(jdbcTemplate).execute(any(ConnectionCallback.class));

        AcquisitionCongeScheduler scheduler = new AcquisitionCongeScheduler(jdbcTemplate);

        scheduler.acquerirCongesMensuels();

        LocalDate premierJour = LocalDate.now(ZoneId.of("Indian/Antananarivo"))
                .withDayOfMonth(1);
        verify(statement).setObject(1, premierJour, java.sql.Types.DATE);
        verify(statement).execute();
    }
}
