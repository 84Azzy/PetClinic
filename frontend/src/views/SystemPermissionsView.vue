<template>
  <ResourceCrud
    title="权限管理"
    description="维护后端下发的菜单、按钮与接口权限树"
    endpoint="/system/permissions"
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
  { key: "code", label: "权限编码" },
  { key: "name", label: "名称" },
  { key: "type", label: "类型" },
  { key: "path", label: "路由" },
  { key: "status", label: "状态" },
];
const fields: Field[] = [
  { key: "parentId", label: "父级 ID", type: "number" },
  { key: "code", label: "权限编码", required: true },
  { key: "name", label: "名称", required: true },
  {
    key: "type",
    label: "类型",
    type: "select",
    required: true,
    options: [
      { label: "菜单", value: "MENU" },
      { label: "按钮", value: "BUTTON" },
      { label: "接口", value: "API" },
    ],
  },
  { key: "path", label: "路由" },
  { key: "icon", label: "图标" },
  { key: "sortOrder", label: "排序", type: "number" },
];
</script>
