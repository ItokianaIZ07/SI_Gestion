<script setup>
import { ref, onMounted, onUnmounted } from "vue";
import SignaturePadLib from "signature_pad";

const canvas = ref(null);
const pad = ref(null);
const emit = defineEmits(["validate", "cancel"]);

onMounted(() => {
  pad.value = new SignaturePadLib(canvas.value, {
    penColor: "#1a1a1a",
    minWidth: 0.8,
    maxWidth: 2.2,
    backgroundColor: "rgba(255,255,255,0)",
  });
});

onUnmounted(() => {
  pad.value = null;
});

function effacer() {
  pad.value.clear();
}

function valider() {
  if (pad.value.isEmpty()) {
    alert("Veuillez signer avant de valider.");
    return;
  }
  emit("validate", pad.value.toDataURL("image/png"));
}

function annuler() {
  emit("cancel");
}
</script>

<template>
  <div class="sig-overlay">
    <div class="sig-card">
      <h3 class="sig-title">Signez dans le cadre ci-dessous</h3>
      <p class="sig-hint">Utilisez la souris ou le doigt pour tracer votre signature.</p>

      <canvas ref="canvas" class="sig-canvas"></canvas>

      <div class="sig-actions">
        <button type="button" class="sig-btn sig-btn--ghost" @click="annuler">
          Annuler
        </button>
        <button type="button" class="sig-btn sig-btn--ghost" @click="effacer">
          Effacer
        </button>
        <button type="button" class="sig-btn sig-btn--primary" @click="valider">
          Valider ma signature
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sig-overlay {
  position: fixed;
  inset: 0;
  z-index: 999;
  display: grid;
  place-items: center;
  background: rgba(0, 0, 0, 0.45);
  padding: 20px;
}

.sig-card {
  width: 100%;
  max-width: 560px;
  background: #fff;
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.25);
}

.sig-title { margin: 0 0 6px; font-size: 17px; color: #292e2a; }
.sig-hint { margin: 0 0 16px; font-size: 12px; color: #818781; }

.sig-canvas {
  display: block;
  width: 100%;
  height: 200px;
  border: 2px dashed #bfbfbf;
  border-radius: 10px;
  background: #fff;
  touch-action: none;
  cursor: crosshair;
}

.sig-actions {
  display: flex;
  gap: 10px;
  margin-top: 18px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.sig-btn {
  padding: 10px 18px;
  border-radius: 8px;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.sig-btn--ghost { border: 1px solid #d4d4d4; background: #fff; color: #292e2a; }
.sig-btn--ghost:hover { background: #f7f7f7; }
.sig-btn--primary { border: 1px solid #015f09; background: #015f09; color: #fff; }
.sig-btn--primary:hover { background: #014b07; }
</style>