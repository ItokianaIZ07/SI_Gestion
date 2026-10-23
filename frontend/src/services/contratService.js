//Brandon
import { api } from "./api";

//Candidats RETENU sans employe (a embaucher)
export async function getCandidatsAEmbaucher() {
  const { data } = await api.get("/rh/contrats/a-embaucher");
  return data;
}

//Employes sans contrat, contrats BROUILLON ou EN_ATTENTE_SIGNATURE
export async function getContrats() {
  const data = await api.get("/rh/contrats");
  return data;
}

//Transition recrutement → employe + contrat + onboarding (transactionnel)
export async function embaucherCandidat(payload) {
  const data = await api.post("/rh/contrats/embaucher", payload);
  return data;
}

//Marque un contrat comme signe (une des deux parties)
export async function signerContrat(idContrat, partie) {
  const data = await api.post(`/rh/contrats/${idContrat}/signer`, { partie });
  return data;
}

//Detail d'un contrat + son historique
export async function getContrat(idContrat) {
  const data = await api.get(`/rh/contrats/${idContrat}`);
  return data;
}