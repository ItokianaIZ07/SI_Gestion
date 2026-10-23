//Brandon

package com.si.rh.controller;

import com.si.rh.dto.*;
import com.si.rh.repository.UtilisateurRepository;
import com.si.rh.service.ContratService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/rh/contrats")
@RequiredArgsConstructor
public class ContratController {

    private final ContratService service;
    private final UtilisateurRepository utilisateurRepo;

    @GetMapping("/a-embaucher")
    public List<CandidatAEmbaucherDTO> aEmbaucher() {
        return service.getCandidatsAEmbaucher();
    }

    @GetMapping
    public List<ContratDTO> contrats() {
        return service.getContrats();
    }

    @GetMapping("/{id}")
    public ContratDTO detail(@PathVariable Integer id) {
        return service.getContrat(id);
    }

    @PostMapping("/embaucher")
    public ContratDTO embaucher(@RequestBody EmbaucheRequest req,
                                Authentication authentication) {
        Integer idUser = extraireIdUtilisateur(authentication);
        return service.embaucher(req, idUser);
    }

    @PostMapping("/{id}/signer")
    public ContratDTO signer(@PathVariable Integer id,
                            @RequestBody SignatureRequest req) {
        return service.signer(id, req.getPartie(), req.getSignatureBase64());
    }

    private Integer extraireIdUtilisateur(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            throw new IllegalStateException("Utilisateur non authentifié");
        String email = authentication.getName();
        return utilisateurRepo.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable : " + email))
            .getId();
    }

    @GetMapping("/{id}/signature/{partie}")
    public ResponseEntity<byte[]> getSignature(@PathVariable Integer id,
                                            @PathVariable String partie) {
        byte[] image = service.getSignature(id, partie);
        if (image == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(image);
    }
}