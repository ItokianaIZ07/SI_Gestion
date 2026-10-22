<script setup>
import { computed } from "vue";
import { libelle, ton } from "@/config/libelles";

const props = defineProps({
  statut: { type: String, default: "" },
});

const texte = computed(() => libelle(props.statut));
const classe = computed(() => `g-badge--${ton(props.statut)}`);
</script>

<template>
  <span class="g-badge" :class="classe">{{ texte }}</span>
</template>
