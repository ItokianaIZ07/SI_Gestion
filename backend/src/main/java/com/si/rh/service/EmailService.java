//Brandon

package com.si.rh.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

    /** Envoyé au moment de l'embauche : informe que le contrat est en préparation. */
    public void envoyerContratEnPreparation(String email, String prenom,
                                            String nomPoste, String reference) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Votre embauche est confirmée - " + reference);
            helper.setText(buildHtmlContratEnPreparation(prenom, nomPoste, reference), true);

            mailSender.send(message);
        } catch (Exception ex) {
            System.err.println("[MAIL] Échec envoi contrat en préparation : " + ex.getMessage());
        }
    }

    /** Envoyé après la signature RH : rappelle au candidat qu'il doit signer. */
    public void envoyerLienSignatureApresRH(String email, String prenom,
                                            String nomPoste, String reference,
                                            String lien) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Votre contrat est prêt à signer - " + reference);
            helper.setText(buildHtmlLienSignature(prenom, nomPoste, reference, lien), true);

            mailSender.send(message);
        } catch (Exception ex) {
            System.err.println("[MAIL] Échec envoi lien signature : " + ex.getMessage());
        }
    }

    /** Envoyé quand le contrat est signé par les deux parties. */
    public void envoyerConfirmationContratActif(String email, String prenom,
                                                String reference, String nomPoste) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Votre contrat " + reference + " est définitif");
            helper.setText(buildHtmlContratActif(prenom, reference, nomPoste), true);

            mailSender.send(message);
        } catch (Exception ex) {
            System.err.println("[MAIL] Échec envoi confirmation : " + ex.getMessage());
        }
    }

    /* ---------- Templates HTML ---------- */

    private String buildHtmlContratEnPreparation(String prenom, String nomPoste,
                                                  String reference) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; color: #292e2a; max-width: 600px; margin: 0 auto; padding: 20px;">
              <h2 style="color: #015f09; margin-bottom: 8px;">Bienvenue chez nous !</h2>
              <p style="color: #666; font-size: 12px; margin: 0 0 24px;">Référence : <strong>%s</strong></p>

              <p>Bonjour <strong>%s</strong>,</p>

              <p>Nous avons le plaisir de vous confirmer votre embauche pour le poste de
                 <strong>%s</strong>.</p>

              <p>Votre contrat est actuellement en cours de préparation par le service RH.</p>

              <p>Vous recevrez un <strong>second e-mail</strong> dès que le contrat sera prêt
                 à être signé, avec un lien sécurisé pour y accéder.</p>

              <p>Aucune action de votre part n'est nécessaire pour le moment.</p>

              <p style="margin-top: 24px;">Cordialement,<br>Service RH</p>
            </body>
            </html>
            """.formatted(reference, prenom, nomPoste);
    }

    private String buildHtmlLienSignature(String prenom, String nomPoste,
                                           String reference, String lien) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; color: #292e2a; max-width: 600px; margin: 0 auto; padding: 20px;">
              <h2 style="color: #015f09; margin-bottom: 8px;">Votre contrat est prêt à signer</h2>
              <p style="color: #666; font-size: 12px; margin: 0 0 24px;">Référence : <strong>%s</strong></p>

              <p>Bonjour <strong>%s</strong>,</p>

              <p>Le service RH a signé votre contrat pour le poste de <strong>%s</strong>.</p>
              <p>Il ne manque plus que votre signature pour que le contrat soit définitif.</p>

              <div style="margin: 28px 0; text-align: center;">
                <a href="%s"
                   style="display: inline-block; padding: 14px 28px; background: #015f09; color: #ffffff;
                          text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 15px;">
                  Signer mon contrat
                </a>
              </div>

              <p style="font-size: 12px; color: #888;">
                Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br>
                <a href="%s" style="color: #015f09; word-break: break-all;">%s</a>
              </p>

              <p style="font-size: 12px; color: #888; margin-top: 32px;">
                Ce lien est personnel et à usage unique. Il expire dans 7 jours.
              </p>

              <p style="margin-top: 24px;">Cordialement,<br>Service RH</p>
            </body>
            </html>
            """.formatted(reference, prenom, nomPoste, lien, lien, lien);
    }

    private String buildHtmlContratActif(String prenom, String reference, String nomPoste) {
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; color: #292e2a; max-width: 600px; margin: 0 auto; padding: 20px;">
              <h2 style="color: #24752b; margin-bottom: 8px;">✓ Votre contrat est définitif</h2>
              <p style="color: #666; font-size: 12px; margin: 0 0 24px;">Référence : <strong>%s</strong></p>

              <p>Bonjour <strong>%s</strong>,</p>

              <p>Nous avons le plaisir de vous confirmer que votre contrat pour le poste de
                 <strong>%s</strong> a été signé par les deux parties.</p>

              <p>Il est désormais <strong>définitif et actif</strong>.</p>

              <p>Le service RH reviendra vers vous prochainement pour les prochaines étapes
                 de votre intégration.</p>

              <p>Bienvenue dans l'équipe !</p>

              <p style="margin-top: 24px;">Cordialement,<br>Service RH</p>
            </body>
            </html>
            """.formatted(reference, prenom, nomPoste);
    }
}