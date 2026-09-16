<template>
  <div>
    <PageHeader
      title="预约就诊"
      description="查询预约进度，并为自己的宠物选择兽医与可用时段"
    >
      <el-button
        v-if="canCreate"
        v-permission="{
          roles: ['OWNER'],
          authorities: ['visit:create'],
        }"
        type="primary"
        :icon="Plus"
        @click="openCreate"
      >
        创建预约
      </el-button>
    </PageHeader>

    <div class="panel">
      <div class="filter-bar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索就诊原因或请求编号"
          clearable
          :prefix-icon="Search"
          @keyup.enter="search"
        />

        <el-select
          v-model="query.petId"
          placeholder="全部宠物"
          clearable
          filterable
        >
          <el-option
            v-for="pet in pets"
            :key="pet.id"
            :label="pet.name"
            :value="pet.id"
          />
        </el-select>

        <el-select
          v-model="query.vetId"
          placeholder="全部兽医"
          clearable
          filterable
        >
          <el-option
            v-for="vet in vets"
            :key="vet.id"
            :label="vet.name"
            :value="vet.id"
          />
        </el-select>

        <el-select
          v-model="query.status"
          placeholder="全部状态"
          clearable
        >
          <el-option label="待就诊" value="SCHEDULED" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>

        <el-button type="primary" plain @click="search">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        stripe
        empty-text="暂无预约记录"
      >
        <el-table-column prop="id" label="ID" width="70" />

        <el-table-column label="宠物" min-width="110">
          <template #default="{ row }">
            <strong>{{ petName(row.petId) }}</strong>
          </template>
        </el-table-column>

        <el-table-column label="接诊兽医" min-width="110">
          <template #default="{ row }">
            {{ vetName(row.vetId) }}
          </template>
        </el-table-column>

        <el-table-column label="预约时间" min-width="190">
          <template #default="{ row }">
            {{ appointmentTime(row.slotId) }}
          </template>
        </el-table-column>

        <el-table-column
          prop="reason"
          label="就诊原因"
          min-width="200"
          show-overflow-tooltip
        />

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>

        <el-table-column label="创建时间" min-width="155">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column
          v-if="showActions"
          label="操作"
          width="150"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              v-if="canCancel(row)"
              v-permission="{
                roles: ['OWNER'],
                authorities: ['visit:cancel'],
              }"
              link
              type="danger"
              @click="cancel(row)"
            >
              取消预约
            </el-button>

            <el-button
              v-if="canComplete(row)"
              v-permission="{ roles: ['ADMIN', 'STAFF'] }"
              link
              type="success"
              @click="complete(row)"
            >
              完成就诊
            </el-button>

            <span
              v-if="!canCancel(row) && !canComplete(row)"
              class="muted"
            >
              -
            </span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          background
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @current-change="load"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      title="创建预约"
      width="580px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-alert
        v-if="pets.length === 0"
        title="当前没有可预约的宠物，请先联系诊所建立有效宠物档案。"
        type="warning"
        :closable="false"
        show-icon
        class="form-alert"
      />

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
      >
        <el-form-item label="宠物" prop="petId">
          <el-select
            v-model="form.petId"
            placeholder="请选择宠物"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="pet in pets"
              :key="pet.id"
              :label="pet.name"
              :value="pet.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="兽医" prop="vetId">
          <el-select
            v-model="form.vetId"
            placeholder="请选择兽医"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="vet in vets"
              :key="vet.id"
              :label="vet.name"
              :value="vet.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="就诊日期" prop="date">
          <el-date-picker
            v-model="form.date"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择就诊日期"
            :disabled-date="disablePastDate"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="可用时段" prop="slotId">
          <el-select
            v-model="form.slotId"
            :loading="slotsLoading"
            :disabled="!form.vetId || !form.date"
            :placeholder="slotPlaceholder"
            style="width: 100%"
          >
            <el-option
              v-for="slot in availableSlots"
              :key="slot.id"
              :label="availableSlotLabel(slot)"
              :value="slot.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="就诊原因" prop="reason">
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请简要描述宠物症状或本次就诊目的"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="saving"
          :disabled="pets.length === 0"
          @click="save"
        >
          确认预约
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { Plus, Search } from "@element-plus/icons-vue";
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
} from "element-plus";

import PageHeader from "@/components/PageHeader.vue";
import StatusTag from "@/components/StatusTag.vue";
import { listPets } from "@/api/pets";
import {
  cancelVisit,
  completeVisit,
  createVisit,
  getSlot,
  listAvailableSlots,
  listVetsForVisit,
  listVisits,
  type VisitQuery,
} from "@/api/visits";
import { useAuthStore } from "@/stores/auth";
import type { Pet, ScheduleSlot, VetSummary, Visit } from "@/types";

interface VisitForm {
  petId?: number;
  vetId?: number;
  date?: string;
  slotId?: number;
  reason: string;
  requestId: string;
}

const auth = useAuthStore();
const rows = ref<Visit[]>([]);
const pets = ref<Pet[]>([]);
const vets = ref<VetSummary[]>([]);
const availableSlots = ref<ScheduleSlot[]>([]);
const slotsById = ref<Record<number, ScheduleSlot>>({});
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const slotsLoading = ref(false);
const dialogVisible = ref(false);
const formRef = ref<FormInstance>();

const query = reactive<VisitQuery>({
  page: 1,
  size: 10,
  keyword: "",
  status: "",
  petId: undefined,
  vetId: undefined,
});

const createEmptyForm = (): VisitForm => ({
  petId: undefined,
  vetId: undefined,
  date: undefined,
  slotId: undefined,
  reason: "",
  requestId: "",
});

const form = reactive<VisitForm>(createEmptyForm());

const canCreate = computed(
  () => auth.hasRole("OWNER") && auth.hasAuthority("visit:create"),
);

const canCancelPermission = computed(
  () => auth.hasRole("OWNER") && auth.hasAuthority("visit:cancel"),
);

const canCompletePermission = computed(
  () => auth.hasRole("ADMIN") || auth.hasRole("STAFF"),
);

const showActions = computed(
  () => canCancelPermission.value || canCompletePermission.value,
);

const slotPlaceholder = computed(() => {
  if (!form.vetId || !form.date) return "请先选择兽医和日期";
  if (slotsLoading.value) return "正在查询可用时段";
  if (availableSlots.value.length === 0) return "当天暂无可用时段";
  return "请选择可用时段";
});

const rules: FormRules = {
  petId: [{ required: true, message: "请选择宠物", trigger: "change" }],
  vetId: [{ required: true, message: "请选择兽医", trigger: "change" }],
  date: [{ required: true, message: "请选择就诊日期", trigger: "change" }],
  slotId: [{ required: true, message: "请选择可用时段", trigger: "change" }],
  reason: [
    { required: true, message: "请输入就诊原因", trigger: "blur" },
    { min: 1, max: 500, message: "就诊原因不能超过 500 个字符", trigger: "blur" },
  ],
};

const load = async () => {
  loading.value = true;
  try {
    const params: VisitQuery = { page: query.page, size: query.size };
    const keyword = query.keyword?.trim();
    if (keyword) params.keyword = keyword;
    if (query.status) params.status = query.status;
    if (query.petId) params.petId = query.petId;
    if (query.vetId) params.vetId = query.vetId;

    const response = await listVisits(params);
    rows.value = response.data.records;
    total.value = response.data.total;
    await loadMissingSlots(rows.value);
  } finally {
    loading.value = false;
  }
};

const loadLookups = async () => {
  /*
   * TODO【新知识：Promise.all 并发请求】
   * 两个下拉字典互不依赖，因此可以同时发请求，而不是先等宠物再查兽医。
   * Promise.all 会等待数组中的所有 Promise 都成功；结果顺序与传入顺序一致。
   */
  const [petResponse, vetResponse] = await Promise.all([
    listPets({ page: 1, size: 100, status: "ACTIVE" }),
    listVetsForVisit(),
  ]);
  pets.value = petResponse.data.records;
  vets.value = vetResponse.data.records;
};

const loadMissingSlots = async (visits: Visit[]) => {
  /*
   * TODO【新知识：Set 去重与展开语法】
   * 多条预约可能引用同一个时段。Set 只保留唯一 slotId，避免重复请求；
   * [...new Set(...)] 再把 Set 展开回数组，方便使用 filter 和 map。
   */
  const missingIds = [
    ...new Set(visits.map((visit) => visit.slotId)),
  ].filter((id) => !slotsById.value[id]);

  if (missingIds.length === 0) return;

  const responses = await Promise.all(missingIds.map((id) => getSlot(id)));
  const next = { ...slotsById.value };
  responses.forEach((response) => {
    next[response.data.id] = response.data;
  });
  slotsById.value = next;
};

const search = () => {
  query.page = 1;
  load();
};

const resetQuery = () => {
  query.page = 1;
  query.size = 10;
  query.keyword = "";
  query.status = "";
  query.petId = undefined;
  query.vetId = undefined;
  load();
};

const handleSizeChange = () => {
  query.page = 1;
  load();
};

const resetForm = () => {
  Object.assign(form, createEmptyForm());
  availableSlots.value = [];
  formRef.value?.clearValidate();
};

const openCreate = () => {
  resetForm();
  /*
   * TODO【新知识：幂等键 requestId】
   * 每次打开表单只生成一次 UUID，保存失败后再次点击仍复用同一个 requestId。
   * 网络超时导致前端“不知道后端是否成功”时，重试不会创建重复预约。
   * 下次重新打开表单才代表一次新业务操作，所以届时再生成新的 UUID。
   */
  form.requestId = crypto.randomUUID();
  dialogVisible.value = true;
};

/*
 * TODO【新知识：watch 监听联动条件】
 * watch 观察兽医和日期两个数据源。任意一个变化时，旧 slotId 都应失效，
 * 然后在两个条件齐全时重新查询可用时段，这就是“联动下拉框”。
 * 回调参数 [vetId, date] 对应前面两个监听函数返回值，属于数组解构。
 */
watch(
  [() => form.vetId, () => form.date],
  async ([vetId, date]) => {
    form.slotId = undefined;
    availableSlots.value = [];
    if (!vetId || !date) return;

    slotsLoading.value = true;
    try {
      const response = await listAvailableSlots(vetId, date);
      availableSlots.value = response.data;
    } finally {
      slotsLoading.value = false;
    }
  },
);

const save = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  const petId = form.petId;
  const slotId = form.slotId;
  const reason = form.reason.trim();
  if (!petId || !slotId || !reason) return;

  saving.value = true;
  try {
    await createVisit({ petId, slotId, reason, requestId: form.requestId });
    ElMessage.success("预约创建成功");
    dialogVisible.value = false;
    await load();
  } finally {
    saving.value = false;
  }
};

const canCancel = (visit: Visit) =>
  canCancelPermission.value &&
  visit.status === "SCHEDULED" &&
  visit.createdBy === auth.user?.id;

const canComplete = (visit: Visit) =>
  canCompletePermission.value && visit.status === "SCHEDULED";

const cancel = async (visit: Visit) => {
  try {
    const result = await ElMessageBox.prompt(
      `请输入取消“${petName(visit.petId)}”预约的原因`,
      "取消预约",
      {
        type: "warning",
        confirmButtonText: "确认取消",
        cancelButtonText: "暂不取消",
        inputPlaceholder: "请输入取消原因",
        inputPattern: /\S+/,
        inputErrorMessage: "取消原因不能为空",
        inputValidator: (value) =>
          value.length <= 255 || "取消原因不能超过 255 个字符",
      },
    );
    await cancelVisit(visit.id, result.value.trim());
    ElMessage.success("预约已取消，可用时段已释放");
    await load();
  } catch (error) {
    if (error === "cancel" || error === "close") return;
    throw error;
  }
};

const complete = async (visit: Visit) => {
  const confirmed = await ElMessageBox.confirm(
    `确认“${petName(visit.petId)}”已经完成本次就诊吗？`,
    "完成就诊",
    {
      type: "success",
      confirmButtonText: "确认完成",
      cancelButtonText: "取消",
    },
  ).catch(() => false);

  if (!confirmed) return;
  await completeVisit(visit.id);
  ElMessage.success("预约已标记为完成");
  await load();
};

const petName = (id: number) =>
  pets.value.find((pet) => pet.id === id)?.name || `宠物 #${id}`;

const vetName = (id: number) =>
  vets.value.find((vet) => vet.id === id)?.name || `兽医 #${id}`;

const formatDateTime = (value?: string) =>
  value ? value.replace("T", " ").slice(0, 16) : "-";

const appointmentTime = (slotId: number) => {
  const slot = slotsById.value[slotId];
  if (!slot) return `时段 #${slotId}`;
  return `${formatDateTime(slot.startTime)} - ${slot.endTime.slice(11, 16)}`;
};

const availableSlotLabel = (slot: ScheduleSlot) =>
  `${slot.startTime.slice(11, 16)} - ${slot.endTime.slice(11, 16)}${
    slot.note ? ` · ${slot.note}` : ""
  }`;

const disablePastDate = (date: Date) => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return date.getTime() < today.getTime();
};

onMounted(async () => {
  await loadLookups();
  await load();
});
</script>

<style scoped>
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.filter-bar .el-input {
  width: 250px;
}

.filter-bar .el-select {
  width: 150px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.form-alert {
  margin-bottom: 18px;
}

.muted {
  color: #a8abb2;
}

@media (max-width: 760px) {
  .filter-bar .el-input,
  .filter-bar .el-select {
    width: 100%;
  }
}
</style>
