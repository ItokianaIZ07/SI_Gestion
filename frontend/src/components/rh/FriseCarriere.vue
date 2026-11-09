<script setup>
import { formatDate } from "@/config/libelles";

defineProps({
  carriere: { type: Array, default: () => [] },
});
</script>

<template>
  <p v-if="!carriere.length" class="g-vide">Aucune affectation enregistrée pour ce salarié.</p>
  <ol v-else class="frise" aria-label="Historique de carrière">
    <li v-for="a in carriere" :key="a.id ?? a.dateDebut" class="frise__etape"
      :class="{ 'frise__etape--courante': a.courante }">
      <span class="frise__point" aria-hidden="true"></span>
      <div class="frise__contenu">
        <p class="frise__periode">
          {{ formatDate(a.dateDebut) }} → {{ a.dateFin ? formatDate(a.dateFin) : "aujourd'hui" }}
          <span v-if="a.courante" class="g-badge g-badge--success">Actuelle</span>
        </p>
        <p class="frise__poste">{{ a.poste }} <span>· {{ a.departement }}</span></p>
        <p v-if="a.superieurNom" class="frise__detail">Supérieur : {{ a.superieurNom }}</p>
        <p v-if="a.motif" class="frise__detail">Motif : {{ a.motif }}</p>
      </div>
    </li>
  </ol>
</template>

<style scoped>
.frise {
  margin: 0;
  padding: 18px 22px 18px 34px;
  list-style: none;
  position: relative;
}

.frise::before {
  content: "";
  position: absolute;
  left: 27px;
  top: 24px;
  bottom: 24px;
  width: 2px;
  background: var(--color-border);
}

.frise__etape {
  position: relative;
  padding: 0 0 18px 22px;
}

.frise__etape:last-child {
  padding-bottom: 0;
}

.frise__point {
  position: absolute;
  left: -12px;
  top: 5px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--color-card);
  border: 2px solid var(--color-border-strong);
}

.frise__etape--courante .frise__point {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.frise__periode {
  margin: 0 0 2px;
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: .02em;
  color: var(--color-muted);
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.frise__poste {
  margin: 0;
  font-weight: 600;
  color: var(--color-ink);
}

.frise__poste span {
  font-weight: 400;
  color: var(--color-muted);
}

.frise__detail {
  margin: 2px 0 0;
  font-size: 13px;
  color: var(--color-text);
}
</style>
