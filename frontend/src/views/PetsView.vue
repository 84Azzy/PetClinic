<template>
  <ResourceCrud
    title="宠物档案"
    description="预留纵向业务：页面与 API 契约已经就绪"
    endpoint="/pets"
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
  { key: "name", label: "宠物名" },
  { key: "ownerId", label: "主人 ID" },
  { key: "typeId", label: "类型 ID" },
  { key: "breed", label: "品种" },
  { key: "gender", label: "性别" },
  { key: "birthDate", label: "生日", type: "date" },
  { key: "status", label: "状态" },
];
const fields: Field[] = [
  { key: "ownerId", label: "主人 ID", type: "number", required: true },
  { key: "typeId", label: "类型 ID", type: "number", required: true },
  { key: "name", label: "宠物名", required: true },
  {
    key: "gender",
    label: "性别",
    type: "select",
    options: [
      { label: "公", value: "MALE" },
      { label: "母", value: "FEMALE" },
    ],
  },
  { key: "breed", label: "品种" },
  { key: "birthDate", label: "生日", type: "date" },
  { key: "color", label: "毛色" },
  { key: "microchipNo", label: "芯片号" },
  { key: "allergies", label: "过敏史", type: "textarea" },
];
</script>
