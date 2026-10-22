import axios from 'axios'

// Même origine grâce au proxy Vite (/api -> localhost:8080) : la session (cookie JSESSIONID) est envoyée.
const http = axios.create({ baseURL: '/api', withCredentials: true })

// Spring Security exige un jeton CSRF pour tout POST/PUT/DELETE.
// On demande le cookie XSRF-TOKEN une fois ; axios envoie ensuite l'en-tête X-XSRF-TOKEN tout seul.
let csrfPret = false
async function assurerCsrf() {
  if (csrfPret || document.cookie.includes('XSRF-TOKEN=')) {
    csrfPret = true
    return
  }
  await http.get('/auth/csrf')
  csrfPret = true
}

http.interceptors.request.use(async (config) => {
  const methode = (config.method || 'get').toLowerCase()
  if (!['get', 'head', 'options'].includes(methode)) await assurerCsrf()
  return config
})

// Toutes les routes sont sous /api/finance/** : protégées pour FINANCE et DIRECTION (SecurityConfig).
export const financeApi = {
  // Demandes
  getDemandes: () => http.get('/finance/demandes').then(r => r.data),
  valider: (id) => http.post(`/finance/demandes/${id}/valider`),
  refuser: (id, remarque) => http.post(`/finance/demandes/${id}/refuser`, { remarque }),

  // Grille salariale
  getGrilles: () => http.get('/finance/grilles-salaire').then(r => r.data),
  getPostes: () => http.get('/finance/postes').then(r => r.data),
  creerGrille: (g) => http.post('/finance/grilles-salaire', g),
  modifierGrille: (id, g) => http.put(`/finance/grilles-salaire/${id}`, g),

  // Budget
  getBudget: (annee) => http.get('/finance/budget', { params: { annee } }).then(r => r.data)
}