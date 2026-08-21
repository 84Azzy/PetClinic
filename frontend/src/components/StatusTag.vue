<template>
  <el-tag :type="tagType" effect="light">{{ label }}</el-tag>
</template>
<script setup lang="ts">
import { computed } from "vue";
const p = defineProps<{ status?: string }>();
const labels: Record<string, string> = {
  ACTIVE: "启用",
  INACTIVE: "停用",
  AVAILABLE: "可预约",
  BOOKED: "已预约",
  CLOSED: "已关闭",
  SCHEDULED: "待就诊",
  COMPLETED: "已完成",
  CANCELLED: "已取消",
  PUBLISHED: "已发布",
  DRAFT: "草稿",
  WITHDRAWN: "已撤回",
  PENDING: "待处理",
  REPLIED: "已回复",
  VALID: "有效",
  OVERDUE: "已逾期",
};
const label = computed(() => labels[p.status || ""] || p.status || "-");
const tagType = computed(() =>
  [
    "ACTIVE",
    "AVAILABLE",
    "COMPLETED",
    "PUBLISHED",
    "REPLIED",
    "VALID",
  ].includes(p.status || "")
    ? "success"
    : ["INACTIVE", "CLOSED", "CANCELLED", "WITHDRAWN", "OVERDUE"].includes(
          p.status || "",
        )
      ? "danger"
      : ["BOOKED", "SCHEDULED", "PENDING"].includes(p.status || "")
        ? "warning"
        : "info",
);
</script>
