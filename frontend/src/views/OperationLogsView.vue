<template>
  <div>
    <PageHeader title="操作日志" description="查看业务写操作、耗时与异常结果" />
    <div class="panel">
      <el-table :data="rows" v-loading="loading" stripe
        ><el-table-column prop="username" label="操作人" /><el-table-column
          prop="module"
          label="模块" /><el-table-column
          prop="operation"
          label="操作" /><el-table-column
          prop="httpMethod"
          label="方法" /><el-table-column
          prop="requestUri"
          label="请求路径"
          min-width="210" /><el-table-column
          prop="responseStatus"
          label="状态码" /><el-table-column
          prop="durationMs"
          label="耗时(ms)" /><el-table-column
          prop="createdAt"
          label="时间"
          min-width="170"
      /></el-table>
    </div>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from "vue";
import PageHeader from "@/components/PageHeader.vue";
import { listResource } from "@/api/resource";
const rows = ref<any[]>([]),
  loading = ref(false);
onMounted(async () => {
  loading.value = true;
  try {
    const r = await listResource<any>("/operation-logs", {
      page: 1,
      size: 100,
    });
    if (!Array.isArray(r.data)) rows.value = r.data.records;
  } finally {
    loading.value = false;
  }
});
</script>
