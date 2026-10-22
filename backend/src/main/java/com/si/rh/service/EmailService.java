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
                                   LocalDateTime date, String type, String lieu) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setTo(email);
        m.setSubject("Convocation à un entretien");
        m.setText("""
            Bonjour %s,

            Votre entretien (%s) est prévu le %s.
            Lieu : %s.

            Cordialement,
            Service RH
            """.formatted(prenom, type, date.format(FMT), lieu));
        mailSender.send(m);
    }

    public void envoyerResultat(String email, String prenom, String decision) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setTo(email);
        m.setSubject("Résultat de votre candidature");
        m.setText(("RETENU".equals(decision)
            ? "Bonjour %s,\n\nNous avons le plaisir de vous informer que votre candidature est retenue.\n\nCordialement,\nService RH"
            : "Bonjour %s,\n\nNous vous remercions pour votre candidature. Après étude, nous ne pouvons pas y donner suite.\n\nCordialement,\nService RH")
            .formatted(prenom));
        mailSender.send(m);
    }

    public void envoyerContratASigner(String email, String prenom, String reference) {
        SimpleMailMessage m = new SimpleMailMessage();
        m.setTo(email);
        m.setSubject("Votre contrat est prêt à signer");
        m.setText("""
            Bonjour %s,

            Votre contrat %s est disponible. Merci de vous connecter pour le signer.

            Cordialement,
            Service RH
            """.formatted(prenom, reference));
        mailSender.send(m);
    }
}