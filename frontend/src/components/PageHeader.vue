<template>
  <header class="page-header">
    <div>
      <h1>{{ title }}</h1>
      <p v-if="description">{{ description }}</p>
    </div>
    <div v-if="$slots.default" class="page-actions"><slot /></div>
  </header>
</template>
<script setup lang="ts">
defineProps<{ title: string; description?: string; eyebrow?: string }>();
</script>
<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}
h1 {
  font-size: 28px;
  margin: 0 0 8px;
  font-weight: 700;
  letter-spacing: -0.5px;
}
p {
  font-size: 14px;
  color: var(--clinic-muted);
  margin: 0;
}
.page-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
@media (max-width: 639px) {
  .page-header {
    flex-wrap: wrap;
    gap: 16px;
  }
  h1 {
    font-size: 24px;
  }
  p {
    font-size: 14px;
  }
}
</style>
