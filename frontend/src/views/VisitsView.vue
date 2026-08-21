<template>
  <div>
    <PageHeader
      title="预约就诊"
      description="预留事务业务：创建、查询、取消和完成接口均已定义"
      ><el-button type="primary" :icon="Plus" @click="visible = true"
        >创建预约</el-button
      ></PageHeader
    >
    <div class="panel">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索预约"
          clearable
        /><el-select v-model="query.status" clearable placeholder="全部状态"
          ><el-option label="待就诊" value="SCHEDULED" /><el-option
            label="已完成"
            value="COMPLETED" /><el-option
            label="已取消"
            value="CANCELLED" /></el-select
        ><el-button type="primary" plain @click="load">查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe
        ><el-table-column prop="id" label="ID" width="70" /><el-table-column
          prop="petId"
          label="宠物 ID"
        /><el-table-column prop="vetId" label="兽医 ID" /><el-table-column
          prop="slotId"
          label="时段 ID"
        /><el-table-column
          prop="reason"
          label="就诊原因"
          min-width="220"
        /><el-table-column prop="status" label="状态"
          ><template #default="s"
            ><StatusTag :status="s.row.status" /></template></el-table-column
        ><el-table-column label="操作" width="130"
          ><template #default="s"
            ><el-button
              link
              type="danger"
              :disabled="s.row.status !== 'SCHEDULED'"
              @click="cancel(s.row.id)"
              >取消预约</el-button
            ></template
          ></el-table-column
        ></el-table
      >
      <div class="pager">
        <el-pagination
          background
          layout="total,prev,pager,next"
          :total="total"
          v-model:current-page="query.page"
          @current-change="load"
        />
      </div>
    </div>
    <el-dialog v-model="visible" title="创建预约" width="520px"
      ><el-form :model="form" label-width="90px"
        ><el-form-item label="宠物 ID"
          ><el-input-number v-model="form.petId" :min="1" /></el-form-item
        ><el-form-item label="时段 ID"
          ><el-input-number v-model="form.slotId" :min="1" /></el-form-item
        ><el-form-item label="就诊原因"
          ><el-input
            v-model="form.reason"
            type="textarea"
            :rows="4" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="visible = false">取消</el-button
        ><el-button type="primary" @click="create"
          >确认预约</el-button
        ></template
      ></el-dialog
    >
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { Plus } from "@element-plus/icons-vue";
import { ElMessageBox } from "element-plus";
import PageHeader from "@/components/PageHeader.vue";
import StatusTag from "@/components/StatusTag.vue";
import { createResource, listResource, postAction } from "@/api/resource";
const rows = ref<Record<string, any>[]>([]),
  total = ref(0),
  loading = ref(false),
  visible = ref(false);
const query = reactive({ page: 1, size: 10, keyword: "", status: "" });
const form = reactive({ petId: 1, slotId: 2, reason: "常规健康检查" });
const load = async () => {
  loading.value = true;
  try {
    const r = await listResource<any>("/visits", query);
    if (!Array.isArray(r.data)) {
      rows.value = r.data.records;
      total.value = r.data.total;
    }
  } finally {
    loading.value = false;
  }
};
const create = async () => {
  await createResource("/visits", { ...form, requestId: crypto.randomUUID() });
  visible.value = false;
  load();
};
const cancel = async (id: number) => {
  const { value } = await ElMessageBox.prompt("请输入取消原因", "取消预约", {
    inputValue: "行程有变",
  });
  await postAction(`/visits/${id}/cancel`, { reason: value });
  load();
};
onMounted(load);
</script>
<style scoped>
.filter-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.filter-bar .el-input {
  width: 260px;
}
.filter-bar .el-select {
  width: 150px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
