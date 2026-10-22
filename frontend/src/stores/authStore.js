import { defineStore } from "pinia";
import { ref } from "vue";
import api from "@/services/api.js";

class User{
    constructor(email, role){
        this.email = email;
        this.role = role;
    }
}

export const useAuthStore = defineStore('role', () =>{
    const estAuthentifie = ref(false);
    const user = ref(null);

    const clearSession = () => {
        user.value = null;
        estAuthentifie.value = false;
    };

    // const logout = async () =>{
    //     try {
    //         await api.post("/auth/logout");
    //     } finally {
    //         clearSession();
    //     }
    //     return { success: true };
    // }
    const chargee = ref(false);
    // const estAuthentifie = ref(false);
    // const user = ref(null);


    const addRole = (newRole, force = false) => {
        if(!force && chargee){
            return;
        }
        if(newRole.trim() === "" && user.value.role.trim() !== ""){
            return;
        }
        user.value.role = newRole.toUpperCase();
        chargee.value = true;
    }

    const getRole = () =>{
        return user.value.role;
    }

    const logout = async () =>{
        user.value = null;
        chargee.value = false;
        estAuthentifie.value = false;

        const response = await fetch("/api/auth/logout", {
            method: "POST"
        });

        if (!response.ok) {
            throw new Error('La déconnexion a échoué'); 
        }

        return {success: true}
    }

    const syncRole = (newRole) =>{
        addRole(newRole);
        return getRole();
    }

    const authentifie = (role, email) =>{
        user.value = new User(email, role);
        estAuthentifie.value = true;
    }

    const restaurerSession = async () => {
        try {
            const { data } = await api.get("/auth/me");
            authentifie(data.role, user.value?.email ?? "");
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
                    message: error.response.data?.message ?? "Email ou mot de passe incorrect",
                };
            }

            throw error;
        }
    }

    return { login, logout, restaurerSession, clearSession, user, estAuthentifie };
},{persist: true});
