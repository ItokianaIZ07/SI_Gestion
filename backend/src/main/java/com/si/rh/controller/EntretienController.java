//Brandon

package com.si.rh.controller;

import com.si.rh.dto.*;
import com.si.rh.repository.UtilisateurRepository;
import com.si.rh.service.EntretienService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rh/entretiens")
@RequiredArgsConstructor
public class EntretienController {

    private final EntretienService service;
    private final UtilisateurRepository utilisateurRepo;

    @GetMapping("/candidatures")
    public List<CandidatureEntretienDTO> candidatures() {
        return service.getCandidaturesAvecEntretiens();
    }

    @GetMapping("/intervieweurs")
    public List<IntervieweurDTO> intervieweurs() {
        return service.getIntervieweurs();
    }

    @PostMapping
    public EntretienDTO creer(@RequestBody EntretienRequest req) {
        req.setId(null);
        return service.enregistrer(req);
    }

    @PutMapping("/{id}")
    public EntretienDTO modifier(@PathVariable Integer id, @RequestBody EntretienRequest req) {
        req.setId(id);
        return service.enregistrer(req);
    }

    @PostMapping("/{id}/convocation")
    public ResponseEntity<Void> convoquer(@PathVariable Integer id) {
        service.convoquer(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/candidatures/{id}/resultat")
    public ResponseEntity<Void> resultat(@PathVariable Integer id,
                                         @RequestBody Map<String, String> body,
                                         Authentication authentication) {
        Integer idDecideur = extraireIdUtilisateur(authentication);
        service.notifierResultat(id, body.get("decision"), idDecideur);
        return ResponseEntity.ok().build();
    }

    private Integer extraireIdUtilisateur(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            throw new IllegalStateException("Utilisateur non authentifié");
        String email = authentication.getName();
        return utilisateurRepo.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable : " + email))
            .getId();
    }
}