//Brandon

package com.si.rh.service;


import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    public void envoyerConvocation(String email, String prenom,
                                LocalDateTime date, String type, String lieu,
                                String nomPoste) {
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setTo(email);
            m.setSubject("Convocation à un entretien - " + nomPoste);
            m.setText("""
                Bonjour %s,

                Nous avons le plaisir de vous convoquer à un entretien pour le poste de %s.

                Type d'entretien : %s
                Date : %s
                Lieu : %s

                Cordialement,
                Service RH
                """.formatted(prenom, nomPoste, type, date.format(FMT), lieu));
            mailSender.send(m);
        } catch (Exception ex) {
            System.err.println("[MAIL] Échec convocation : " + ex.getMessage());
        }
    }

    public void envoyerResultat(String email, String prenom, String nomPoste,
                                String decision) {
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setTo(email);
            m.setSubject("Résultat de votre candidature - " + nomPoste);
            m.setText(("RETENU".equals(decision)
                ? "Bonjour %s,\n\nNous avons le plaisir de vous informer que votre candidature pour le poste de %s est retenue.\n\nCordialement,\nService RH"
                : "Bonjour %s,\n\nNous vous remercions pour votre candidature pour le poste de %s. Après étude, nous ne pouvons pas y donner suite.\n\nCordialement,\nService RH")
                .formatted(prenom, nomPoste));
            mailSender.send(m);
        } catch (Exception ex) {
            System.err.println("[MAIL] Échec résultat : " + ex.getMessage());
        }
    }

    public void envoyerContratASigner(String email, String prenom,
                                    String nomPoste, String reference) {
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setTo(email);
            m.setSubject("Votre contrat est prêt à signer");
            m.setText("""
                Bonjour %s,

                Nous avons le plaisir de vous informer que votre contrat pour le poste de %s
                est désormais disponible.

                Référence du contrat : %s

                Merci de vous connecter à votre espace pour le consulter et le signer.

                Cordialement,
                Service RH
                """.formatted(prenom, nomPoste, reference));
            mailSender.send(m);
        } catch (Exception ex) {
            System.err.println("[MAIL] Échec envoi contrat : " + ex.getMessage());
        }
    }
}

