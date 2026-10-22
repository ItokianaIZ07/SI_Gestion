// Libellés, couleurs et petits utilitaires d'affichage partagés par les vues.

export const LIBELLES = {
  // StatutGlobalBesoin
  EN_ATTENTE_RH: "En attente RH",
  EN_ATTENTE_FINANCE: "En attente Finance",
  EN_ATTENTE_DIRECTION: "En attente Direction",
  ACCEPTE: "Accepté",
  REFUSE: "Refusé",
  ANNULE: "Annulé",
  // Priorite
  BASSE: "Basse",
  NORMALE: "Normale",
  HAUTE: "Haute",
  URGENTE: "Urgente",
  // StatutRecrutement
  BROUILLON: "Brouillon",
  OUVERT: "Ouvert",
  SUSPENDU: "Suspendu",
  TERMINE: "Terminé",
  // StatutOffre
  VALIDEE: "Validée",
  ARCHIVEE: "Archivée",
  // StatutPublication
  PUBLIEE: "Publiée",
  EXPIREE: "Expirée",
  RETIREE: "Retirée",
};

export const TONS = {
  ACCEPTE: "success", OUVERT: "success", VALIDEE: "success", PUBLIEE: "success",
  EN_ATTENTE_RH: "warning", EN_ATTENTE_FINANCE: "warning", EN_ATTENTE_DIRECTION: "warning",
  SUSPENDU: "warning", HAUTE: "warning",
  REFUSE: "danger", ANNULE: "danger", URGENTE: "danger",
  BROUILLON: "neutral", BASSE: "neutral", NORMALE: "neutral",
  TERMINE: "neutral", ARCHIVEE: "neutral", EXPIREE: "neutral", RETIREE: "neutral",
};

export const libelle = (code) => LIBELLES[code] ?? code ?? "—";
export const ton = (code) => TONS[code] ?? "neutral";

const enOptions = (codes) => codes.map((value) => ({ value, label: libelle(value) }));

export const STATUTS_BESOIN = enOptions([
  "EN_ATTENTE_RH", "EN_ATTENTE_FINANCE", "EN_ATTENTE_DIRECTION", "ACCEPTE", "REFUSE", "ANNULE",
]);
export const PRIORITES = enOptions(["BASSE", "NORMALE", "HAUTE", "URGENTE"]);
export const STATUTS_RECRUTEMENT = enOptions(["BROUILLON", "OUVERT", "SUSPENDU", "TERMINE", "ANNULE"]);
export const STATUTS_OFFRE = enOptions(["BROUILLON", "VALIDEE", "ARCHIVEE"]);
export const STATUTS_PUBLICATION = enOptions(["BROUILLON", "PUBLIEE", "EXPIREE", "RETIREE"]);

// ---------- Besoin : lecture tolérante du DTO (plat ou imbriqué) ----------
export const posteDe = (b) => b?.posteNom ?? b?.poste?.nom ?? "—";
export const departementDe = (b) => b?.departementNom ?? b?.poste?.departement?.nom ?? "—";
export const typeDe = (b) => b?.typeBesoinLibelle ?? b?.typeBesoin?.libelle ?? "—";

// ---------- Dates & montants ----------
export function aujourdhui() {
  const d = new Date();
  const p = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

export function formatDate(valeur) {
  if (!valeur) return "—";
  const simple = /^(\d{4})-(\d{2})-(\d{2})$/.exec(valeur);
  if (simple) return `${simple[3]}/${simple[2]}/${simple[1]}`;
  const d = new Date(valeur);
  return Number.isNaN(d.getTime()) ? "—" : d.toLocaleDateString("fr-FR");
}

export function formatMontant(n) {
  if (n === null || n === undefined || n === "") return null;
  return `${new Intl.NumberFormat("fr-FR").format(Number(n))} Ar`;
}

export function formatSalaire(min, max) {
  const a = formatMontant(min);
  const b = formatMontant(max);
  if (a && b) return `${a} – ${b}`;
  if (a) return `À partir de ${a}`;
  if (b) return `Jusqu'à ${b}`;
  return "Non communiqué";
}
