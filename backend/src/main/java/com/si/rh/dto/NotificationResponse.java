package com.si.rh.dto;

import com.si.rh.model.Notification;

import java.time.OffsetDateTime;

public record NotificationResponse(
        Integer id,
        Integer idUtilisateur,
        Integer idBesoin,
        Integer idCandidature,
        String typeNotification,
        String titre,
        String message,
        OffsetDateTime dateCreation,
        OffsetDateTime dateLecture
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUtilisateur().getId(),
                notification.getBesoin() == null ? null : notification.getBesoin().getId(),
                notification.getCandidature() == null ? null : notification.getCandidature().getId(),
                notification.getTypeNotification(),
                notification.getTitre(),
                notification.getMessage(),
                notification.getDateCreation(),
                notification.getDateLecture()
        );
    }
}
