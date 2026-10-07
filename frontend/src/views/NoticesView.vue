<template>
  <div>
    <PageHeader
      title="诊所公告"
      description="门诊安排、服务通知与宠物健康提醒。"
      ><el-button v-if="isManager" type="primary" :icon="Plus" @click="edit()"
        >新建公告</el-button
      ></PageHeader
    >
    <section class="panel">
      <form class="filter-bar" @submit.prevent>
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索公告标题或内容"
          aria-label="搜索公告"
          :prefix-icon="Search"
        /><el-select
          v-if="isManager"
          v-model="status"
          clearable
          placeholder="全部状态"
          aria-label="筛选公告状态"
          ><el-option label="已发布" value="PUBLISHED" /><el-option
            label="草稿"
            value="DRAFT" /><el-option
            label="已撤回"
            value="WITHDRAWN" /></el-select
        ><el-button
          @click="
            keyword = '';
            status = '';
          "
          >重置</el-button
        >
      </form>
      <PageState v-if="loading" kind="loading" /><PageState
        v-else-if="error"
        kind="error"
        @retry="load"
      /><PageState
        v-else-if="!filtered.length"
        kind="empty"
        title="暂无符合条件的公告"
        description="发布后的有效通知会显示在这里。"
      />
      <div v-else class="notice-list">
        <article v-for="row in displayed" :key="row.id" class="notice-row">
          <span class="notice-symbol"><ClinicIcon name="file" /></span>
          <div class="notice-copy">
            <div class="notice-title">
              <button @click="detail(row)">{{ row.title }}</button
              ><StatusTag v-if="isManager" :status="row.status" />
            </div>
            <p>{{ row.content }}</p>
            <small
              >{{
                row.publishedAt
                  ? "发布于 " + formatDate(row.publishedAt, true)
                  : "尚未发布"
              }}<span v-if="row.expiresAt">
                · 到期 {{ formatDate(row.expiresAt, true) }}</span
              ></small
            >
          </div>
          <div class="notice-actions">
            <el-button type="primary" link @click="detail(row)"
              >查看全文</el-button
            ><template v-if="isManager"
              ><el-button link @click="edit(row)">编辑</el-button
              ><el-button
                link
                type="primary"
                :disabled="!!actingId"
                @click="
                  action(
                    row,
                    row.status === 'PUBLISHED' ? 'withdraw' : 'publish',
                  )
                "
                >{{ row.status === "PUBLISHED" ? "撤回" : "发布" }}</el-button
              ><el-popconfirm
                title="确认删除此公告？"
                confirm-button-text="删除"
                cancel-button-text="取消"
                @confirm="remove(row.id)"
                ><template #reference
                  ><el-button link type="danger" :disabled="!!actingId"
                    >删除</el-button
                  ></template
                ></el-popconfirm
              ></template
            >
          </div>
        </article>
      </div>
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          :page-size="10"
          :total="filtered.length"
          layout="total, prev, pager, next"
        />
      </div>
    </section>
    <el-drawer v-model="detailVisible" title="公告全文" size="680px"
      ><article v-if="selected" class="notice-body">
        <StatusTag v-if="isManager" :status="selected.status" />
        <h2>{{ selected.title }}</h2>
        <p class="caption">
          {{
            selected.publishedAt
              ? "发布于 " + formatDate(selected.publishedAt, true)
              : "公告草稿"
          }}
        </p>
        <div class="reading-text">{{ selected.content }}</div>
        <p v-if="selected.expiresAt" class="caption">
          有效期至 {{ formatDate(selected.expiresAt, true) }}
        </p>
      </article></el-drawer
    >
    <el-dialog
      v-model="visible"
      :title="editingId ? '编辑公告' : '新建公告'"
      width="680px"
      destroy-on-close
      :show-close="!saving"
      :close-on-click-modal="!saving"
      :close-on-press-escape="!saving"
      ><el-form ref="formRef" :model="form" label-position="top"
        ><el-form-item
          label="公告标题"
          prop="title"
          :rules="[
            { required: true, message: '请填写公告标题', trigger: 'blur' },
          ]"
          ><el-input
            v-model="form.title"
            maxlength="200"
            show-word-limit /></el-form-item
        ><el-form-item
          label="公告正文"
          prop="content"
          :rules="[{ required: true, message: '请填写正文', trigger: 'blur' }]"
          ><el-input v-model="form.content" type="textarea" :rows="8"
        /></el-form-item>
        <div class="form-grid">
          <el-form-item label="到期时间（可选）"
            ><el-date-picker
              v-model="form.expiresAt"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%" /></el-form-item
          ><el-form-item label="展示排序"
            ><el-input-number v-model="form.sortOrder" :min="0"
          /></el-form-item>
        </div>
        <p class="caption">
          {{
            rows.some(
              (row) => row.id === editingId && row.status === "PUBLISHED",
            )
              ? "这条公告已发布，保存后将更新主人可见的内容。"
              : "保存后可在公告列表中发布，确认后主人才能看到。"
          }}
        </p></el-form
      ><template #footer
        ><el-button :disabled="saving" @click="visible = false">取消</el-button
        ><el-button
          type="primary"
          :loading="saving"
          :disabled="saving"
          @click="save"
          >保存公告</el-button
        ></template
      ></el-dialog
    >
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { Plus, Search } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox, type FormInstance } from "element-plus";
import PageHeader from "@/components/PageHeader.vue";
import PageState from "@/components/PageState.vue";
import ClinicIcon from "@/components/ClinicIcon.vue";
import StatusTag from "@/components/StatusTag.vue";
import {
  createResource,
  deleteResource,
  postAction,
  updateResource,
} from "@/api/resource";
import { listAll, formatDate, type ClinicRow } from "@/utils/clinic";
import { useAuthStore } from "@/stores/auth";
const auth = useAuthStore(),
  isManager = computed(
    () =>
      auth.hasAuthority("notice:manage") &&
      (auth.hasRole("ADMIN") || auth.hasRole("STAFF")),
  );
const rows = ref<ClinicRow[]>([]),
  selected = ref<ClinicRow>(),
  loading = ref(false),
  error = ref(false),
  visible = ref(false),
  detailVisible = ref(false),
  saving = ref(false),
  editingId = ref<number>(),
  actingId = ref<number>(),
  formRef = ref<FormInstance>(),
  keyword = ref(""),
  status = ref(""),
  page = ref(1),
  form = reactive<ClinicRow>({});
const filtered = computed(() =>
    rows.value.filter(
      (row) =>
        (!status.value || row.status === status.value) &&
        (!keyword.value.trim() ||
          (row.title + " " + row.content).includes(keyword.value.trim())),
    ),
  ),
  displayed = computed(() =>
    filtered.value.slice((page.value - 1) * 10, page.value * 10),
  );
watch([keyword, status], () => (page.value = 1));
async function load() {
  loading.value = true;
  error.value = false;
  try {
    rows.value = await listAll(
      isManager.value ? "/notices" : "/notices/active",
    );
    if (selected.value)
      selected.value =
        rows.value.find((r) => r.id === selected.value?.id) || selected.value;
    if (page.value > 1 && !displayed.value.length) page.value--;
  } catch {
    error.value = true;
  } finally {
    loading.value = false;
  }
}
function detail(row: ClinicRow) {
  selected.value = row;
  detailVisible.value = true;
}
function edit(row?: ClinicRow) {
  editingId.value = row?.id;
  Object.assign(form, {
    title: row?.title || "",
    content: row?.content || "",
    expiresAt: row?.expiresAt || null,
    sortOrder: row?.sortOrder || 0,
  });
  visible.value = true;
}
async function save() {
  if (saving.value || !(await formRef.value?.validate().catch(() => false)))
    return;
  if (saving.value) return;
  saving.value = true;
  try {
    const data = {
      ...form,
      title: form.title.trim(),
      content: form.content.trim(),
    };
    editingId.value
      ? await updateResource("/notices", editingId.value, data)
      : await createResource("/notices", data);
    visible.value = false;
    ElMessage.success("公告已保存");
    await load();
  } catch {
  } finally {
    saving.value = false;
  }
}
async function action(row: ClinicRow, operation: string) {
  if (actingId.value) return;
  const yes = await ElMessageBox.confirm(
    operation === "publish"
      ? "发布后主人将看到这条公告。"
      : "撤回后主人将不再看到这条公告。",
    operation === "publish" ? "发布公告" : "撤回公告",
    {
      confirmButtonText: operation === "publish" ? "确认发布" : "确认撤回",
      cancelButtonText: "取消",
    },
  ).catch(() => false);
  if (!yes) return;
  actingId.value = row.id;
  try {
    await postAction("/notices/" + row.id + "/" + operation);
    ElMessage.success(operation === "publish" ? "公告已发布" : "公告已撤回");
    await load();
  } catch {
  } finally {
    actingId.value = undefined;
  }
}
async function remove(id: number) {
  if (actingId.value) return;
  actingId.value = id;
  try {
    await deleteResource("/notices", id);
    ElMessage.success("公告已删除");
    await load();
  } catch {
  } finally {
    actingId.value = undefined;
  }
}
onMounted(load);
</script>
<style scoped>
.notice-row {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  padding: 24px 0;
  border-bottom: 1px solid var(--clinic-border);
}
.notice-row:first-child {
  padding-top: 0;
}
.notice-symbol {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  flex: none;
  background: #edf5f0;
  color: var(--clinic-primary);
  border-radius: 6px;
}
.notice-symbol svg {
  width: 24px;
}
.notice-copy {
  flex: 1;
  min-width: 0;
}
.notice-title {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}
.notice-title button {
  font-size: 20px;
  font-weight: 600;
  color: var(--clinic-ink);
  border: 0;
  padding: 0;
  background: none;
  text-align: left;
  cursor: pointer;
  overflow-wrap: anywhere;
}
.notice-title button:hover {
  color: var(--clinic-primary);
}
.notice-copy p {
  margin: 12px 0;
  color: var(--clinic-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  overflow-wrap: anywhere;
}
.notice-copy small {
  color: var(--clinic-muted);
  font-size: 12px;
}
.notice-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  max-width: 180px;
  justify-content: flex-end;
}
.notice-actions .el-button {
  margin: 0;
}
.notice-body h2 {
  margin: 20px 0 12px;
  font-size: 28px;
  overflow-wrap: anywhere;
}
.notice-body .reading-text {
  margin: 24px 0 32px;
}
@media (max-width: 767px) {
  .notice-row {
    flex-wrap: wrap;
    gap: 12px;
  }
  .notice-copy {
    flex-basis: calc(100% - 56px);
  }
  .notice-actions {
    max-width: none;
    width: 100%;
    justify-content: flex-start;
    margin-left: 56px;
  }
  .notice-title button {
    font-size: 16px;
  }
}
</style>
