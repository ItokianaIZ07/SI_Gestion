<script setup>
import { computed, onMounted, ref } from 'vue'
import NotificationBesoinItem from '@/components/Manager/NotificationBesoinItem.vue'
import {
  chargerNotifications,
  marquerNotificationCommeLue,
  notifications,
} from '@/services/notificationStore.js'

const chargement = ref(true)
const erreur = ref('')
const notificationEnCours = ref(null)
const filtre = ref('TOUTES')
const notificationsFiltrees = computed(() => notifications.value.filter((notification) => {
  if (filtre.value === 'NON_LUES') return !notification.dateLecture
  if (filtre.value === 'LUES') return Boolean(notification.dateLecture)
  return true
}))
const nombreNonLues = computed(
  () => notifications.value.filter((notification) => !notification.dateLecture).length,
)
const nombreLues = computed(() => notifications.value.length - nombreNonLues.value)
const optionsFiltre = computed(() => [
  { valeur: 'TOUTES', libelle: 'Toutes', nombre: notifications.value.length },
  { valeur: 'NON_LUES', libelle: 'Non lues', nombre: nombreNonLues.value },
  { valeur: 'LUES', libelle: 'Lues', nombre: nombreLues.value },
])

async function charger() {
  chargement.value = true
  erreur.value = ''
  try {
    await chargerNotifications()
  } catch (error) {
    erreur.value = error.message || 'Impossible de charger les notifications.'
  } finally {
    chargement.value = false
  }
}

async function selectionnerNotification(notification) {
  if (notification.dateLecture || notificationEnCours.value === notification.id) return

  notificationEnCours.value = notification.id
  erreur.value = ''
  try {
    await marquerNotificationCommeLue(notification.id)
  } catch (error) {
    erreur.value = error.message || 'Impossible de marquer cette notification comme lue.'
  } finally {
    notificationEnCours.value = null
  }
}

onMounted(charger)
</script>

<template>
  <section class="page-notifications">
    <header class="entete">
      <div>
        <p class="sur-titre">Espace utilisateur</p>
        <h1>Notifications</h1>
        <p class="description">Suivez les changements d’état de vos demandes de besoins.</p>
      </div>
      <button class="actualiser" type="button" :disabled="chargement" @click="charger">
        {{ chargement ? 'Chargement…' : 'Actualiser' }}
      </button>
    </header>

    <p v-if="erreur" class="message message--erreur" role="alert">{{ erreur }}</p>
    <p v-if="chargement" class="message" role="status">Chargement des notifications…</p>
    <template v-else>
      <nav class="filtres" aria-label="Filtrer les notifications">
        <button
          v-for="option in optionsFiltre"
          :key="option.valeur"
          type="button"
          class="filtre"
          :class="{ 'filtre--actif': filtre === option.valeur }"
          :aria-pressed="filtre === option.valeur"
          @click="filtre = option.valeur"
        >
          {{ option.libelle }}
          <span>{{ option.nombre }}</span>
        </button>
      </nav>
      <div v-if="notificationsFiltrees.length" class="liste">
        <NotificationBesoinItem
          v-for="notification in notificationsFiltrees"
          :key="notification.id"
          :notification="notification"
          :disabled="notificationEnCours === notification.id"
          @selectionner="selectionnerNotification"
        />
      </div>
      <p v-else class="vide">
        {{ notifications.length
          ? 'Aucune notification ne correspond à ce filtre.'
          : 'Vous n’avez aucune notification pour le moment.' }}
      </p>
    </template>
  </section>
</template>

<style scoped>
.page-notifications {
  max-width: 1000px;
  margin: 0 auto;
}

.entete {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
}

.sur-titre {
  margin: 0 0 6px;
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: var(--color-ink);
  font-size: clamp(24px, 3vw, 32px);
}

.description {
  margin: 8px 0 0;
  color: var(--color-muted);
}

.actualiser {
  padding: 9px 14px;
  color: var(--color-primary);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.actualiser:disabled {
  cursor: wait;
  opacity: 0.65;
}

.liste {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filtres {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.filtre {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  color: var(--color-muted);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 999px;
  cursor: pointer;
}

.filtre--actif {
  color: var(--color-primary);
  background: var(--color-primary-soft);
  border-color: var(--color-primary);
  font-weight: 700;
}

.filtre span {
  font-size: 12px;
  font-weight: 700;
}

.message,
.vide {
  padding: 20px;
  color: var(--color-muted);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.message--erreur {
  color: var(--color-danger);
}

@media (max-width: 640px) {
  .entete {
    flex-direction: column;
  }
}
</style>
