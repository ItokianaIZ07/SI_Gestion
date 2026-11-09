import { api } from "@/services/api.js";

const BASE = "/rh/variables-paie";

function queryString(filtres) {
  const params = new URLSearchParams();
  for (const [cle, valeur] of Object.entries(filtres)) {
    if (valeur !== "" && valeur !== null && valeur !== undefined) {
      params.set(cle, valeur);
    }
  }
  return params.toString();
}

export const variablesPaieApi = {
  departements: () => api.get(`${BASE}/departements`),
  recapitulatif: (filtres) => api.get(`${BASE}?${queryString(filtres)}`),
  exporterCsv: (filtres) => api.getBlob(`${BASE}/export.csv?${queryString(filtres)}`),
};
