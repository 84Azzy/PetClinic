<template>
  <span class="pet-avatar" :style="{ width: size + 'px', height: size + 'px' }">
    <img
      v-if="safeUrl && !failed"
      :src="safeUrl"
      :alt="name || '宠物照片'"
      :width="size"
      :height="size"
      loading="lazy"
      @error="failed = true"
    />
    <ClinicIcon v-else name="pet" />
  </span>
</template>
<script setup lang="ts">
import { computed, ref, watch } from "vue";
import ClinicIcon from "./ClinicIcon.vue";
const props = withDefaults(
  defineProps<{ src?: string; name?: string; size?: number }>(),
  { size: 52 },
);
const failed = ref(false);
const safeUrl = computed(() => {
  if (!props.src) return "";
  try {
    const url = new URL(props.src, window.location.origin);
    return ["http:", "https:"].includes(url.protocol) ? url.href : "";
  } catch {
    return "";
  }
});
watch(
  () => props.src,
  () => (failed.value = false),
);
</script>
<style scoped>
.pet-avatar {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  background: #eaf2ed;
  border-radius: 10px;
  overflow: hidden;
  color: #548371;
}
img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
svg {
  width: 48%;
  height: 48%;
}
</style>
