<script setup>
defineProps({
  selectCandidat: {
    type: Function,
    required: true,
  },
  candidat: {
    type: Object,
    required: true,
  },
});

function initiales(candidat) {
  const prenom = candidat.prenom ?? "";
  const nom = candidat.nom ?? "";

  return `${prenom.charAt(0)}${nom.charAt(0)}`.toUpperCase() || "?";
}

function dateLisible(date) {
  if (!date) return "Date non disponible";

  const valeur = new Date(date);

  if (Number.isNaN(valeur.getTime())) return date;

  return new Intl.DateTimeFormat("fr-FR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(valeur);
}
</script>

<template>
  <button type="button" class="cv-item" @click="selectCandidat(candidat)">
    <div class="cv-avatar">
      {{ initiales(candidat) }}
    </div>

    <div class="cv-info">
      <div class="cv-name-row">
        <h3>
          {{ candidat.prenom ?? "" }}
          {{ candidat.nom ?? "Nom inconnu" }}
        </h3>

        <span v-if="candidat.statut" class="cv-status" :class="{
          new: candidat.statut === 'NOUVEAU',
          viewed: candidat.statut === 'CONSULTE',
        }">
          {{ candidat.statut }}
        </span>
      </div>

      <p v-if="candidat.email" class="cv-detail">
        {{ candidat.email }}
      </p>

      <p v-if="candidat.telephone" class="cv-detail">
        {{ candidat.telephone }}
      </p>

      <span class="cv-date">
        <span aria-hidden="true">◷</span>
        Candidat créé le
        {{ dateLisible(candidat.dateCreation) }}
      </span>
    </div>
  </button>
</template>

<style scoped>
.cv-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  padding: 14px 12px;
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.cv-item:hover {
  background: var(--color-primary-soft);
  border-color: var(--color-primary-line);
}

.cv-item:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.cv-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  border: 1px solid #e8ebe6;
  border-radius: 10px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
}

.cv-info {
  flex: 1;
  min-width: 0;
}

.cv-name-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 6px;
}

.cv-name-row h3 {
  margin: 0;
  color: #292e2a;
  font-size: 14px;
  font-weight: 650;
  overflow-wrap: anywhere;
}

.cv-detail {
  margin: 6px 0 0;
  color: #777e78;
  font-size: 12px;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.cv-date {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 8px;
  color: #989e98;
  font-size: 11px;
}

.cv-status {
  display: inline-flex;
  align-items: center;
  padding: 4px 7px;
  border-radius: 5px;
  font-size: 10px;
  font-weight: 600;
  white-space: nowrap;
}

.cv-status.new {
  background: #eaf5e9;
  color: #24752b;
}

.cv-status.viewed {
  background: #f0f1ef;
  color: #747a74;
}
</style>