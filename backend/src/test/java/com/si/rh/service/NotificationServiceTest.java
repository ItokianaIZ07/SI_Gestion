package com.si.rh.service;

import com.si.rh.model.Besoin;
import com.si.rh.model.Notification;
import com.si.rh.model.Utilisateur;
import com.si.rh.repository.NotificationRepository;
import com.si.rh.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, utilisateurRepository);
    }

    @Test
    void rhAcceptanceNotifiesFinanceUsers() {
        assertNextStageNotifications("RH", "FINANCE");
    }

    @Test
    void financeAcceptanceNotifiesDirectionUsers() {
        assertNextStageNotifications("FINANCE", "DIRECTION");
    }

    @Test
    void directionAcceptanceDoesNotNotifyAnotherRole() {
        service.notifierEtapeSuivante(besoin(), "DIRECTION");

        verifyNoInteractions(utilisateurRepository, notificationRepository);
    }

    private void assertNextStageNotifications(String etape, String roleAttendu) {
        Utilisateur destinataire = new Utilisateur();
        destinataire.setId(7);
        when(utilisateurRepository.findByRole_Code(roleAttendu)).thenReturn(List.of(destinataire));
        Besoin besoin = besoin();

        service.notifierEtapeSuivante(besoin, etape);

        ArgumentCaptor<Iterable<Notification>> notifications = ArgumentCaptor.forClass(Iterable.class);
        verify(notificationRepository).saveAll(notifications.capture());
        Notification notification = notifications.getValue().iterator().next();
        assertEquals(destinataire, notification.getUtilisateur());
        assertEquals(besoin, notification.getBesoin());
        assertEquals("BESOIN_A_VALIDER", notification.getTypeNotification());
    }

    private Besoin besoin() {
        Besoin besoin = new Besoin();
        besoin.setId(42);
        return besoin;
    }
}
