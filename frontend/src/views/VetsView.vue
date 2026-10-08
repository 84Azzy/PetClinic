<template>
  <ResourceCrud
    title="兽医管理"
    description="查看执业资料与诊疗简介，维护接诊团队。"
    endpoint="/vets"
    :columns="columns"
    :fields="fields"
    :after-save="saveAvatar"
    write-authority="vet:manage"
    delete-label="停用"
    @form-open="prepareAvatar"
    @form-closed="resetAvatarState"
  >
    <template #cell-name="{ row }">
      <div class="vet-name-cell">
        <VetAvatar
          :src="row.avatarUrl"
          :name="row.name"
          :size="44"
          :version="avatarVersion(row)"
        />
        <div>
          <strong>{{ row.name }}</strong>
          <small>{{ row.biography || "诊疗简介未记录" }}</small>
        </div>
      </div>
    </template>

    <template #form-extra="{ saving }">
      <el-form-item label="兽医头像" class="avatar-form-item">
        <div class="avatar-upload-field">
          <VetAvatar
            :src="
              avatarPreviewUrl ||
              (!removeExistingAvatar ? currentAvatarUrl : '')
            "
            name="兽医头像预览"
            :size="84"
            :version="avatarDialogVersion"
          />
          <div class="avatar-upload-actions">
            <el-upload
              accept="image/jpeg,image/png,image/webp"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="selectAvatar"
            >
              <el-button :disabled="saving">
                {{ currentAvatarUrl || avatarFile ? "更换头像" : "上传头像" }}
              </el-button>
            </el-upload>
            <el-button
              v-if="avatarFile || (currentAvatarUrl && !removeExistingAvatar)"
              link
              type="danger"
              :disabled="saving"
              @click="removeAvatar"
            >
              移除头像
            </el-button>
            <small>支持 JPEG、PNG、WebP，文件不超过 5MB。</small>
          </div>
        </div>
      </el-form-item>
    </template>

    <template #detail="{ row }">
      <div class="vet-detail-head">
        <VetAvatar
          :src="row.avatarUrl"
          :name="row.name"
          :size="88"
          :version="avatarVersion(row)"
        />
        <div>
          <h2>{{ row.name }}</h2>
          <p>
            {{
              row.licenseNo
                ? "执业证号 " + row.licenseNo
                : "执业资料未记录"
            }}
          </p>
        </div>
        <StatusTag :status="row.status" />
      </div>
      <dl class="detail-grid vet-detail-grid">
        <div class="detail-item">
          <dt>手机号</dt>
          <dd>{{ row.phone || "未记录" }}</dd>
        </div>
        <div class="detail-item">
          <dt>邮箱</dt>
          <dd>{{ row.email || "未记录" }}</dd>
        </div>
        <div class="detail-item wide">
          <dt>诊疗简介</dt>
          <dd>{{ row.biography || "暂未填写诊疗简介。" }}</dd>
        </div>
      </dl>
      <p class="caption">兽医编号 #{{ row.id }}</p>
    </template>
  </ResourceCrud>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from "vue";
import { ElMessage, type UploadFile } from "element-plus";
import ResourceCrud, {
  type Column,
  type Field,
} from "@/components/ResourceCrud.vue";
import VetAvatar from "@/components/VetAvatar.vue";
import StatusTag from "@/components/StatusTag.vue";
import { deleteVetAvatar, uploadVetAvatar } from "@/api/vets";
import type { ClinicRow } from "@/utils/clinic";

const columns: Column[] = [
  { key: "name", label: "兽医", width: 220 },
  { key: "licenseNo", label: "执业证号" },
  { key: "phone", label: "手机号" },
  { key: "email", label: "邮箱" },
  { key: "status", label: "状态" },
];
const fields: Field[] = [
  { key: "name", label: "姓名", required: true },
  { key: "phone", label: "手机号" },
  { key: "email", label: "邮箱" },
  { key: "licenseNo", label: "执业证号", required: true },
  { key: "biography", label: "简介", type: "textarea" },
  {
    key: "status",
    label: "状态",
    type: "select",
    options: [
      { label: "启用", value: "ACTIVE" },
      { label: "停用", value: "INACTIVE" },
    ],
  },
];

const avatarFile = ref<File>();
const avatarPreviewUrl = ref("");
const currentAvatarUrl = ref("");
const removeExistingAvatar = ref(false);
const avatarDialogRevision = ref(0);
const avatarRevisions = ref<Record<number, number>>({});
const avatarDialogVersion = computed(() => avatarDialogRevision.value);

function avatarVersion(row: ClinicRow) {
  return `${row.updatedAt || ""}:${avatarRevisions.value[row.id] || 0}`;
}

function clearAvatarPreview() {
  if (avatarPreviewUrl.value) URL.revokeObjectURL(avatarPreviewUrl.value);
  avatarPreviewUrl.value = "";
}

function resetAvatarState() {
  clearAvatarPreview();
  avatarFile.value = undefined;
  currentAvatarUrl.value = "";
  removeExistingAvatar.value = false;
}

function prepareAvatar(row?: ClinicRow) {
  resetAvatarState();
  currentAvatarUrl.value = row?.avatarUrl || "";
  avatarDialogRevision.value++;
}

function selectAvatar(uploadFile: UploadFile) {
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
  clearAvatarPreview();
  avatarFile.value = file;
  avatarPreviewUrl.value = URL.createObjectURL(file);
  removeExistingAvatar.value = false;
}

function removeAvatar() {
  clearAvatarPreview();
  avatarFile.value = undefined;
  removeExistingAvatar.value = !!currentAvatarUrl.value;
}

async function saveAvatar(row: ClinicRow) {
  let saved = row;
  if (avatarFile.value) {
    saved = (await uploadVetAvatar(row.id, avatarFile.value)).data;
  } else if (removeExistingAvatar.value && currentAvatarUrl.value) {
    await deleteVetAvatar(row.id);
    saved = { ...row, avatarUrl: undefined };
  }
  if (avatarFile.value || removeExistingAvatar.value) {
    avatarRevisions.value = {
      ...avatarRevisions.value,
      [row.id]: (avatarRevisions.value[row.id] || 0) + 1,
    };
  }
  return saved;
}

onBeforeUnmount(clearAvatarPreview);
</script>

<style scoped>
.vet-name-cell,
.vet-detail-head,
.avatar-upload-field {
  display: flex;
  align-items: center;
  gap: 14px;
}
.vet-name-cell {
  min-width: 0;
}
.vet-name-cell > div,
.vet-detail-head > div {
  min-width: 0;
  flex: 1;
}
.vet-name-cell strong,
.vet-name-cell small {
  display: block;
}
.vet-name-cell small {
  max-width: 260px;
  overflow: hidden;
  color: var(--clinic-muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.avatar-form-item {
  margin-top: 4px;
}
.avatar-upload-actions {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 8px;
}
.avatar-upload-actions small {
  color: var(--clinic-muted);
  line-height: 1.5;
}
.vet-detail-head {
  padding-bottom: 24px;
  border-bottom: 1px solid var(--clinic-border);
}
.vet-detail-head h2 {
  margin: 0 0 6px;
  font-size: 28px;
}
.vet-detail-head p {
  margin: 0;
  color: var(--clinic-muted);
}
.vet-detail-grid {
  margin: 24px 0 32px;
}
@media (max-width: 480px) {
  .avatar-upload-field,
  .vet-detail-head {
    align-items: flex-start;
    flex-wrap: wrap;
  }
}
</style>
