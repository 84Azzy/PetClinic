<template>
  <div>
    <PageHeader title="公告管理" description="发布诊所通知、门诊安排与健康提醒"
      ><el-button type="primary" :icon="Plus" @click="open"
        >新建公告</el-button
      ></PageHeader
    >
    <div class="panel">
      <el-table :data="rows" v-loading="loading" stripe
        ><el-table-column
          prop="title"
          label="标题"
          min-width="180"
        /><el-table-column
          prop="content"
          label="内容"
          min-width="280"
          show-overflow-tooltip
        /><el-table-column
          prop="publishedAt"
          label="发布时间"
          width="170"
        /><el-table-column prop="status" label="状态"
          ><template #default="s"
            ><StatusTag :status="s.row.status" /></template></el-table-column
        ><el-table-column label="操作" width="220"
          ><template #default="s"
            ><el-button link type="primary" @click="edit(s.row)">编辑</el-button
            ><el-button
              v-if="s.row.status !== 'PUBLISHED'"
              link
              type="success"
              @click="action(s.row.id, 'publish')"
              >发布</el-button
            ><el-button
              v-else
              link
              type="warning"
              @click="action(s.row.id, 'withdraw')"
              >撤回</el-button
            ><el-button link type="danger" @click="remove(s.row.id)"
              >删除</el-button
            ></template
          ></el-table-column
        ></el-table
      >
    </div>
    <el-dialog
      v-model="visible"
      :title="editing ? '编辑公告' : '新建公告'"
      width="600px"
      ><el-form :model="form" label-width="80px"
        ><el-form-item label="标题"
          ><el-input v-model="form.title" /></el-form-item
        ><el-form-item label="内容"
          ><el-input
            v-model="form.content"
            type="textarea"
            :rows="7" /></el-form-item
        ><el-form-item label="到期时间"
          ><el-date-picker
            v-model="form.expiresAt"
            value-format="YYYY-MM-DDTHH:mm:ss"
            type="datetime" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="visible = false">取消</el-button
        ><el-button type="primary" @click="save">保存</el-button></template
      ></el-dialog
    >
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { Plus } from "@element-plus/icons-vue";
import PageHeader from "@/components/PageHeader.vue";
import StatusTag from "@/components/StatusTag.vue";
import {
  createResource,
  deleteResource,
  listResource,
  postAction,
  updateResource,
} from "@/api/resource";
const rows = ref<any[]>([]),
  loading = ref(false),
  visible = ref(false),
  editing = ref<number>();
const form = reactive<any>({
  title: "",
  content: "",
  expiresAt: null,
  sortOrder: 0,
});
const load = async () => {
  loading.value = true;
  try {
    const r = await listResource<any>("/notices", { page: 1, size: 100 });
    if (!Array.isArray(r.data)) rows.value = r.data.records;
  } finally {
    loading.value = false;
  }
};
const open = () => {
  editing.value = undefined;
  Object.assign(form, {
    title: "",
    content: "",
    expiresAt: null,
    sortOrder: 0,
  });
  visible.value = true;
};
const edit = (r: any) => {
  editing.value = r.id;
  Object.assign(form, r);
  visible.value = true;
};
const save = async () => {
  editing.value
    ? await updateResource("/notices", editing.value, form)
    : await createResource("/notices", form);
  visible.value = false;
  load();
};
const action = async (id: number, a: string) => {
  await postAction(`/notices/${id}/${a}`);
  load();
};
const remove = async (id: number) => {
  await deleteResource("/notices", id);
  load();
};
onMounted(load);
</script>
