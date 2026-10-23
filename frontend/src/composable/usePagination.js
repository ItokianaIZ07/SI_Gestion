import { computed, reactive, ref, watch } from "vue";

/**
 * Pagination côté client d'une liste (tableau ou ref/computed de tableau).
 *
 *   const pg = usePagination(lignes, 10);
 *   <tr v-for="l in pg.items">…</tr>
 *   <Pagination v-bind="pg.barre" @update:page="pg.aller" />
 *
 * Revient à la page 1 quand la liste change (recherche, filtre, rechargement).
 */
export function usePagination(source, taille = 10) {
  const page = ref(1);
  const tailleRef = ref(taille);

  const liste = computed(() => {
    const v = typeof source === "function" ? source() : source?.value ?? source;
    return Array.isArray(v) ? v : [];
  });

  const total = computed(() => liste.value.length);
  const pageCount = computed(() => Math.max(1, Math.ceil(total.value / tailleRef.value)));

  const items = computed(() => {
    const debut = (page.value - 1) * tailleRef.value;
    return liste.value.slice(debut, debut + tailleRef.value);
  });

  const debut = computed(() => (total.value ? (page.value - 1) * tailleRef.value + 1 : 0));
  const fin = computed(() => Math.min(page.value * tailleRef.value, total.value));

  function aller(n) {
    page.value = Math.min(Math.max(1, Number(n) || 1), pageCount.value);
  }

  watch(liste, () => aller(1)); // nouveau jeu de résultats → page 1
  watch(pageCount, () => aller(page.value));

  return reactive({
    page,
    taille: tailleRef,
    total,
    pageCount,
    items,
    debut,
    fin,
    aller,
    barre: computed(() => ({
      page: page.value,
      pageCount: pageCount.value,
      total: total.value,
      debut: debut.value,
      fin: fin.value,
    })),
  });
}
