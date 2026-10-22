<script setup>
defineProps({
  selectCV: {
    type: Function,
    required: true,
  },
  cv: {
    type: Object,
    required: true
  }
});
</script>

<template>
  <button type="button" class="cv-item" @click="selectCV(cv)">
    <div class="cv-avatar">{{ cv.prenom.charAt(0) }}{{ cv.nom.charAt(0) }}</div>

    <div class="cv-info">
      <div class="cv-name-row">
        <h3>{{ cv.prenom }} {{ cv.nom }}</h3>

        <span
          class="cv-status"
          :class="{
            new: cv.statut === 'Nouveau',
            viewed: cv.statut === 'Consulté',
          }"
        >
          {{ cv.statut }}
        </span>
      </div>

      <p class="cv-position">{{ cv.poste }}</p>

      <span class="cv-date">
        <span aria-hidden="true">◷</span>
        Reçu le {{ cv.date }}
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
    border-radius: 9px;
    background-color: #ffffff;
    text-align: left;
    cursor: pointer;
    transition:
        background-color 0.2s ease,
        border-color 0.2s ease,
        box-shadow 0.2s ease;
}

.cv-item:hover {
    background-color: #f7f8f6;
    border-color: #e6e9e4;
}

.cv-item.active {
    background-color: #f3f8f2;
    border-color: #b8d5b8;
    box-shadow: inset 3px 0 0 var(--color-primary);
}

/* Avatar */
.cv-avatar {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 42px;
    height: 42px;
    border: 1px solid #e8ebe6;
    border-radius: 10px;
    background-color: #f3f4f1;
    color: #606a60;
    font-size: 13px;
    font-weight: 700;
}

.cv-item.active .cv-avatar {
    border-color: #d5e7d4;
    background-color: #e5f1e3;
    color: var(--color-primary);
}

/* Informations candidat */
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

.cv-position {
    margin: 7px 0;
    color: #777e78;
    font-size: 13px;
    line-height: 1.4;
}

.cv-date {
    display: flex;
    align-items: center;
    gap: 5px;
    color: #989e98;
    font-size: 11px;
}

/* Statuts */
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
    background-color: #eaf5e9;
    color: #24752b;
}

.cv-status.viewed {
    background-color: #f0f1ef;
    color: #747a74;
}
</style>
