<script setup>
defineProps({
  notification: {
    type: Object,
    required: true,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['selectionner'])

function formaterDate(date) {
  if (!date) return ''
  return new Intl.DateTimeFormat('fr-FR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(date))
}
</script>

<template>
  <button
    type="button"
    class="notification"
    :disabled="disabled"
    :class="{ 'notification--non-lue': !notification.dateLecture }"
    @click="$emit('selectionner', notification)"
  >
    <span class="notification__indicateur" aria-hidden="true">
      <span v-if="!notification.dateLecture" />
    </span>
    <span class="notification__contenu">
      <span class="notification__entete">
        <strong>{{ notification.titre }}</strong>
        <time v-if="notification.dateCreation" :datetime="notification.dateCreation">
          {{ formaterDate(notification.dateCreation) }}
        </time>
      </span>
      <span class="notification__message">{{ notification.message }}</span>
      <span v-if="!notification.dateLecture" class="notification__etat">Non lue</span>
    </span>
  </button>
</template>

<style scoped>
.notification {
  display: flex;
  width: 100%;
  gap: 14px;
  padding: 18px 20px;
  text-align: left;
  color: var(--color-ink);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: border-color 0.15s, background-color 0.15s;
}

.notification:hover {
  border-color: var(--color-primary);
}

.notification:disabled {
  cursor: wait;
  opacity: 0.7;
}

.notification--non-lue {
  background: var(--color-primary-soft);
}

.notification__indicateur {
  display: grid;
  place-items: center;
  flex: none;
  width: 24px;
  height: 24px;
}

.notification__indicateur span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--color-primary);
}

.notification__contenu {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 7px;
}

.notification__entete {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.notification__entete strong {
  font-size: 15px;
}

.notification time,
.notification__etat {
  flex: none;
  color: var(--color-muted);
  font-size: 12px;
}

.notification__message {
  color: var(--color-muted);
  font-size: 14px;
  line-height: 1.5;
}

.notification__etat {
  color: var(--color-primary);
  font-weight: 700;
}

@media (max-width: 640px) {
  .notification__entete {
    flex-direction: column;
    gap: 4px;
  }
}
</style>
