<script setup>
import { ref } from "vue";
import { useAuthStore } from "@/stores/authStore"; 
import "../assets/theme.css";
import { useRouter } from "vue-router";

const email = ref("direction@exemple.mg");
const password = ref("Test1234!");
const loading = ref(false);
const errorMessage = ref("");
const showPassword = ref(false);
const store = useAuthStore();
const router = useRouter();

async function handleLogin() {
  errorMessage.value = "";

  if (!email.value.trim() || !password.value) {
    errorMessage.value = "Veuillez remplir tous les champs.";
    return;
  }

  loading.value = true;

  try {
    const response = await store.login(
      email.value.trim(),
      password.value
    );

    if (!response.success) {
      errorMessage.value = response.message;
      return;
    }

    await router.replace("/si/app");
    console.log("Connexion réussie");
  } catch (error) {
    console.error("Erreur de connexion :", error);
    errorMessage.value =
      "Impossible de contacter le serveur. Réessayez.";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login-page">
    <!-- Panneau visuel -->
    <aside class="login-visuel" aria-hidden="false">
      <div class="login-visuel__marque">
        <span class="login-visuel__logo">RH</span>
        <span>Gestion RH</span>
      </div>

      <div class="login-visuel__contenu">
        <h2>Pilotez vos recrutements,<br />de la demande à l'embauche.</h2>
        <p>Besoins, offres, candidatures, entretiens et contrats : un seul espace, clair et sécurisé.</p>

        <ul class="login-visuel__liste">
          <li><span class="coche">✓</span> Suivi complet de chaque recrutement</li>
          <li><span class="coche">✓</span> Candidatures et CV centralisés</li>
          <li><span class="coche">✓</span> Contrats et signatures en ligne</li>
        </ul>
      </div>

      <svg class="login-visuel__illu" viewBox="0 0 420 250" fill="none" aria-hidden="true">
        <rect x="40" y="20" width="340" height="200" rx="18" fill="#fff" fill-opacity=".97" />
        <rect x="40" y="20" width="340" height="40" rx="18" fill="#eef6ee" />
        <rect x="40" y="46" width="340" height="14" fill="#eef6ee" />
        <circle cx="62" cy="40" r="5" fill="#cfe3d1" /><circle cx="80" cy="40" r="5" fill="#cfe3d1" /><circle cx="98" cy="40" r="5" fill="#cfe3d1" />
        <rect x="60" y="80" width="90" height="64" rx="10" fill="#eef6ee" />
        <rect x="72" y="92" width="34" height="8" rx="4" fill="#80af84" />
        <rect x="72" y="110" width="50" height="14" rx="5" fill="#015f09" />
        <rect x="165" y="80" width="90" height="64" rx="10" fill="#eef6ee" />
        <rect x="177" y="92" width="34" height="8" rx="4" fill="#80af84" />
        <rect x="177" y="110" width="40" height="14" rx="5" fill="#015f09" />
        <rect x="270" y="80" width="90" height="64" rx="10" fill="#eef6ee" />
        <rect x="282" y="92" width="34" height="8" rx="4" fill="#80af84" />
        <rect x="282" y="110" width="56" height="14" rx="5" fill="#015f09" />
        <path d="M60 196l36-26 34 14 44-36 40 18 50-34 56 20" stroke="#80af84" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" />
        <circle cx="344" cy="170" r="6" fill="#015f09" />
        <circle cx="352" cy="210" r="30" fill="#80af84" /><path d="M340 210l9 9 16-18" stroke="#fff" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" />
      </svg>
    </aside>

    <!-- Panneau formulaire -->
    <div class="login-panneau">
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
          <label for="email">Email</label>

          <input
            id="email"
            v-model="email"
            type="text"
            placeholder="Entrez votre email"
            autocomplete="email"
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
  </div>
</template>

<style scoped>
/* Page : deux panneaux */

.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 1fr);
  background: var(--color-card);
}

/* Panneau visuel (vert) */

.login-visuel {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 32px;
  padding: clamp(32px, 4vw, 56px);
  overflow: hidden;
  color: #fff;
  background:
    radial-gradient(520px 360px at 105% -5%, rgba(255, 255, 255, 0.14), transparent 70%),
    radial-gradient(420px 300px at -10% 105%, rgba(128, 175, 132, 0.35), transparent 70%),
    linear-gradient(145deg, #013d06 0%, #015f09 55%, #0b7c16 100%);
}

.login-visuel::after {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: radial-gradient(rgba(255, 255, 255, 0.09) 1px, transparent 1px);
  background-size: 22px 22px;
  mask-image: linear-gradient(180deg, transparent, #000 30%, transparent);
}

.login-visuel > * { position: relative; z-index: 1; }

.login-visuel__marque { display: flex; align-items: center; gap: 12px; font-weight: 700; font-size: 1.1rem; }

.login-visuel__logo {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 11px;
  background: #fff;
  color: var(--color-primary);
  font-size: 15px;
  font-weight: 800;
}

.login-visuel__contenu h2 { margin: 0 0 14px; color: #fff; font-size: clamp(1.6rem, 2.6vw, 2.2rem); line-height: 1.2; }
.login-visuel__contenu p { margin: 0; max-width: 46ch; color: rgba(255, 255, 255, 0.82); font-size: 1rem; }

.login-visuel__liste { list-style: none; margin: 24px 0 0; padding: 0; display: grid; gap: 12px; }
.login-visuel__liste li { display: flex; align-items: center; gap: 12px; font-weight: 500; color: rgba(255, 255, 255, 0.92); }

.coche {
  display: grid;
  place-items: center;
  flex: none;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
  font-size: 12px;
  font-weight: 700;
}

.login-visuel__illu { width: min(420px, 100%); height: auto; align-self: center; filter: drop-shadow(0 18px 30px rgba(0, 30, 4, 0.35)); }

/* Panneau formulaire (blanc) */

.login-panneau {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  background:
    radial-gradient(600px 260px at 50% -8%, var(--color-primary-soft), transparent 70%),
    var(--color-card);
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 8px 0;
  animation: entree-login 0.5s ease both;
}

@keyframes entree-login { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: none; } }

/* En-tête */

.login-header {
  text-align: left;
  margin-bottom: 28px;
}

.login-logo {
  width: 58px;
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 0 20px;
  border-radius: 14px;
  box-shadow: 0 8px 18px rgba(1, 95, 9, 0.28);
  background-color: var(--color-primary);
  color: var(--color-card);
  font-size: 20px;
  font-weight: 700;
}

.login-header h1 {
  margin: 0 0 8px;
  color: var(--color-ink);
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
  border: 1px solid var(--color-border-strong);
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
  box-shadow: var(--shadow-focus);
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
  background-color: var(--color-primary-hover);
}

.login-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Pied de page */

.login-footer {
  margin: 28px 0 0;
  padding-top: 20px;
  border-top: 1px solid var(--color-border);
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

@media (max-width: 900px) {
  .login-page { grid-template-columns: 1fr; }
  .login-visuel { display: none; }
  .login-panneau { min-height: 100vh; }
}


@media (max-width: 480px) {
  .login-card {
    padding: 0;
  }

  .login-header h1 {
    font-size: 23px;
  }
}
</style>
