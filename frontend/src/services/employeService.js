import http, { api } from "./api";

export const employeApi = {
  liste: () => api.get("/rh/employes"),
  fiche: (id) => api.get(`/rh/employes/${id}`),
  carriere: (id) => api.get(`/rh/employes/${id}/carriere`),
  referentiels: () => api.get("/rh/employes/referentiels-affectation"),
  changerAffectation: (id, demande) => api.post(`/rh/employes/${id}/affectations`, demande),

  documents: (id) => api.get(`/rh/employes/${id}/documents`),
  deposerDocument(id, { type, fichier, dateExpiration, delaiAlerteJours }) {
    const fd = new FormData();
    fd.append("type", type);
    fd.append("fichier", fichier);
    if (dateExpiration) fd.append("dateExpiration", dateExpiration);
    if (delaiAlerteJours !== "" && delaiAlerteJours !== null && delaiAlerteJours !== undefined) {
      fd.append("delaiAlerteJours", String(delaiAlerteJours));
    }
    return api.upload(`/rh/employes/${id}/documents`, fd);
  },
  modifierExpiration: (docId, dateExpiration, delaiAlerteJours) =>
    api.patch(`/rh/documents/${docId}/expiration`, {
      dateExpiration: dateExpiration || null,
      delaiAlerteJours: delaiAlerteJours === "" ? null : delaiAlerteJours,
    }),
  marquerRenouvele: (docId, dateRenouvellement) =>
    api.patch(`/rh/documents/${docId}/renouvellement`, dateRenouvellement ? { dateRenouvellement } : {}),
  async fichierDocument(docId) {
    const reponse = await http.get(`/rh/documents/${docId}/fichier`, {
      responseType: "blob",
      headers: { Accept: "*/*" },
    });
    return reponse.data;
  },
};

export const alertesDocumentsApi = {
  liste: () => api.get("/rh/alertes/documents"),
  generer: () => api.post("/rh/alertes/documents/generer"),
};
