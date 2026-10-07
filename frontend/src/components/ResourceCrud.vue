<template>
  <div>
    <PageHeader :title="title" :description="description"
      ><el-button
        v-if="createEnabled && canWrite"
        type="primary"
        :icon="Plus"
        @click="openCreate"
        >新增{{ entityName }}</el-button
      ></PageHeader
    >
    <section class="panel">
      <form
        v-if="keywordEnabled || statuses.length || $slots.filters"
        class="filter-bar"
        @submit.prevent="search"
      >
        <el-input
          v-if="keywordEnabled"
          v-model="query.keyword"
          clearable
          :placeholder="searchPlaceholder"
          :prefix-icon="Search"
          aria-label="搜索关键词"
        />
        <el-select
          v-if="statuses.length"
          v-model="query.status"
          clearable
          placeholder="全部状态"
          aria-label="筛选状态"
          ><el-option
            v-for="item in statuses"
            :key="item.value"
            :label="item.label"
            :value="item.value"
        /></el-select>
        <slot name="filters" :query="query" :search="search" />
        <el-button
          v-if="keywordEnabled || statuses.length || $slots.filters"
          native-type="submit"
          type="primary"
          plain
          >查询</el-button
        >
        <el-button
          v-if="keywordEnabled || statuses.length || $slots.filters"
          @click="reset"
          >重置</el-button
        >
      </form>
      <PageState v-if="error" kind="error" @retry="load" />
      <template v-else>
        <div class="table-wrap desktop-table">
          <el-table
            v-loading="loading"
            :data="displayRows"
            row-key="id"
            :default-expand-all="tree"
            :tree-props="{ children: 'children' }"
          >
            <el-table-column
              v-for="col in columns"
              :key="col.key"
              :prop="col.key"
              :label="col.label"
              :min-width="col.width || 120"
              show-overflow-tooltip
            >
              <template #default="{ row }"
                ><slot
                  :name="'cell-' + col.key"
                  :row="row"
                  :value="row[col.key]"
                  ><StatusTag
                    v-if="col.key === 'status'"
                    :status="row.status"
                  /><span v-else>{{
                    format(row[col.key], col.type, col, row)
                  }}</span></slot
                ></template
              >
            </el-table-column>
            <el-table-column
              label="操作"
              :width="canWrite ? 190 : 80"
              fixed="right"
            >
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail(row)"
                  >查看</el-button
                >
                <el-button
                  v-if="canWrite"
                  link
                  type="primary"
                  @click="openEdit(row)"
                  >编辑</el-button
                >
                <el-popconfirm
                  v-if="canWrite"
                  :title="'确定' + deleteLabel + '这条记录吗？'"
                  confirm-button-text="确认"
                  cancel-button-text="取消"
                  @confirm="remove(row.id)"
                >
                  <template #reference
                    ><el-button
                      link
                      type="danger"
                      :loading="removingId === row.id"
                      :disabled="
                        removingId !== undefined ||
                        (deleteLabel === '停用' && row.status === 'INACTIVE')
                      "
                      >{{ deleteLabel }}</el-button
                    ></template
                  >
                </el-popconfirm>
              </template>
            </el-table-column>
            <template #empty
              ><PageState
                kind="empty"
                :title="
                  query.keyword || query.status
                    ? '没有符合条件的记录'
                    : '还没有' + entityName
                "
                :description="
                  query.keyword || query.status
                    ? '试试调整筛选条件，或重置后查看全部记录。'
                    : '新增记录后，它们会显示在这里。'
                "
            /></template>
          </el-table>
        </div>
        <div class="mobile-records" v-loading="loading">
          <article
            v-for="row in mobileRows"
            :key="row.id"
            class="mobile-record-card"
          >
            <dl>
              <div v-for="col in columns" :key="col.key">
                <dt>{{ col.label }}</dt>
                <dd>
                  <slot
                    :name="'cell-' + col.key"
                    :row="row"
                    :value="row[col.key]"
                    ><StatusTag
                      v-if="col.key === 'status'"
                      :status="row.status"
                    /><span v-else>{{
                      format(row[col.key], col.type, col, row)
                    }}</span></slot
                  >
                </dd>
              </div>
            </dl>
            <div class="mobile-card-actions">
              <el-button type="primary" @click="openDetail(row)"
                >查看详情</el-button
              ><el-button v-if="canWrite" @click="openEdit(row)">编辑</el-button
              ><el-popconfirm
                v-if="canWrite"
                :title="'确认' + deleteLabel + '此记录？'"
                confirm-button-text="确认"
                cancel-button-text="取消"
                @confirm="remove(row.id)"
                ><template #reference
                  ><el-button
                    type="danger"
                    plain
                    :disabled="
                      removingId !== undefined ||
                      (deleteLabel === '停用' && row.status === 'INACTIVE')
                    "
                    >{{ deleteLabel }}</el-button
                  ></template
                ></el-popconfirm
              >
            </div>
          </article>
          <PageState
            v-if="!loading && !mobileRows.length"
            kind="empty"
            title="没有符合条件的记录"
            description="调整筛选条件，或新增一条记录。"
          />
        </div>
        <div v-if="!tree" class="pager">
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            background
            layout="total, sizes, prev, pager, next"
            :page-sizes="[10, 20, 50]"
            :total="total"
            @current-change="load"
            @size-change="search"
          />
        </div>
      </template>
    </section>
    <el-dialog
      v-model="visible"
      :title="(editingId ? '编辑' : '新增') + entityName"
      width="620px"
      destroy-on-close
      :close-on-click-modal="!saving"
      :close-on-press-escape="!saving"
      :show-close="!saving"
    >
      <el-form
        ref="formRef"
        :model="form"
        label-position="top"
        @submit.prevent="save"
      >
        <div class="form-grid">
          <el-form-item
            v-for="field in fields"
            :key="field.key"
            :class="{ 'full-row': field.type === 'textarea' }"
            :label="field.label"
            :prop="field.key"
            :rules="
              field.required
                ? [
                    {
                      required: true,
                      message: '请填写' + field.label,
                      trigger: field.type === 'select' ? 'change' : 'blur',
                    },
                  ]
                : []
            "
          >
            <el-select
              v-if="field.type === 'select'"
              v-model="form[field.key]"
              filterable
              clearable
              :placeholder="'请选择' + field.label"
              style="width: 100%"
              ><el-option
                v-for="item in field.options"
                :key="String(item.value)"
                :label="item.label"
                :value="item.value"
            /></el-select>
            <el-date-picker
              v-else-if="field.type === 'date' || field.type === 'datetime'"
              v-model="form[field.key]"
              :type="field.type"
              :value-format="
                field.type === 'date' ? 'YYYY-MM-DD' : 'YYYY-MM-DDTHH:mm:ss'
              "
              style="width: 100%"
            />
            <el-input-number
              v-else-if="field.type === 'number'"
              v-model="form[field.key]"
              :min="field.min ?? (field.key === 'sortOrder' ? 0 : 1)"
              style="width: 100%"
            />
            <el-input
              v-else
              v-model="form[field.key]"
              :type="
                field.type === 'textarea'
                  ? 'textarea'
                  : field.type === 'password'
                    ? 'password'
                    : 'text'
              "
              :show-password="field.type === 'password'"
              :rows="4"
              :placeholder="'请输入' + field.label"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer
        ><el-button :disabled="saving" @click="visible = false">取消</el-button
        ><el-button
          type="primary"
          :loading="saving"
          :disabled="saving"
          @click="save"
          >保存{{ entityName }}</el-button
        ></template
      >
    </el-dialog>
    <el-drawer
      v-model="detailVisible"
      :title="entityName + '详情'"
      size="620px"
    >
      <template v-if="selected">
        <slot name="detail" :row="selected">
          <dl class="detail-grid">
            <div
              v-for="col in detailColumns"
              :key="col.key"
              :class="['detail-item', { wide: col.type === 'textarea' }]"
            >
              <dt>{{ col.label }}</dt>
              <dd>
                <StatusTag
                  v-if="col.key === 'status'"
                  :status="selected.status"
                /><span v-else>{{
                  format(selected[col.key], col.type, col, selected)
                }}</span>
              </dd>
            </div>
          </dl>
          <p class="caption" style="margin-top: 32px">
            记录编号 #{{ selected.id }}
          </p>
        </slot>
      </template>
    </el-drawer>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { Plus, Search } from "@element-plus/icons-vue";
import { ElMessage, type FormInstance } from "element-plus";
import PageHeader from "./PageHeader.vue";
import PageState from "./PageState.vue";
import StatusTag from "./StatusTag.vue";
import {
  listResource,
  createResource,
  updateResource,
  deleteResource,
} from "@/api/resource";
import { useAuthStore } from "@/stores/auth";
import { canAccess, type Role } from "@/utils/permission";
import { buildTree, flattenTree, type ClinicRow } from "@/utils/clinic";
export interface Column {
  key: string;
  label: string;
  width?: number;
  type?: "text" | "textarea" | "date" | "datetime";
  formatter?: (value: unknown, row: ClinicRow) => string;
}
export interface Field {
  key: string;
  label: string;
  type?:
    | "text"
    | "textarea"
    | "number"
    | "date"
    | "datetime"
    | "select"
    | "password";
  required?: boolean;
  min?: number;
  defaultValue?: unknown;
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
    keywordEnabled?: boolean;
    searchPlaceholder?: string;
    statusOptions?: { label: string; value: string }[];
    deleteLabel?: string;
    tree?: boolean;
    extraParams?: Record<string, unknown>;
  }>(),
  {
    createEnabled: true,
    keywordEnabled: true,
    searchPlaceholder: "输入名称或关键词",
    deleteLabel: "删除",
    tree: false,
  },
);
const auth = useAuthStore();
const entityName = computed(() => p.title.replace(/管理$/, ""));
const canWrite = computed(() =>
  canAccess(auth.permissions, {
    roles: p.writeRoles,
    authorities: p.writeAuthority ? [p.writeAuthority] : [],
  }),
);
const statuses = computed(
  () =>
    p.statusOptions ||
    (p.columns.some((col) => col.key === "status")
      ? [
          { label: "启用", value: "ACTIVE" },
          { label: "停用", value: "INACTIVE" },
        ]
      : []),
);
const emit = defineEmits<{ reset: [] }>();
const rows = ref<ClinicRow[]>([]),
  serverTotal = ref(0),
  clientMode = ref(false),
  loading = ref(false),
  error = ref(false),
  saving = ref(false),
  visible = ref(false),
  detailVisible = ref(false);
const editingId = ref<number>(),
  removingId = ref<number>(),
  selected = ref<ClinicRow>(),
  formRef = ref<FormInstance>();
const query = reactive({ page: 1, size: 10, keyword: "", status: "" });
const form = reactive<ClinicRow>({});
const filteredRows = computed(() =>
  clientMode.value
    ? rows.value.filter(
        (row) =>
          (!query.status || row.status === query.status) &&
          (!query.keyword.trim() ||
            p.columns.some((col) =>
              String(row[col.key] || "")
                .toLowerCase()
                .includes(query.keyword.trim().toLowerCase()),
            )),
      )
    : rows.value,
);
const total = computed(() =>
  clientMode.value ? filteredRows.value.length : serverTotal.value,
);
const displayRows = computed(() =>
  p.tree
    ? buildTree(filteredRows.value)
    : clientMode.value
      ? filteredRows.value.slice(
          (query.page - 1) * query.size,
          query.page * query.size,
        )
      : rows.value,
);
const mobileRows = computed(() =>
  p.tree ? filteredRows.value : displayRows.value,
);
const detailColumns = computed(() => [
  ...p.columns,
  ...p.fields
    .filter(
      (field) =>
        !p.columns.some((col) => col.key === field.key) &&
        field.type !== "password" &&
        field.key !== "password",
    )
    .map((field) => ({
      key: field.key,
      label: field.label,
      type: field.type === "textarea" ? ("textarea" as const) : undefined,
    })),
]);
let generation = 0;
async function load() {
  const current = ++generation;
  loading.value = true;
  error.value = false;
  try {
    const response = await listResource<ClinicRow>(p.endpoint, {
      page: query.page,
      size: p.tree ? 100 : query.size,
      ...p.extraParams,
      ...(query.keyword.trim() ? { keyword: query.keyword.trim() } : {}),
      ...(query.status ? { status: query.status } : {}),
    });
    if (current !== generation) return;
    clientMode.value = Array.isArray(response.data);
    if (Array.isArray(response.data)) {
      rows.value = p.tree ? flattenTree(response.data) : response.data;
      serverTotal.value = rows.value.length;
    } else {
      rows.value = response.data.records;
      serverTotal.value = response.data.total;
    }
    if (
      p.tree &&
      !Array.isArray(response.data) &&
      response.data.total > rows.value.length
    ) {
      const all = [...rows.value];
      for (let page = 2; all.length < response.data.total; page++) {
        const next = await listResource<ClinicRow>(p.endpoint, {
          page,
          size: 100,
          ...p.extraParams,
        });
        if (current !== generation) return;
        if (Array.isArray(next.data) || !next.data.records.length) break;
        all.push(...next.data.records);
      }
      rows.value = all;
    }
    if (selected.value)
      selected.value =
        rows.value.find((row) => row.id === selected.value?.id) ||
        selected.value;
  } catch {
    if (current === generation) error.value = true;
  } finally {
    if (current === generation) loading.value = false;
  }
}
function search() {
  query.page = 1;
  load();
}
function reset() {
  query.keyword = "";
  query.status = "";
  query.page = 1;
  emit("reset");
  load();
}
function clearForm() {
  Object.keys(form).forEach((key) => delete form[key]);
}
function openCreate() {
  editingId.value = undefined;
  clearForm();
  p.fields.forEach((field) => {
    if (field.defaultValue !== undefined) form[field.key] = field.defaultValue;
    else if (field.key === "status") form[field.key] = "ACTIVE";
  });
  visible.value = true;
}
function openEdit(row: ClinicRow) {
  editingId.value = row.id;
  clearForm();
  p.fields.forEach((field) => {
    if (field.key !== "password") form[field.key] = row[field.key];
  });
  visible.value = true;
}
function openDetail(row: ClinicRow) {
  selected.value = row;
  detailVisible.value = true;
}
async function save() {
  if (saving.value || !(await formRef.value?.validate().catch(() => false)))
    return;
  if (saving.value) return;
  saving.value = true;
  try {
    const data = Object.fromEntries(
      p.fields
        .filter(
          (field) =>
            form[field.key] !== undefined &&
            !(field.key === "password" && !form[field.key]),
        )
        .map((field) => [field.key, form[field.key]]),
    );
    editingId.value
      ? await updateResource(p.endpoint, editingId.value, data)
      : await createResource(p.endpoint, data);
    ElMessage.success("记录已保存");
    visible.value = false;
    await load();
  } catch {
    /* The shared HTTP handler provides the API error; keep the form for retry. */
  } finally {
    saving.value = false;
  }
}
async function remove(id: number) {
  if (removingId.value !== undefined) return;
  removingId.value = id;
  try {
    await deleteResource(p.endpoint, id);
    ElMessage.success("记录已" + (p.deleteLabel === "停用" ? "停用" : "删除"));
    if (
      rows.value.length === 1 &&
      query.page > 1 &&
      (p.deleteLabel !== "停用" || query.status === "ACTIVE")
    )
      query.page--;
    await load();
  } catch {
  } finally {
    removingId.value = undefined;
  }
}
function format(
  value: any,
  type?: string,
  column?: Column,
  row?: ClinicRow,
): string {
  if (column?.formatter && row) return column.formatter(value, row);
  if (value === null || value === undefined || value === "") return "未记录";
  if ((type === "date" || type === "datetime") && typeof value === "string")
    return value.replace("T", " ").slice(0, type === "date" ? 10 : 16);
  return String(value);
}
watch(() => p.extraParams, search, { deep: true });
onMounted(load);
defineExpose({ reload: load });
</script>
