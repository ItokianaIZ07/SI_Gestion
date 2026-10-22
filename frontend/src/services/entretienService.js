//Brandon
import api from "./api";

//Liste les candidatures avec leurs entretiens (statut RETENU ou PRESELECTIONNE). */
export async function getCandidaturesAvecEntretiens() {
  const { data } = await api.get("/rh/entretiens/candidatures");
  return data;
}

//Cree ou met à jour un entretien
export async function enregistrerEntretien(payload) {
  if (payload.id) {
    const { data } = await api.put(`/api/rh/entretiens/${payload.id}`, payload);
    return data;
  }
  const { data } = await api.post("/rh/entretiens", payload);
  return data;
}

//Envoie la convocation par e-mail
export async function envoyerConvocation(idEntretien) {
  const { data } = await api.post(`/rh/entretiens/${idEntretien}/convocation`);
  return data;
}

//Envoie la notification de resultat
export async function envoyerResultat(idCandidature, decision) {
  const { data } = await api.post(
    `/rh/entretiens/candidatures/${idCandidature}/resultat`,
    { decision }
  );
  return data;
}

//Liste les utilisateurs RH/Manager pour le selecteur d'intervieweur
export async function getIntervieweurs() {
  const { data } = await api.get("/rh/entretiens/intervieweurs");
  return data;
}