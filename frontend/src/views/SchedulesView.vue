<template>
  <div>
    <PageHeader
      title="排班管理"
      description="按兽医查看每日时段，让可预约时间一目了然。"
      ><el-button v-if="canWrite" type="primary" :icon="Plus" @click="edit()"
        >新增时段</el-button
      ></PageHeader
    >
    <section class="panel filter-panel">
      <div class="filter-bar">
        <div class="date-control">
          <el-button aria-label="前一天" @click="moveDay(-1)"
            ><ClinicIcon
              class="direction-icon"
              name="chevron-left" /></el-button
          ><el-date-picker
            v-model="date"
            type="date"
            value-format="YYYY-MM-DD"
            :clearable="false"
            aria-label="排班日期"
          /><el-button aria-label="后一天" @click="moveDay(1)"
            ><ClinicIcon class="direction-icon" name="chevron-right"
          /></el-button>
        </div>
        <el-button @click="date = today()">今天</el-button
        ><el-select
          v-model="vetId"
          placeholder="全部兽医"
          aria-label="筛选排班兽医"
          filterable
          clearable
          ><el-option
            v-for="vet in vets"
            :key="vet.id"
            :label="vet.name"
            :value="vet.id" /></el-select
        ><el-select
          v-model="status"
          placeholder="全部状态"
          aria-label="筛选时段状态"
          clearable
          ><el-option label="可预约" value="AVAILABLE" /><el-option
            label="已预约"
            value="BOOKED" /><el-option
            label="已关闭"
            value="CLOSED" /></el-select
        ><el-button @click="load">刷新</el-button>
      </div>
    </section>
    <div class="schedule-summary">
      <strong>{{ date }} 的接诊安排</strong
      ><span class="caption"
        >共 {{ slots.length }} 个时段 ·
        {{ slots.filter((s) => s.status === "AVAILABLE").length }}
        个可预约</span
      >
    </div>
    <PageState v-if="loading" class="panel" kind="loading" /><PageState
      v-else-if="error"
      class="panel"
      kind="error"
      @retry="load"
    />
    <div v-else class="schedule-board">
      <section v-for="vet in visibleVets" :key="vet.id" class="schedule-column">
        <header>
          <VetAvatar
            :src="vet.avatarUrl"
            :name="vet.name"
            :size="44"
            :version="vet.updatedAt"
          />
          <div>
            <h2>{{ vet.name }}</h2>
            <p>
              {{
                vet.licenseNo ? "执业证号 " + vet.licenseNo : "执业资料未记录"
              }}
            </p>
          </div>
        </header>
        <div class="schedule-slots">
          <p v-if="!vetSlots(vet.id).length" class="schedule-empty">
            当天暂无{{ status ? "符合条件的" : "" }}时段
          </p>
          <article
            v-for="slot in vetSlots(vet.id)"
            :key="slot.id"
            :class="['slot-card', slot.status.toLowerCase()]"
          >
            <div class="slot-top">
              <strong
                >{{ slot.startTime.slice(11, 16) }} –
                {{ slot.endTime.slice(11, 16) }}</strong
              ><StatusTag :status="slot.status" />
            </div>
            <p>{{ slot.note || "常规接诊" }}</p>
            <div v-if="canWrite" class="slot-actions">
              <el-button
                type="primary"
                link
                :disabled="slot.status === 'BOOKED' || !!actingId"
                @click="edit(slot)"
                >编辑</el-button
              ><el-popconfirm
                v-if="slot.status === 'AVAILABLE'"
                title="关闭后主人将无法预约此时段，确认关闭？"
                confirm-button-text="关闭时段"
                cancel-button-text="取消"
                @confirm="act(slot, 'close')"
                ><template #reference
                  ><el-button link :disabled="!!actingId"
                    >关闭</el-button
                  ></template
                ></el-popconfirm
              ><el-popconfirm
                title="确认删除此排班时段？"
                confirm-button-text="删除"
                cancel-button-text="取消"
                @confirm="act(slot, 'delete')"
                ><template #reference
                  ><el-button
                    link
                    type="danger"
                    :disabled="slot.status === 'BOOKED' || !!actingId"
                    >删除</el-button
                  ></template
                ></el-popconfirm
              >
            </div>
            <small v-if="slot.status === 'BOOKED'" class="caption"
              >已预约，保留现有就诊安排</small
            >
          </article>
          <el-button
            v-if="canWrite"
            class="add-slot"
            plain
            :icon="Plus"
            @click="edit(undefined, vet.id)"
            >新增时段</el-button
          >
        </div>
      </section>
      <PageState
        v-if="!visibleVets.length"
        class="panel"
        kind="empty"
        title="暂无兽医资料"
        description="维护兽医资料后即可安排接诊时段。"
      />
    </div>
    <el-dialog
      v-model="visible"
      :title="editingId ? '编辑排班时段' : '新增排班时段'"
      width="600px"
      destroy-on-close
      :show-close="!saving"
      :close-on-click-modal="!saving"
      :close-on-press-escape="!saving"
      ><el-form ref="formRef" :model="form" label-position="top">
        <el-form-item
          label="接诊兽医"
          prop="vetId"
          :rules="[
            { required: true, message: '请选择兽医', trigger: 'change' },
          ]"
          ><el-select v-model="form.vetId" filterable style="width: 100%"
            ><el-option
              v-for="vet in vets"
              :key="vet.id"
              :label="vet.name"
              :value="vet.id" /></el-select
        ></el-form-item>
        <div class="form-grid">
          <el-form-item
            label="开始时间"
            prop="startTime"
            :rules="[
              { required: true, message: '请选择开始时间', trigger: 'change' },
            ]"
            ><el-date-picker
              v-model="form.startTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%" /></el-form-item
          ><el-form-item
            label="结束时间"
            prop="endTime"
            :rules="[
              { required: true, message: '请选择结束时间', trigger: 'change' },
            ]"
            ><el-date-picker
              v-model="form.endTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
          /></el-form-item>
        </div>
        <el-form-item label="接诊备注"
          ><el-input
            v-model="form.note"
            type="textarea"
            :rows="3"
            placeholder="例如：仅接诊猫科、复诊时段"
        /></el-form-item> </el-form
      ><template #footer
        ><el-button :disabled="saving" @click="visible = false">取消</el-button
        ><el-button
          type="primary"
          :disabled="saving"
          :loading="saving"
          @click="save"
          >保存时段</el-button
        ></template
      ></el-dialog
    >
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { Plus } from "@element-plus/icons-vue";
import { ElMessage, type FormInstance } from "element-plus";
import PageHeader from "@/components/PageHeader.vue";
import PageState from "@/components/PageState.vue";
import ClinicIcon from "@/components/ClinicIcon.vue";
import VetAvatar from "@/components/VetAvatar.vue";
import StatusTag from "@/components/StatusTag.vue";
import {
  createResource,
  updateResource,
  deleteResource,
  postAction,
} from "@/api/resource";
import { listAll, resolveRows, type ClinicRow } from "@/utils/clinic";
import { useAuthStore } from "@/stores/auth";
const auth = useAuthStore(),
  route = useRoute(),
  vets = ref<ClinicRow[]>([]),
  slots = ref<ClinicRow[]>([]),
  loading = ref(false),
  error = ref(false),
  visible = ref(false),
  saving = ref(false),
  editingId = ref<number>(),
  actingId = ref<number>(),
  formRef = ref<FormInstance>();
const today = () => {
  const d = new Date();
  return (
    d.getFullYear() +
    "-" +
    String(d.getMonth() + 1).padStart(2, "0") +
    "-" +
    String(d.getDate()).padStart(2, "0")
  );
};
const date = ref(
    typeof route.query.date === "string" &&
      /^\d{4}-\d{2}-\d{2}$/.test(route.query.date)
      ? route.query.date
      : today(),
  ),
  vetId = ref<number>(),
  status = ref("");
const form = reactive<ClinicRow>({}),
  canWrite = computed(
    () =>
      (auth.hasRole("ADMIN") || auth.hasRole("STAFF")) &&
      auth.hasAuthority("schedule:manage"),
  );
const visibleVets = computed(() =>
    vets.value.filter((v) => !vetId.value || v.id === vetId.value),
  ),
  vetSlots = (id: number) => slots.value.filter((s) => s.vetId === id);
let generation = 0;
async function load() {
  const current = ++generation;
  loading.value = true;
  error.value = false;
  try {
    const result = await listAll("/slots", {
      date: date.value,
      vetId: vetId.value,
      status: status.value || undefined,
    });
    if (current !== generation) return;
    slots.value = result;
    vets.value = await resolveRows(
      "/vets",
      result.map((s) => s.vetId),
      vets.value,
    );
  } catch {
    if (current === generation) error.value = true;
  } finally {
    if (current === generation) loading.value = false;
  }
}
function moveDay(offset: number) {
  const d = new Date(date.value + "T12:00:00");
  d.setDate(d.getDate() + offset);
  date.value =
    d.getFullYear() +
    "-" +
    String(d.getMonth() + 1).padStart(2, "0") +
    "-" +
    String(d.getDate()).padStart(2, "0");
}
function edit(slot?: ClinicRow, id?: number) {
  editingId.value = slot?.id;
  Object.assign(form, {
    vetId: slot?.vetId || id || vetId.value,
    startTime: slot?.startTime || date.value + "T09:00:00",
    endTime: slot?.endTime || date.value + "T09:30:00",
    note: slot?.note || "",
  });
  visible.value = true;
}
async function save() {
  if (saving.value || !(await formRef.value?.validate().catch(() => false)))
    return;
  if (form.endTime <= form.startTime) {
    ElMessage.warning("结束时间必须晚于开始时间");
    return;
  }
  if (saving.value) return;
  saving.value = true;
  try {
    editingId.value
      ? await updateResource("/slots", editingId.value, form)
      : await createResource("/slots", form);
    visible.value = false;
    ElMessage.success("排班已保存");
    await load();
  } catch {
  } finally {
    saving.value = false;
  }
}
async function act(slot: ClinicRow, action: string) {
  if (actingId.value || slot.status === "BOOKED") return;
  actingId.value = slot.id;
  try {
    action === "close"
      ? await postAction("/slots/" + slot.id + "/close")
      : await deleteResource("/slots", slot.id);
    ElMessage.success(action === "close" ? "时段已关闭" : "时段已删除");
    await load();
  } catch {
  } finally {
    actingId.value = undefined;
  }
}
watch([date, vetId, status], load);
onMounted(async () => {
  try {
    vets.value = await listAll("/vets");
  } catch {}
  await load();
});
</script>
<style scoped>
.date-control {
  display: flex;
  gap: 8px;
  align-items: center;
}
.date-control .el-button {
  margin: 0;
}
.schedule-summary {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin: 24px 0 16px;
  flex-wrap: wrap;
}
.schedule-board {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
}
.schedule-column {
  border: 1px solid var(--clinic-border);
  border-radius: 10px;
  background: white;
  min-width: 0;
}
.schedule-column header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px;
  border-bottom: 1px solid var(--clinic-border);
}
.schedule-column h2 {
  font-size: 16px;
  margin: 0;
}
.schedule-column header p {
  font-size: 12px;
  color: var(--clinic-muted);
  margin: 6px 0 0;
  overflow-wrap: anywhere;
}
.schedule-slots {
  padding: 16px;
}
.slot-card {
  padding: 16px;
  border: 1px solid var(--clinic-border);
  border-radius: 6px;
  margin-bottom: 12px;
}
.slot-card.available {
  border-left: 3px solid #4d9479;
}
.slot-card.booked {
  border-left: 3px solid #b98942;
  background: #fffcf6;
}
.slot-card.closed {
  background: #f5f6f5;
}
.slot-top {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: space-between;
  align-items: center;
}
.slot-top strong {
  font-size: 16px;
}
.slot-card p {
  font-size: 12px;
  color: var(--clinic-muted);
  margin: 12px 0 8px;
  overflow-wrap: anywhere;
}
.slot-actions {
  display: flex;
  gap: 12px;
}
.slot-actions .el-button {
  margin: 0;
  min-height: 28px;
}
.add-slot {
  width: 100%;
  margin-top: 8px;
}
.schedule-empty {
  text-align: center;
  color: var(--clinic-muted);
  padding: 32px 8px;
  font-size: 14px;
}
@media (max-width: 639px) {
  .date-control {
    width: 100%;
  }
  .date-control :deep(.el-date-editor) {
    flex: 1;
    min-width: 0;
  }
  .schedule-board {
    grid-template-columns: 1fr;
  }
}
</style>
