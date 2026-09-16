<template>
  <ResourceCrud
    title="疫苗记录"
    description="跟踪接种批次、日期和下次到期时间"
    endpoint="/vaccinations"
    :columns="columns"
    :fields="fields"
    write-authority="vaccination:manage"
    :write-roles="['ADMIN', 'STAFF']"
  />
</template>
<script setup lang="ts">
import ResourceCrud, {
  type Column,
  type Field,
} from "@/components/ResourceCrud.vue";
const columns: Column[] = [
  { key: "petId", label: "宠物 ID" },
  { key: "vaccineName", label: "疫苗名称" },
  { key: "batchNo", label: "批次" },
  { key: "vaccinatedDate", label: "接种日期", type: "date" },
  { key: "nextDueDate", label: "下次到期", type: "date" },
  { key: "status", label: "状态" },
];
const fields: Field[] = [
  { key: "petId", label: "宠物 ID", type: "number", required: true },
  { key: "vaccineName", label: "疫苗名称", required: true },
  { key: "batchNo", label: "批次" },
  { key: "vaccinatedDate", label: "接种日期", type: "date", required: true },
  { key: "nextDueDate", label: "下次到期", type: "date" },
  { key: "veterinarian", label: "接种医生" },
  { key: "notes", label: "备注", type: "textarea" },
];
</script>
