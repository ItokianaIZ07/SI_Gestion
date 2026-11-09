import { api } from "./api";

export const auditApi = {
  rechercher(filtres = {}) {
    const params = new URLSearchParams();
    Object.entries(filtres).forEach(([cle, valeur]) => {
      if (valeur !== "" && valeur !== null && valeur !== undefined) params.append(cle, valeur);
    });
    const query = params.toString();
    return api.get(`/direction/audit${query ? `?${query}` : ""}`);
  },
  tables: () => api.get("/direction/audit/tables"),
  utilisateurs: () => api.get("/direction/audit/utilisateurs"),
};
