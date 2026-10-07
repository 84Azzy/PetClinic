<template>
  <div>
    <PageHeader
      title="电子病历"
      description="把症状、诊断与治疗连接成完整的诊疗记录。"
      ><el-button v-if="canWrite" type="primary" :icon="Plus" @click="edit()"
        >新建病历</el-button
      ></PageHeader
    >
    <section class="panel filter-panel">
      <form class="filter-bar" @submit.prevent="search">
        <el-select
          v-model="query.petId"
          clearable
          filterable
          placeholder="全部宠物"
          aria-label="筛选病历宠物"
          ><el-option
            v-for="pet in pets"
            :key="pet.id"
            :label="pet.name"
            :value="pet.id" /></el-select
        ><el-button type="primary" plain native-type="submit">查询</el-button
        ><el-button
          @click="
            query.petId = undefined;
            search();
          "
          >重置</el-button
        >
      </form>
    </section>
    <PageState v-if="error" class="panel" kind="error" @retry="load" />
    <div v-else class="record-layout">
      <section class="record-list">
        <div class="record-list-head">
          <span>诊疗记录</span><span class="caption">{{ total }} 份病历</span>
        </div>
        <PageState v-if="loading" kind="loading" /><PageState
          v-else-if="!rows.length"
          kind="empty"
          title="暂无病历"
          description="选择一次就诊，建立诊疗记录。"
        />
        <button
          v-for="row in rows"
          v-else
          :key="row.id"
          :class="['record-row', { selected: selected?.id === row.id }]"
          :aria-pressed="selected?.id === row.id"
          @click="select(row)"
        >
          <PetAvatar :src="pet(row.petId)?.photoUrl" :size="44" />
          <div>
            <strong>{{ petName(row.petId) }}</strong
            ><small
              >{{ formatDate(row.createdAt) }} · {{ vetName(row.vetId) }}</small
            ><small class="diagnosis-preview">{{ row.diagnosis }}</small>
          </div>
        </button>
        <div class="pager">
          <el-pagination
            v-model:current-page="query.page"
            :page-size="query.size"
            :total="total"
            layout="prev, pager, next"
            small
            @current-change="load"
          />
        </div>
      </section>
      <article ref="bodyRef" class="record-content" tabindex="-1">
        <PageState
          v-if="!selected"
          kind="empty"
          title="选择一份病历"
          description="完整诊疗过程会显示在这里。"
        /><template v-else>
          <div class="panel-head">
            <div>
              <h2>{{ petName(selected.petId) }}的就诊记录</h2>
              <p>
                {{ formatDate(selected.createdAt, true) }} · 接诊兽医
                {{ vetName(selected.vetId) }}
              </p>
            </div>
            <el-button v-if="canWrite" @click="edit(selected)"
              >编辑病历</el-button
            >
          </div>
          <div class="medical-reference">
            <span>病历 #{{ selected.id }} · 就诊 #{{ selected.visitId }}</span
            ><el-button
              v-if="auth.canVisitPath('/pets')"
              link
              type="primary"
              @click="router.push('/pets?petId=' + selected.petId)"
              >查看宠物档案<ClinicIcon class="direction-icon" name="arrow"
            /></el-button>
          </div>
          <section
            v-for="field in bodyFields"
            :key="field.key"
            class="detail-section"
          >
            <h3>{{ field.label }}</h3>
            <p class="reading-text">{{ selected[field.key] || "未记录" }}</p>
          </section>
          <div v-if="canWrite" class="medical-footer">
            <span class="caption"
              >最近更新 {{ formatDate(selected.updatedAt, true) }}</span
            ><el-popconfirm
              title="确定删除这份病历吗？"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="remove(selected!.id)"
              ><template #reference
                ><el-button type="danger" link :disabled="removing"
                  >删除病历</el-button
                ></template
              ></el-popconfirm
            >
          </div>
        </template>
      </article>
    </div>
    <el-dialog
      v-model="visible"
      :title="editingId ? '编辑病历' : '建立电子病历'"
      width="720px"
      destroy-on-close
      :show-close="!saving"
      :close-on-press-escape="!saving"
      :close-on-click-modal="!saving"
    >
      <el-form ref="formRef" :model="form" label-position="top"
        ><el-form-item
          label="关联就诊"
          prop="visitId"
          :rules="[
            { required: true, message: '请选择就诊', trigger: 'change' },
          ]"
          ><el-select
            v-model="form.visitId"
            :disabled="!!editingId"
            filterable
            placeholder="选择宠物、兽医对应的就诊"
            style="width: 100%"
            ><el-option
              v-for="visit in eligibleVisits"
              :key="visit.id"
              :label="visitLabel(visit)"
              :value="visit.id"
          /></el-select>
          <p class="caption" style="margin: 8px 0 0">
            一条就诊对应一份病历；编辑时保留原就诊关系。
          </p></el-form-item
        >
        <div class="form-grid">
          <el-form-item
            v-for="field in bodyFields"
            :key="field.key"
            :label="field.label"
            :prop="field.key"
            :class="{ 'full-row': field.key === 'notes' }"
            :rules="
              field.required
                ? [
                    {
                      required: true,
                      message: '请填写' + field.label,
                      trigger: 'blur',
                    },
                  ]
                : []
            "
            ><el-input
              v-model="form[field.key]"
              type="textarea"
              :rows="4"
              :placeholder="'填写' + field.label"
          /></el-form-item>
        </div> </el-form
      ><template #footer
        ><el-button :disabled="saving" @click="visible = false">取消</el-button
        ><el-button
          type="primary"
          :disabled="saving"
          :loading="saving"
          @click="save"
          >保存病历</el-button
        ></template
      >
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Plus } from "@element-plus/icons-vue";
import { ElMessage, type FormInstance } from "element-plus";
import PageHeader from "@/components/PageHeader.vue";
import PageState from "@/components/PageState.vue";
import PetAvatar from "@/components/PetAvatar.vue";
import ClinicIcon from "@/components/ClinicIcon.vue";
import {
  listResource,
  createResource,
  updateResource,
  deleteResource,
} from "@/api/resource";
import {
  listAll,
  resolveRows,
  formatDate,
  type ClinicRow,
} from "@/utils/clinic";
import { useAuthStore } from "@/stores/auth";
const auth = useAuthStore(),
  router = useRouter(),
  route = useRoute();
const routePetId = Number(route.query.petId);
const canWrite = computed(
  () =>
    (auth.hasRole("ADMIN") || auth.hasRole("STAFF")) &&
    auth.hasAuthority("medical:manage"),
);
const rows = ref<ClinicRow[]>([]),
  pets = ref<ClinicRow[]>([]),
  vets = ref<ClinicRow[]>([]),
  visits = ref<ClinicRow[]>([]),
  recordVisitIds = ref<number[]>([]),
  selected = ref<ClinicRow>(),
  total = ref(0),
  loading = ref(false),
  error = ref(false),
  visible = ref(false),
  saving = ref(false),
  removing = ref(false),
  editingId = ref<number>(),
  formRef = ref<FormInstance>(),
  bodyRef = ref<HTMLElement>();
const query = reactive({
    page: 1,
    size: 10,
    petId:
      Number.isSafeInteger(routePetId) && routePetId > 0
        ? routePetId
        : undefined,
  }),
  form = reactive<ClinicRow>({});
const bodyFields = [
  { key: "symptoms", label: "症状与主诉", required: true },
  { key: "diagnosis", label: "诊断", required: true },
  { key: "treatment", label: "治疗方案", required: true },
  { key: "prescription", label: "处方", required: false },
  { key: "notes", label: "补充备注", required: false },
];
const pet = (id: number) => pets.value.find((p) => p.id === id),
  petName = (id: number) => pet(id)?.name || "宠物 #" + id,
  vetName = (id: number) =>
    vets.value.find((p) => p.id === id)?.name || "兽医 #" + id;
const eligibleVisits = computed(() =>
  visits.value.filter(
    (v) => !recordVisitIds.value.includes(v.id) || v.id === form.visitId,
  ),
);
const visitLabel = (visit: ClinicRow) =>
  petName(visit.petId) +
  " · " +
  vetName(visit.vetId) +
  " · " +
  formatDate(visit.createdAt) +
  " · 就诊 #" +
  visit.id;
let generation = 0;
async function load() {
  const current = ++generation;
  loading.value = true;
  error.value = false;
  try {
    const r = await listResource<ClinicRow>("/medical-records", query);
    if (current !== generation) return;
    rows.value = Array.isArray(r.data) ? r.data : r.data.records;
    total.value = Array.isArray(r.data) ? r.data.length : r.data.total;
    const found = await Promise.allSettled([
      auth.hasAuthority("pet:manage")
        ? resolveRows(
            "/pets",
            rows.value.map((r) => r.petId),
            pets.value,
          )
        : Promise.resolve(pets.value),
      resolveRows(
        "/vets",
        rows.value.map((r) => r.vetId),
        vets.value,
      ),
    ]);
    if (current !== generation) return;
    if (found[0].status === "fulfilled") pets.value = found[0].value;
    if (found[1].status === "fulfilled") vets.value = found[1].value;
    selected.value =
      rows.value.find((r) => r.id === selected.value?.id) || rows.value[0];
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
async function select(row: ClinicRow) {
  selected.value = row;
  if (innerWidth < 640) {
    await nextTick();
    bodyRef.value?.scrollIntoView({ block: "start" });
    bodyRef.value?.focus();
  }
}
async function edit(row?: ClinicRow) {
  editingId.value = row?.id;
  Object.keys(form).forEach((key) => delete form[key]);
  Object.assign(
    form,
    row
      ? {
          visitId: row.visitId,
          ...Object.fromEntries(
            bodyFields.map((field) => [field.key, row[field.key] || ""]),
          ),
        }
      : {
          visitId: undefined,
          ...Object.fromEntries(bodyFields.map((field) => [field.key, ""])),
        },
  );
  visible.value = true;
  if (auth.hasAuthority("visit:manage")) {
    const found = await Promise.allSettled([
      listAll<ClinicRow>("/visits"),
      listAll<ClinicRow>("/medical-records"),
    ]);
    if (found[0].status === "fulfilled") visits.value = found[0].value;
    if (found[1].status === "fulfilled")
      recordVisitIds.value = found[1].value.map((r) => r.visitId);
  }
  if (row && !visits.value.some((v) => v.id === row.visitId))
    visits.value.push({
      id: row.visitId,
      petId: row.petId,
      vetId: row.vetId,
      createdAt: row.createdAt,
    });
}
async function save() {
  if (saving.value || !(await formRef.value?.validate().catch(() => false)))
    return;
  if (saving.value) return;
  saving.value = true;
  try {
    const data = {
      visitId: form.visitId,
      ...Object.fromEntries(
        bodyFields.map((f) => [f.key, form[f.key]?.trim() || undefined]),
      ),
    };
    const response = editingId.value
      ? await updateResource<ClinicRow>(
          "/medical-records",
          editingId.value,
          data,
        )
      : await createResource<ClinicRow>("/medical-records", data);
    selected.value = response.data;
    if (!editingId.value) query.page = 1;
    visible.value = false;
    ElMessage.success("病历已保存");
    await load();
  } catch {
  } finally {
    saving.value = false;
  }
}
async function remove(id: number) {
  if (removing.value) return;
  removing.value = true;
  try {
    await deleteResource("/medical-records", id);
    ElMessage.success("病历已删除");
    if (rows.value.length === 1 && query.page > 1) query.page--;
    await load();
  } catch {
  } finally {
    removing.value = false;
  }
}
onMounted(async () => {
  const results = await Promise.allSettled([
    auth.hasAuthority("pet:manage")
      ? listAll<ClinicRow>("/pets")
      : Promise.resolve([]),
    listAll<ClinicRow>("/vets"),
  ]);
  if (results[0].status === "fulfilled") pets.value = results[0].value;
  if (results[1].status === "fulfilled") vets.value = results[1].value;
  await load();
});
</script>
<style scoped>
.diagnosis-preview {
  display: -webkit-box !important;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.record-row > div {
  min-width: 0;
}
.medical-reference {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 16px;
  background: var(--clinic-canvas);
  border-radius: 6px;
  font-size: 12px;
  color: var(--clinic-muted);
}
.medical-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24px;
}
.detail-section p {
  margin-bottom: 0;
}
@media (max-width: 767px) {
  .record-layout {
    grid-template-columns: 1fr;
  }
  .record-content {
    border-top: 1px solid var(--clinic-border);
  }
  .record-list {
    border-right: 0;
  }
}
</style>
