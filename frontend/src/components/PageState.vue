<template>
  <div
    v-if="kind === 'loading'"
    class="page-state skeleton-state"
    role="status"
    aria-live="polite"
  >
    <span class="sr-only">正在加载</span>
    <div v-for="n in 3" :key="n" class="skeleton-line" />
  </div>
  <div v-else class="page-state" :role="kind === 'error' ? 'alert' : 'status'">
    <ClinicIcon :name="kind === 'error' ? 'chat' : 'file'" />
    <strong>{{
      title || (kind === "error" ? "暂时无法加载" : "还没有记录")
    }}</strong>
    <p>
      {{
        description ||
        (kind === "error"
          ? "请检查网络后重试，已有内容不会被修改。"
          : "记录将在这里清晰呈现。")
      }}
    </p>
    <el-button v-if="kind === 'error'" @click="$emit('retry')"
      >重新加载</el-button
    ><slot />
  </div>
</template>
<script setup lang="ts">
import ClinicIcon from "./ClinicIcon.vue";
defineProps<{
  kind: "loading" | "empty" | "error";
  title?: string;
  description?: string;
}>();
defineEmits<{ retry: [] }>();
</script>
<style scoped>
.page-state {
  padding: 44px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 12px;
}
.page-state > svg {
  width: 32px;
  height: 32px;
  color: #7a9788;
  margin-bottom: 4px;
}
strong {
  font-size: 16px;
  font-weight: 500;
}
p {
  color: var(--clinic-muted);
  font-size: 14px;
  max-width: 360px;
  margin: 0 0 4px;
}
.skeleton-state {
  align-items: stretch;
}
.skeleton-line {
  height: 40px;
  border-radius: 6px;
  background: #edf3ef;
  animation: pulse 1.6s ease-in-out infinite;
}
.skeleton-line:last-child {
  width: 65%;
}
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  clip: rect(0, 0, 0, 0);
  overflow: hidden;
}
@keyframes pulse {
  50% {
    opacity: 0.5;
  }
}
</style>
