<script setup>
defineProps({
  title: {
    type: String,
    required: true,
  },
  data: {
    type: Array,
    required: true,
  },
});

const model = defineModel();
</script>

<template>
  <label>
    <span>{{ title }}</span>
    <select v-model="model">
      <option v-for="data in data" :key="data.value" :value="data.value">
        {{ data.label }}
      </option>
    </select>
  </label>
</template>

<style scoped>
label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--muted);
  font-size: 11px;
}
select {
  width: 100%;
  min-width: 0;
  padding: 9px 8px;
  border: 1px solid var(--line);
  border-radius: 7px;
  background: #fff;
  color: var(--ink);
  font: inherit;
}
</style>
