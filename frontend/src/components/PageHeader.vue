<template>
  <div class="page-header">
    <div>
      <div class="eyebrow teal">{{ eyebrow }}</div>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
    </div>
    <slot />
  </div>
</template>
<script setup lang="ts">
withDefaults(
  defineProps<{ title: string; description: string; eyebrow?: string }>(),
  { eyebrow: "PET CLINIC MANAGEMENT" },
);
</script>
<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}
.page-header h2 {
  margin: 5px 0;
  font-size: 24px;
}
.page-header p {
  margin: 0;
  color: #87939f;
  font-size: 13px;
}
@media (max-width: 640px) {
  .page-header {
    align-items: flex-start;
    gap: 12px;
  }
  .page-header p {
    display: none;
  }
}
</style>
