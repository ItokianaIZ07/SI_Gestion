import { api } from "@/services/api.js";

const BASE = "/rh/plannings";

export const planningTravailApi = {
  liste: (filtres = {}) => {
    const params = new URLSearchParams();
    for (const [cle, valeur] of Object.entries(filtres)) {
      if (valeur !== "" && valeur !== null && valeur !== undefined) {
        params.set(cle, valeur);
      }
    }
    const query = params.toString();
    return api.get(query ? `${BASE}?${query}` : BASE);
  },
  options: () => api.get(`${BASE}/options`),
  apercu: (idEmploye, dateDebut) =>
    api.get(`${BASE}/apercu?idEmploye=${idEmploye}&dateDebut=${dateDebut}`),
  ficheEmploye: (idEmploye) => api.get(`${BASE}/employes/${idEmploye}`),
  historique: (idEmploye) => api.get(`${BASE}/employes/${idEmploye}/historique`),
  affecter: (planning) => api.post(BASE, planning),
};
