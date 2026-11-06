package com.si.rh.service;

import com.si.rh.dto.rh.variablesPaie.VariablesPaieBrutes;
import com.si.rh.dto.rh.variablesPaie.VariablesPaieLigne;
import com.si.rh.dto.rh.variablesPaie.VariablesPaieRecapitulatif;
import com.si.rh.dto.rh.variablesPaie.VariablesPaieTotaux;
import com.si.rh.dto.rh.variablesPaie.DepartementPaieOption;
import com.si.rh.exception.RegleMetierException;
import com.si.rh.model.Utilisateur;
import com.si.rh.repository.DepartementRepository;
import com.si.rh.repository.UtilisateurRepository;
import com.si.rh.repository.VariablesPaieMensuellesRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class VariablesPaieMensuellesService {

    private static final int ANNEE_MIN = 1900;
    private static final int ANNEE_MAX = 9999;
    private static final char BOM_UTF8 = '\uFEFF';
    private static final String SEPARATEUR_CSV = ";";

    private final VariablesPaieMensuellesRepository variablesPaieRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DepartementRepository departementRepository;

    public VariablesPaieMensuellesService(
            VariablesPaieMensuellesRepository variablesPaieRepository,
            UtilisateurRepository utilisateurRepository,
            DepartementRepository departementRepository) {
        this.variablesPaieRepository = variablesPaieRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.departementRepository = departementRepository;
    }

    public List<DepartementPaieOption> getDepartements() {
        return departementRepository.findAllByOrderByNomAsc().stream()
                .map(departement -> new DepartementPaieOption(departement.getId(), departement.getNom()))
                .toList();
    }

    public VariablesPaieRecapitulatif getRecapitulatif(
            Integer annee,
            Integer mois,
            Integer idDepartement) {
        validerPeriode(annee, mois);
        return construireRecapitulatif(
                annee, mois, variablesPaieRepository.findRecapitulatif(annee, mois, idDepartement));
    }

    public VariablesPaieRecapitulatif getRecapitulatifManager(
            Integer annee,
            Integer mois,
            Integer idDepartement,
            Integer idUtilisateur) {
        validerPeriode(annee, mois);
        verifierManager(idUtilisateur);
        return construireRecapitulatif(
                annee, mois,
                variablesPaieRepository.findRecapitulatifManager(annee, mois, idDepartement, idUtilisateur));
    }

    public byte[] exporterCsv(Integer annee, Integer mois, Integer idDepartement) {
        return encoderCsv(construireCsv(getRecapitulatif(annee, mois, idDepartement)));
    }

    public byte[] exporterCsvManager(
            Integer annee,
            Integer mois,
            Integer idDepartement,
            Integer idUtilisateur) {
        return encoderCsv(construireCsv(
                getRecapitulatifManager(annee, mois, idDepartement, idUtilisateur)));
    }

    public String nomFichierCsv(Integer annee, Integer mois) {
        validerPeriode(annee, mois);
        return String.format(Locale.ROOT, "variables_paie_%04d-%02d.csv", annee, mois);
    }

    public String minutesVersHeuresMinutes(Long minutes) {
        long valeur = minutes == null ? 0 : minutes;
        return String.format(Locale.ROOT, "%02d:%02d", valeur / 60, valeur % 60);
    }

    public BigDecimal minutesVersHeuresDecimales(Long minutes) {
        return BigDecimal.valueOf(minutes == null ? 0 : minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private VariablesPaieRecapitulatif construireRecapitulatif(
            Integer annee,
            Integer mois,
            List<VariablesPaieBrutes> donnees) {
        List<VariablesPaieLigne> lignes = donnees.stream()
                .map(this::convertirLigne)
                .toList();

        long prevues = 0;
        long travaillees = 0;
        long retard = 0;
        long nbRetards = 0;
        long supValidees = 0;
        long supAValider = 0;
        for (VariablesPaieBrutes ligne : donnees) {
            prevues += ligne.minutesPrevues();
            travaillees += ligne.minutesTravaillees();
            retard += ligne.minutesRetard();
            nbRetards += ligne.nbRetards();
            supValidees += ligne.minutesSupValidees();
            supAValider += ligne.minutesSupAValider();
        }

        VariablesPaieTotaux totaux = new VariablesPaieTotaux(
                minutesVersHeuresMinutes(prevues),
                minutesVersHeuresDecimales(prevues),
                minutesVersHeuresMinutes(travaillees),
                minutesVersHeuresDecimales(travaillees),
                minutesVersHeuresMinutes(retard),
                minutesVersHeuresDecimales(retard),
                nbRetards,
                minutesVersHeuresMinutes(supValidees),
                minutesVersHeuresDecimales(supValidees),
                minutesVersHeuresMinutes(supAValider),
                minutesVersHeuresDecimales(supAValider));

        return new VariablesPaieRecapitulatif(annee, mois, lignes, totaux);
    }

    private VariablesPaieLigne convertirLigne(VariablesPaieBrutes ligne) {
        return new VariablesPaieLigne(
                ligne.idEmploye(),
                ligne.matricule(),
                ligne.nom(),
                ligne.prenom(),
                ligne.departement(),
                ligne.annee(),
                ligne.mois(),
                minutesVersHeuresMinutes(ligne.minutesPrevues()),
                minutesVersHeuresDecimales(ligne.minutesPrevues()),
                minutesVersHeuresMinutes(ligne.minutesTravaillees()),
                minutesVersHeuresDecimales(ligne.minutesTravaillees()),
                minutesVersHeuresMinutes(ligne.minutesRetard()),
                minutesVersHeuresDecimales(ligne.minutesRetard()),
                ligne.nbRetards(),
                minutesVersHeuresMinutes(ligne.minutesSupValidees()),
                minutesVersHeuresDecimales(ligne.minutesSupValidees()),
                minutesVersHeuresMinutes(ligne.minutesSupAValider()),
                minutesVersHeuresDecimales(ligne.minutesSupAValider()));
    }

    private String construireCsv(VariablesPaieRecapitulatif recapitulatif) {
        List<String> lignes = new ArrayList<>();
        lignes.add(String.join(SEPARATEUR_CSV,
                "Matricule", "Nom", "Prénom", "Département", "Année", "Mois",
                "Temps prévu (hh:mm)", "Temps prévu (heures décimales)",
                "Temps travaillé (hh:mm)", "Temps travaillé (heures décimales)",
                "Retard (hh:mm)", "Retard (heures décimales)", "Nombre de retards",
                "Heures sup validées (hh:mm)", "Heures sup validées (heures décimales)",
                "Heures sup à valider (hh:mm)", "Heures sup à valider (heures décimales)"));

        for (VariablesPaieLigne ligne : recapitulatif.lignes()) {
            lignes.add(String.join(SEPARATEUR_CSV,
                    csv(ligne.matricule()),
                    csv(ligne.nom()),
                    csv(ligne.prenom()),
                    csv(ligne.departement()),
                    ligne.annee().toString(),
                    ligne.mois().toString(),
                    csv(ligne.tempsPrevu()),
                    csv(ligne.tempsPrevuDecimal().toPlainString()),
                    csv(ligne.tempsTravaille()),
                    csv(ligne.tempsTravailleDecimal().toPlainString()),
                    csv(ligne.retard()),
                    csv(ligne.retardDecimal().toPlainString()),
                    ligne.nbRetards().toString(),
                    csv(ligne.heuresSupValidees()),
                    csv(ligne.heuresSupValideesDecimal().toPlainString()),
                    csv(ligne.heuresSupAValider()),
                    csv(ligne.heuresSupAValiderDecimal().toPlainString())));
        }

        VariablesPaieTotaux total = recapitulatif.totaux();
        lignes.add(String.join(SEPARATEUR_CSV,
                "TOTAL", "", "", "", recapitulatif.annee().toString(), recapitulatif.mois().toString(),
                csv(total.tempsPrevu()), csv(total.tempsPrevuDecimal().toPlainString()),
                csv(total.tempsTravaille()), csv(total.tempsTravailleDecimal().toPlainString()),
                csv(total.retard()), csv(total.retardDecimal().toPlainString()),
                total.nbRetards().toString(),
                csv(total.heuresSupValidees()), csv(total.heuresSupValideesDecimal().toPlainString()),
                csv(total.heuresSupAValider()), csv(total.heuresSupAValiderDecimal().toPlainString())));

        return String.join("\r\n", lignes) + "\r\n";
    }

    private String csv(String valeur) {
        if (valeur.contains(";") || valeur.contains("\"")
                || valeur.contains("\r") || valeur.contains("\n")) {
            return "\"" + valeur.replace("\"", "\"\"") + "\"";
        }
        return valeur;
    }

    private byte[] encoderCsv(String contenu) {
        return (BOM_UTF8 + contenu).getBytes(StandardCharsets.UTF_8);
    }

    private void verifierManager(Integer idUtilisateur) {
        if (idUtilisateur == null || idUtilisateur <= 0) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "L'identifiant de l'utilisateur manager est obligatoire.");
        }
        Utilisateur utilisateur = utilisateurRepository.findById(idUtilisateur)
                .orElseThrow(() -> new RegleMetierException(
                        HttpStatus.FORBIDDEN, "Utilisateur manager introuvable."));
        if (utilisateur.getRole() == null || !"MANAGER".equals(utilisateur.getRole().getCode())) {
            throw new RegleMetierException(HttpStatus.FORBIDDEN,
                    "Le filtre d'équipe est réservé aux utilisateurs MANAGER.");
        }
    }

    private void validerPeriode(Integer annee, Integer mois) {
        if (mois == null || mois < 1 || mois > 12) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST, "Le mois doit être compris entre 1 et 12.");
        }
        if (annee == null || annee < ANNEE_MIN || annee > ANNEE_MAX) {
            throw new RegleMetierException(HttpStatus.BAD_REQUEST,
                    "L'année doit être comprise entre " + ANNEE_MIN + " et " + ANNEE_MAX + ".");
        }
    }
}
