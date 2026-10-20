<script setup>
import { ref } from "vue";
import "../assets/theme.css";

const username = ref("");
const password = ref("");
const loading = ref(false);
const errorMessage = ref("");
const showPassword = ref(false);

async function handleLogin() {
  errorMessage.value = "";

  if (!username.value.trim() || !password.value) {
    errorMessage.value = "Veuillez remplir tous les champs.";
    return;
  }

  console.log({
    username: username.value,
    password: password.value,
  });

  const response = await fetch("/api/login", {
    method: "POST",
    body: JSON.stringify({
      username: username.value,
      password: password.value,
    }),
  });

  if(!response.ok){
    errorMessage.value = await response.json().error;
    return;
  }

}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">
          <span>RH</span>
        </div>

        <h1>Connexion</h1>
        <p>Connectez-vous à votre espace de gestion</p>
      </div>

      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">Nom d'utilisateur</label>

          <input
            id="username"
            v-model="username"
            type="text"
            placeholder="Entrez votre nom d'utilisateur"
            autocomplete="username"
            required
          />
        </div>

        <div class="form-group">
          <label for="password">Mot de passe</label>

          <div class="password-input">
            <input
              id="password"
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="Entrez votre mot de passe"
              autocomplete="current-password"
              required
            />

            <button
              type="button"
              class="toggle-password"
              @click="showPassword = !showPassword"
              :aria-label="
                showPassword
                  ? 'Masquer le mot de passe'
                  : 'Afficher le mot de passe'
              "
            >
              <svg
                v-if="showPassword"
                xmlns="http://www.w3.org/2000/svg"
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M3 3l18 18" />
                <path d="M10.6 10.6a2 2 0 0 0 2.8 2.8" />
                <path
                  d="M9.9 5.2A11 11 0 0 1 12 5c5 0 9 7 9 7a15 15 0 0 1-4 4.5"
                />
                <path d="M6.6 6.6C4.1 8.2 3 12 3 12s4 7 9 7a9 9 0 0 0 3-.5" />
              </svg>

              <svg
                v-else
                xmlns="http://www.w3.org/2000/svg"
                width="20"
                height="20"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            </button>
          </div>
        </div>

        <p v-if="errorMessage" class="error-message" role="alert">
          {{ errorMessage }}
        </p>

        <button type="submit" class="login-button" :disabled="loading">
          {{ loading ? "Connexion en cours..." : "Se connecter" }}
        </button>
      </form>

      <p class="login-footer">Système de gestion des ressources humaines</p>
    </div>
  </div>
</template>

<style scoped>
/* Page */

.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  box-sizing: border-box;
  background-color: var(--color-background);
}

/* Carte */

.login-card {
  width: 100%;
  max-width: 420px;
  box-sizing: border-box;
  padding: 36px;
  background-color: var(--color-card);
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgb(0 0 0 / 5%);
}

/* En-tête */

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  width: 58px;
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 18px;
  border-radius: 14px;
  background-color: var(--color-primary);
  color: var(--color-card);
  font-size: 20px;
  font-weight: 700;
}

.login-header h1 {
  margin: 0 0 8px;
  color: #222222;
  font-size: 26px;
  font-weight: 700;
}

.login-header p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.5;
}

/* Formulaire */

form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  color: #374151;
  font-size: 14px;
  font-weight: 600;
}

.form-group input {
  width: 100%;
  box-sizing: border-box;
  padding: 12px 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  outline: none;
  background-color: var(--color-card);
  color: #222222;
  font-size: 14px;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.form-group input::placeholder {
  color: #9ca3af;
}

.form-group input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgb(1 95 9 / 10%);
}

/* Erreur */

.error-message {
  margin: -4px 0 0;
  padding: 10px 12px;
  border-radius: 8px;
  background-color: var(--color-danger-background);
  color: var(--color-danger);
  font-size: 13px;
}

/* Bouton */

.login-button {
  width: 100%;
  padding: 13px 16px;
  border: none;
  border-radius: 8px;
  background-color: var(--color-primary);
  color: var(--color-card);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition:
    background-color 0.2s,
    opacity 0.2s;
}

.login-button:hover:not(:disabled) {
  background-color: #014b07;
}

.login-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Pied de page */

.login-footer {
  margin: 28px 0 0;
  padding-top: 20px;
  border-top: 1px solid #e5e7eb;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
  text-align: center;
}

.password-input {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
}

.password-input input {
  width: 100%;
  padding-right: 46px;
}

.toggle-password {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);

  display: flex;
  align-items: center;
  justify-content: center;

  padding: 4px;
  border: none;
  background: transparent;
  color: #6b7280;
  cursor: pointer;

  transition: color 0.2s ease;
}

.toggle-password:hover {
  color: var(--color-primary);
}

.toggle-password:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
  border-radius: 4px;
}

.toggle-password svg {
  display: block;
  pointer-events: none;
}

/* Responsive */

@media (max-width: 480px) {
  .login-card {
    padding: 26px 20px;
  }

  .login-header h1 {
    font-size: 23px;
  }
}
</style>
