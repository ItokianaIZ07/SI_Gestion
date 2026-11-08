import axios from 'axios'

// Même origine grâce au proxy Vite (/api -> localhost:8080) : la session (cookie JSESSIONID) est envoyée.
// Spring Security exige un jeton CSRF pour les validations, annulations et décisions de remplacement :
// on demande le cookie XSRF-TOKEN une seule fois, puis axios renvoie l'en-tête X-XSRF-TOKEN tout seul.
let csrfPret = false
async function assurerCsrf() {
  if (csrfPret || document.cookie.includes('XSRF-TOKEN=')) { csrfPret = true; return }
  await axios.get('/api/auth/csrf', { withCredentials: true })
  csrfPret = true
}

function creerClient(baseURL) {
  const client = axios.create({ baseURL, withCredentials: true })
  client.interceptors.request.use(async (config) => {
    if (!['get', 'head', 'options'].includes((config.method || 'get').toLowerCase())) await assurerCsrf()
    return config
  })
  return client
}

const conge = creerClient('/api/conge')
const employes = creerClient('/api/manager/employes')
const auth = creerClient('/api/auth')

// Le backend nomme le statut `statutGlobal` (liste « à valider ») : on l'expose aussi sous `statut`
// pour les demandes, afin que les pages fonctionnent dès que /demandes renverra l'un ou l'autre.
const avecStatut = (liste) => liste.map(d => ({ ...d, statut: d.statutGlobal ?? d.statut }))

export const congeApi = {
  // ───── Soldes, types, demandes ─────

  // GET /api/conge/soldes : [{ idEmploye, nomEmploye, prenomEmploye, matriculeEmploye, emailEmploye, typeConge, solde }]
  getSoldes: () => conge.get('/soldes').then(r => r.data),

  // GET /api/conge/type-conge → [{ id, code, libelle, description, regle }]
  // regle : { deductionSolde, justificatifObligatoire, remplacantObligatoire, acquisitionAutorisee, joursAcquisMois, plafondJours, … }
  getTypesConge: () => conge.get('/type-conge').then(r => r.data.map(t => ({
    id: t.idType,
    code: t.code,
    libelle: t.libelleTypeConge,
    description: t.description,
    regle: t.regle
  }))),

  // GET /api/conge/demandes : [{ idDemande, idEmploye, …, dateDebutConge, dateFinConge, typeConge, nombreJourConge, motif, idEmployeRemplacant }]
  getDemandes: () => conge.get('/demandes').then(r => avecStatut(r.data)),

  // GET /api/conge/demandes/{idEmploye}
  getDemandesEmploye: (idEmploye) => conge.get(`/demandes/${idEmploye}`).then(r => avecStatut(r.data)),

  // POST /api/conge/demandes : { idEmploye, idTypeConge, idEmployeRemplacant?, dateDebut, dateFin, nombreJours, motif }
  creerDemande: (demande) => conge.post('/demandes', demande).then(r => r.data),

  // ───── Circuit de validation (MANAGER et RH) ─────

  // GET /api/conge/demandes/en-attente → [{ idDemande, idEmploye, nomEmploye, prenomEmploye, matriculeEmploye, emailEmploye,
  //   dateDebut, dateFin, typeConge, nombreJours, motif, statutGlobal, dateDemande }]
  getDemandesEnAttente: () => conge.get('/demandes/en-attente').then(r => r.data),

  // POST /api/conge/demandes/{id}/validation : { decision: 'ACCEPTE' | 'REFUSE', commentaire? }
  // → { idDemande, etapeValidee, decision, statutGlobal, commentaire, dateDecision }
  deciderDemande: (id, decision, commentaire) =>
    conge.post(`/demandes/${id}/validation`, { decision, commentaire: commentaire || null }).then(r => r.data),

  // GET /api/conge/demandes/{id}/validations → [{ idValidation, idDemande, idUtilisateur, emailValidateur, ordre, etape, decision, commentaire, dateValidation }]
  getHistoriqueValidations: (id) => conge.get(`/demandes/${id}/validations`).then(r => r.data),

  // POST /api/conge/demandes/{id}/annulation : { motif } (EMPLOYE concerné ou RH, demande VALIDEE)
  // → { idDemande, statutGlobal, joursRestitues, dateAnnulation, motif }
  annulerDemande: (id, motif) => conge.post(`/demandes/${id}/annulation`, { motif }).then(r => r.data),

  // ───── Remplacements (rôle EMPLOYE) ─────

  // GET /api/conge/remplacements/moi → [{ idRemplacement, idDemande, nomEmployeDemandeur, …, typeConge, dateDebut, dateFin, nombreJours, dateProposition, decision, commentaire, dateDecision }]
  getMesPropositions: () => conge.get('/remplacements/moi').then(r => r.data),

  // POST /api/conge/remplacements/{id}/decision : { decision: 'ACCEPTE' | 'REFUSE', commentaire? }
  deciderRemplacement: (id, decision, commentaire) =>
    conge.post(`/remplacements/${id}/decision`, { decision, commentaire: commentaire || null }).then(r => r.data),

  // ───── Employés (choix du remplaçant) ─────

  // GET /api/manager/employes/par-departement → [{ idDepartement, nomDepartement, employes: [{ id, matricule, nom, prenom, email }] }]
  getEmployesParDepartement: () => employes.get('/par-departement').then(r => r.data),

  // GET /api/manager/employes/departement/{id}/employes → [{ id, matricule, nom, prenom, email }]
  getEmployesDuDepartement: (idDepartement) => employes.get(`/departement/${idDepartement}/employes`).then(r => r.data),

  // ───── Connexion employé (sans mot de passe, provisoire) ─────

  // POST /api/auth/employe/login : { email, matricule, numeroCin } → { success, message, role: 'EMPLOYE', employe: { id, matricule, nom, prenom, email } }
  connexionEmploye: (identifiants) => auth.post('/employe/login', identifiants).then(r => r.data),

  // GET /api/auth/employe/me : même format que la connexion (nécessite la session)
  getEmployeConnecte: () => auth.get('/employe/me').then(r => r.data)
}