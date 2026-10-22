package com.si.rh.dto.cv;

import com.si.rh.model.enums.StatutLecture;
import java.time.OffsetDateTime;

public record CvDTO(Integer id, Integer candidatId, String details, String nomFichier, String mimeType,
        OffsetDateTime dateDepot, StatutLecture statutLecture) {
}
