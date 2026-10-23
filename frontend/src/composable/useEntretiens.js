//Brandon
import { ref } from "vue";
import {
  getCandidaturesAvecEntretiens,
  enregistrerEntretien,
  envoyerConvocation,
  envoyerResultat,
  getIntervieweurs,
} from "@/services/entretienService";

export function useEntretiens() {
  const candidatures = ref([]);
  const intervieweurs = ref([]);
  const chargement = ref(false);
  const erreur = ref("");
  const message = ref("");

  async function charger() {
    chargement.value = true;
    erreur.value = "";
    try {
      const [c, i] = await Promise.all([
        getCandidaturesAvecEntretiens(),
        getIntervieweurs(),
      ]);
      candidatures.value = Array.isArray(c) ? c : [];
      intervieweurs.value = Array.isArray(i) ? i : [];
    } catch (e) {
      erreur.value =
        e.response?.data?.message ??
        "Impossible de charger les candidatures.";
    } finally {
      chargement.value = false;
    }
  }

  async function sauvegarder(payload) {
    try {
      await enregistrerEntretien(payload);
      message.value = "Entretien enregistré.";
      await charger();
      return { success: true };
    } catch (e) {
      return {
        success: false,
        message: e.response?.data?.message ?? "Échec de l'enregistrement.",
      };
    }
  }

  async function convoquer(idEntretien) {
    try {
      await envoyerConvocation(idEntretien);
      message.value = "Convocation envoyée par e-mail.";
      return { success: true };
    } catch (e) {
      return {
        success: false,
        message: e.response?.data?.message ?? "Échec de l'envoi.",
      };
    }
  }

  async function notifierResultat(idCandidature, decision) {
    try {
      await envoyerResultat(idCandidature, decision);
      message.value = "Résultat envoyé au candidat.";
      return { success: true };
    } catch (e) {
      return {
        success: false,
        message: e.response?.data?.message ?? "Échec de l'envoi.",
      };
    }
  }

  return {
    candidatures,
    intervieweurs,
    chargement,
    erreur,
    message,
    charger,
    sauvegarder,
    convoquer,
    notifierResultat,
  };
}