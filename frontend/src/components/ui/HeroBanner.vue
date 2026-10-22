<script setup>
defineProps({
  eyebrow: { type: String, default: "" },
  titre: { type: String, required: true },
  texte: { type: String, default: "" },
});
</script>

<template>
  <section class="hero">
    <div class="hero__texte">
      <p v-if="eyebrow" class="hero__eyebrow">{{ eyebrow }}</p>
      <h1>{{ titre }}</h1>
      <p v-if="texte" class="hero__p">{{ texte }}</p>
      <div class="hero__slot"><slot /></div>
    </div>

    <!-- Illustration : poste, candidats, validation -->
    <svg class="hero__illu" viewBox="0 0 320 220" fill="none" aria-hidden="true">
      <circle cx="250" cy="60" r="86" fill="#fff" fill-opacity=".07" />
      <circle cx="290" cy="190" r="54" fill="#fff" fill-opacity=".06" />
      <rect x="56" y="38" width="170" height="128" rx="14" fill="#fff" />
      <rect x="74" y="58" width="64" height="9" rx="4.5" fill="#015f09" />
      <rect x="74" y="76" width="118" height="6" rx="3" fill="#cfe3d1" />
      <rect x="74" y="90" width="96" height="6" rx="3" fill="#e6ebe6" />
      <rect x="74" y="116" width="52" height="24" rx="12" fill="#eef6ee" />
      <rect x="132" y="116" width="52" height="24" rx="12" fill="#eef6ee" />
      <circle cx="226" cy="150" r="26" fill="#80af84" />
      <path d="M214 150l8 8 15-17" stroke="#fff" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" />
      <g>
        <circle cx="60" cy="176" r="19" fill="#fff" stroke="#015f09" stroke-width="3" />
        <circle cx="60" cy="171" r="6" fill="#015f09" />
        <path d="M48 187c2-7 22-7 24 0" stroke="#015f09" stroke-width="3" stroke-linecap="round" />
        <circle cx="96" cy="184" r="15" fill="#eef6ee" stroke="#80af84" stroke-width="3" />
        <circle cx="96" cy="180" r="5" fill="#80af84" />
        <path d="M86 192c2-5 18-5 20 0" stroke="#80af84" stroke-width="3" stroke-linecap="round" />
      </g>
    </svg>
  </section>
</template>

<style scoped>
.hero {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  width: 100%;
  padding: clamp(24px, 4vw, 40px) clamp(24px, 4vw, 44px);
  overflow: hidden;
  color: #fff;
  background: linear-gradient(115deg, #014d07 0%, #015f09 50%, #0b7c16 100%);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}

.hero__texte { position: relative; z-index: 1; max-width: 520px; }
.hero__eyebrow { margin: 0 0 8px; font-size: 12px; font-weight: 600; letter-spacing: .1em; text-transform: uppercase; color: rgba(255, 255, 255, .7); }
.hero h1 { margin: 0 0 10px; color: #fff; font-size: clamp(1.5rem, 3vw, 2.1rem); line-height: 1.15; }
.hero__p { margin: 0; color: rgba(255, 255, 255, .85); font-size: 1rem; }
.hero__slot { margin-top: 20px; }
.hero__slot:empty { display: none; }
.hero__illu { flex: none; width: min(300px, 38%); height: auto; }

@media (max-width: 720px) {
  .hero__illu { display: none; }
}
</style>
