import { api } from "./api";

// Zone RH : liste des CV / candidatures et décisions de tri.
export const candidaturesRhApi = {
  /** Sans argument : tous les CV déposés. Avec recrutementId : les candidatures de cette offre. */
  liste: (recrutementId) =>
    api.get(
      recrutementId === undefined || recrutementId === null
        ? "/rh/candidatures"
        : `/rh/candidatures?recrutementId=${encodeURIComponent(recrutementId)}`,
    ),

  // Seule une candidature EN_ATTENTE peut être présélectionnée ou rejetée (le serveur le vérifie).
  preselectionner: (id, remarque) =>
    api.post(`/rh/candidatures/${id}/preselectionner`, remarque ? { remarque } : undefined),
  rejeter: (id, remarque) =>
    api.post(`/rh/candidatures/${id}/rejeter`, remarque ? { remarque } : undefined),

  marquerLu: (cvId) => api.post(`/rh/cvs/${cvId}/lu`),
};
