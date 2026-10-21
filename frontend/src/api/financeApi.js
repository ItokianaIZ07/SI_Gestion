import axios from 'axios'

// Si le projet a déjà une instance axios (avec baseURL, token/session...), importez-la ici à la place.
const http = axios.create({ baseURL: '/api' })

export const financeApi = {
  // Demandes
  getDemandes: () => http.get('/finance/demandes').then(r => r.data),
  // Valider une demande
  valider: (id) => http.post(`/finance/demandes/${id}/valider`),
  refuser: (id, remarque) => http.post(`/finance/demandes/${id}/refuser`, { remarque }),

  // Grille salariale
  getGrilles: () => http.get('/grilles-salaire').then(r => r.data),
  getPostes: () => http.get('/postes').then(r => r.data),
  creerGrille: (g) => http.post('/grilles-salaire', g),
  modifierGrille: (id, g) => http.put(`/grilles-salaire/${id}`, g),

  // Budget
  getBudget: (annee) => http.get('/finance/budget', { params: { annee } }).then(r => r.data)
}