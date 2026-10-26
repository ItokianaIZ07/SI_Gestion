package com.si.rh.service;

import com.si.rh.model.Contrat;
import com.si.rh.model.SignatureToken;
import com.si.rh.repository.ContratRepository;
import com.si.rh.repository.SignatureTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class SignatureTokenService {

    private final SignatureTokenRepository tokenRepo;
    private final ContratRepository contratRepo;
    private final EmailService emailService;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final int EXPIRATION_JOURS = 7;

    /**
     * Appelé après la signature RH :
     * génère un token, stocke son hash et envoie le lien au candidat.
     */
    @Transactional
    public void genererEtEnvoyerLienApresRH(Integer idContrat, String emailCandidat,
                                             String nomCandidat, String nomPoste) {
        Contrat ct = contratRepo.findById(idContrat)
            .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable"));

        // 1. Token aléatoire cryptographiquement sûr
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String tokenClair = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        // 2. Stocker uniquement le HASH
        SignatureToken st = new SignatureToken();
        st.setIdContrat(idContrat);
        st.setTokenHash(sha256(tokenClair));
        st.setEmailDestinataire(emailCandidat);
        st.setDateExpiration(OffsetDateTime.now().plusDays(EXPIRATION_JOURS));
        tokenRepo.save(st);

        // 3. Envoyer le lien par mail
        String lien = "http://localhost:5173/si/client/signer-contrat/" + tokenClair;
        emailService.envoyerLienSignatureApresRH(
            emailCandidat, nomCandidat, nomPoste, ct.getReference(), lien);
    }

    /** Valide le token et retourne le contrat associé. */
    @Transactional(readOnly = true)
    public Contrat validerToken(String tokenClair) {
        String hash = sha256(tokenClair);
        SignatureToken st = tokenRepo.findByTokenHashAndUtiliseFalse(hash)
            .orElseThrow(() -> new IllegalArgumentException("Lien invalide ou déjà utilisé"));

        if (st.getDateExpiration().isBefore(OffsetDateTime.now())) {
            throw new IllegalStateException("Ce lien a expiré");
        }

        return contratRepo.findById(st.getIdContrat())
            .orElseThrow(() -> new IllegalArgumentException("Contrat introuvable"));
    }

    /** Marque le token comme utilisé. */
    @Transactional
    public void marquerUtilise(String tokenClair) {
        String hash = sha256(tokenClair);
        tokenRepo.findByTokenHashAndUtiliseFalse(hash).ifPresent(st -> {
            st.setUtilise(true);
            st.setDateUtilisation(OffsetDateTime.now());
            tokenRepo.save(st);
        });
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}