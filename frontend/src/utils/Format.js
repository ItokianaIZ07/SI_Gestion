const nf = new Intl.NumberFormat('fr-FR')
export const ariary = (n) => `${nf.format(n ?? 0)} Ar`
export const dateFr = (iso) => (iso ? new Date(iso).toLocaleDateString('fr-FR') : '—')
export const messageErreur = (e) =>
  e?.response?.data?.message || 'Une erreur est survenue. Réessayez.'