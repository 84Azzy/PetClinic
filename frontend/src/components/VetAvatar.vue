<template>
  <span
    class="vet-avatar"
    :style="{ width: size + 'px', height: size + 'px' }"
    role="img"
    :aria-label="name ? name + '的头像' : '兽医头像'"
  >
    <img
      v-if="displayUrl && !failed"
      :src="displayUrl"
      :alt="name || '兽医头像'"
      :width="size"
      :height="size"
      loading="lazy"
      @error="failed = true"
    />
    <ClinicIcon v-else name="doctor" />
  </span>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from "vue";
import http from "@/api/http";
import ClinicIcon from "./ClinicIcon.vue";

const props = withDefaults(
  defineProps<{
    src?: string;
    name?: string;
    size?: number;
    version?: string | number;
  }>(),
  { size: 48 },
);

const failed = ref(false);
const displayUrl = ref("");
let generatedUrl = "";
let generation = 0;

function safeDirectUrl(value: string) {
  try {
    const url = new URL(value, window.location.origin);
    return ["http:", "https:", "blob:"].includes(url.protocol)
      ? url.href
      : "";
  } catch {
    return "";
  }
}

function clearGeneratedUrl() {
  if (generatedUrl) URL.revokeObjectURL(generatedUrl);
  generatedUrl = "";
}

watch(
  () => [props.src, props.version] as const,
  async ([src]) => {
    const current = ++generation;
    failed.value = false;
    displayUrl.value = "";
    clearGeneratedUrl();
    if (!src) return;
    if (!src.startsWith("/api/")) {
      displayUrl.value = safeDirectUrl(src);
      return;
    }
    try {
      const blob = await http.get<any, Blob>(src.slice(4), {
        responseType: "blob",
        timeout: 30000,
      });
      if (current !== generation) return;
      generatedUrl = URL.createObjectURL(blob);
      displayUrl.value = generatedUrl;
    } catch {
      if (current === generation) failed.value = true;
    }
  },
  { immediate: true },
);

onBeforeUnmount(() => {
  generation++;
  clearGeneratedUrl();
});
</script>

<style scoped>
.vet-avatar {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 50%;
  background: #eaf2ed;
  color: var(--clinic-primary);
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
