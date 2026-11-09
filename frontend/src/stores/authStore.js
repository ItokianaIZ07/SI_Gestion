import { defineStore } from "pinia";
import { ref } from "vue";
import api from "@/services/api.js";

class User {
  constructor(email, role) {
    this.email = email;
    this.role = role;
  }
}

export const useAuthStore = defineStore(
  "role",
  () => {
    const clearSession = () => {
      user.value = null;
      employe.value = null;
      estAuthentifie.value = false;
    };

    const chargee = ref(false);
    const estAuthentifie = ref(false);
    const user = ref(null);
    // Identité de l'employé connecté (rôle EMPLOYE) : { id, matricule, nom, prenom, email }
    const employe = ref(null);

    const addRole = (newRole, force = false) => {
      if (!force && chargee) {
        return;
      }
      if (newRole.trim() === "" && user.value.role.trim() !== "") {
        return;
      }
      user.value.role = newRole.toUpperCase();
      chargee.value = true;
    };

    const getRole = () => {
      return user.value.role;
    };

    const logout = async () => {
      user.value = null;
      employe.value = null;
      chargee.value = false;
      estAuthentifie.value = false;

      const response = await fetch("/api/auth/logout", {
        method: "POST",
      });

      if (!response.ok) {
        throw new Error("La déconnexion a échoué");
      }

      return { success: true };
    };

    const syncRole = (newRole) => {
      addRole(newRole);
      return getRole();
    };

    const authentifie = (role, email) => {
      user.value = new User(email, role);
      estAuthentifie.value = true;
    };

    const restaurerSession = async () => {
      try {
        const { data } = await api.get("/auth/me");
        if (data.role?.toUpperCase() === "EMPLOYE") {
          // L'identité de l'employé (son id) n'est pas dans /auth/me : on la relit.
          const { data: moi } = await api.get("/auth/employe/me");
          employe.value = moi.employe;
          authentifie(data.role, moi.employe?.email ?? "");
        } else {
          authentifie(data.role, user.value?.email ?? "");
        }
        return true;
      } catch (error) {
        if (error.response?.status === 401) {
          clearSession();
          return false;
        }

        throw error;
      }
    };

    const login = async (email, password) => {
      try {
        const { data } = await api.post("/auth/login", {
          email,
          motDePasse: password,
        });
        authentifie(data.role, email);
        return { success: true, message: "Authentification réussie!" };
      } catch (error) {
        if (error.response) {
          return {
            success: false,
            message:
              error.response.data?.message ?? "Email ou mot de passe incorrect",
          };
        }

        throw error;
      }
    };

    // Connexion de l'employé : e-mail + matricule + numéro de CIN (sans mot de passe).
    const loginEmploye = async ({ email, matricule, numeroCin }) => {
      try {
        const { data } = await api.post("/auth/employe/login", {
          email,
          matricule,
          numeroCin,
        });
        if (!data?.success || !data.employe) {
          return { success: false, message: data?.message ?? "Identifiants incorrects." };
        }
        employe.value = data.employe;
        authentifie(data.role ?? "EMPLOYE", data.employe.email ?? email);
        return { success: true };
      } catch (error) {
        // `api` renvoie une ApiError (status, message) ; on accepte aussi une erreur axios brute.
        const statut = error.status ?? error.response?.status;
        if (statut) {
          return {
            success: false,
            message:
              statut === 401
                ? "E-mail, matricule ou numéro de CIN incorrect."
                : error.response?.data?.message ?? error.message ?? "Connexion impossible.",
          };
        }
        throw error;
      }
    };

    return {
      login,
      loginEmploye,
      logout,
      restaurerSession,
      clearSession,
      user,
      employe,
      estAuthentifie,
    };
  },
  { persist: true },
);
