import { api } from "@/services/api";

const BASE = "/rh/regimes-travail";

export const regimeTravailApi = {
  liste: () => api.get(BASE),
  parId: (id) => api.get(`${BASE}/${id}`),
  creer: (regime) => api.post(BASE, regime),
  modifier: (id, regime) => api.put(`${BASE}/${id}`, regime),
  supprimer: (id) => api.delete(`${BASE}/${id}`),
};
