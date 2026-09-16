<template>
  <div>
    <PageHeader :title="title" :description="description"
      ><el-button
        v-if="createEnabled && canWrite"
        type="primary"
        :icon="Plus"
        @click="openCreate"
        >新增{{ title }}</el-button
      ></PageHeader
    >
    <div class="panel">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="输入关键词搜索"
          :prefix-icon="Search"
          @keyup.enter="load"
        /><el-select v-model="query.status" clearable placeholder="全部状态"
          ><el-option label="启用" value="ACTIVE" /><el-option
            label="停用"
            value="INACTIVE" /></el-select
        ><el-button type="primary" plain @click="load">查询</el-button
        ><el-button @click="reset">重置</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" stripe
        ><el-table-column prop="id" label="ID" width="72" /><el-table-column
          v-for="col in columns"
          :key="col.key"
          :prop="col.key"
          :label="col.label"
          :min-width="col.width || 110"
          show-overflow-tooltip
          ><template #default="scope"
            ><StatusTag
              v-if="col.key === 'status'"
              :status="scope.row[col.key]"
            /><span v-else>{{
              format(scope.row[col.key], col.type, col, scope.row)
            }}</span></template
          ></el-table-column
        ><el-table-column
          v-if="canWrite"
          label="操作"
          width="155"
          fixed="right"
          ><template #default="scope"
            ><el-button link type="primary" @click="openEdit(scope.row)"
              >编辑</el-button
            ><el-popconfirm
              title="确定执行删除/停用操作吗？"
              @confirm="remove(scope.row.id)"
              ><template #reference
                ><el-button link type="danger">删除</el-button></template
              ></el-popconfirm
            ></template
          ></el-table-column
        ></el-table
      >
      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="query.size"
          v-model:current-page="query.page"
          @current-change="load"
        />
      </div>
    </div>
    <el-dialog
      v-model="visible"
      :title="`${editingId ? '编辑' : '新增'}${title}`"
      width="560px"
      destroy-on-close
      ><el-form ref="formRef" :model="form" label-width="100px"
        ><el-form-item
          v-for="field in fields"
          :key="field.key"
          :label="field.label"
          :prop="field.key"
          :rules="
            field.required
              ? [
                  {
                    required: true,
                    message: `请输入${field.label}`,
                    trigger: 'blur',
                  },
                ]
              : []
          "
          ><el-select
            v-if="field.type === 'select'"
            v-model="form[field.key]"
            style="width: 100%"
            clearable
            ><el-option
              v-for="o in field.options"
              :key="String(o.value)"
              :label="o.label"
              :value="o.value" /></el-select
          ><el-date-picker
            v-else-if="field.type === 'date'"
            v-model="form[field.key]"
            value-format="YYYY-MM-DD"
            type="date"
            style="width: 100%" /><el-date-picker
            v-else-if="field.type === 'datetime'"
            v-model="form[field.key]"
            value-format="YYYY-MM-DDTHH:mm:ss"
            type="datetime"
            style="width: 100%" /><el-input-number
            v-else-if="field.type === 'number'"
            v-model="form[field.key]"
            style="width: 100%"
            :min="1" /><el-input
            v-else
            v-model="form[field.key]"
            :type="field.type === 'textarea' ? 'textarea' : 'text'"
            :rows="
              field.type === 'textarea' ? 4 : undefined
            " /></el-form-item></el-form
      ><template #footer
        ><el-button @click="visible = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save"
          >保存</el-button
        ></template
      ></el-dialog
    >
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { Plus, Search } from "@element-plus/icons-vue";
import type { FormInstance } from "element-plus";
import {
  createResource,
  deleteResource,
  listResource,
  updateResource,
} from "@/api/resource";
import type { PageQuery } from "@/types";
import PageHeader from "./PageHeader.vue";
import StatusTag from "./StatusTag.vue";
import { useAuthStore } from "@/stores/auth";
import { canAccess, type Role } from "@/utils/permission";
export interface Column {
  key: string;
  label: string;
  width?: number;
  type?: "date" | "datetime";
  formatter?: (value: unknown, row: Record<string, any>) => string;
}
export interface Field {
  key: string;
  label: string;
  type?: "text" | "textarea" | "number" | "date" | "datetime" | "select";
  required?: boolean;
  options?: { label: string; value: string | number }[];
}
const p = withDefaults(
  defineProps<{
    title: string;
    description: string;
    endpoint: string;
    columns: Column[];
    fields: Field[];
    createEnabled?: boolean;
    writeAuthority?: string;
    writeRoles?: Role[];
  }>(),
  { createEnabled: true },
);
const auth = useAuthStore();

/*
 * TODO【新知识：把权限条件作为组件 props】
 * ResourceCrud 不再猜测每个页面谁能修改；父页面把后端对应的权限码和角色传进来。
 * 同一个 canWrite 同时控制新增、编辑、删除，公共组件改一次，所有 CRUD 页面都会生效。
 */
const canWrite = computed(() =>
  canAccess(auth.permissions, {
    roles: p.writeRoles,
    authorities: p.writeAuthority ? [p.writeAuthority] : [],
  }),
);
const loading = ref(false),
  saving = ref(false),
  visible = ref(false),
  editingId = ref<number>();
const rows = ref<Record<string, any>[]>([]),
  total = ref(0);
const query = reactive({ page: 1, size: 10, keyword: "", status: "" });
const form = reactive<Record<string, any>>({});
const formRef = ref<FormInstance>();
const load = async () => {
  loading.value = true;
  try {
    /*
     * TODO【新知识：请求参数规范化】
     * 空字符串也是一个真实的 HTTP 参数。直接发送 status=""，后端会按空状态查询而得到 0 条。
     * 用条件展开只发送用户真正填写的筛选项。
     */
    const params: PageQuery & Record<string, unknown> = {
      page: query.page,
      size: query.size,
      ...(query.keyword.trim() ? { keyword: query.keyword.trim() } : {}),
      ...(query.status ? { status: query.status } : {}),
    };
    const res = await listResource<Record<string, any>>(p.endpoint, params);
    if (Array.isArray(res.data)) {
      rows.value = res.data;
      total.value = res.data.length;
    } else {
      rows.value = res.data.records;
      total.value = res.data.total;
    }
  } finally {
    loading.value = false;
  }
};
const reset = () => {
  query.keyword = "";
  query.status = "";
  query.page = 1;
  load();
};
const openCreate = () => {
  editingId.value = undefined;
  Object.keys(form).forEach((k) => delete form[k]);
  visible.value = true;
};
const openEdit = (row: Record<string, any>) => {
  editingId.value = row.id;
  Object.keys(form).forEach((k) => delete form[k]);
  Object.assign(form, row);
  visible.value = true;
};
const save = async () => {
  if (!(await formRef.value?.validate().catch(() => false))) return;
  saving.value = true;
  try {
    if (editingId.value)
      await updateResource(p.endpoint, editingId.value, form);
    else await createResource(p.endpoint, form);
    visible.value = false;
    await load();
  } finally {
    saving.value = false;
  }
};
const remove = async (id: number) => {
  await deleteResource(p.endpoint, id);
  await load();
};
const format = (
  v: any,
  type?: string,
  column?: Column,
  row?: Record<string, any>,
) => {
  if (column?.formatter && row) return column.formatter(v, row);
  if (v == null) return "-";
  if ((type === "date" || type === "datetime") && typeof v === "string")
    return v.replace("T", " ").slice(0, type === "date" ? 10 : 16);
  return v;
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
@media (max-width: 640px) {
  .filter-bar {
    flex-wrap: wrap;
  }
  .filter-bar .el-input {
    width: 100%;
  }
}
</style>
