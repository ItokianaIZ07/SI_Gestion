<script setup>
defineProps({
  budget: { type: Object, required: true },
})

const ariary = (montant) =>
  new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency: 'MGA',
    maximumFractionDigits: 0,
  }).format(montant ?? 0)

const pourcentageUtilise = (budget) => {
  const total = Number(budget.montant) || 0
  if (total <= 0) return 0
  return Math.max(0, Math.min(100, Math.round(
    ((Number(budget.masseSalariale) + Number(budget.depenses)) / total) * 100,
  )))
}
</script>

<template>
  <section class="budget">
    <div class="budget__heading">
      <div>
        <p class="eyebrow">Suivi financier</p>
        <h2>Budget départemental {{ budget.annee }}</h2>
      </div>
      <span class="usage">{{ pourcentageUtilise(budget) }} % utilisé</span>
    </div>

    <div class="budget__bar" role="img" :aria-label="`${pourcentageUtilise(budget)} % du budget utilisé`">
      <span :style="{ width: `${pourcentageUtilise(budget)}%` }"></span>
    </div>

    <div class="budget__stats">
      <article>
        <span>Budget alloué</span>
        <strong>{{ ariary(budget.montant) }}</strong>
      </article>
      <article>
        <span>Masse salariale</span>
        <strong>{{ ariary(budget.masseSalariale) }}</strong>
      </article>
      <article>
        <span>Dépenses internes</span>
        <strong>{{ ariary(budget.depenses) }}</strong>
      </article>
      <article>
        <span>Budget restant</span>
        <strong>{{ ariary(budget.restant) }}</strong>
      </article>
    </div>
    <p class="budget__note">Le budget restant correspond au montant alloué moins la masse salariale et les dépenses internes.</p>
  </section>
</template>

<style scoped>
.budget { display: grid; gap: 16px; padding: 22px; border: 1px solid var(--color-border); border-radius: var(--radius-lg); background: var(--color-card); box-shadow: var(--shadow-sm); }
.budget__heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.eyebrow { margin: 0 0 4px; color: var(--color-muted); font-size: .8rem; font-weight: 700; text-transform: uppercase; letter-spacing: .08em; }
h2 { margin: 0; font-size: 1.3rem; }
.usage { color: #015f09; font-weight: 700; white-space: nowrap; }
.budget__bar { height: 10px; overflow: hidden; border-radius: 999px; background: #e8eee8; }
.budget__bar span { display: block; height: 100%; border-radius: inherit; background: #168326; transition: width .2s ease; }
.budget__stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: 12px; }
.budget__stats article { display: grid; gap: 8px; padding: 16px; border-radius: var(--radius-md); background: var(--color-background); }
.budget__stats span, .budget__note { color: var(--color-muted); font-size: .88rem; }
.budget__stats strong { font-size: 1.1rem; }
.budget__note { margin: 0; }
@media (max-width: 540px) {
  .budget__heading { align-items: flex-start; flex-direction: column; }
}
</style>
