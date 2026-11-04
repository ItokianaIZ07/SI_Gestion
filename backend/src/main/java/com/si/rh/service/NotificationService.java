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

import java.time.OffsetDateTime;
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

    @Transactional
    public void notifierChangementEtatBesoin(Besoin besoin, String etape, String decision) {
        var notification = new Notification();
        notification.setUtilisateur(besoin.getDemandeur());
        notification.setBesoin(besoin);
        notification.setCandidature(null);
        notification.setTypeNotification("CHANGEMENT_ETAT_BESOIN");
        notification.setTitre("Mise à jour de votre demande n°" + besoin.getId());
        notification.setMessage(
                "L'étape " + etape + " a "
                        + ("ACCEPTE".equals(decision) ? "accepté" : "refusé")
                        + " votre demande. Statut actuel : "
                        + besoin.getStatutGlobal().name().replace('_', ' ').toLowerCase()
                        + "."
        );
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifierEtapeSuivante(Besoin besoin, String etapeAcceptee) {
        String roleSuivant = switch (etapeAcceptee) {
            case "RH" -> "FINANCE";
            case "FINANCE" -> "DIRECTION";
            default -> null;
        };
        if (roleSuivant == null) {
            return;
        }

        var notifications = utilisateurRepository.findByRole_Code(roleSuivant).stream()
                .map(utilisateur -> {
                    var notification = new Notification();
                    notification.setUtilisateur(utilisateur);
                    notification.setBesoin(besoin);
                    notification.setCandidature(null);
                    notification.setTypeNotification("BESOIN_A_VALIDER");
                    notification.setTitre("Demande de besoin à valider");
                    notification.setMessage(
                            "La demande n°" + besoin.getId() + " a été acceptée par "
                                    + etapeAcceptee + " et attend votre décision."
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

    @Transactional
    public NotificationResponse marquerCommeLue(Integer id, String email) {
        var utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Utilisateur introuvable"
                ));
        var notification = notificationRepository
                .findByIdAndUtilisateur_Id(id, utilisateur.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification introuvable"
                ));

        if (notification.getDateLecture() == null) {
            notification.setDateLecture(OffsetDateTime.now());
        }
        return NotificationResponse.from(notificationRepository.save(notification));
    }
}
