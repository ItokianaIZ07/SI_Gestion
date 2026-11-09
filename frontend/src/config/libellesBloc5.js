const ETATS_VALIDITE = {
  VALIDE: { label: "Valide", ton: "success" },
  BIENTOT_EXPIRE: { label: "Bientôt expiré", ton: "warning" },
  EXPIRE: { label: "Expiré", ton: "danger" },
  RENOUVELE: { label: "Renouvelé", ton: "neutral" },
  SANS_EXPIRATION: { label: "Sans expiration", ton: "neutral" },
};

export const etatValidite = (code) => ETATS_VALIDITE[code] ?? { label: code ?? "—", ton: "neutral" };

export const STATUTS_EMPLOYE = [
  { value: "ACTIF", label: "Actif" },
  { value: "SUSPENDU", label: "Suspendu" },
  { value: "PARTI", label: "Parti" },
];

export const ACTIONS_AUDIT = [
  { value: "INSERT", label: "Création" },
  { value: "UPDATE", label: "Modification" },
  { value: "DELETE", label: "Suppression" },
];

export const libelleAction = (code) => ACTIONS_AUDIT.find((a) => a.value === code)?.label ?? code;
