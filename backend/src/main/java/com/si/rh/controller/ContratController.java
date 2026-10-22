//Brandon

package com.si.rh.controller;

import com.si.rh.dto.CandidatAEmbaucherDTO;
import com.si.rh.dto.ContratDTO;
import com.si.rh.dto.EmbaucheRequest;
import com.si.rh.dto.SignatureRequest;
import com.si.rh.service.ContratService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rh/contrats")
@RequiredArgsConstructor
public class ContratController {

    private final ContratService service;

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
                                @AuthenticationPrincipal(expression = "id") Integer idUser) {
        return service.embaucher(req, idUser);
    }

    @PostMapping("/{id}/signer")
    public ContratDTO signer(@PathVariable Integer id,
                             @RequestBody SignatureRequest req) {
        return service.signer(id, req.getPartie());
    }
}