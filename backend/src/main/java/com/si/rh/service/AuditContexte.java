package com.si.rh.service;

import com.si.rh.repository.UtilisateurRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuditContexte {

    private final JdbcTemplate jdbc;
    private final UtilisateurRepository utilisateurRepository;

    public AuditContexte(JdbcTemplate jdbc, UtilisateurRepository utilisateurRepository) {
        this.jdbc = jdbc;
        this.utilisateurRepository = utilisateurRepository;
    }

    public void appliquer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return;
        }
        utilisateurRepository.findByEmail(auth.getName()).ifPresent(u -> definir("app.user_id", String.valueOf(u.getId())));

        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes requete) {
            String ip = requete.getRequest().getRemoteAddr();
            if (ip != null && ip.matches("[0-9a-fA-F:.]+")) {
                definir("app.client_ip", ip);
            }
        }
    }

    private void definir(String cle, String valeur) {
        jdbc.queryForObject("SELECT set_config(?, ?, true)", String.class, cle, valeur);
    }
}
