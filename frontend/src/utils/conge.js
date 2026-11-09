// Utilitaires du module Congés (affichage et calculs côté interface).

// Statuts renvoyés par le backend (statutGlobal d'une demande).
const STATUTS = {
  EN_ATTENTE_MANAGER: ['En attente manager', 'fin-wait'],
  EN_ATTENTE_RH: ['En attente RH', 'fin-wait'],
  VALIDEE: ['Validée', 'fin-ok'],
  REFUSEE: ['Refusée', 'fin-ko'],
  ANNULEE: ['Annulée', 'fin-none']
}
export const STATUTS_LISTE = Object.entries(STATUTS).map(([code, [libelle]]) => ({ code, libelle }))
export const statutDe = (code) => {
  const [libelle, classe] = STATUTS[code] ?? ['Non communiqué', 'fin-none']
  return { libelle, classe }
}

// Décision d'une étape de validation ou d'une proposition de remplacement.
const DECISIONS = {
  ACCEPTE: ['Accepté', 'fin-ok'],
  REFUSE: ['Refusé', 'fin-ko'],
  EN_ATTENTE: ['En attente', 'fin-wait']
}
export const decisionDe = (code) => {
  const [libelle, classe] = DECISIONS[code] ?? [code ?? '—', 'fin-none']
  return { libelle, classe }
}

// Étape du circuit : MANAGER puis RH.
export const ETAPES = { MANAGER: 'Manager', RH: 'RH' }
export const etapeDe = (code) => ETAPES[code] ?? code ?? '—'
// Étape que doit traiter une demande selon son statut global.
export const etapeCourante = (statutGlobal) =>
  statutGlobal === 'EN_ATTENTE_MANAGER' ? 'Manager' : statutGlobal === 'EN_ATTENTE_RH' ? 'RH' : '—'

export const nomComplet = (x) => `${x.prenomEmploye} ${x.nomEmploye}`

const nf = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
export const jours = (n) => (n == null ? '—' : `${nf.format(n)} j`)

// « 06/11/2026 10:15 » à partir d'un LocalDateTime du backend.
export const dateHeure = (iso) =>
  iso ? new Date(iso).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' }) : '—'

/**
 * Jours du lundi au vendredi entre deux dates 'AAAA-MM-JJ' (bornes incluses).
 * Les jours fériés ne sont pas pris en compte ici : le backend reste la référence du décompte.
 */
export function joursOuvres(debut, fin) {
  if (!debut || !fin || fin < debut) return 0
  const d = new Date(`${debut}T12:00:00Z`)
  const f = new Date(`${fin}T12:00:00Z`)
  let n = 0
  for (; d <= f; d.setUTCDate(d.getUTCDate() + 1)) {
    const j = d.getUTCDay()
    if (j !== 0 && j !== 6) n++
  }
  return n
}