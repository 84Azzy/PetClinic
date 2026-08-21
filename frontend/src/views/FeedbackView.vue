<template>
  <div>
    <PageHeader title="意见反馈" description="收集宠主建议并跟踪回复状态"
      ><el-button type="primary" :icon="Plus" @click="visible = true"
        >提交反馈</el-button
      ></PageHeader
    >
    <div class="panel">
      <el-table :data="rows" v-loading="loading" stripe
        ><el-table-column
          prop="category"
          label="分类"
          width="110"
        /><el-table-column prop="title" label="标题" /><el-table-column
          prop="content"
          label="内容"
          min-width="260"
          show-overflow-tooltip
        /><el-table-column
          prop="reply"
          label="回复"
          min-width="220"
          show-overflow-tooltip
        /><el-table-column prop="status" label="状态"
          ><template #default="s"
            ><StatusTag :status="s.row.status" /></template></el-table-column
        ><el-table-column label="操作" width="120"
          ><template #default="s"
            ><el-button link type="primary" @click="reply(s.row.id)"
              >回复</el-button
            ><el-button link type="warning" @click="close(s.row.id)"
              >关闭</el-button
            ></template
          ></el-table-column
        ></el-table
      >
    </div>
    <el-dialog v-model="visible" title="提交反馈" width="520px"
      ><el-form :model="form" label-width="80px"
        ><el-form-item label="分类"
          ><el-select v-model="form.category"
            ><el-option label="服务建议" value="SERVICE" /><el-option
              label="系统问题"
              value="SYSTEM" /><el-option
              label="其他"
              value="OTHER" /></el-select></el-form-item
        ><el-form-item label="标题"
          ><el-input v-model="form.title" /></el-form-item
        ><el-form-item label="内容"
          ><el-input
            v-model="form.content"
            type="textarea"
            :rows="5" /></el-form-item
        ><el-form-item label="联系方式"
          ><el-input v-model="form.contact" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="visible = false">取消</el-button
        ><el-button type="primary" @click="submit">提交</el-button></template
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
const rows = ref<any[]>([]),
  loading = ref(false),
  visible = ref(false);
const form = reactive({
  category: "SERVICE",
  title: "",
  content: "",
  contact: "",
});
const load = async () => {
  loading.value = true;
  try {
    const r = await listResource<any>("/feedback", { page: 1, size: 100 });
    if (!Array.isArray(r.data)) rows.value = r.data.records;
  } finally {
    loading.value = false;
  }
};
const submit = async () => {
  await createResource("/feedback", form);
  visible.value = false;
  load();
};
const reply = async (id: number) => {
  const { value } = await ElMessageBox.prompt("请输入回复内容", "回复反馈");
  await postAction(`/feedback/${id}/reply`, { reply: value });
  load();
};
const close = async (id: number) => {
  await postAction(`/feedback/${id}/close`);
  load();
};
onMounted(load);
</script>
