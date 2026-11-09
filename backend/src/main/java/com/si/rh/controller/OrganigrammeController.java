package com.si.rh.controller;

import com.si.rh.dto.rh.organigramme.DepartementOrganigramme;
import com.si.rh.dto.rh.organigramme.HierarchieEmploye;
import com.si.rh.dto.rh.organigramme.OrganigrammeNoeud;
import com.si.rh.service.OrganigrammeService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rh/organigramme")
public class OrganigrammeController {

    private final OrganigrammeService organigrammeService;

    public OrganigrammeController(OrganigrammeService organigrammeService) {
        this.organigrammeService = organigrammeService;
    }

    @GetMapping
    public List<OrganigrammeNoeud> getOrganigramme(Authentication authentication) {
        return organigrammeService.getOrganigramme(authentication.getName(), role(authentication));
    }

    @GetMapping("/departements")
    public List<DepartementOrganigramme> getDepartements(Authentication authentication) {
        return organigrammeService.getDepartements(authentication.getName(), role(authentication));
    }

    @GetMapping("/employes/{idEmploye}/hierarchie")
    public HierarchieEmploye getHierarchie(
            @PathVariable Integer idEmploye,
            Authentication authentication) {
        return organigrammeService.getHierarchieEmploye(
                idEmploye, authentication.getName(), role(authentication));
    }

    private String role(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse("");
    }
}
