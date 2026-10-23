<script setup>
import { computed } from "vue";

const props = defineProps({
  page: { type: Number, required: true },
  pageCount: { type: Number, required: true },
  total: { type: Number, default: 0 },
  debut: { type: Number, default: 0 },
  fin: { type: Number, default: 0 },
  compact: { type: Boolean, default: false }, // version étroite : précédent / x sur y / suivant
});

const emit = defineEmits(["update:page"]);

/* 1 … 4 5 [6] 7 8 … 20 */
const pages = computed(() => {
  const { page, pageCount } = props;
  if (pageCount <= 7) return Array.from({ length: pageCount }, (_, i) => i + 1);
  const set = new Set([1, pageCount, page, page - 1, page + 1]);
  if (page <= 3) [2, 3, 4].forEach((n) => set.add(n));
  if (page >= pageCount - 2) [pageCount - 1, pageCount - 2, pageCount - 3].forEach((n) => set.add(n));
  const triees = [...set].filter((n) => n >= 1 && n <= pageCount).sort((a, b) => a - b);
  const res = [];
  triees.forEach((n, i) => {
    if (i && n - triees[i - 1] > 1) res.push("…");
    res.push(n);
  });
  return res;
});

const va = (n) => emit("update:page", n);
</script>

<template>
  <nav v-if="pageCount > 1" class="pagination" :class="{ 'pagination--compact': compact }" aria-label="Pagination">
    <p v-if="!compact" class="pagination__info">
      <strong>{{ debut }}–{{ fin }}</strong> sur {{ total }}
    </p>

    <div class="pagination__controles">
      <button type="button" class="pagination__btn pagination__btn--fleche" :disabled="page <= 1" aria-label="Page précédente" @click="va(page - 1)">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 18l-6-6 6-6" /></svg>
        <span v-if="!compact">Précédent</span>
      </button>

      <span v-if="compact" class="pagination__etat">{{ page }} / {{ pageCount }}</span>

      <template v-else>
        <template v-for="(p, i) in pages" :key="i">
          <span v-if="p === '…'" class="pagination__points">…</span>
          <button v-else type="button" class="pagination__btn" :class="{ 'pagination__btn--actif': p === page }" :aria-current="p === page ? 'page' : undefined" @click="va(p)">{{ p }}</button>
        </template>
      </template>

      <button type="button" class="pagination__btn pagination__btn--fleche" :disabled="page >= pageCount" aria-label="Page suivante" @click="va(page + 1)">
        <span v-if="!compact">Suivant</span>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18l6-6-6-6" /></svg>
      </button>
    </div>
  </nav>
</template>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 14px 20px;
  border-top: 1px solid var(--color-border);
  background: var(--color-card);
}

.pagination__info { margin: 0; font-size: 13px; color: var(--color-muted); }
.pagination__info strong { color: var(--color-ink); font-weight: 600; }

.pagination__controles { display: flex; align-items: center; gap: 4px; }

.pagination__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-width: 34px;
  height: 34px;
  padding: 0 10px;
  font: inherit;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--color-text);
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background-color .15s, color .15s, border-color .15s;
}

.pagination__btn:hover:not(:disabled):not(.pagination__btn--actif) { background: var(--color-primary-soft); color: var(--color-primary); }
.pagination__btn:focus-visible { outline: none; box-shadow: var(--shadow-focus); }
.pagination__btn:disabled { opacity: .4; cursor: not-allowed; }

.pagination__btn--fleche { border-color: var(--color-border-strong); background: var(--color-card); }

.pagination__btn--actif {
  color: #fff;
  background: var(--color-primary);
  border-color: var(--color-primary);
  box-shadow: var(--shadow-sm);
  cursor: default;
}

.pagination__points { min-width: 22px; text-align: center; color: var(--color-faint); }
.pagination__etat { padding: 0 10px; font-size: 13px; font-weight: 600; color: var(--color-muted); font-variant-numeric: tabular-nums; }

/* Version étroite (panneaux latéraux) */
.pagination--compact { justify-content: center; padding: 10px 12px; }
.pagination--compact .pagination__controles { gap: 8px; }

@media (max-width: 640px) {
  .pagination { justify-content: center; padding: 12px; }
  .pagination__btn--fleche span { display: none; }
}
</style>
