package com.si.rh.dto;

import java.util.List;

public record PageResultat<T>(List<T> contenu, int page, int taille, long total, int totalPages) {
}
