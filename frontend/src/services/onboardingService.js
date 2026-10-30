import http, { api } from "./api";

export const onboardingApi = {
  liste: () => api.get("/rh/onboardings"),
  detail: (id) => api.get(`/rh/onboardings/${id}`),

  // ----- Tâches -----
  changerStatutTache: (tacheId, statut, remarque) =>
    api.patch(`/rh/onboardings/taches/${tacheId}/statut`, remarque ? { statut, remarque } : { statut }),
  ajouterTache: (onboardingId, tache) => api.post(`/rh/onboardings/${onboardingId}/taches`, tache),

  // ----- Documents de l'employé -----
  ajouterDocument(onboardingId, type, fichier) {
    const fd = new FormData();
    fd.append("type", type);
    fd.append("fichier", fichier);
    return api.upload(`/rh/onboardings/${onboardingId}/documents`, fd);
  },
  changerStatutDocument: (docId, statut) =>
    api.patch(`/rh/onboardings/documents/${docId}/statut`, { statut }),
  supprimerDocument: (docId) => api.delete(`/rh/onboardings/documents/${docId}`),
  async fichierDocument(docId) {
    const reponse = await http.get(`/rh/onboardings/documents/${docId}/fichier`, {
      responseType: "blob",
      headers: { Accept: "*/*" },
    });
    return reponse.data;
  },
};

// ----- Modèles de tâches (copiés dans chaque nouvel onboarding à l'embauche) -----
export const modelesOnboardingApi = {
  liste: () => api.get("/rh/onboarding-modeles"),
  creer: (modele) => api.post("/rh/onboarding-modeles", modele),
  modifier: (id, modele) => api.put(`/rh/onboarding-modeles/${id}`, modele),
};
