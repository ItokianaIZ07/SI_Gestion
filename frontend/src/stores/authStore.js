import { defineStore } from "pinia";
import { ref } from "vue";

class User{
    constructor(username, role){
        this.username = username;
        this.role = role;
    }
}

export const authStore = defineStore('role', () =>{
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
        chargee = true;
    }

    const getRole = () =>{
        return user.value.role;
    }

    const logout = () =>{
        user.value = null;
        chargee = false;
        estAuthentifie = false;
    }

    const syncRole = (newRole) =>{
        addRole(newRole);
        return getRole();
    }

    const authentifie = (role, username) =>{
        syncRole(role);
        user.value = new User(username, role);
        estAuthentifie.value = true;
    }

    const login = async (username, password) => {
        if(estAuthentifie.value){
            return;
        }
        const response = await fetch("/api/login", {
            method: "POST",
            body: JSON.stringify({
                username: username,
                password: password,
            }),
        });
        if(!response.ok){
            const errorMessage =  await response.json().error;
            return {success: false,  message: errorMessage};
        }
        const data = await response.json();
        authentifie(data.role, username);
    }

    return { login, logout };
}); 