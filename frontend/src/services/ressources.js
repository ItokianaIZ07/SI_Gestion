import { api } from "./api";

const crud = (base) => ({
  liste: () => api.get(base),
  parId: (id) => api.get(`${base}/${id}`),
  creer: (donnees) => api.post(base, donnees),
  modifier: (id, donnees) => api.put(`${base}/${id}`, donnees),
  supprimer: (id) => api.delete(`${base}/${id}`),
});

export const besoinsApi = {
  liste: () => api.get("/rh/besoins"),
  parId: (id) => api.get(`/rh/besoins/${id}`),
};

export const recrutementsApi = crud("/recrutements");
export const offresApi = crud("/offres-emploi");
export const publicationsApi = crud("/publications-offres");
export const cvsApi = {
  ...crud("/cvs"),
  parCandidat: (candidatId) =>
    api.get(`/cvs/candidat/${candidatId}`),
  fichier: (id) =>
    api.getBlob(`/cvs/${id}/fichier`),
};
export const candidatsApi = crud("/candidats");
export const candidaturesApi = crud("/candidatures");

export const clientApi = {
  publications: () => api.get("/client/publications-offres"),
  offres: () => api.get("/client/offres-emploi"),
  deposerCv(metadata, fichier) {
    const fd = new FormData();
    fd.append("metadata", new Blob(
      [JSON.stringify(metadata)],
      { type: "application/json" }));
    fd.append("fichier", fichier);
    return api.upload("/client/depots-cv", fd);
  },
};
