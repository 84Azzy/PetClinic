<template>
  <ResourceCrud
    title="系统用户"
    description="维护登录账号、诊所身份与联系资料。"
    endpoint="/system/users"
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
import { accountName } from "@/utils/clinic";
const columns: Column[] = [
  { key: "username", label: "账号" },
  { key: "displayName", label: "姓名" },
  {
    key: "accountType",
    label: "账户身份",
    formatter: (value) => accountName(String(value)),
  },
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
  { key: "password", label: "密码（编辑时留空保留原密码）", type: "password" },
];
</script>
