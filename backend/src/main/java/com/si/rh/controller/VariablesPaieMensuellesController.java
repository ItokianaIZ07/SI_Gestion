package com.si.rh.controller;

import com.si.rh.dto.rh.variablesPaie.DepartementPaieOption;
import com.si.rh.dto.rh.variablesPaie.VariablesPaieRecapitulatif;
import com.si.rh.service.VariablesPaieMensuellesService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rh/variables-paie")
public class VariablesPaieMensuellesController {

    private static final MediaType CSV_UTF8 = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final VariablesPaieMensuellesService variablesPaieService;

    public VariablesPaieMensuellesController(VariablesPaieMensuellesService variablesPaieService) {
        this.variablesPaieService = variablesPaieService;
    }

    @GetMapping("/departements")
    public List<DepartementPaieOption> departements() {
        return variablesPaieService.getDepartements();
    }

    @GetMapping
    public VariablesPaieRecapitulatif recapitulatif(
            @RequestParam Integer annee,
            @RequestParam Integer mois,
            @RequestParam(required = false) Integer idDepartement) {
        return variablesPaieService.getRecapitulatif(annee, mois, idDepartement);
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exporterCsv(
            @RequestParam Integer annee,
            @RequestParam Integer mois,
            @RequestParam(required = false) Integer idDepartement) {
        String nomFichier = variablesPaieService.nomFichierCsv(annee, mois);
        byte[] contenu = variablesPaieService.exporterCsv(annee, mois, idDepartement);
        return ResponseEntity.ok()
                .contentType(CSV_UTF8)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(nomFichier, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(contenu);
    }
}
