package com.si.rh.service;

import com.si.rh.dto.conge.RegleCongeResponse;
import com.si.rh.dto.conge.TypeCongeResponse;
import com.si.rh.model.RegleConge;
import com.si.rh.model.TypeConge;
import com.si.rh.repository.RegleCongeRepository;
import com.si.rh.repository.TypeCongeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TypeCongeService {
    private final TypeCongeRepository typeCongeRepository;
    private final RegleCongeRepository regleCongeRepository;

    public TypeCongeService(
            TypeCongeRepository typeCongeRepository,
            RegleCongeRepository regleCongeRepository) {
        this.typeCongeRepository = typeCongeRepository;
        this.regleCongeRepository = regleCongeRepository;
    }

    @Transactional(readOnly = true)
    public List<TypeCongeResponse> getAllType() {
        LocalDate dateReference = LocalDate.now();
        Map<Integer, RegleConge> reglesApplicables = new HashMap<>();
        for (RegleConge regle : regleCongeRepository.findAllApplicableRules(dateReference)) {
            reglesApplicables.putIfAbsent(regle.getTypeConge().getId(), regle);
        }

        return typeCongeRepository.findByActifTrueOrderByLibelleAsc()
                .stream()
                .filter(typeConge -> reglesApplicables.containsKey(typeConge.getId()))
                .map(typeConge -> toResponse(
                        typeConge,
                        reglesApplicables.get(typeConge.getId())))
                .toList();
    }

    private TypeCongeResponse toResponse(TypeConge typeConge, RegleConge regle) {
        return new TypeCongeResponse(
                typeConge.getId(),
                typeConge.getCode(),
                typeConge.getLibelle(),
                typeConge.isActif(),
                typeConge.getDescription(),
                new RegleCongeResponse(
                        regle.getId(),
                        regle.isDeductionSolde(),
                        regle.isJustificatifObligatoire(),
                        regle.isRemplacantObligatoire(),
                        regle.isAcquisitionAutorisee(),
                        regle.getJoursAcquisMois(),
                        regle.getPlafondJours(),
                        regle.getDateDebutValidite(),
                        regle.getDateFinValidite()));
    }
}
