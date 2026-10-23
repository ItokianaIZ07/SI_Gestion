package com.si.rh.repository;

import com.si.rh.model.SignatureToken;   // ← AJOUT
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface SignatureTokenRepository extends JpaRepository<SignatureToken, Integer> {
    Optional<SignatureToken> findByTokenHashAndUtiliseFalse(String tokenHash);
}