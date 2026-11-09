import { api } from "@/services/api";

const BASE = "/rh/jours-feries";

export const jourFerieApi = {
  liste: (annee) => api.get(`${BASE}?annee=${annee}`),
  annees: () => api.get(`${BASE}/annees`),
  ajouter: (jourFerie) => api.post(BASE, jourFerie),
  modifier: (dateJour, jourFerie) =>
    api.put(`${BASE}/${dateJour}`, jourFerie),
  supprimer: (dateJour) => api.delete(`${BASE}/${dateJour}`),
};
