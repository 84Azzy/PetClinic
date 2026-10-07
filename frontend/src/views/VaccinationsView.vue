<template>
  <ResourceCrud
    title="疫苗记录"
    description="查看接种记录，及时跟进下一次免疫安排。"
    endpoint="/vaccinations"
    :columns="columns"
    :fields="fields"
    :keyword-enabled="false"
    :status-options="[]"
    :extra-params="{ petId, overdue: overdue || undefined }"
    write-authority="vaccination:manage"
    :write-roles="['ADMIN', 'STAFF']"
    @reset="
      petId = undefined;
      overdue = false;
    "
  >
    <template #filters
      ><el-select
        v-model="petId"
        placeholder="全部宠物"
        aria-label="筛选接种宠物"
        clearable
        filterable
        ><el-option
          v-for="pet in pets"
          :key="pet.id"
          :label="pet.name"
          :value="pet.id" /></el-select
      ><el-checkbox v-model="overdue">仅看已到期</el-checkbox></template
    >
    <template #cell-petId="{ row }"
      ><div class="vaccine-pet">
        <PetAvatar :src="pet(row.petId)?.photoUrl" :size="36" /><strong>{{
          petName(row.petId)
        }}</strong>
      </div></template
    >
    <template #cell-nextDueDate="{ row }"
      ><span :class="{ due: isDue(row) }">{{
        formatDate(row.nextDueDate)
      }}</span
      ><small v-if="isDue(row)" class="cell-secondary due"
        >已到期，建议联系主人</small
      ></template
    >
    <template #cell-status="{ row }"
      ><StatusTag
        :status="
          row.nextDueDate && row.status !== 'COMPLETED'
            ? isDue(row)
              ? 'OVERDUE'
              : 'VALID'
            : row.status
        "
    /></template>
  </ResourceCrud>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import ResourceCrud, {
  type Column,
  type Field,
} from "@/components/ResourceCrud.vue";
import PetAvatar from "@/components/PetAvatar.vue";
import StatusTag from "@/components/StatusTag.vue";
import { listAll, formatDate, type ClinicRow } from "@/utils/clinic";
import { useAuthStore } from "@/stores/auth";
const auth = useAuthStore(),
  pets = ref<ClinicRow[]>([]),
  petId = ref<number>(),
  overdue = ref(false);
const pet = (id: number) => pets.value.find((p) => p.id === id),
  petName = (id: number) => pet(id)?.name || "宠物 #" + id;
const isDue = (row: ClinicRow) =>
  !!row.nextDueDate &&
  row.status !== "COMPLETED" &&
  row.nextDueDate < new Date().toLocaleDateString("en-CA");
const columns: Column[] = [
  {
    key: "petId",
    label: "接种宠物",
    width: 180,
    formatter: (value) => petName(Number(value)),
  },
  { key: "vaccineName", label: "疫苗名称", width: 160 },
  { key: "vaccinatedDate", label: "接种日期", type: "date" },
  { key: "nextDueDate", label: "下次到期", type: "date", width: 190 },
  { key: "veterinarian", label: "接种医生" },
  { key: "status", label: "状态" },
];
const fields = computed<Field[]>(() => [
  {
    key: "petId",
    label: "接种宠物",
    type: "select",
    required: true,
    options: pets.value.map((p) => ({
      label: p.name + " · 档案 #" + p.id,
      value: p.id,
    })),
  },
  { key: "vaccineName", label: "疫苗名称", required: true },
  { key: "vaccinatedDate", label: "接种日期", type: "date", required: true },
  { key: "nextDueDate", label: "下次到期", type: "date" },
  { key: "batchNo", label: "接种批次" },
  { key: "veterinarian", label: "接种医生" },
  { key: "notes", label: "接种备注", type: "textarea" },
]);
onMounted(async () => {
  if (auth.hasAuthority("pet:manage"))
    try {
      pets.value = await listAll("/pets");
    } catch {}
});
</script>
<style scoped>
.vaccine-pet {
  display: flex;
  align-items: center;
  gap: 12px;
}
.due {
  color: #9b621e;
}
</style>
