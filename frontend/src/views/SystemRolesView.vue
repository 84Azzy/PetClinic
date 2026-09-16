<template>
  <ResourceCrud
    title="角色管理"
    description="RBAC 留白：维护角色并绑定权限"
    endpoint="/system/roles"
    :columns="columns"
    :fields="fields"
    write-authority="system:manage"
  />
</template>
<script setup lang="ts">
import ResourceCrud, {
  type Column,
  type Field,
} from "@/components/ResourceCrud.vue";
const columns: Column[] = [
  { key: "code", label: "角色编码" },
  { key: "name", label: "角色名称" },
  { key: "description", label: "说明" },
  { key: "status", label: "状态" },
];
const fields: Field[] = [
  { key: "code", label: "角色编码", required: true },
  { key: "name", label: "角色名称", required: true },
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
