<template>
  <ResourceCrud
    title="电子病历"
    description="记录症状、诊断、治疗方案和处方"
    endpoint="/medical-records"
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
  { key: "visitId", label: "就诊 ID" },
  { key: "petId", label: "宠物 ID" },
  { key: "vetId", label: "兽医 ID" },
  { key: "diagnosis", label: "诊断", width: 180 },
  { key: "treatment", label: "治疗方案", width: 180 },
];
const fields: Field[] = [
  { key: "visitId", label: "就诊 ID", type: "number", required: true },
  { key: "symptoms", label: "症状", type: "textarea", required: true },
  { key: "diagnosis", label: "诊断", type: "textarea", required: true },
  { key: "treatment", label: "治疗方案", type: "textarea", required: true },
  { key: "prescription", label: "处方", type: "textarea" },
  { key: "notes", label: "备注", type: "textarea" },
];
</script>
