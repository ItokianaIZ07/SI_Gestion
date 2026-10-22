package com.si.rh.controller;

// BY : Fenosoa
// Transforme les erreurs en {"message": "..."} (le front lit ce champ).
// Limité aux contrôleurs du module Finance : ajouter ici les prochains (demandes, budget...).

import com.si.rh.exception.RegleMetierException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = { GrilleSalaireController.class })
public class FinanceExceptionHandler {

    @ExceptionHandler(RegleMetierException.class)
    public ResponseEntity<Map<String, String>> regleMetier(RegleMetierException e) {
        return ResponseEntity.status(e.getStatut()).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> donneesInvalides(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "Données invalides.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> corpsIllisible(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Données invalides : vérifiez les montants et les dates."));
    }

    /** Filet de sécurité : une contrainte de la base (ex. chevauchement) refuse l'enregistrement. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> contrainteBase(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Enregistrement refusé : la période chevauche une autre grille de ce poste."));
    }
}