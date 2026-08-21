<template>
  <ResourceCrud
    title="主人档案"
    description="维护宠物主人联系方式与账号绑定关系"
    endpoint="/owners"
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
  { key: "name", label: "姓名" },
  { key: "phone", label: "手机号" },
  { key: "email", label: "邮箱" },
  { key: "address", label: "地址", width: 180 },
  { key: "status", label: "状态" },
];
const fields: Field[] = [
  { key: "userId", label: "账号 ID", type: "number", required: true },
  { key: "name", label: "姓名", required: true },
  { key: "phone", label: "手机号", required: true },
  { key: "email", label: "邮箱" },
  { key: "address", label: "地址" },
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
