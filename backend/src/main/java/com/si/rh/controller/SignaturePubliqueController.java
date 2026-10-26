package com.si.rh.controller;

import com.si.rh.dto.ContratDTO;
import com.si.rh.model.Contrat;
import com.si.rh.service.ContratService;
import com.si.rh.service.SignatureTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;


import java.util.Map;

@RestController
@RequestMapping("/api/public/signature")
@RequiredArgsConstructor
public class SignaturePubliqueController {

    private final SignatureTokenService tokenService;
    private final ContratService contratService;

    @GetMapping("/{token}")
    public ContratDTO getContratParToken(@PathVariable String token) {
        Contrat c = tokenService.validerToken(token);
        return contratService.getContrat(c.getId());
    }

    @PostMapping("/{token}/signer")
    public ContratDTO signerParToken(@PathVariable String token,
                                    @RequestBody Map<String, String> body) {
        Contrat c = tokenService.validerToken(token);
        ContratDTO resultat = contratService.signerCandidat(c.getId(), body.get("signatureBase64"));
        // tokenService.marquerUtilise(token);
        return resultat;
    }

    // BY : Brandon
    @GetMapping("/{token}/signature/{partie}")
    public ResponseEntity<byte[]> getSignatureParToken(
            @PathVariable String token,
            @PathVariable String partie) {

        // Valide le token (expiration + non utilisé) puis récupère le contrat
        Contrat c = tokenService.validerToken(token);
        byte[] image = contratService.getSignature(c.getId(), partie);

        if (image == null || image.length == 0) {
            return ResponseEntity.noContent().build();   // 204 : pas de signature
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }
}