<script setup>
import { ref, watch, onBeforeUnmount } from "vue";

const props = defineProps({
  message: { type: String, default: "" },
  type: { type: String, default: "erreur" } // 'erreur' | 'succes'
});

const emit = defineEmits(["fermer"]);

const visible = ref(false);
let timer = null;

watch(
  () => props.message,
  (nouveauMessage) => {
    if (timer) {
      clearTimeout(timer);
      timer = null;
    }

    visible.value = !!nouveauMessage;

    if (nouveauMessage) {
      timer = setTimeout(() => {
        visible.value = false;
        timer = null;
      }, 3000);
    }
  },
  { immediate: true }
);

watch(visible, (estVisible) => {
  if (!estVisible && props.message) {
    emit("fermer");
  }
});

onBeforeUnmount(() => {
  if (timer) {
    clearTimeout(timer);
  }
});
</script>

<template>
  <Teleport to="body">
    <Transition name="alerte">
      <div
        v-if="visible && message"
        class="g-alerte"
        :class="`g-alerte--${type}`"
        role="alert"
        aria-live="polite"
      >
        <!-- Icône -->
        <div class="g-alerte__icone" aria-hidden="true">
          <!-- Succès -->
          <svg
            v-if="type === 'succes'"
            viewBox="0 0 24 24"
            fill="none"
          >
            <path
              d="M5 12.5L9.5 17L19 7"
              stroke="currentColor"
              stroke-width="2.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>

          <!-- Erreur -->
          <svg
            v-else
            viewBox="0 0 24 24"
            fill="none"
          >
            <path
              d="M12 8V13"
              stroke="currentColor"
              stroke-width="2.5"
              stroke-linecap="round"
            />
            <circle
              cx="12"
              cy="16.5"
              r="1"
              fill="currentColor"
            />
            <path
              d="M10.3 3.9L2.7 17.1C1.9 18.5 2.9 20.2 4.5 20.2H19.5C21.1 20.2 22.1 18.5 21.3 17.1L13.7 3.9C12.9 2.5 11.1 2.5 10.3 3.9Z"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linejoin="round"
            />
          </svg>
        </div>

        <!-- Contenu -->
        <div class="g-alerte__contenu">
          <strong class="g-alerte__titre">
            {{ type === "succes" ? "Succès" : "Une erreur est survenue" }}
          </strong>

          <p class="g-alerte__message">
            {{ message }}
          </p>
        </div>

        <!-- Barre de progression -->
        <div class="g-alerte__progression"></div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* Conteneur principal */
.g-alerte {
  --alerte-couleur: #015f09;
  --alerte-fond: #ffffff;
  --alerte-bordure: #80af84;

  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 99999;

  display: flex;
  align-items: flex-start;
  gap: 14px;

  width: min(420px, calc(100vw - 32px));
  box-sizing: border-box;
  padding: 18px 20px;

  color: #242824;
  background: var(--alerte-fond);
  border: 1px solid var(--alerte-bordure);
  border-left: 5px solid var(--alerte-couleur);
  border-radius: 12px;

  box-shadow:
    0 12px 32px rgb(0 0 0 / 12%),
    0 3px 8px rgb(0 0 0 / 5%);

  overflow: hidden;
}

/* Notification de succès */
.g-alerte--succes {
  --alerte-couleur: #015f09;
  --alerte-fond: #ffffff;
  --alerte-bordure: #80af84;
}

/* Notification d'erreur */
.g-alerte--erreur {
  --alerte-couleur: #cd3d3d;
  --alerte-fond: #ffffff;
  --alerte-bordure: #ffc1c1;
}

/* Icône */
.g-alerte__icone {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  width: 38px;
  height: 38px;
  color: var(--alerte-couleur);
  background: color-mix(in srgb, var(--alerte-couleur) 10%, white);
  border-radius: 50%;
}

.g-alerte__icone svg {
  width: 22px;
  height: 22px;
}

/* Texte */
.g-alerte__contenu {
  flex: 1;
  min-width: 0;
  padding-top: 1px;
}

.g-alerte__titre {
  display: block;
  margin-bottom: 5px;
  color: var(--alerte-couleur);
  font-size: 14px;
  font-weight: 700;
}

.g-alerte__message {
  margin: 0;
  color: #4b514b;
  font-size: 13px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

/* Barre de progression : 3 secondes */
.g-alerte__progression {
  position: absolute;
  bottom: 0;
  left: 0;

  width: 100%;
  height: 3px;

  background: var(--alerte-couleur);
  transform-origin: left;
  animation: alerte-progression 3s linear forwards;
}

@keyframes alerte-progression {
  from {
    transform: scaleX(1);
  }

  to {
    transform: scaleX(0);
  }
}

/* Animation d'entrée et de sortie */
.alerte-enter-active,
.alerte-leave-active {
  transition:
    opacity 0.3s ease,
    transform 0.3s ease;
}

.alerte-enter-from,
.alerte-leave-to {
  opacity: 0;
  transform: translateX(35px);
}

/* Petits écrans */
@media (max-width: 480px) {
  .g-alerte {
    top: 12px;
    right: 12px;
    width: calc(100vw - 24px);
    padding: 15px;
  }
}

/* Respect des préférences d'accessibilité */
@media (prefers-reduced-motion: reduce) {
  .alerte-enter-active,
  .alerte-leave-active {
    transition: none;
  }

  .g-alerte__progression {
    animation: none;
  }
}
</style>
