package com.si.rh.dto.document;

import java.time.LocalDate;

public record DocumentExpirationRequest(LocalDate dateExpiration, Integer delaiAlerteJours) {
}
