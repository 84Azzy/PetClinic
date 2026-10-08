<template>
  <div>
    <PageHeader
      title="预约就诊"
      :description="
        canCreate
          ? '为宠物选择医生与时间，跟进每一次就诊。'
          : '查看预约安排，准备并完成接诊。'
      "
      ><el-button
        v-if="canCreate"
        type="primary"
        :icon="Plus"
        @click="openCreate"
        >创建预约</el-button
      ></PageHeader
    >
    <section class="panel">
      <form class="filter-bar" @submit.prevent="search">
        <el-input
          v-model="query.keyword"
          placeholder="搜索就诊原因"
          aria-label="搜索就诊原因"
          :prefix-icon="Search"
          clearable
        />
        <el-select
          v-model="query.petId"
          placeholder="全部宠物"
          aria-label="筛选宠物"
          filterable
          clearable
          ><el-option
            v-for="pet in pets"
            :key="pet.id"
            :value="pet.id"
            :label="pet.name"
        /></el-select>
        <el-select
          v-model="query.vetId"
          placeholder="全部兽医"
          aria-label="筛选兽医"
          filterable
          clearable
          ><el-option
            v-for="vet in vets"
            :key="vet.id"
            :value="vet.id"
            :label="vet.name"
        /></el-select>
        <el-select
          v-model="query.status"
          placeholder="全部状态"
          aria-label="筛选预约状态"
          clearable
          ><el-option label="待就诊" value="SCHEDULED" /><el-option
            label="已完成"
            value="COMPLETED" /><el-option label="已取消" value="CANCELLED"
        /></el-select>
        <el-button native-type="submit" type="primary" plain>查询</el-button
        ><el-button @click="resetQuery">重置</el-button>
      </form>
      <PageState v-if="error" kind="error" @retry="load" />
      <template v-else>
        <el-table
          v-loading="loading"
          :data="rows"
          row-key="id"
          class="desktop-table"
        >
          <el-table-column label="就诊宠物" min-width="180"
            ><template #default="{ row }"
              ><div class="pet-cell">
                <PetAvatar
                  :src="pet(row.petId)?.photoUrl"
                  :name="petName(row.petId)"
                  :size="40"
                />
                <div>
                  <strong>{{ petName(row.petId) }}</strong
                  ><small>{{ pet(row.petId)?.breed || "品种未记录" }}</small>
                </div>
              </div></template
            ></el-table-column
          >
          <el-table-column label="接诊兽医" min-width="170"
            ><template #default="{ row }"
              ><div class="vet-cell">
                <VetAvatar
                  :src="vet(row.vetId)?.avatarUrl"
                  :name="vetName(row.vetId)"
                  :size="36"
                  :version="vet(row.vetId)?.updatedAt"
                />
                <span>{{ vetName(row.vetId) }}</span>
              </div></template
            ></el-table-column
          >
          <el-table-column label="预约时间" min-width="210"
            ><template #default="{ row }">{{
              appointmentTime(row.slotId)
            }}</template></el-table-column
          >
          <el-table-column
            prop="reason"
            label="就诊原因"
            min-width="180"
            show-overflow-tooltip
          />
          <el-table-column label="状态" width="100"
            ><template #default="{ row }"
              ><StatusTag :status="row.status" /></template
          ></el-table-column>
          <el-table-column
            label="操作"
            :width="canCreate || isStaff ? 180 : 80"
            fixed="right"
            ><template #default="{ row }">
              <el-button type="primary" link @click="openDetail(row)"
                >查看</el-button
              >
              <el-button
                v-if="canCancel(row)"
                type="danger"
                link
                :disabled="!!actingId"
                @click="cancel(row)"
                >取消预约</el-button
              >
              <el-button
                v-if="canComplete(row)"
                type="primary"
                link
                :disabled="!!actingId"
                @click="complete(row)"
                >完成就诊</el-button
              >
            </template></el-table-column
          >
          <template #empty
            ><PageState
              kind="empty"
              :title="filtered ? '没有符合条件的预约' : '还没有预约记录'"
              :description="
                filtered
                  ? '调整条件或重置筛选，查看其他预约。'
                  : canCreate
                    ? '从上方创建预约开始，选择宠物、医生和时间。'
                    : '新的预约会显示在这里。'
              "
          /></template>
        </el-table>
        <div v-loading="loading" class="mobile-records">
          <article v-for="row in rows" :key="row.id" class="mobile-record-card">
            <div class="panel-head">
              <div class="pet-cell">
                <PetAvatar
                  :src="pet(row.petId)?.photoUrl"
                  :size="44"
                /><strong>{{ petName(row.petId) }}</strong>
              </div>
              <StatusTag :status="row.status" />
            </div>
            <dl>
              <div>
                <dt>接诊兽医</dt>
                <dd class="vet-cell">
                  <VetAvatar
                    :src="vet(row.vetId)?.avatarUrl"
                    :name="vetName(row.vetId)"
                    :size="32"
                    :version="vet(row.vetId)?.updatedAt"
                  />
                  <span>{{ vetName(row.vetId) }}</span>
                </dd>
              </div>
              <div>
                <dt>预约时间</dt>
                <dd>{{ appointmentTime(row.slotId) }}</dd>
              </div>
              <div>
                <dt>就诊原因</dt>
                <dd>{{ row.reason }}</dd>
              </div>
            </dl>
            <div class="mobile-card-actions">
              <el-button type="primary" plain @click="openDetail(row)"
                >查看详情</el-button
              ><el-button
                v-if="canCancel(row)"
                type="danger"
                plain
                :disabled="!!actingId"
                @click="cancel(row)"
                >取消预约</el-button
              ><el-button
                v-if="canComplete(row)"
                :disabled="!!actingId"
                @click="complete(row)"
                >完成就诊</el-button
              >
            </div>
          </article>
          <PageState
            v-if="!loading && !rows.length"
            kind="empty"
            title="没有符合条件的预约"
            description="调整筛选，或创建新的预约。"
          />
        </div>
        <div class="pager">
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="load"
            @size-change="search"
          />
        </div>
      </template>
    </section>
    <el-drawer v-model="detailVisible" title="预约详情" size="600px">
      <template v-if="selected"
        ><div class="pet-identity">
          <PetAvatar :src="pet(selected.petId)?.photoUrl" :size="72" />
          <div>
            <h2>{{ petName(selected.petId) }}</h2>
            <p>
              {{ vetName(selected.vetId) }} ·
              {{ appointmentTime(selected.slotId) }}
            </p>
          </div>
          <StatusTag :status="selected.status" />
        </div>
        <dl class="detail-grid" style="margin-top: 32px">
          <div class="detail-item wide">
            <dt>就诊原因</dt>
            <dd>{{ selected.reason }}</dd>
          </div>
          <div class="detail-item">
            <dt>创建时间</dt>
            <dd>{{ formatDate(selected.createdAt, true) }}</dd>
          </div>
          <div class="detail-item">
            <dt>预约编号</dt>
            <dd>#{{ selected.id }}</dd>
          </div>
          <div v-if="selected.cancelReason" class="detail-item wide">
            <dt>取消原因</dt>
            <dd>{{ selected.cancelReason }}</dd>
          </div>
          <div v-if="selected.cancelledAt" class="detail-item">
            <dt>取消时间</dt>
            <dd>{{ formatDate(selected.cancelledAt, true) }}</dd>
          </div>
        </dl>
        <div class="detail-actions">
          <el-button
            v-if="auth.canVisitPath('/pets')"
            @click="router.push('/pets?petId=' + selected.petId)"
            >查看宠物档案</el-button
          ><el-button
            v-if="canCancel(selected)"
            type="danger"
            plain
            :disabled="!!actingId"
            @click="cancel(selected)"
            >取消预约</el-button
          ><el-button
            v-if="canComplete(selected)"
            type="primary"
            :disabled="!!actingId"
            @click="complete(selected)"
            >完成就诊</el-button
          >
        </div>
      </template>
    </el-drawer>
    <el-dialog
      v-model="dialogVisible"
      title="为宠物预约就诊"
      width="640px"
      destroy-on-close
      :close-on-click-modal="!saving"
      :close-on-press-escape="!saving"
      :show-close="!saving"
      @closed="resetForm"
    >
      <el-steps :active="step" simple class="booking-steps"
        ><el-step title="宠物与原因" /><el-step title="医生与时段" /><el-step
          title="确认预约"
      /></el-steps>
      <el-alert
        v-if="!activePets.length"
        title="暂无可预约的宠物，请联系诊所建立有效档案。"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div v-show="step === 0">
          <el-form-item label="就诊宠物" prop="petId"
            ><el-select
              v-model="form.petId"
              placeholder="请选择宠物"
              filterable
              style="width: 100%"
              ><el-option
                v-for="item in activePets"
                :key="item.id"
                :label="item.name"
                :value="item.id" /></el-select></el-form-item
          ><el-form-item label="就诊原因" prop="reason"
            ><el-input
              v-model="form.reason"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="描述症状、持续时间或本次就诊目的"
          /></el-form-item>
        </div>
        <div v-show="step === 1">
          <div class="form-grid">
            <el-form-item label="接诊兽医" prop="vetId"
              ><el-select
                v-model="form.vetId"
                placeholder="请选择兽医"
                filterable
                style="width: 100%"
                ><el-option
                  v-for="item in activeVets"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id" /></el-select></el-form-item
            ><el-form-item label="就诊日期" prop="date"
              ><el-date-picker
                v-model="form.date"
                type="date"
                value-format="YYYY-MM-DD"
                :disabled-date="disablePastDate"
                style="width: 100%"
            /></el-form-item>
          </div>
          <el-form-item label="可用时段" prop="slotId"
            ><el-select
              v-model="form.slotId"
              :loading="slotsLoading"
              :disabled="!form.vetId || !form.date || slotsLoading"
              :placeholder="slotPlaceholder"
              style="width: 100%"
              ><el-option
                v-for="slot in availableSlots"
                :key="slot.id"
                :label="availableSlotLabel(slot)"
                :value="slot.id" /></el-select
          ></el-form-item>
          <el-alert
            v-if="
              form.vetId &&
              form.date &&
              !slotsLoading &&
              !availableSlots.length &&
              !slotsError
            "
            title="当天暂无可用时段，请更换日期或兽医。"
            type="info"
            :closable="false"
          />
          <PageState
            v-if="slotsError"
            kind="error"
            title="时段查询失败"
            description="可以保留当前选择并重试。"
            @retry="loadSlots"
          />
        </div>
        <div v-if="step === 2" class="booking-review">
          <h3>确认本次安排</h3>
          <dl class="detail-grid">
            <div class="detail-item">
              <dt>宠物</dt>
              <dd>{{ petName(form.petId!) }}</dd>
            </div>
            <div class="detail-item">
              <dt>接诊兽医</dt>
              <dd>{{ vetName(form.vetId!) }}</dd>
            </div>
            <div class="detail-item wide">
              <dt>就诊时间</dt>
              <dd>
                {{ form.date }} ·
                {{
                  availableSlotLabel(
                    availableSlots.find((s) => s.id === form.slotId)!,
                  )
                }}
              </dd>
            </div>
            <div class="detail-item wide">
              <dt>就诊原因</dt>
              <dd>{{ form.reason }}</dd>
            </div>
          </dl>
          <p class="caption">
            确认后创建预约。请按预约时间到诊，携带既往健康记录。
          </p>
        </div>
      </el-form>
      <template #footer
        ><el-button
          :disabled="saving"
          @click="step ? step-- : (dialogVisible = false)"
          >{{ step ? "上一步" : "取消" }}</el-button
        ><el-button
          v-if="step < 2"
          type="primary"
          :disabled="!activePets.length || slotsLoading"
          @click="nextStep"
          >下一步</el-button
        ><el-button
          v-else
          type="primary"
          :loading="saving"
          :disabled="saving"
          @click="save"
          >确认创建预约</el-button
        ></template
      >
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Plus, Search } from "@element-plus/icons-vue";
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
} from "element-plus";
import PageHeader from "@/components/PageHeader.vue";
import PageState from "@/components/PageState.vue";
import PetAvatar from "@/components/PetAvatar.vue";
import VetAvatar from "@/components/VetAvatar.vue";
import StatusTag from "@/components/StatusTag.vue";
import {
  cancelVisit,
  completeVisit,
  createVisit,
  getSlot,
  listAvailableSlots,
  listVisits,
  type VisitQuery,
} from "@/api/visits";
import { getResource } from "@/api/resource";
import { listAll, resolveRows, formatDate } from "@/utils/clinic";
import { useAuthStore } from "@/stores/auth";
import type { Pet, ScheduleSlot, VetSummary, Visit } from "@/types";
const auth = useAuthStore(),
  route = useRoute(),
  router = useRouter();
const rows = ref<Visit[]>([]),
  pets = ref<Pet[]>([]),
  vets = ref<VetSummary[]>([]),
  availableSlots = ref<ScheduleSlot[]>([]),
  slotsById = ref<Record<number, ScheduleSlot>>({});
const total = ref(0),
  loading = ref(false),
  error = ref(false),
  saving = ref(false),
  actingId = ref<number>(),
  slotsLoading = ref(false),
  slotsError = ref(false),
  dialogVisible = ref(false),
  detailVisible = ref(false),
  selected = ref<Visit>(),
  step = ref(0),
  formRef = ref<FormInstance>();
const query = reactive<VisitQuery>({
  page: 1,
  size: 10,
  keyword: "",
  status: "",
  petId: undefined,
  vetId: undefined,
});
interface VisitForm {
  petId?: number;
  vetId?: number;
  date?: string;
  slotId?: number;
  reason: string;
  requestId: string;
}
const emptyForm = (): VisitForm => ({
  petId: undefined,
  vetId: undefined,
  date: undefined,
  slotId: undefined,
  reason: "",
  requestId: "",
});
const form = reactive(emptyForm());
const isStaff = computed(() => auth.hasRole("ADMIN") || auth.hasRole("STAFF"));
const canCreate = computed(
  () => auth.hasRole("OWNER") && auth.hasAuthority("visit:create"),
);
const activePets = computed(() =>
  pets.value.filter((p) => p.status === "ACTIVE"),
);
const activeVets = computed(() =>
  vets.value.filter((p) => p.status === "ACTIVE"),
);
const filtered = computed(
  () => query.keyword || query.petId || query.vetId || query.status,
);
const rules: FormRules = {
  petId: [{ required: true, message: "请选择宠物", trigger: "change" }],
  reason: [
    { required: true, message: "请输入就诊原因", trigger: "blur" },
    { max: 500, message: "不能超过500字", trigger: "blur" },
  ],
  vetId: [{ required: true, message: "请选择兽医", trigger: "change" }],
  date: [{ required: true, message: "请选择日期", trigger: "change" }],
  slotId: [{ required: true, message: "请选择可用时段", trigger: "change" }],
};
const slotPlaceholder = computed(() =>
  !form.vetId || !form.date
    ? "请先选择兽医和日期"
    : slotsLoading.value
      ? "正在查询时段"
      : slotsError.value
        ? "查询失败，请重试"
        : !availableSlots.value.length
          ? "当天暂无可用时段"
          : "请选择时段",
);
let generation = 0,
  slotGeneration = 0;
async function load() {
  const current = ++generation;
  loading.value = true;
  error.value = false;
  try {
    const params = Object.fromEntries(
      Object.entries(query).filter(([, v]) => v !== "" && v !== undefined),
    );
    const response = await listVisits(params);
    if (current !== generation) return;
    rows.value = response.data.records;
    total.value = response.data.total;
    const ids = [...new Set(rows.value.map((r) => r.slotId))].filter(
      (id) => !slotsById.value[id],
    );
    const found = await Promise.allSettled(ids.map(getSlot));
    if (current !== generation) return;
    found.forEach((r) => {
      if (r.status === "fulfilled")
        slotsById.value[r.value.data.id] = r.value.data;
    });
    if (auth.hasAuthority("pet:manage"))
      pets.value = (await resolveRows(
        "/pets",
        rows.value.map((r) => r.petId),
        pets.value,
      )) as Pet[];
    vets.value = (await resolveRows(
      "/vets",
      rows.value.map((r) => r.vetId),
      vets.value,
    )) as VetSummary[];
    if (selected.value)
      selected.value =
        rows.value.find((r) => r.id === selected.value?.id) || selected.value;
  } catch {
    if (current === generation) error.value = true;
  } finally {
    if (current === generation) loading.value = false;
  }
}
async function lookups() {
  const results = await Promise.allSettled([
    auth.hasAuthority("pet:manage")
      ? listAll<Pet>("/pets")
      : Promise.resolve([]),
    listAll<VetSummary>("/vets"),
  ]);
  if (results[0].status === "fulfilled") pets.value = results[0].value;
  if (results[1].status === "fulfilled") vets.value = results[1].value;
}
function search() {
  query.page = 1;
  load();
}
function resetQuery() {
  Object.assign(query, {
    page: 1,
    keyword: "",
    status: "",
    petId: undefined,
    vetId: undefined,
  });
  load();
}
function resetForm() {
  slotGeneration++;
  Object.assign(form, emptyForm());
  availableSlots.value = [];
  step.value = 0;
  formRef.value?.clearValidate();
}
function openCreate() {
  resetForm();
  form.requestId = crypto.randomUUID();
  dialogVisible.value = true;
}
async function nextStep() {
  const valid = await formRef.value
    ?.validateField(
      step.value === 0 ? ["petId", "reason"] : ["vetId", "date", "slotId"],
    )
    .catch(() => false);
  if (valid) step.value++;
}
async function loadSlots() {
  const current = ++slotGeneration;
  form.slotId = undefined;
  availableSlots.value = [];
  slotsError.value = false;
  slotsLoading.value = false;
  if (!form.vetId || !form.date) return;
  slotsLoading.value = true;
  try {
    const r = await listAvailableSlots(form.vetId, form.date);
    if (current === slotGeneration) availableSlots.value = r.data;
  } catch {
    if (current === slotGeneration) slotsError.value = true;
  } finally {
    if (current === slotGeneration) slotsLoading.value = false;
  }
}
watch([() => form.vetId, () => form.date], loadSlots);
async function save() {
  if (saving.value || !canCreate.value) return;
  if (!(await formRef.value?.validate().catch(() => false))) return;
  if (!form.petId || !form.slotId || !form.reason.trim()) return;
  if (saving.value) return;
  saving.value = true;
  try {
    await createVisit({
      petId: form.petId,
      slotId: form.slotId,
      reason: form.reason.trim(),
      requestId: form.requestId,
    });
    ElMessage.success("预约创建成功");
    dialogVisible.value = false;
    await load();
  } catch {
  } finally {
    saving.value = false;
  }
}
function canCancel(visit: Visit) {
  return (
    auth.hasRole("OWNER") &&
    auth.hasAuthority("visit:cancel") &&
    visit.status === "SCHEDULED" &&
    visit.createdBy === auth.user?.id
  );
}
function canComplete(visit: Visit) {
  return isStaff.value && visit.status === "SCHEDULED";
}
async function cancel(visit: Visit) {
  if (actingId.value || !canCancel(visit)) return;
  try {
    const result = await ElMessageBox.prompt(
      "请输入取消“" + petName(visit.petId) + "”预约的原因",
      "取消预约",
      {
        type: "warning",
        confirmButtonText: "确认取消",
        cancelButtonText: "保留预约",
        inputPlaceholder: "取消原因",
        inputValidator: (v) =>
          (!!v?.trim() && v.length <= 255) || "请填写1至255字的原因",
      },
    );
    actingId.value = visit.id;
    await cancelVisit(visit.id, result.value.trim());
    ElMessage.success("预约已取消");
    await load();
  } catch {
  } finally {
    actingId.value = undefined;
  }
}
async function complete(visit: Visit) {
  if (actingId.value || !canComplete(visit)) return;
  const yes = await ElMessageBox.confirm(
    "确认“" + petName(visit.petId) + "”已完成本次就诊？",
    "完成就诊",
    { confirmButtonText: "确认完成", cancelButtonText: "取消" },
  ).catch(() => false);
  if (!yes) return;
  actingId.value = visit.id;
  try {
    await completeVisit(visit.id);
    ElMessage.success("本次就诊已完成");
    await load();
  } catch {
  } finally {
    actingId.value = undefined;
  }
}
const pet = (id: number) => pets.value.find((p) => p.id === id),
  petName = (id: number) => pet(id)?.name || "宠物 #" + id,
  vet = (id: number) => vets.value.find((v) => v.id === id),
  vetName = (id: number) =>
    vet(id)?.name || "兽医 #" + id;
const appointmentTime = (id: number) =>
  slotsById.value[id]
    ? formatDate(slotsById.value[id].startTime, true) +
      " – " +
      slotsById.value[id].endTime.slice(11, 16)
    : "时段 #" + id;
const availableSlotLabel = (slot?: ScheduleSlot) =>
  slot
    ? slot.startTime.slice(11, 16) +
      " – " +
      slot.endTime.slice(11, 16) +
      (slot.note ? " · " + slot.note : "")
    : "时段不可用";
const disablePastDate = (date: Date) => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return date < today;
};
function openDetail(row: Visit) {
  selected.value = row;
  detailVisible.value = true;
}
async function openRouteDetail() {
  const id = Number(route.query.visitId);
  if (!id) return;
  const row = rows.value.find((r) => r.id === id);
  if (row) {
    openDetail(row);
    return;
  }
  try {
    const r = await getResource<Visit>("/visits", id);
    await Promise.allSettled([
      getSlot(r.data.slotId).then((s) => (slotsById.value[s.data.id] = s.data)),
      lookups(),
    ]);
    openDetail(r.data);
  } catch {}
}
watch(() => route.query.visitId, openRouteDetail);
onMounted(async () => {
  await lookups();
  await load();
  await openRouteDetail();
});
</script>
<style scoped>
.pet-cell,
.vet-cell {
  display: flex;
  gap: 12px;
  align-items: center;
}
.pet-cell strong,
.pet-cell small {
  display: block;
}
.pet-cell small {
  color: var(--clinic-muted);
  font-size: 12px;
  margin-top: 4px;
}
.detail-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 32px;
}
.booking-steps {
  margin-bottom: 24px;
}
.booking-review {
  padding: 20px;
  background: var(--clinic-canvas);
  border-radius: 10px;
}
.booking-review h3 {
  margin-bottom: 24px;
}
.booking-review .caption {
  margin-top: 24px;
}
@media (max-width: 639px) {
  .booking-steps {
    padding: 16px 8px;
    gap: 8px;
  }
  .booking-steps :deep(.el-step.is-simple) {
    min-width: 0;
    flex-direction: column;
    gap: 8px;
  }
  .booking-steps :deep(.el-step.is-simple .el-step__head) {
    padding-right: 0;
  }
  .booking-steps :deep(.el-step.is-simple .el-step__main) {
    justify-content: center;
  }
  .booking-steps :deep(.el-step.is-simple .el-step__arrow) {
    display: none;
  }
  .booking-steps :deep(.el-step.is-simple .el-step__title) {
    font-size: 12px;
    line-height: 1.5;
    white-space: nowrap;
    text-align: center;
  }
}
</style>
