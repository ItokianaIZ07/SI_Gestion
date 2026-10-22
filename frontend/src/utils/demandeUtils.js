export function prioriteLabel(priorite) {
  const labels = {
    URGENTE: "Urgente",
    HAUTE: "Haute",
    NORMALE: "Normale",
    BASSE: "Basse",
  };
  return labels[priorite] ?? priorite ?? "Non définie";
}

export function formatDate(date) {
  if (!date) return "Non renseignée";
  const valeur = new Date(date);
  if (Number.isNaN(valeur.getTime())) return "Date invalide";

  return new Intl.DateTimeFormat("fr-FR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(valeur);
}

export function statutLabel(statut) {
  const labels = {
    EN_ATTENTE: "En attente",
    ACCEPTE: "Acceptée",
    REFUSE: "Refusée",
    ANNULE: "Annulée",
    NON_DEFINI: "Statut non défini",
  };
  return labels[statut] ?? statut ?? "Non défini";
}

// exemple d'arguments: (null, "Test", "", "PHP")
/**
 * Retourne la première valeur non null
 * @param {list} valeurs 
 * @returns {string}
 */

export function premierNonVide(...valeurs) { 
  return valeurs.find((v) => v !== null && v !== undefined && v !== "") ?? "";
}

export function nomPoste(b) {
  return premierNonVide(
    b.poste?.nom,
    b.poste?.libelle,
    b.posteNom,
    typeof b.poste === "string" ? b.poste : null,
    "Poste non précisé",
  );
}

export function nomDepartement(b) {
  return premierNonVide(
    b.poste?.departement?.nom,
    b.poste?.departement?.libelle,
    b.departement?.nom,
    b.departement?.libelle,
    b.departementNom,
    b.departement,
    "Département non précisé",
  );
}

export function nomDemandeur(b) {
  const objet = b.demandeur ?? b.utilisateurDemandeur ?? b.utilisateur ?? {};
  const nom = [objet.prenom, objet.nom].filter(Boolean).join(" ").trim();
  return premierNonVide(
    nom,
    typeof b.demandeur === "string" ? b.demandeur : null,
    b.demandeurNom,
    b.nomDemandeur,
    "Demandeur non précisé",
  );
}

export function nomTypeBesoin(b) {
  return premierNonVide(
    b.typeBesoin?.libelle,
    b.typeBesoin?.nom,
    b.typeBesoin?.libelleType,
    b.typeBesoinLibelle,
    b.typeBesoin,
    "Non précisé",
  );
}

export function normaliserBesoin(b) {
  return {
    ...b,
    poste: nomPoste(b),
    departement: nomDepartement(b),
    demandeur: nomDemandeur(b),
    type: nomTypeBesoin(b),
    lieu: premierNonVide(b.lieuTravail, b.lieu_travail, "Non renseigné"),
    statut: premierNonVide(
      b.statutGlobal,
      b.statut_global,
      b.statut,
      "NON_DEFINI",
    ),
    dateDemande: premierNonVide(b.dateDemande, b.date_demande, null),
    dateArrivee: premierNonVide(
      b.dateArriveeSouhaitee,
      b.date_arrivee_souhaitee,
      null,
    ),
    experience: premierNonVide(
      b.experienceRequise,
      b.experience_requise,
      "Non précisée",
    ),
    niveauEtude: premierNonVide(b.niveauEtude, b.niveau_etude, "Non précisé"),
    competences: premierNonVide(
      b.competencesRequises,
      b.competences_requises,
      "Non précisées",
    ),
    salaire: premierNonVide(b.salaireEstime, b.salaire_estime, null),
    priorite: premierNonVide(b.priorite, "NORMALE"),
    effectif: premierNonVide(b.effectif, 1),
    motif: premierNonVide(b.motif, "Aucun motif renseigné"),
  };
}

export function formatSalaire(montant) {
  if (montant === null || montant === undefined || montant === "") {
    return "Non estimé";
  }

  const valeur = Number(montant);
  if (!Number.isFinite(valeur)) return "Non estimé";

  return new Intl.NumberFormat("fr-FR", {
    style: "currency",
    currency: "MGA",
    maximumFractionDigits: 0,
  }).format(valeur);
}