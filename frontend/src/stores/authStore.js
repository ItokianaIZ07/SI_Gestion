import { defineStore } from "pinia";
import { ref } from "vue";

class User{
    constructor(email, role){
        this.email = email;
        this.role = role;
    }
}

export const useAuthStore = defineStore('role', () =>{
    const chargee = ref(false);
    const estAuthentifie = ref(false);
    const user = ref(null);


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

    const logout = () =>{
        user.value = null;
        chargee.value = false;
        estAuthentifie.value = false;
    }

    const syncRole = (newRole) =>{
        addRole(newRole);
        return getRole();
    }

    const authentifie = (role, email) =>{
        user.value = new User(email, role);
        addRole(role);
        estAuthentifie.value = true;
    }

    const login = async (email, password) => {
        if(estAuthentifie.value){
            return {success: true};
        }

        const response = await fetch("/api/auth/login", {
            method: "POST",
            headers: {
                "Content-type": "application/json"
            },
            body: JSON.stringify({
                email: email,
                motDePasse: password,
            }),
        });
        if(!response.ok){
            const errorMessage =  await response.json().error;
            return {success: false,  message: errorMessage};
        }
        const data = await response.json();
        authentifie(data.role, email);

        return {success: true, message: "Authentification réussie!"}
    }

    return { login, logout, user, estAuthentifie };
},{persist: true}); 