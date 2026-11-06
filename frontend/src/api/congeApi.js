import axios from 'axios'

// Même origine grâce au proxy Vite (/api -> localhost:8080) : la session (cookie JSESSIONID) est envoyée.
const http = axios.create({ baseURL: '/api/conge', withCredentials: true })

// Spring Security exige un jeton CSRF pour tout POST : on demande le cookie XSRF-TOKEN une seule fois.
let csrfPret = false
async function assurerCsrf() {
  if (csrfPret || document.cookie.includes('XSRF-TOKEN=')) { csrfPret = true; return }
  await axios.get('/api/auth/csrf', { withCredentials: true })
  csrfPret = true
}
http.interceptors.request.use(async (config) => {
  if (!['get', 'head', 'options'].includes((config.method || 'get').toLowerCase())) await assurerCsrf()
  return config
})

// Types de congé utilisés tant que l'API n'expose pas GET /api/conge/types.
// ⚠ Les identifiants sont ceux de l'exemple de la documentation : à remplacer dès que le backend fournit la liste.
const TYPES_PAR_DEFAUT = [
  { id: 1, libelle: 'Congé annuel' },
  { id: 2, libelle: 'Congé maladie' }
]

export const congeApi = {
  // GET /api/conge/soldes : [{ idEmploye, nomEmploye, prenomEmploye, matriculeEmploye, emailEmploye, typeConge, solde }]
  getSoldes: () => http.get('/soldes').then(r => r.data),

  // GET /api/conge/demandes : [{ idDemande, idEmploye, nomEmploye, …, dateDebutConge, dateFinConge, typeConge, nombreJourConge, motif }]
  getDemandes: () => http.get('/demandes').then(r => r.data),

  // GET /api/conge/demandes/{idEmploye}
  getDemandesEmploye: (idEmploye) => http.get(`/demandes/${idEmploye}`).then(r => r.data),

  // POST /api/conge/demandes : { idEmploye, idTypeConge, dateDebut, dateFin, nombreJours, motif }
  creerDemande: (demande) => http.post('/demandes', demande).then(r => r.data),

  // GET /api/conge/types (à demander au backend) : [{ id, libelle }]. Repli sur la liste par défaut si la route n'existe pas.
  getTypesConge: () => http.get('/types').then(r => r.data).catch(() => TYPES_PAR_DEFAUT)
}