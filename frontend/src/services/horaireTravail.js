import { api } from "@/services/api";

const BASE = "/rh/horaires-travail";

export const horaireTravailApi = {
  liste: () => api.get(BASE),
  parId: (id) => api.get(`${BASE}/${id}`),
  creer: (horaire) => api.post(BASE, horaire),
  modifier: (id, horaire) => api.put(`${BASE}/${id}`, horaire),
  desactiver: (id) => api.patch(`${BASE}/${id}/desactiver`),
  reactiver: (id) => api.patch(`${BASE}/${id}/reactiver`),
  supprimer: (id) => api.delete(`${BASE}/${id}`),
};
