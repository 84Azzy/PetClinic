<template>
  <div class="brand">
    <img src="/images/clinic-mark.png" alt="" width="42" height="42" />
    <div v-if="!compact"><strong>宠安诊所</strong><small>诊疗管理</small></div>
  </div>
</template>
<script setup lang="ts">
defineProps<{ compact?: boolean }>();
</script>
