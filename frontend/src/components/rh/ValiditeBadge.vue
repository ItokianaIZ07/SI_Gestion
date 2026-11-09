<script setup>
import { computed } from "vue";
import { etatValidite } from "@/config/libellesBloc5";

const props = defineProps({
  etat: { type: String, default: "" },
  joursRestants: { type: Number, default: null },
});

const infos = computed(() => etatValidite(props.etat));
const detail = computed(() => {
  if (props.joursRestants === null || props.joursRestants === undefined) return "";
  if (props.etat === "EXPIRE") return ` (depuis ${Math.abs(props.joursRestants)} j)`;
  if (props.etat === "BIENTOT_EXPIRE") return ` (J-${props.joursRestants})`;
  return "";
});
</script>

<template>
  <span class="g-badge" :class="`g-badge--${infos.ton}`">{{ infos.label }}{{ detail }}</span>
</template>
