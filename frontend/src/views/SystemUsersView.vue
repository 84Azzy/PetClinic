<template>
  <ResourceCrud
    title="系统用户"
    description="RBAC 留白：维护账号并分配角色"
    endpoint="/system/users"
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
  { key: "username", label: "账号" },
  { key: "displayName", label: "姓名" },
  { key: "accountType", label: "账户类型" },
  { key: "phone", label: "手机" },
  { key: "status", label: "状态" },
];
const fields: Field[] = [
  { key: "username", label: "账号", required: true },
  { key: "displayName", label: "姓名", required: true },
  { key: "phone", label: "手机" },
  { key: "email", label: "邮箱" },
  {
    key: "accountType",
    label: "账户类型",
    type: "select",
    required: true,
    options: [
      { label: "管理员", value: "ADMIN" },
      { label: "员工", value: "STAFF" },
      { label: "宠主", value: "OWNER" },
    ],
  },
  { key: "password", label: "初始密码" },
];
</script>
