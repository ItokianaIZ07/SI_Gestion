package com.si.rh.dto.audit;

import java.time.OffsetDateTime;
import java.util.Map;

public record AuditDTO(Long id, Integer idUtilisateur, String utilisateurNom, String action, String table,
        String identifiant, Map<String, Object> anciennesValeurs, Map<String, Object> nouvellesValeurs,
        String adresseIp, OffsetDateTime dateAction) {
}
