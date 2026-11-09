package com.si.rh.dto.rh.organigramme;

import java.util.List;

public record HierarchieEmploye(
        OrganigrammePersonne employe,
        OrganigrammePersonne superieurDirect,
        boolean superieurParti,
        List<OrganigrammePersonne> chaineSuperieurs,
        List<OrganigrammePersonne> subordonnesDirects,
        List<OrganigrammePersonne> subordonnesIndirects) {
}
