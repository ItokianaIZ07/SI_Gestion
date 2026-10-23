//Brandon
import { api } from "./api";

//Candidats RETENU sans employe (a embaucher)
export async function getCandidatsAEmbaucher() {
  const data  = await api.get("/rh/contrats/a-embaucher");
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
// RH signe (avec signature manuscrite)
export async function signerContrat(idContrat, partie, signatureBase64 = null) {
  const data = await api.post(`/rh/contrats/${idContrat}/signer`, {
    partie,
    signatureBase64,
  });
  return data;
}


//Detail d'un contrat + son historique
export async function getContrat(idContrat) {
  const data = await api.get(`/rh/contrats/${idContrat}`);
  return data;
}

/** Récupère le contrat associé à un token (page publique, pas de login). */
export async function getContratParToken(token) {
  const data = await api.get(`/public/signature/${token}`);
  return data;
}

// Candidat signe via token (avec signature manuscrite)
export async function signerContratParToken(token, signatureBase64) {
  const data = await api.post(`/public/signature/${token}/signer`, {
    signatureBase64,
  });
  return data;
}