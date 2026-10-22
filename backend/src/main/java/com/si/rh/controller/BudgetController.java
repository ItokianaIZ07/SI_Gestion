package com.si.rh.controller;

// BY : Fenosoa
// GET /api/finance/budget?annee=2026  (réservé FINANCE et DIRECTION par SecurityConfig)

import com.si.rh.dto.BudgetResponse;
import com.si.rh.service.BudgetService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/budget")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public BudgetResponse budget(@RequestParam(required = false) Integer annee) {
        return budgetService.calculer(annee);
    }
}