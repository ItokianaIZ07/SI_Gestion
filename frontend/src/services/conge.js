// Utilitaires du module Congés (affichage et calculs côté interface).

// Statuts prévus par le backend ; la réponse actuelle de /demandes n'en contient pas encore.
const STATUTS = {
  EN_ATTENTE_MANAGER: ['En attente manager', 'fin-wait'],
  EN_ATTENTE_RH: ['En attente RH', 'fin-wait'],
  ACCEPTE: ['Acceptée', 'fin-ok'],
  REFUSE: ['Refusée', 'fin-ko'],
  ANNULE: ['Annulée', 'fin-none']
}
export const STATUTS_LISTE = Object.entries(STATUTS).map(([code, [libelle]]) => ({ code, libelle }))
export const statutDe = (code) => {
  const [libelle, classe] = STATUTS[code] ?? ['Non communiqué', 'fin-none']
  return { libelle, classe }
}

export const nomComplet = (x) => `${x.prenomEmploye} ${x.nomEmploye}`

const nf = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
export const jours = (n) => (n == null ? '—' : `${nf.format(n)} j`)

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