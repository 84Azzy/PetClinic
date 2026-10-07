<template>
  <ResourceCrud
    title="兽医管理"
    description="查看执业资料与诊疗简介，维护接诊团队。"
    endpoint="/vets"
    :columns="columns"
    :fields="fields"
    write-authority="vet:manage"
    delete-label="停用"
  />
</template>
<script setup lang="ts">
import ResourceCrud, {
  type Column,
  type Field,
} from "@/components/ResourceCrud.vue";
const columns: Column[] = [
  { key: "name", label: "姓名" },
  { key: "licenseNo", label: "执业证号" },
  { key: "phone", label: "手机号" },
  { key: "email", label: "邮箱" },
  { key: "status", label: "状态" },
];
const fields: Field[] = [
  { key: "name", label: "姓名", required: true },
  { key: "phone", label: "手机号" },
  { key: "email", label: "邮箱" },
  { key: "licenseNo", label: "执业证号", required: true },
  { key: "biography", label: "简介", type: "textarea" },
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
