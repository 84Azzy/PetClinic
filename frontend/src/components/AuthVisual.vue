<template>
  <section class="login-visual">
    <ClinicBrand />
    <div class="visual-copy">
      <h1>{{ heading }}</h1>
      <p>{{ description }}</p>
    </div>
    <div>
      <img
        class="auth-photo"
        src="/images/veterinary-care.webp"
        alt="兽医在诊室为猫咪进行检查"
        width="960"
        height="720"
      />
      <p class="auth-image-caption">从一次预约，到一份完整的健康记录。</p>
    </div>
  </section>
</template>
<script setup lang="ts">
import ClinicBrand from "./ClinicBrand.vue";
withDefaults(defineProps<{ heading?: string; description?: string }>(), {
  heading: "让每一次诊疗，\n更安心、更高效",
  description: "连接宠物、主人与诊所，认真记录每一段健康旅程。",
});
</script>
<style scoped>
h1 {
  white-space: pre-line;
}
</style>
