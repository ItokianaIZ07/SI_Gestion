<script setup>
// Page 5 : connexion de l'employé (e-mail + matricule + numéro de CIN).
// POST /api/auth/employe/login puis GET /api/auth/employe/me (dans authStore.loginEmploye).
import { ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import '@/assets/theme.css'

const auth = useAuthStore()
const router = useRouter()

const email = ref('')
const matricule = ref('')
const numeroCin = ref('')
const afficherCin = ref(false)
const chargement = ref(false)
const erreur = ref('')

async function seConnecter() {
  erreur.value = ''
  if (!email.value.trim() || !matricule.value.trim() || !numeroCin.value.trim()) {
    erreur.value = 'Veuillez remplir tous les champs.'
    return
  }
  chargement.value = true
  try {
    const r = await auth.loginEmploye({
      email: email.value.trim(),
      matricule: matricule.value.trim(),
      numeroCin: numeroCin.value.trim()
    })
    if (!r.success) { erreur.value = r.message; return }
    await router.replace('/si/app/employe/conges')
  } catch {
    erreur.value = 'Impossible de contacter le serveur. Réessayez.'
  } finally {
    chargement.value = false
  }
}
</script>

<template>
  <main class="emp-page">
    <section class="emp-carte" aria-labelledby="emp-titre">
      <div class="emp-marque"><span class="emp-logo">RH</span><span>Gestion RH</span></div>
      <h1 id="emp-titre">Espace employé</h1>
      <p class="emp-sous">Connectez-vous pour consulter vos congés et faire une demande.</p>

      <form novalidate @submit.prevent="seConnecter">
        <label class="emp-champ">E-mail
          <input v-model="email" type="email" autocomplete="email" placeholder="prenom.nom@entreprise.mg" required>
        </label>
        <label class="emp-champ">Matricule
          <input v-model="matricule" type="text" autocomplete="username" placeholder="EMP001" required>
        </label>
        <label class="emp-champ">Numéro de CIN
          <span class="emp-cin">
            <input v-model="numeroCin" :type="afficherCin ? 'text' : 'password'" autocomplete="off" required>
            <button type="button" class="emp-voir" :aria-pressed="afficherCin" @click="afficherCin = !afficherCin">
              {{ afficherCin ? 'Masquer' : 'Afficher' }}
            </button>
          </span>
        </label>

        <p v-if="erreur" class="emp-erreur" role="alert">{{ erreur }}</p>
        <button type="submit" class="emp-bouton" :disabled="chargement">{{ chargement ? 'Connexion…' : 'Se connecter' }}</button>
      </form>

      <p class="emp-lien">Vous êtes RH, manager, finance ou direction ? <RouterLink to="/">Connexion du personnel de gestion</RouterLink></p>
    </section>
  </main>
</template>

<style scoped>
.emp-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; background: var(--color-background, #fafbfa); font-family: inherit; }
.emp-carte { width: min(430px, 100%); background: var(--color-card, #fff); border: 1px solid var(--color-border, #e6ebe6); border-radius: 16px; padding: 32px; box-shadow: 0 10px 30px rgba(1, 40, 8, .08); }
.emp-marque { display: flex; align-items: center; gap: 10px; font-weight: 700; color: var(--color-primary, #015f09); margin-bottom: 20px; }
.emp-logo { background: var(--color-primary, #015f09); color: #fff; border-radius: 8px; padding: 4px 8px; font-size: 14px; }
h1 { margin: 0 0 6px; font-size: 1.5rem; color: var(--color-ink, #17211a); }
.emp-sous { margin: 0 0 22px; color: var(--color-muted, #6a766d); }
form { display: grid; gap: 14px; }
.emp-champ { display: grid; gap: 6px; font-size: 13.5px; font-weight: 600; color: var(--color-ink, #17211a); }
.emp-champ input { min-height: 42px; padding: 0 12px; border: 1px solid var(--color-border-strong, #d3dad4); border-radius: 8px; font: inherit; font-weight: 400; background: #fff; }
.emp-champ input:focus { outline: 2px solid var(--color-primary, #015f09); outline-offset: 1px; }
.emp-cin { display: flex; gap: 8px; }
.emp-cin input { flex: 1; min-width: 0; }
.emp-voir { border: 1px solid var(--color-border-strong, #d3dad4); background: #fff; border-radius: 8px; padding: 0 12px; cursor: pointer; font: inherit; font-size: 13px; }
.emp-erreur { margin: 0; padding: 10px 12px; border-radius: 8px; background: #fdeeee; color: #9b1c13; font-size: 14px; }
.emp-bouton { min-height: 44px; border: 0; border-radius: 8px; background: var(--color-primary, #015f09); color: #fff; font: inherit; font-weight: 600; cursor: pointer; }
.emp-bouton:disabled { opacity: .6; cursor: wait; }
.emp-lien { margin: 20px 0 0; font-size: 13px; color: var(--color-muted, #6a766d); }
.emp-lien a { color: var(--color-primary, #015f09); font-weight: 600; }
</style>
