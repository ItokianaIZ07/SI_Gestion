import { api } from "./api";

const crud = (base) => ({
  liste: () => api.get(base),
  parId: (id) => api.get(`${base}/${id}`),
  creer: (donnees) => api.post(base, donnees),
  modifier: (id, donnees) => api.put(`${base}/${id}`, donnees),
  supprimer: (id) => api.delete(`${base}/${id}`),
});

export const besoinsApi = {
  liste: () => api.get("/api/besoins"),
  parId: (id) => api.get(`/api/besoins/${id}`),
};

export const recrutementsApi = crud("/api/recrutements");
export const offresApi = crud("/api/offres-emploi");
export const publicationsApi = crud("/api/publications-offres");
export const cvsApi = {
  ...crud("/api/cvs"),
  parCandidat: (candidatId) =>
    api.get(`/api/cvs/candidat/${candidatId}`),
  fichier: (id) =>
    api.getBlob(`/api/cvs/${id}/fichier`),
};
export const candidatsApi = crud("/api/candidats");
export const candidaturesApi = crud("/api/candidatures");

export const clientApi = {
  publications: () => api.get("/api/client/publications-offres"),
  offres: () => api.get("/api/client/offres-emploi"),
  deposerCv(metadata, fichier) {
    const fd = new FormData();
    fd.append("metadata", new Blob(
      [JSON.stringify(metadata)],
      { type: "application/json" }));
    fd.append("fichier", fichier);
    return api.upload("/api/client/depots-cv", fd);
  },
};
