<template>
  <ResourceCrud
    title="兽医排班"
    description="创建、查询和关闭可预约诊疗时段"
    endpoint="/slots"
    :columns="columns"
    :fields="fields"
  />
</template>
<script setup lang="ts">
import ResourceCrud, {
  type Column,
  type Field,
} from "@/components/ResourceCrud.vue";
const columns: Column[] = [
  { key: "vetId", label: "兽医 ID" },
  { key: "startTime", label: "开始时间", type: "datetime", width: 160 },
  { key: "endTime", label: "结束时间", type: "datetime", width: 160 },
  { key: "note", label: "备注" },
  { key: "status", label: "状态" },
];
const fields: Field[] = [
  { key: "vetId", label: "兽医 ID", type: "number", required: true },
  { key: "startTime", label: "开始时间", type: "datetime", required: true },
  { key: "endTime", label: "结束时间", type: "datetime", required: true },
  { key: "note", label: "备注" },
];
</script>
