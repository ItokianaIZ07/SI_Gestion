package com.si.rh.service;

import com.si.rh.dto.NotificationResponse;
import com.si.rh.model.Besoin;
import com.si.rh.model.Notification;
import com.si.rh.repository.NotificationRepository;
import com.si.rh.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class NotificationService {

    private static final String ROLE_RH = "RH";

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UtilisateurRepository utilisateurRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Transactional
    public void notifierNouveauBesoin(Besoin besoin) {
        var utilisateursRh = utilisateurRepository.findByRole_Code(ROLE_RH);
        var notifications = utilisateursRh.stream()
                .map(utilisateur -> {
                    var notification = new Notification();
                    notification.setUtilisateur(utilisateur);
                    notification.setBesoin(besoin);
                    notification.setCandidature(null);
                    notification.setTypeNotification("NOUVEAU_BESOIN");
                    notification.setTitre("Nouveau besoin à traiter");
                    notification.setMessage(
                            "Une nouvelle demande de besoin (n°" + besoin.getId()
                                    + ") est en attente de traitement."
                    );
                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getPourUtilisateur(String email) {
        var utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Utilisateur introuvable"
                ));

        return notificationRepository
                .findByUtilisateur_IdOrderByDateCreationDesc(utilisateur.getId())
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
