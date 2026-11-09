package com.si.rh.service;

import com.si.rh.dto.rh.organigramme.DepartementOrganigramme;
import com.si.rh.dto.rh.organigramme.HierarchieEmploye;
import com.si.rh.dto.rh.organigramme.OrganigrammeLigne;
import com.si.rh.dto.rh.organigramme.OrganigrammeNoeud;
import com.si.rh.dto.rh.organigramme.OrganigrammePersonne;
import com.si.rh.exception.RessourceIntrouvableException;
import com.si.rh.repository.OrganigrammeRepository;
import com.si.rh.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class OrganigrammeService {

    private final OrganigrammeRepository organigrammeRepository;
    private final UtilisateurRepository utilisateurRepository;

    public OrganigrammeService(
            OrganigrammeRepository organigrammeRepository,
            UtilisateurRepository utilisateurRepository) {
        this.organigrammeRepository = organigrammeRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    public List<OrganigrammeNoeud> getOrganigramme(String email, String role) {
        AccesOrganigramme acces = obtenirAcces(email, role);
        List<OrganigrammeLigne> lignes = acces.manager()
                ? organigrammeRepository.findOrganigrammeManager(acces.idUtilisateur())
                : organigrammeRepository.findOrganigramme();
        Set<Integer> idsActifs = acces.manager()
                ? Set.copyOf(organigrammeRepository.findIdsOrganigramme())
                : lignes.stream().map(OrganigrammeLigne::idEmploye).collect(java.util.stream.Collectors.toSet());
        return construireArbre(lignes, idsActifs);
    }

    public List<DepartementOrganigramme> getDepartements(String email, String role) {
        AccesOrganigramme acces = obtenirAcces(email, role);
        return acces.manager()
                ? organigrammeRepository.findDepartementsManager(acces.idUtilisateur())
                : organigrammeRepository.findDepartements();
    }

    public HierarchieEmploye getHierarchieEmploye(Integer idEmploye, String email, String role) {
        AccesOrganigramme acces = obtenirAcces(email, role);
        if (acces.manager()
                && !organigrammeRepository.estDansEquipeManager(
                        acces.idUtilisateur(), idEmploye)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cet employé ne fait pas partie de votre équipe.");
        }

        OrganigrammeLigne ligne = organigrammeRepository.findEmploye(idEmploye)
                .orElseThrow(() -> new RessourceIntrouvableException("Employé introuvable dans l'organigramme."));

        OrganigrammePersonne employe = personne(ligne, null);
        OrganigrammePersonne superieurDirect = null;
        boolean superieurParti = false;
        if (ligne.idSuperieur() != null) {
            var superieurActif = organigrammeRepository.findEmploye(ligne.idSuperieur());
            if (superieurActif.isPresent()) {
                superieurDirect = personne(superieurActif.get(), 1);
            } else {
                superieurParti = true;
                superieurDirect = new OrganigrammePersonne(
                        ligne.idSuperieur(),
                        null,
                        ligne.nomSuperieur(),
                        ligne.prenomSuperieur(),
                        null,
                        null,
                        1);
            }
        }

        List<OrganigrammePersonne> chaine = organigrammeRepository.findChaineSuperieurs(idEmploye);
        List<OrganigrammePersonne> subordonnes = organigrammeRepository.findSubordonnes(idEmploye, false);
        if (acces.manager()) {
            chaine = chaine.stream()
                    .filter(personne -> personneDansEquipe(acces, personne.idEmploye()))
                    .toList();
            subordonnes = subordonnes.stream()
                    .filter(personne -> personneDansEquipe(acces, personne.idEmploye()))
                    .toList();
            if (superieurDirect != null
                    && !superieurParti
                    && !personneDansEquipe(acces, superieurDirect.idEmploye())) {
                superieurDirect = null;
            }
        }

        List<OrganigrammePersonne> subordonnesDirects = subordonnes.stream()
                .filter(personne -> Integer.valueOf(1).equals(personne.niveau()))
                .toList();
        List<OrganigrammePersonne> subordonnesIndirects = subordonnes.stream()
                .filter(personne -> personne.niveau() != null && personne.niveau() > 1)
                .toList();

        return new HierarchieEmploye(
                employe,
                superieurDirect,
                superieurParti,
                chaine,
                subordonnesDirects,
                subordonnesIndirects);
    }

    private boolean personneDansEquipe(AccesOrganigramme acces, Integer idEmploye) {
        return organigrammeRepository.estDansEquipeManager(
                acces.idUtilisateur(), idEmploye);
    }

    private AccesOrganigramme obtenirAcces(String email, String role) {
        if (!Set.of("RH", "DIRECTION", "MANAGER").contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès interdit à l'organigramme.");
        }
        var utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur connecté introuvable."));
        if (!"MANAGER".equals(role)) {
            return new AccesOrganigramme(utilisateur.getId(), false);
        }
        return new AccesOrganigramme(utilisateur.getId(), true);
    }

    private List<OrganigrammeNoeud> construireArbre(List<OrganigrammeLigne> lignes, Set<Integer> idsActifs) {
        Map<Integer, NoeudMutable> noeuds = new LinkedHashMap<>();
        for (OrganigrammeLigne ligne : lignes) {
            noeuds.put(ligne.idEmploye(), new NoeudMutable(ligne));
        }

        List<NoeudMutable> racines = new ArrayList<>();
        for (NoeudMutable noeud : noeuds.values()) {
            NoeudMutable parent = noeuds.get(noeud.ligne.idSuperieur());
            if (parent == null || parent == noeud) {
                racines.add(noeud);
            } else {
                parent.subordonnes.add(noeud);
            }
        }

        Set<Integer> idsAtteints = new java.util.HashSet<>();
        racines.forEach(racine -> marquerAtteints(racine, idsAtteints));
        noeuds.values().stream()
                .filter(noeud -> !idsAtteints.contains(noeud.ligne.idEmploye()))
                .sorted(Comparator.comparing(noeud -> noeud.ligne.nom(), String.CASE_INSENSITIVE_ORDER))
                .forEach(racines::add);

        return racines.stream()
                .map(racine -> versDto(racine, idsActifs, new java.util.HashSet<>()))
                .toList();
    }

    private void marquerAtteints(NoeudMutable noeud, Set<Integer> idsAtteints) {
        if (!idsAtteints.add(noeud.ligne.idEmploye())) {
            return;
        }
        noeud.subordonnes.forEach(enfant -> marquerAtteints(enfant, idsAtteints));
    }

    private OrganigrammeNoeud versDto(
            NoeudMutable noeud, Set<Integer> idsActifs, Set<Integer> chemin) {
        Integer id = noeud.ligne.idEmploye();
        if (!chemin.add(id)) {
            return null;
        }
        List<OrganigrammeNoeud> enfants = noeud.subordonnes.stream()
                .map(enfant -> versDto(enfant, idsActifs, new java.util.HashSet<>(chemin)))
                .filter(java.util.Objects::nonNull)
                .toList();
        return new OrganigrammeNoeud(
                id,
                noeud.ligne.matricule(),
                noeud.ligne.nom(),
                noeud.ligne.prenom(),
                noeud.ligne.departement(),
                noeud.ligne.poste(),
                noeud.ligne.idSuperieur(),
                noeud.ligne.nbSubordonnesDirects(),
                noeud.ligne.idSuperieur() != null && !idsActifs.contains(noeud.ligne.idSuperieur()),
                enfants);
    }

    private OrganigrammePersonne personne(OrganigrammeLigne ligne, Integer niveau) {
        return new OrganigrammePersonne(
                ligne.idEmploye(),
                ligne.matricule(),
                ligne.nom(),
                ligne.prenom(),
                ligne.departement(),
                ligne.poste(),
                niveau);
    }

    private record AccesOrganigramme(Integer idUtilisateur, boolean manager) {
    }

    private static class NoeudMutable {
        private final OrganigrammeLigne ligne;
        private final List<NoeudMutable> subordonnes = new ArrayList<>();

        private NoeudMutable(OrganigrammeLigne ligne) {
            this.ligne = ligne;
        }
    }
}
