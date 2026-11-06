// BY : Brandon
package com.si.rh.controller;

import com.si.rh.exception.RegleMetierException;
import com.si.rh.model.AnomaliePointage;
import com.si.rh.model.Pointage;
import com.si.rh.model.TempsTravailJournalier;
import com.si.rh.model.enums.StatutHeuresSup;
import com.si.rh.model.enums.TypePointage;
import com.si.rh.service.PointageService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pointage")
public class PointageController {

    private final PointageService service;

    public PointageController(PointageService service) {
        this.service = service;
    }

    // ---------------------------------------------------------
    // POINTAGE
    // ---------------------------------------------------------

    @PostMapping
    public Pointage pointer(@RequestBody Map<String, String> body, Authentication auth) {
        Integer idEmploye = Integer.parseInt(body.get("idEmploye"));
        TypePointage type = TypePointage.valueOf(body.get("typePointage"));

        // Permet de saisir une heure manuellement (Vérifications 6.1 à 6.17)
        OffsetDateTime dateHeure = null;
        if (body.containsKey("dateHeure") && body.get("dateHeure") != null && !body.get("dateHeure").isEmpty()) {
            dateHeure = OffsetDateTime.parse(body.get("dateHeure"));
        }

        return service.enregistrer(idEmploye, type, dateHeure, auth.getName());
    }

    /**
     * Liste les pointages d'un employé pour un jour donné.
     * Si "date" est absent, on retourne les pointages du jour actuel.
     */
    @GetMapping("/jour/{idEmploye}")
    public List<Pointage> duJour(
            @PathVariable Integer idEmploye,
            @RequestParam(required = false) String date,
            Authentication auth) {
        LocalDate jour = (date != null && !date.isBlank()) ? LocalDate.parse(date) : LocalDate.now();
        return service.pointagesDuJour(idEmploye, jour, auth.getName());
    }

    @PostMapping("/calculer-jour/{idEmploye}")
    public TempsTravailJournalier recalculerJour(
            @PathVariable Integer idEmploye,
            @RequestParam String date,
            Authentication auth) {
        return service.recalculerJournee(idEmploye, LocalDate.parse(date), auth.getName());
    }

    // ---------------------------------------------------------
    // HEURES SUPPLÉMENTAIRES
    // ---------------------------------------------------------

    @GetMapping("/heures-sup/a-valider")
    public List<TempsTravailJournalier> heuresSup(Authentication auth) {
        return service.heuresSupAValider(auth.getName());
    }

    @PostMapping("/heures-sup/{id}/valider")
    public void valider(@PathVariable Integer id, @RequestBody Map<String, String> body, Authentication auth) {
        service.validerHeuresSup(id, StatutHeuresSup.valueOf(body.get("decision")), auth.getName());
    }

    // ---------------------------------------------------------
    // ANOMALIES
    // ---------------------------------------------------------

    @GetMapping("/anomalies")
    public List<AnomaliePointage> anomalies(Authentication auth) {
        return service.anomalies(auth.getName());
    }

    @PatchMapping("/anomalies/{id}/resoudre")
    public AnomaliePointage resoudre(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String justification = body.get("justification");
        if (justification == null || justification.trim().isEmpty()) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "La justification est obligatoire pour résoudre une anomalie.");
        }
        return service.resoudreAnomalie(id, justification.trim(), auth.getName());
    }

    // ---------------------------------------------------------
    // RÉCAP MENSUEL
    // ---------------------------------------------------------

    @GetMapping("/recap-mensuel")
    public Map<String, Object> recap(
            @RequestParam Integer departementId,
            @RequestParam int annee,
            @RequestParam int mois,
            Authentication auth) {
        return service.recapitulatifMensuel(departementId, annee, mois, auth.getName());
    }
}