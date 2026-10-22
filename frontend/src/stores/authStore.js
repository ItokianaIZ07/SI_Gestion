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
    "auth",
    () => {
        const chargee = ref(false);
        const estAuthentifie = ref(false);
        const user = ref(null);

        // Effacer la session locale
        const clearSession = () => {
            user.value = null;
            estAuthentifie.value = false;
            chargee.value = false;
        };

        // Enregistrer l'utilisateur authentifié
        const authentifie = (role, email) => {
            user.value = new User(email, role);
            estAuthentifie.value = true;
            chargee.value = true;
        };

        // Ajouter ou modifier le rôle
        const addRole = (newRole, force = false) => {
            if (!user.value) {
                return;
            }

            if (!force && chargee.value) {
                return;
            }

            if (typeof newRole !== "string" || !newRole.trim()) {
                return;
            }

            user.value.role = newRole.toUpperCase();
            chargee.value = true;
        };

        // Récupérer le rôle de l'utilisateur
        const getRole = () => {
            return user.value?.role ?? null;
        };

        // Synchroniser le rôle
        const syncRole = (newRole) => {
            addRole(newRole);
            return getRole();
        };

        // Connexion
        const login = async (email, password) => {
            try {
                const { data } = await api.post("api/auth/login", {
                    email,
                    motDePasse: password,
                });

                authentifie(data.role, email);

                return {
                    success: true,
                    message: "Authentification réussie !",
                };
            } catch (error) {
                if (error.response) {
                    return {
                        success: false,
                        message:
                            error.response.data?.message ??
                            "Email ou mot de passe incorrect",
                    };
                }

                throw error;
            }
        };

        // Restaurer la session depuis le backend
        const restaurerSession = async () => {
            try {
                const { data } = await api.get("api/auth/me");

                authentifie(
                    data.role,
                    data.email ?? user.value?.email ?? ""
                );

                return true;
            } catch (error) {
                if (error.response?.status === 401) {
                    clearSession();
                    return false;
                }

                throw error;
            }
        };

        // Déconnexion
        const logout = async () => {
            try {
                await api.post("api/auth/logout");

                return { success: true };
            } catch (error) {
                return {
                    success: false,
                    message:
                        error.response?.data?.message ??
                        "La déconnexion a échoué",
                };
            } finally {
                clearSession();
            }
        };

        return {
            chargee,
            user,
            estAuthentifie,
            login,
            logout,
            restaurerSession,
            clearSession,
            addRole,
            getRole,
            syncRole,
        };
    },
    {
        persist: true,
    }
);