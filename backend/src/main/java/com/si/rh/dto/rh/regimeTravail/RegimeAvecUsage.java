package com.si.rh.dto.rh.regimeTravail;

import java.math.BigDecimal;

public interface RegimeAvecUsage {
    Integer getId();
    String getCode();
    String getLibelle();
    String getDescription();
    BigDecimal getHeuresHebdomadaires();
    Long getNbPlannings();
}