<template>
  <ResourceCrud
    title="权限管理"
    description="按父子关系查看菜单、操作与接口的访问权限。"
    endpoint="/system/permissions"
    :columns="columns"
    :fields="fields"
    write-authority="system:manage"
    tree
    :keyword-enabled="false"
    :status-options="[]"
  />
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import ResourceCrud, {
  type Column,
  type Field,
} from "@/components/ResourceCrud.vue";
import { listAll, flattenTree, type ClinicRow } from "@/utils/clinic";
const parents = ref<ClinicRow[]>([]);
const labels: Record<string, string> = {
  MENU: "菜单",
  BUTTON: "操作按钮",
  API: "接口",
};
const columns: Column[] = [
  { key: "name", label: "权限名称", width: 240 },
  { key: "code", label: "权限编码", width: 200 },
  {
    key: "type",
    label: "类型",
    formatter: (v) => labels[String(v)] || String(v),
  },
  { key: "path", label: "页面路径", width: 180 },
  { key: "status", label: "状态" },
];
const fields = computed<Field[]>(() => [
  {
    key: "parentId",
    label: "父级权限（留空为根节点）",
    type: "select",
    options: parents.value.map((p) => ({
      label: p.name + " · " + p.code,
      value: p.id,
    })),
  },
  {
    key: "type",
    label: "权限类型",
    type: "select",
    required: true,
    defaultValue: "MENU",
    options: Object.entries(labels).map(([value, label]) => ({ value, label })),
  },
  { key: "name", label: "权限名称", required: true },
  { key: "code", label: "权限编码", required: true },
  { key: "path", label: "页面路径" },
  { key: "icon", label: "图标名称" },
  { key: "sortOrder", label: "展示顺序", type: "number", defaultValue: 0 },
]);
onMounted(async () => {
  try {
    parents.value = flattenTree(await listAll("/system/permissions"));
  } catch {}
});
</script>
