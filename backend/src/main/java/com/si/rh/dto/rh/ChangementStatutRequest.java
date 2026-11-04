package com.si.rh.dto.rh;

import jakarta.validation.constraints.Size;

public record ChangementStatutRequest(@Size(max = 2000) String remarque) {
}
