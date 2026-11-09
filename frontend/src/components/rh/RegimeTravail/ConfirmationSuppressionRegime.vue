<script setup>
defineProps({
  regime: { type: Object, default: null },
  envoi: { type: Boolean, default: false },
});

const emit = defineEmits(["confirmer", "annuler"]);
</script>

<template>
  <div v-if="regime" class="rt-overlay" @click.self="emit('annuler')">
    <section
      class="rt-confirmation"
      role="dialog"
      aria-modal="true"
      aria-labelledby="rt-confirmation-titre"
    >
      <h2 id="rt-confirmation-titre">Supprimer le régime ?</h2>
      <p>
        Confirmez-vous la suppression de
        <strong>{{ regime.libelle }}</strong> ({{ regime.code }}) ?
      </p>
      <div class="rt-confirmation__actions">
        <button class="g-btn" type="button" :disabled="envoi" @click="emit('annuler')">
          Annuler
        </button>
        <button
          class="g-btn g-btn--danger"
          type="button"
          :disabled="envoi"
          @click="emit('confirmer')"
        >
          {{ envoi ? "Suppression…" : "Supprimer" }}
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.rt-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgb(0 0 0 / 42%);
}

.rt-confirmation {
  width: min(460px, 100%);
  padding: 24px;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.rt-confirmation h2 {
  margin: 0 0 12px;
  color: var(--color-ink);
  font-size: 1.2rem;
}

.rt-confirmation p {
  margin: 0;
  color: var(--color-muted);
  line-height: 1.6;
}

.rt-confirmation__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 24px;
}
</style>
