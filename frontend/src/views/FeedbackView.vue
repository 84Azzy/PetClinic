<template>
  <div>
    <PageHeader
      title="意见反馈"
      description="认真处理每一条问题，让服务持续变好。"
      ><el-button type="primary" :icon="Plus" @click="open"
        >提交反馈</el-button
      ></PageHeader
    >
    <section class="panel">
      <form class="filter-bar" @submit.prevent>
        <el-input
          v-model="keyword"
          clearable
          placeholder="搜索反馈标题或内容"
          aria-label="搜索反馈"
          :prefix-icon="Search"
        /><el-select
          v-model="status"
          placeholder="全部处理状态"
          aria-label="筛选反馈状态"
          clearable
          ><el-option label="待处理" value="PENDING" /><el-option
            label="已回复"
            value="REPLIED" /><el-option
            label="已关闭"
            value="CLOSED" /></el-select
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
        title="暂无符合条件的反馈"
        description="你的意见和处理进展会显示在这里。"
      />
      <div v-else>
        <article v-for="row in displayed" :key="row.id" class="feedback-row">
          <div class="feedback-main">
            <div class="feedback-title">
              <el-tag type="info">{{ category(row.category) }}</el-tag
              ><button @click="detail(row)">{{ row.title }}</button
              ><StatusTag :status="row.status" />
            </div>
            <p class="feedback-preview">{{ row.content }}</p>
            <p v-if="row.reply" class="reply-preview">
              <strong>诊所回复</strong> {{ row.reply }}
            </p>
            <small class="caption"
              >{{ formatDate(row.createdAt, true) }} · 反馈 #{{ row.id }}</small
            >
          </div>
          <div class="feedback-actions">
            <el-button type="primary" link @click="detail(row)"
              >查看详情</el-button
            ><el-button
              v-if="isManager && row.status !== 'CLOSED'"
              type="primary"
              link
              :disabled="!!actingId"
              @click="reply(row)"
              >回复</el-button
            ><el-popconfirm
              v-if="isManager && row.status !== 'CLOSED'"
              title="确认关闭此反馈？"
              confirm-button-text="关闭反馈"
              cancel-button-text="取消"
              @confirm="close(row.id)"
              ><template #reference
                ><el-button link :disabled="!!actingId"
                  >关闭</el-button
                ></template
              ></el-popconfirm
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
    <el-drawer v-model="detailVisible" title="反馈处理详情" size="640px"
      ><template v-if="selected"
        ><div class="feedback-title">
          <el-tag type="info">{{ category(selected.category) }}</el-tag
          ><StatusTag :status="selected.status" />
        </div>
        <h2 class="feedback-heading">{{ selected.title }}</h2>
        <p class="caption">
          提交于 {{ formatDate(selected.createdAt, true) }} · 反馈 #{{
            selected.id
          }}
        </p>
        <section class="detail-section">
          <h3>问题描述</h3>
          <p class="reading-text">{{ selected.content }}</p>
        </section>
        <section class="detail-section">
          <h3>诊所回复</h3>
          <p class="reading-text">
            {{ selected.reply || "尚未回复，我们会认真阅读并尽快处理。" }}
          </p>
          <p v-if="selected.repliedAt" class="caption">
            {{ formatDate(selected.repliedAt, true) }}
          </p>
        </section>
        <p v-if="selected.contact" class="caption">
          联系方式：{{ selected.contact }}
        </p>
        <el-button
          v-if="isManager && selected.status !== 'CLOSED'"
          type="primary"
          :disabled="!!actingId"
          @click="reply(selected)"
          >回复反馈</el-button
        ></template
      ></el-drawer
    >
    <el-dialog
      v-model="visible"
      title="提交意见反馈"
      width="600px"
      destroy-on-close
      :show-close="!saving"
      :close-on-click-modal="!saving"
      :close-on-press-escape="!saving"
      ><el-form ref="formRef" :model="form" label-position="top"
        ><el-form-item label="反馈分类"
          ><el-select v-model="form.category"
            ><el-option label="服务建议" value="SERVICE" /><el-option
              label="系统问题"
              value="SYSTEM" /><el-option
              label="其他"
              value="OTHER" /></el-select></el-form-item
        ><el-form-item
          label="反馈标题"
          prop="title"
          :rules="[
            { required: true, message: '请填写反馈标题', trigger: 'blur' },
          ]"
          ><el-input v-model="form.title" maxlength="200" /></el-form-item
        ><el-form-item
          label="问题与建议"
          prop="content"
          :rules="[
            { required: true, message: '请描述问题或建议', trigger: 'blur' },
          ]"
          ><el-input
            v-model="form.content"
            type="textarea"
            :rows="5" /></el-form-item
        ><el-form-item label="联系方式（可选）"
          ><el-input
            v-model="form.contact"
            placeholder="方便诊所进一步与你沟通" /></el-form-item></el-form
      ><template #footer
        ><el-button :disabled="saving" @click="visible = false">取消</el-button
        ><el-button
          type="primary"
          :loading="saving"
          :disabled="saving"
          @click="submit"
          >提交反馈</el-button
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
import StatusTag from "@/components/StatusTag.vue";
import { createResource, postAction } from "@/api/resource";
import { listAll, formatDate, type ClinicRow } from "@/utils/clinic";
import { useAuthStore } from "@/stores/auth";
const auth = useAuthStore(),
  isManager = computed(
    () =>
      auth.hasAuthority("feedback:manage") &&
      (auth.hasRole("ADMIN") || auth.hasRole("STAFF")),
  );
const rows = ref<ClinicRow[]>([]),
  selected = ref<ClinicRow>(),
  loading = ref(false),
  error = ref(false),
  visible = ref(false),
  detailVisible = ref(false),
  saving = ref(false),
  actingId = ref<number>(),
  formRef = ref<FormInstance>(),
  keyword = ref(""),
  status = ref(""),
  page = ref(1),
  form = reactive({ category: "SERVICE", title: "", content: "", contact: "" });
const category = (value: string) =>
  ({ SERVICE: "服务建议", SYSTEM: "系统问题", OTHER: "其他" })[value] || value;
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
      isManager.value ? "/feedback" : "/feedback/mine",
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
function open() {
  Object.assign(form, {
    category: "SERVICE",
    title: "",
    content: "",
    contact: "",
  });
  visible.value = true;
}
async function submit() {
  if (saving.value || !(await formRef.value?.validate().catch(() => false)))
    return;
  if (saving.value) return;
  saving.value = true;
  try {
    await createResource("/feedback", {
      ...form,
      title: form.title.trim(),
      content: form.content.trim(),
    });
    visible.value = false;
    ElMessage.success("反馈已提交");
    await load();
  } catch {
  } finally {
    saving.value = false;
  }
}
async function reply(row: ClinicRow) {
  if (actingId.value) return;
  try {
    const result = await ElMessageBox.prompt(
      row.content,
      "回复“" + row.title + "”",
      {
        inputType: "textarea",
        inputValue: row.reply || "",
        confirmButtonText: "提交回复",
        cancelButtonText: "取消",
        inputValidator: (v) => !!v?.trim() || "请填写回复内容",
      },
    );
    actingId.value = row.id;
    await postAction("/feedback/" + row.id + "/reply", {
      reply: result.value.trim(),
    });
    ElMessage.success("回复已提交");
    await load();
  } catch {
  } finally {
    actingId.value = undefined;
  }
}
async function close(id: number) {
  if (actingId.value) return;
  actingId.value = id;
  try {
    await postAction("/feedback/" + id + "/close");
    ElMessage.success("反馈已关闭");
    await load();
  } catch {
  } finally {
    actingId.value = undefined;
  }
}
onMounted(load);
</script>
<style scoped>
.feedback-row {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  padding: 24px 0;
  border-bottom: 1px solid var(--clinic-border);
}
.feedback-row:first-child {
  padding-top: 0;
}
.feedback-main {
  flex: 1;
  min-width: 0;
}
.feedback-title {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.feedback-title button {
  background: none;
  border: 0;
  padding: 0;
  color: var(--clinic-ink);
  font-size: 20px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  overflow-wrap: anywhere;
}
.feedback-preview {
  margin: 16px 0;
  color: var(--clinic-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  overflow-wrap: anywhere;
}
.reply-preview {
  padding: 12px 16px;
  background: var(--clinic-canvas);
  border-radius: 6px;
  overflow-wrap: anywhere;
}
.reply-preview strong {
  color: var(--clinic-primary);
  margin-right: 12px;
}
.feedback-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
  max-width: 180px;
}
.feedback-actions .el-button {
  margin: 0;
}
.feedback-heading {
  font-size: 24px;
  margin: 24px 0 12px;
  overflow-wrap: anywhere;
}
@media (max-width: 767px) {
  .feedback-row {
    flex-direction: column;
    gap: 12px;
  }
  .feedback-actions {
    max-width: none;
  }
  .feedback-title button {
    font-size: 16px;
  }
}
</style>
