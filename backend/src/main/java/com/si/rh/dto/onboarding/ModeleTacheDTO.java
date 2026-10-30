package com.si.rh.dto.onboarding;

import com.si.rh.model.enums.CategorieOnboarding;

public record ModeleTacheDTO(Integer id, String code, String libelle, CategorieOnboarding categorie,
        Integer delaiJours, Boolean actif) {
}
