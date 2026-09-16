<template>
  <ResourceCrud
    title="兽医排班"
    description="创建、查询和关闭可预约诊疗时段"
    endpoint="/slots"
    :columns="columns"
    :fields="fields"
    write-authority="schedule:manage"
    :write-roles="['ADMIN', 'STAFF']"
  />
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import ResourceCrud, {
  type Column,
  type Field,
} from "@/components/ResourceCrud.vue";
import { listVetsForVisit } from "@/api/visits";
import type { VetSummary } from "@/types";

const vets = ref<VetSummary[]>([]);

const vetName = (value: unknown) => {
  const id = Number(value);
  return vets.value.find((vet) => vet.id === id)?.name ?? `兽医 #${id}`;
};

const columns: Column[] = [
  {
    key: "vetId",
    label: "兽医",
    formatter: (value) => vetName(value),
  },
  { key: "startTime", label: "开始时间", type: "datetime", width: 160 },
  { key: "endTime", label: "结束时间", type: "datetime", width: 160 },
  { key: "note", label: "备注" },
  { key: "status", label: "状态" },
];
/*
 * TODO【新知识：computed 派生表单配置】
 * vets 是异步请求得到的响应式数据；computed 会在 vets 更新后自动重新生成下拉选项。
 * 因此页面不需要手动修改 ResourceCrud 内部的表单。
 */
const fields = computed<Field[]>(() => [
  {
    key: "vetId",
    label: "兽医",
    type: "select",
    required: true,
    options: vets.value.map((vet) => ({ label: vet.name, value: vet.id })),
  },
  { key: "startTime", label: "开始时间", type: "datetime", required: true },
  { key: "endTime", label: "结束时间", type: "datetime", required: true },
  { key: "note", label: "备注" },
]);

onMounted(async () => {
  vets.value = (await listVetsForVisit()).data.records;
});
</script>
