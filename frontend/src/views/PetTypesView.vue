<template>
  <ResourceCrud
    title="宠物类型"
    description="维护猫、犬、兔等基础分类"
    endpoint="/pet-types"
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
  { key: "name", label: "类型名称" },
  { key: "description", label: "说明", width: 260 },
  { key: "status", label: "状态" },
];
const fields: Field[] = [
  { key: "name", label: "类型名称", required: true },
  { key: "description", label: "说明", type: "textarea" },
  {
    key: "status",
    label: "状态",
    type: "select",
    options: [
      { label: "启用", value: "ACTIVE" },
      { label: "停用", value: "INACTIVE" },
    ],
  },
];
</script>
