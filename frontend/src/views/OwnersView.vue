<template>
  <ResourceCrud
    title="主人档案"
    description="联系宠物主人，查看他们与小患者的关联资料。"
    endpoint="/owners"
    :columns="columns"
    :fields="fields"
    write-authority="owner:manage"
    delete-label="停用"
    ><template #detail="{ row }"><OwnerDetail :row="row" /></template
  ></ResourceCrud>
</template>
<script setup lang="ts">
import ResourceCrud, {
  type Column,
  type Field,
} from "@/components/ResourceCrud.vue";
import { computed, onMounted, ref } from "vue";
import { listAll, type ClinicRow } from "@/utils/clinic";
import { useAuthStore } from "@/stores/auth";
import OwnerDetail from "@/components/OwnerDetail.vue";
const columns: Column[] = [
  { key: "name", label: "姓名" },
  { key: "phone", label: "手机号" },
  { key: "email", label: "邮箱" },
  { key: "address", label: "地址", width: 180 },
  { key: "status", label: "状态" },
];
const auth = useAuthStore(),
  users = ref<ClinicRow[]>([]);
const fields = computed<Field[]>(() => [
  {
    key: "userId",
    label: users.value.length ? "绑定主人账号" : "绑定账号编号",
    type: users.value.length ? "select" : "number",
    required: true,
    options: users.value.map((u) => ({
      label: u.displayName + " · " + u.username,
      value: u.id,
    })),
  },
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
]);
onMounted(async () => {
  if (auth.hasAuthority("system:manage"))
    try {
      users.value = (await listAll("/system/users")).filter(
        (u) => u.accountType === "OWNER",
      );
    } catch {}
});
</script>
