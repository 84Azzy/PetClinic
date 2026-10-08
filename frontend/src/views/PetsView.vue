<template>
  <div>
    <PageHeader
      title="宠物档案"
      description="从身份资料到健康记录，了解每一位小患者。"
      ><el-button
        v-if="canCreate"
        type="primary"
        :icon="Plus"
        @click="openCreate"
        >新增宠物</el-button
      ></PageHeader
    >
    <section class="panel filter-panel">
      <form class="filter-bar" @submit.prevent="search">
        <el-input
          v-model="query.keyword"
          placeholder="搜索宠物名称"
          aria-label="搜索宠物名称"
          :prefix-icon="Search"
          clearable
        /><el-select
          v-if="isStaff && auth.hasAuthority('owner:manage')"
          v-model="query.ownerId"
          placeholder="全部主人"
          aria-label="筛选主人"
          clearable
          filterable
          ><el-option
            v-for="owner in owners"
            :key="owner.id"
            :label="owner.name + ' · ' + owner.phone"
            :value="owner.id" /></el-select
        ><el-select
          v-model="query.status"
          placeholder="全部状态"
          aria-label="筛选档案状态"
          clearable
          ><el-option label="启用" value="ACTIVE" /><el-option
            label="停用"
            value="INACTIVE" /></el-select
        ><el-button native-type="submit" type="primary" plain>查询</el-button
        ><el-button @click="resetQuery">重置</el-button>
      </form>
    </section>
    <PageState v-if="error" kind="error" class="panel" @retry="load" />
    <div v-else class="record-layout dossier-layout">
      <section class="record-list">
        <div class="record-list-head">
          <span>宠物名单</span
          ><span class="caption">共 {{ total }} 份档案</span>
        </div>
        <PageState v-if="loading" kind="loading" />
        <PageState
          v-else-if="!rows.length"
          kind="empty"
          title="没有符合条件的宠物"
          description="调整筛选条件，或联系诊所建立档案。"
        />
        <button
          v-for="row in rows"
          v-else
          :key="row.id"
          :class="['record-row', { selected: selected?.id === row.id }]"
          :aria-pressed="selected?.id === row.id"
          @click="selectPet(row, true)"
        >
          <PetAvatar
            :src="row.photoUrl"
            :name="row.name"
            :size="52"
            :version="photoVersion(row)"
          />
          <div class="registry-text">
            <strong>{{ row.name }}</strong
            ><small
              >{{ row.breed || typeName(row.typeId) }} ·
              {{ genderName(row.gender) }} · {{ ageText(row.birthDate) }}</small
            ><small>主人：{{ ownerName(row.ownerId) }}</small>
          </div>
          <ClinicIcon
            class="registry-arrow direction-icon"
            name="chevron-right"
          />
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
      <section
        ref="contentRef"
        class="record-content"
        tabindex="-1"
        aria-label="宠物详情"
      >
        <PageState
          v-if="!selected"
          kind="empty"
          title="选择一份宠物档案"
          description="身份资料和健康记录会显示在这里。"
        />
        <template v-else>
          <div class="pet-identity">
            <PetAvatar
              :src="selected.photoUrl"
              :name="selected.name"
              :size="96"
              :version="photoVersion(selected)"
            />
            <div>
              <h2>
                {{ selected.name }} <StatusTag :status="selected.status" />
              </h2>
              <p>
                {{ selected.breed || typeName(selected.typeId) }} ·
                {{ genderName(selected.gender) }} ·
                {{ ageText(selected.birthDate) }}
              </p>
              <small class="caption">档案编号 #{{ selected.id }}</small>
            </div>
            <div class="pet-identity-actions">
              <el-button
                v-if="canUpdate"
                type="primary"
                plain
                @click="openEdit(selected)"
                >修改照片</el-button
              >
              <el-button v-if="canUpdate" @click="openEdit(selected)"
                >编辑资料</el-button
              ><el-button
                v-if="canDelete"
                type="danger"
                plain
                :disabled="selected.status === 'INACTIVE' || disabling"
                @click="disable(selected)"
                >停用</el-button
              >
            </div>
          </div>
          <div class="dossier-facts">
            <div>
              <span>宠物主人</span
              ><strong>{{ ownerName(selected.ownerId) }}</strong>
            </div>
            <div>
              <span>联系电话</span
              ><a
                v-if="owner(selected.ownerId)?.phone"
                :href="'tel:' + owner(selected.ownerId)?.phone"
                >{{ owner(selected.ownerId)?.phone }}</a
              ><strong v-else>未记录</strong>
            </div>
            <div>
              <span>出生日期</span
              ><strong>{{ formatDate(selected.birthDate) }}</strong>
            </div>
            <div>
              <span>芯片号码</span
              ><strong>{{ selected.microchipNo || "未记录" }}</strong>
            </div>
          </div>
          <div :class="['allergy-note', { known: !!selected.allergies }]">
            <ClinicIcon name="file" />
            <div>
              <strong>过敏史</strong>
              <p>
                {{
                  selected.allergies || "暂未记录过敏史，就诊时请与主人确认。"
                }}
              </p>
            </div>
          </div>
          <el-tabs v-model="tab"
            ><el-tab-pane label="基础资料" name="profile" /><el-tab-pane
              v-if="canMedical"
              label="健康记录"
              name="medical" /><el-tab-pane
              v-if="canVaccine"
              label="疫苗记录"
              name="vaccinations"
          /></el-tabs>
          <dl v-if="tab === 'profile'" class="detail-grid">
            <div
              v-for="item in profileFields"
              :key="item.label"
              class="detail-item"
            >
              <dt>{{ item.label }}</dt>
              <dd>{{ item.value || "未记录" }}</dd>
            </div>
          </dl>
          <template v-else
            ><PageState v-if="healthLoading" kind="loading" /><PageState
              v-else-if="healthError"
              kind="error"
              title="健康记录加载失败"
              @retry="loadHealth" /><PageState
              v-else-if="!healthRows.length"
              kind="empty"
              :title="tab === 'medical' ? '暂无电子病历' : '暂无疫苗记录'"
              description="诊所记录后，会按时间显示在这里。" />
            <div v-else>
              <article
                v-for="record in healthRows"
                :key="record.id"
                class="timeline-entry"
              >
                <div class="timeline-date">
                  {{
                    formatDate(
                      tab === "medical"
                        ? record.createdAt
                        : record.vaccinatedDate,
                    )
                  }}<br />记录 #{{ record.id }}
                </div>
                <div class="timeline-body">
                  <h3>
                    {{
                      tab === "medical" ? record.diagnosis : record.vaccineName
                    }}
                  </h3>
                  <dl class="detail-grid">
                    <template v-if="tab === 'medical'"
                      ><div
                        v-for="item in [
                          { key: 'symptoms', label: '症状' },
                          { key: 'treatment', label: '治疗' },
                          { key: 'prescription', label: '处方' },
                        ]"
                        :key="item.key"
                        class="detail-item wide"
                      >
                        <dt>{{ item.label }}</dt>
                        <dd>{{ record[item.key] || "未记录" }}</dd>
                      </div></template
                    ><template v-else
                      ><div class="detail-item">
                        <dt>下次到期</dt>
                        <dd>{{ formatDate(record.nextDueDate) }}</dd>
                      </div>
                      <div class="detail-item">
                        <dt>接种医生</dt>
                        <dd>{{ record.veterinarian || "未记录" }}</dd>
                      </div>
                      <div class="detail-item">
                        <dt>接种批次</dt>
                        <dd>{{ record.batchNo || "未记录" }}</dd>
                      </div></template
                    >
                  </dl>
                </div>
              </article>
              <el-button
                v-if="
                  tab === 'medical' && auth.canVisitPath('/medical-records')
                "
                type="primary"
                link
                @click="
                  $router.push({
                    path: '/medical-records',
                    query: { petId: selected.id },
                  })
                "
                >查看完整病历<ClinicIcon class="direction-icon" name="arrow" />
              </el-button></div
          ></template>
        </template>
      </section>
    </div>
    <el-dialog
      v-model="dialogVisible"
      :title="editingId === null ? '新增宠物' : '编辑宠物'"
      width="620px"
      destroy-on-close
      :close-on-click-modal="!saving"
      :close-on-press-escape="!saving"
      :show-close="!saving"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="主人" prop="ownerId">
            <el-select
              v-model="form.ownerId"
              placeholder="请选择宠物主人"
              filterable
              style="width: 100%"
            >
              <el-option
                v-for="owner in owners"
                :key="owner.id"
                :label="`${owner.name}（${owner.phone}）`"
                :value="owner.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="宠物类型" prop="typeId">
            <el-select
              v-model="form.typeId"
              placeholder="请选择宠物类型"
              style="width: 100%"
            >
              <el-option
                v-for="type in petTypes"
                :key="type.id"
                :label="type.name"
                :value="type.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="宠物名称" prop="name">
            <el-input
              v-model="form.name"
              maxlength="50"
              show-word-limit
              placeholder="例如：糯米"
            />
          </el-form-item>

          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="form.gender">
              <el-radio value="MALE">公</el-radio>
              <el-radio value="FEMALE">母</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="品种" prop="breed">
            <el-input v-model="form.breed" placeholder="例如：英短" />
          </el-form-item>

          <el-form-item label="出生日期" prop="birthDate">
            <el-date-picker
              v-model="form.birthDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择出生日期"
              :disabled-date="disableFutureDate"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="毛色" prop="color">
            <el-input v-model="form.color" placeholder="例如：蓝白" />
          </el-form-item>

          <el-form-item label="芯片号" prop="microchipNo">
            <el-input v-model="form.microchipNo" placeholder="没有可以不填" />
          </el-form-item>
        </div>

        <el-form-item label="宠物照片">
          <div class="photo-upload-field">
            <PetAvatar
              :src="photoPreviewUrl || (!removeExistingPhoto ? currentPhotoUrl : '')"
              :name="form.name || '宠物照片预览'"
              :size="76"
            />
            <div class="photo-upload-actions">
              <el-upload
                accept="image/jpeg,image/png,image/webp"
                :auto-upload="false"
                :show-file-list="false"
                :on-change="selectPhoto"
              >
                <el-button :disabled="saving">
                  {{ currentPhotoUrl || photoFile ? "更换照片" : "上传照片" }}
                </el-button>
              </el-upload>
              <el-button
                v-if="photoFile || (currentPhotoUrl && !removeExistingPhoto)"
                link
                type="danger"
                :disabled="saving"
                @click="removePhoto"
                >移除照片</el-button
              >
              <small>支持 JPEG、PNG、WebP，文件不超过 5MB。</small>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="过敏史" prop="allergies">
          <el-input
            v-model="form.allergies"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="没有可以不填"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button :disabled="saving" @click="dialogVisible = false">
          取消
        </el-button>

        <el-button
          type="primary"
          :loading="saving"
          :disabled="saving"
          @click="save"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from "vue";
import { useRoute } from "vue-router";
import { Plus, Search } from "@element-plus/icons-vue";
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type UploadFile,
} from "element-plus";
import PageHeader from "@/components/PageHeader.vue";
import PageState from "@/components/PageState.vue";
import PetAvatar from "@/components/PetAvatar.vue";
import ClinicIcon from "@/components/ClinicIcon.vue";
import StatusTag from "@/components/StatusTag.vue";
import { useAuthStore } from "@/stores/auth";
import {
  createPet,
  deletePetPhoto,
  disablePet,
  getMyOwner,
  listPets,
  listPetTypes,
  updatePet,
  uploadPetPhoto,
  type PetQuery,
} from "@/api/pets";
import { getResource } from "@/api/resource";
import {
  listAll,
  resolveRows,
  ageText,
  formatDate,
  type ClinicRow,
} from "@/utils/clinic";
import type { OwnerSummary, Pet, PetForm, PetType } from "@/types";
const auth = useAuthStore(),
  route = useRoute();
const rows = ref<Pet[]>([]),
  owners = ref<OwnerSummary[]>([]),
  petTypes = ref<PetType[]>([]),
  selected = ref<Pet>(),
  total = ref(0),
  loading = ref(false),
  error = ref(false),
  saving = ref(false),
  disabling = ref(false),
  dialogVisible = ref(false),
  editingId = ref<number | null>(null),
  formRef = ref<FormInstance>(),
  contentRef = ref<HTMLElement>();
const photoFile = ref<File>(),
  photoPreviewUrl = ref(""),
  currentPhotoUrl = ref(""),
  removeExistingPhoto = ref(false),
  photoRevisions = ref<Record<number, number>>({});
const query = reactive<PetQuery>({
  page: 1,
  size: 10,
  keyword: "",
  status: "ACTIVE",
  ownerId: undefined,
});
const createEmptyForm = (): PetForm => ({
  ownerId: undefined,
  typeId: undefined,
  name: "",
  gender: undefined,
  breed: "",
  birthDate: undefined,
  color: "",
  microchipNo: "",
  allergies: "",
});
const form = reactive<PetForm>(createEmptyForm());
const isStaff = computed(() => auth.hasRole("ADMIN") || auth.hasRole("STAFF")),
  canCreate = computed(() => isStaff.value && auth.hasAuthority("pet:create")),
  canUpdate = computed(() => isStaff.value && auth.hasAuthority("pet:update")),
  canDelete = computed(() => isStaff.value && auth.hasAuthority("pet:delete"));
const canMedical = computed(
    () => isStaff.value && auth.hasAuthority("medical:manage"),
  ),
  canVaccine = computed(
    () => isStaff.value && auth.hasAuthority("vaccination:manage"),
  );
const tab = ref("profile"),
  healthRows = ref<ClinicRow[]>([]),
  healthLoading = ref(false),
  healthError = ref(false);
const rules: FormRules = {
  ownerId: [{ required: true, message: "请选择宠物主人", trigger: "change" }],
  typeId: [{ required: true, message: "请选择宠物类型", trigger: "change" }],
  name: [
    { required: true, message: "请输入宠物名称", trigger: "blur" },
    { min: 1, max: 50, message: "宠物名称不能超过50字", trigger: "blur" },
  ],
};
let generation = 0,
  healthGeneration = 0;
async function load() {
  const current = ++generation;
  loading.value = true;
  error.value = false;
  try {
    const params = Object.fromEntries(
      Object.entries(query).filter(
        ([, value]) => value !== "" && value !== undefined,
      ),
    );
    const r = await listPets(params);
    if (current !== generation) return;
    rows.value = r.data.records;
    total.value = r.data.total;
    if (isStaff.value && auth.hasAuthority("owner:manage"))
      owners.value = (await resolveRows(
        "/owners",
        rows.value.map((r) => r.ownerId),
        owners.value,
      )) as OwnerSummary[];
    const currentPet = rows.value.find((p) => p.id === selected.value?.id);
    selectPet(currentPet || rows.value[0]);
  } catch {
    if (current === generation) error.value = true;
  } finally {
    if (current === generation) loading.value = false;
  }
}
async function loadOptions() {
  const results = await Promise.allSettled([
    listPetTypes(),
    isStaff.value && auth.hasAuthority("owner:manage")
      ? listAll<OwnerSummary>("/owners")
      : auth.hasRole("OWNER")
        ? getMyOwner().then((r) => [r.data])
        : Promise.resolve([]),
  ]);
  if (results[0].status === "fulfilled") petTypes.value = results[0].value.data;
  if (results[1].status === "fulfilled") owners.value = results[1].value;
}
async function selectPet(pet?: Pet, scroll = false) {
  const changed = selected.value?.id !== pet?.id;
  selected.value = pet;
  if (changed) {
    healthGeneration++;
    healthRows.value = [];
    tab.value = "profile";
  }
  if (scroll && window.innerWidth < 640) {
    await nextTick();
    contentRef.value?.scrollIntoView({ behavior: "auto", block: "start" });
    contentRef.value?.focus();
  }
}
async function loadHealth() {
  const pet = selected.value;
  if (
    !pet ||
    tab.value === "profile" ||
    (tab.value === "medical" && !canMedical.value) ||
    (tab.value === "vaccinations" && !canVaccine.value)
  )
    return;
  const current = ++healthGeneration;
  healthLoading.value = true;
  healthError.value = false;
  healthRows.value = [];
  try {
    const records = await listAll<ClinicRow>(
      tab.value === "medical" ? "/medical-records" : "/vaccinations",
      { petId: pet.id },
    );
    if (current === healthGeneration)
      healthRows.value = records.sort((a, b) =>
        String(b.createdAt || b.vaccinatedDate).localeCompare(
          String(a.createdAt || a.vaccinatedDate),
        ),
      );
  } catch {
    if (current === healthGeneration) healthError.value = true;
  } finally {
    if (current === healthGeneration) healthLoading.value = false;
  }
}
watch(tab, loadHealth);
function search() {
  query.page = 1;
  load();
}
function resetQuery() {
  Object.assign(query, {
    page: 1,
    size: 10,
    keyword: "",
    status: "ACTIVE",
    ownerId: undefined,
  });
  load();
}
function resetForm() {
  editingId.value = null;
  Object.assign(form, createEmptyForm());
  resetPhotoState();
  formRef.value?.clearValidate();
}
function openCreate() {
  resetForm();
  dialogVisible.value = true;
}
function openEdit(pet: Pet) {
  resetPhotoState();
  editingId.value = pet.id;
  currentPhotoUrl.value = pet.photoUrl || "";
  Object.assign(
    form,
    createEmptyForm(),
    Object.fromEntries(
      Object.keys(createEmptyForm()).map((key) => [
        key,
        (pet as unknown as ClinicRow)[key],
      ]),
    ),
  );
  dialogVisible.value = true;
}
function clearPhotoPreview() {
  if (photoPreviewUrl.value) URL.revokeObjectURL(photoPreviewUrl.value);
  photoPreviewUrl.value = "";
}
function resetPhotoState() {
  clearPhotoPreview();
  photoFile.value = undefined;
  currentPhotoUrl.value = "";
  removeExistingPhoto.value = false;
}
function selectPhoto(uploadFile: UploadFile) {
  const file = uploadFile.raw;
  if (!file) return;
  if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
    ElMessage.warning("仅支持 JPEG、PNG 或 WebP 图片");
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning("图片大小不能超过 5MB");
    return;
  }
  clearPhotoPreview();
  photoFile.value = file;
  photoPreviewUrl.value = URL.createObjectURL(file);
  removeExistingPhoto.value = false;
}
function removePhoto() {
  clearPhotoPreview();
  photoFile.value = undefined;
  removeExistingPhoto.value = !!currentPhotoUrl.value;
}
function photoVersion(pet: Pet) {
  return `${pet.updatedAt || ""}:${photoRevisions.value[pet.id] || 0}`;
}
async function save() {
  if (saving.value || !(await formRef.value?.validate().catch(() => false)))
    return;
  if (saving.value) return;
  saving.value = true;
  const creating = editingId.value === null;
  let recordSaved = false;
  try {
    const data = {
      ...form,
      name: form.name.trim(),
      ...Object.fromEntries(
        ["breed", "color", "microchipNo", "allergies"].map(
          (key) => [key, (form as ClinicRow)[key]?.trim() || undefined],
        ),
      ),
    };
    const response =
      editingId.value === null
        ? await createPet(data)
        : await updatePet(editingId.value, data);
    recordSaved = true;
    editingId.value = response.data.id;
    let savedPet = response.data;
    if (photoFile.value) {
      savedPet = (await uploadPetPhoto(response.data.id, photoFile.value)).data;
    } else if (removeExistingPhoto.value && currentPhotoUrl.value) {
      await deletePetPhoto(response.data.id);
      savedPet = { ...savedPet, photoUrl: undefined };
    }
    if (photoFile.value || removeExistingPhoto.value) {
      photoRevisions.value = {
        ...photoRevisions.value,
        [response.data.id]: (photoRevisions.value[response.data.id] || 0) + 1,
      };
    }
    selected.value = savedPet;
    if (creating) query.page = 1;
    ElMessage.success("宠物档案已保存");
    dialogVisible.value = false;
    await load();
  } catch {
    if (recordSaved) {
      ElMessage.warning("档案已保存，但照片处理失败；请在当前窗口重试");
      await load();
    }
  } finally {
    saving.value = false;
  }
}
async function disable(pet: Pet) {
  if (disabling.value) return;
  const yes = await ElMessageBox.confirm(
    "停用“" + pet.name + "”后将不能创建预约，确认停用吗？",
    "停用宠物",
    {
      type: "warning",
      confirmButtonText: "确认停用",
      cancelButtonText: "取消",
    },
  ).catch(() => false);
  if (!yes) return;
  disabling.value = true;
  try {
    await disablePet(pet.id);
    ElMessage.success("档案已停用");
    if (rows.value.length === 1 && (query.page || 1) > 1) query.page!--;
    await load();
  } catch {
  } finally {
    disabling.value = false;
  }
}
const typeName = (id: number) =>
    petTypes.value.find((t) => t.id === id)?.name || "类型 #" + id,
  owner = (id: number) => owners.value.find((o) => o.id === id),
  ownerName = (id: number) =>
    auth.hasRole("OWNER")
      ? auth.user?.displayName || "本人"
      : owner(id)?.name || "主人 #" + id;
const genderName = (value?: string) =>
    value === "MALE" ? "公" : value === "FEMALE" ? "母" : "性别未记录",
  disableFutureDate = (date: Date) => date.getTime() > Date.now();
const profileFields = computed(() =>
  selected.value
    ? [
        { label: "宠物类型", value: typeName(selected.value.typeId) },
        { label: "品种", value: selected.value.breed },
        { label: "性别", value: genderName(selected.value.gender) },
        { label: "毛色", value: selected.value.color },
        { label: "建档时间", value: formatDate(selected.value.createdAt) },
        { label: "最近更新", value: formatDate(selected.value.updatedAt) },
      ]
    : [],
);
async function selectRoutePet() {
  const id = Number(route.query.petId);
  if (!id) return;
  const pet = rows.value.find((p) => p.id === id);
  if (pet) {
    selectPet(pet);
    return;
  }
  try {
    const r = await getResource<Pet>("/pets", id);
    if (isStaff.value && auth.hasAuthority("owner:manage"))
      owners.value = (await resolveRows(
        "/owners",
        [r.data.ownerId],
        owners.value,
      )) as OwnerSummary[];
    selectPet(r.data);
  } catch {}
}
watch(() => route.query.petId, selectRoutePet);
onMounted(async () => {
  await loadOptions();
  await load();
  await selectRoutePet();
});
onBeforeUnmount(clearPhotoPreview);
</script>
<style scoped>
.dossier-layout {
  grid-template-columns: 340px minmax(0, 1fr);
  min-height: 640px;
}
.registry-text {
  min-width: 0;
  flex: 1;
}
.registry-text strong {
  overflow-wrap: anywhere;
}
.registry-arrow {
  color: var(--clinic-muted);
  font-size: 20px;
}
.dossier-facts {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
  margin-bottom: 24px;
  padding: 20px 0;
  border-top: 1px solid var(--clinic-border);
  border-bottom: 1px solid var(--clinic-border);
}
.dossier-facts span {
  display: block;
  font-size: 12px;
  color: var(--clinic-muted);
  margin-bottom: 8px;
}
.dossier-facts strong,
.dossier-facts a {
  font-weight: 500;
  overflow-wrap: anywhere;
}
.allergy-note {
  display: flex;
  gap: 12px;
  background: var(--clinic-canvas);
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 16px;
}
.allergy-note.known {
  background: #fff6e9;
  color: #855718;
}
.allergy-note svg {
  width: 20px;
  height: 20px;
  flex: none;
  margin-top: 3px;
}
.allergy-note p {
  margin: 4px 0 0;
  overflow-wrap: anywhere;
}
.pet-identity h2 .el-tag {
  vertical-align: middle;
  margin-left: 8px;
}
.pet-identity small {
  display: block;
  margin-top: 8px;
}
.photo-upload-field {
  display: flex;
  align-items: center;
  gap: 16px;
}
.photo-upload-actions {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 8px;
}
.photo-upload-actions small {
  color: var(--clinic-muted);
  line-height: 1.5;
}
@media (max-width: 1199px) {
  .dossier-layout {
    grid-template-columns: 280px minmax(0, 1fr);
  }
  .dossier-facts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .pet-identity {
    flex-wrap: wrap;
  }
  .pet-identity-actions {
    width: 100%;
    margin: 0;
  }
}
@media (max-width: 767px) {
  .dossier-layout {
    grid-template-columns: 1fr;
  }
  .record-list {
    border-right: 0;
  }
  .record-content {
    border-top: 1px solid var(--clinic-border);
  }
  .dossier-facts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
