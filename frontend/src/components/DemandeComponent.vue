<script setup>
import { prioriteLabel, formatDate, statutLabel } from '@/utils/demandeUtils';

const emit = defineEmits(["select"]);

defineProps({
  demande: {
    type: Object,
    required: true,
  }
});

</script>
<template>
  <button
    type="button"
    class="request-item"
    @click="emit('select', demande)"
  >
    <div class="request-item-top">
      <span class="request-id">#{{ demande.id }}</span>
      <span class="priority" :class="String(demande.priorite).toLowerCase()">
        {{ prioriteLabel(demande.priorite) }}
      </span>
    </div>
    <h3>{{ demande.poste }}</h3>
    <p class="department">{{ demande.departement }}</p>
    <div class="request-item-bottom">
      <span>{{ demande.effectif }} poste(s)</span>
      <span>{{ formatDate(demande.dateDemande) }}</span>
    </div>
    <span class="status-label">{{ statutLabel(demande.statut) }}</span>
  </button>
</template>

<style scoped>
    .request-item {
    width: 100%;
    padding: 14px;
    border: 1px solid transparent;
    border-radius: 9px;
    background: white;
    text-align: left;
    cursor: pointer;
    transition: background 0.2s, border-color 0.2s;
}

.request-item:hover {
    background: #fafbf9;
    border-color: #e8ebe6;
}

.request-item.active {
    border-color: #bfd8bc;
    background: #f4f8f2;
    box-shadow: inset 3px 0 var(--color-primary);
}

.request-item-top,
.request-item-bottom {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 7px;
}

.request-id,
.request-item-bottom,
.department {
    color: var(--muted);
    font-size: 11px;
}

.request-item h3 {
    margin: 12px 0 5px;
    color: var(--ink);
    font-size: 14px;
}

.department {
    margin: 0 0 13px;
}

.request-item-bottom {
    padding-bottom: 12px;
    border-bottom: 1px solid #eff0ed;
}

.status-label {
    display: inline-block;
    margin-top: 10px;
    color: #6d756d;
    font-size: 10px;
}

/* Priorités */
.priority {
    display: inline-block;
    padding: 5px 8px;
    border-radius: 5px;
    background: #f0f1ef;
    color: #6d746d;
    font-size: 10px;
    font-weight: 700;
}

.priority.urgente {
    background: #fff0ed;
    color: #b74736;
}

.priority.haute {
    background: #fff5e7;
    color: #98621b;
}

.priority.normale,
.priority.basse {
    background: #f0f1ef;
    color: #697169;
}

</style>
