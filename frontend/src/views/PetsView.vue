<template>
  <div>
    <PageHeader
        title="宠物档案"
        description="维护宠物基础资料、主人关系与健康备注"
    >
      <!--
        TODO【Vue 基础：v-if】
        v-if 根据表达式真假决定是否创建这个按钮。canCreate 为 false 时，按钮不会出现在 DOM 中。
        注意：这只是前端体验控制，真正的权限安全仍由后端 @PreAuthorize 保证。
      -->
      <el-button
          v-if="canCreate"
          type="primary"
          :icon="Plus"
          @click="openCreate"
      >
        新增宠物
      </el-button>
    </PageHeader>

    <div class="panel">
      <!-- 查询条件 -->
      <div class="filter-bar">
        <!--
          TODO【Vue 基础：v-model】
          v-model 是双向绑定：输入框内容变化会写入 query.keyword；代码修改 query.keyword 也会更新输入框。
          它大致等价于“传入当前值 + 监听值变化事件”，表单阶段会高频使用。
        -->
        <el-input
            v-model="query.keyword"
            placeholder="输入宠物名称"
            clearable
            :prefix-icon="Search"
            @keyup.enter="search"
        />

        <el-select
            v-if="isStaff"
            v-model="query.ownerId"
            placeholder="全部主人"
            clearable
            filterable
        >
          <!--
            TODO【Vue 基础：v-for】
            v-for 会遍历 owners，为数组中的每个 owner 创建一个 el-option。
            :key 要提供稳定且唯一的标识，帮助 Vue 正确复用和更新节点，这里使用数据库 id。
          -->
          <el-option
              v-for="owner in owners"
              :key="owner.id"
              :label="`${owner.name}（${owner.phone}）`"
              :value="owner.id"
          />
        </el-select>

        <el-select
            v-model="query.status"
            placeholder="全部状态"
            clearable
        >
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="INACTIVE" />
        </el-select>

        <!--
          TODO【Vue 基础：@click】
          @click 是 v-on:click 的简写。用户点击按钮时，Vue 会调用 script 中的 search 函数。
          只写 search，不要写 search()；前者是把函数交给 Vue，后者会在渲染时立即执行。
        -->
        <el-button type="primary" plain @click="search">
          查询
        </el-button>

        <el-button @click="resetQuery">
          重置
        </el-button>
      </div>

      <!-- 宠物表格 -->
      <el-table
          v-loading="loading"
          :data="rows"
          stripe
          empty-text="暂无宠物数据"
      >
        <el-table-column prop="id" label="ID" width="70" />

        <el-table-column label="宠物" min-width="150">
          <!--
            TODO【Vue 基础：作用域插槽 #default】
            el-table-column 是子组件，它通过默认插槽把当前行数据传出来；{ row } 是对象解构。
            因此插槽内部可直接使用 row.name，等价于先接收 scope，再写 scope.row.name。
          -->
          <template #default="{ row }">
            <div class="pet-cell">
              <el-avatar
                  :size="36"
                  :src="row.photoUrl || undefined"
                  class="pet-avatar"
              >
                {{ row.name.slice(0, 1) }}
              </el-avatar>

              <div>
                <strong>{{ row.name }}</strong>
                <small>{{ typeName(row.typeId) }}</small>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="主人" min-width="130">
          <template #default="{ row }">
            {{ ownerName(row.ownerId) }}
          </template>
        </el-table-column>

        <el-table-column label="性别" width="80">
          <template #default="{ row }">
            {{ genderName(row.gender) }}
          </template>
        </el-table-column>

        <el-table-column prop="breed" label="品种" min-width="110">
          <template #default="{ row }">
            {{ row.breed || "-" }}
          </template>
        </el-table-column>

        <el-table-column prop="birthDate" label="生日" width="120">
          <template #default="{ row }">
            {{ row.birthDate || "-" }}
          </template>
        </el-table-column>

        <el-table-column prop="color" label="毛色" width="100">
          <template #default="{ row }">
            {{ row.color || "-" }}
          </template>
        </el-table-column>

        <el-table-column
            prop="microchipNo"
            label="芯片号"
            min-width="140"
            show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ row.microchipNo || "-" }}
          </template>
        </el-table-column>

        <el-table-column
            prop="allergies"
            label="过敏史"
            min-width="150"
            show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ row.allergies || "无" }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>

        <el-table-column
            v-if="canUpdate || canDelete"
            label="操作"
            width="140"
            fixed="right"
        >
          <template #default="{ row }">
            <el-button
                v-if="canUpdate"
                link
                type="primary"
                @click="openEdit(row)"
            >
              编辑
            </el-button>

            <el-button
                v-if="canDelete"
                link
                type="danger"
                :disabled="row.status === 'INACTIVE'"
                @click="disable(row)"
            >
              停用
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
        v-model="dialogVisible"
        :title="editingId === null ? '新增宠物' : '编辑宠物'"
        width="620px"
        destroy-on-close
        @closed="resetForm"
    >
      <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="90px"
      >
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
            <el-input
                v-model="form.breed"
                placeholder="例如：英短"
            />
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
            <el-input
                v-model="form.color"
                placeholder="例如：蓝白"
            />
          </el-form-item>

          <el-form-item label="芯片号" prop="microchipNo">
            <el-input
                v-model="form.microchipNo"
                placeholder="没有可以不填"
            />
          </el-form-item>
        </div>

        <el-form-item label="照片地址" prop="photoUrl">
          <el-input
              v-model="form.photoUrl"
              placeholder="可选：输入宠物照片 URL"
          />
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
        <el-button @click="dialogVisible = false">
          取消
        </el-button>

        <el-button
            type="primary"
            :loading="saving"
            @click="save"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { Plus, Search } from "@element-plus/icons-vue";
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
} from "element-plus";

import PageHeader from "@/components/PageHeader.vue";
import StatusTag from "@/components/StatusTag.vue";
import { useAuthStore } from "@/stores/auth";

import {
  createPet,
  disablePet,
  listOwners,
  listPets,
  listPetTypes,
  updatePet,
  type PetQuery,
} from "@/api/pets";

import type {
  OwnerSummary,
  Pet,
  PetForm,
  PetType,
} from "@/types";

const auth = useAuthStore();

/*
 * TODO【Vue 基础：ref】
 * ref 用来保存需要响应式更新的单个值，也可以保存数组或组件实例。
 * 在 script 中通过 .value 读写；
 * 在 template 中 Vue 会自动解包，不需要写 .value。
 * 例如 loading.value = true 后，模板中的 v-loading="loading" 会自动显示加载状态。
 */
const rows = ref<Pet[]>([]);
const owners = ref<OwnerSummary[]>([]);
const petTypes = ref<PetType[]>([]);

const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);

const formRef = ref<FormInstance>();

/*
 * TODO【Vue 基础：reactive】
 * reactive 适合保存查询条件、表单等包含多个相关字段的对象。
 * 修改时直接写 query.page，不需要 query.value.page。
 * reactive 返回的是响应式代理对象，因此通常保留原对象并修改属性，不整体替换。
 */
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
  photoUrl: "",
});

const form = reactive<PetForm>(createEmptyForm());

/*
 * TODO【Vue 基础：computed】
 * computed 根据其他响应式状态派生结果，并会缓存结果。
 * auth 或 permissions 变化后，这些值会自动重新计算。
 * script 中读取计算属性要写 isStaff.value；template 中会自动解包，直接写 canCreate 即可。
 */
const isStaff = computed(
    () => auth.hasRole("ADMIN") || auth.hasRole("STAFF"),
);

const canCreate = computed(
    () =>
        isStaff.value &&
        auth.hasAuthority("pet:create"),
);

const canUpdate = computed(
    () =>
        isStaff.value &&
        auth.hasAuthority("pet:update"),
);

const canDelete = computed(
    () =>
        isStaff.value &&
        auth.hasAuthority("pet:delete"),
);

const rules: FormRules = {
  ownerId: [
    {
      required: true,
      message: "请选择宠物主人",
      trigger: "change",
    },
  ],

  typeId: [
    {
      required: true,
      message: "请选择宠物类型",
      trigger: "change",
    },
  ],

  name: [
    {
      required: true,
      message: "请输入宠物名称",
      trigger: "blur",
    },
    {
      min: 1,
      max: 50,
      message: "宠物名称不能超过 50 个字符",
      trigger: "blur",
    },
  ],
};

/**
 * 查询宠物。
 *
 * 不直接把 query 整体发送出去，是为了过滤空字符串。
 * 当前后端会把 status="" 当成真实查询条件，从而查不到数据。
 */
const load = async () => {
  loading.value = true;

  try {
    const params: PetQuery = {
      page: query.page,
      size: query.size,
    };

    const keyword = query.keyword?.trim();

    if (keyword) {
      params.keyword = keyword;
    }

    if (query.status) {
      params.status = query.status;
    }

    if (query.ownerId) {
      params.ownerId = query.ownerId;
    }

    const response = await listPets(params);

    rows.value = response.data.records;
    total.value = response.data.total;
  } finally {
    loading.value = false;
  }
};

/**
 * 加载宠物类型和主人下拉选项。
 *
 * 所有登录用户都能读取宠物类型；
 * 只有 ADMIN、STAFF 有 owner:manage 权限，可以读取主人列表。
 */
const loadOptions = async () => {
  const typeResponse = await listPetTypes();
  petTypes.value = typeResponse.data;

  if (isStaff.value) {
    const ownerResponse = await listOwners();
    owners.value = ownerResponse.data.records;
  }
};

const search = () => {
  query.page = 1;
  load();
};

const resetQuery = () => {
  query.page = 1;
  query.size = 10;
  query.keyword = "";
  query.status = "ACTIVE";
  query.ownerId = undefined;

  load();
};

const handleSizeChange = () => {
  query.page = 1;
  load();
};

const resetForm = () => {
  editingId.value = null;

  /*
   * TODO【Vue 基础：Object.assign 与响应式对象】
   * Object.assign 将新对象中的字段复制到响应式 form 中。
   * 不能直接写 form = createEmptyForm()，
   * 因为 form 是 const，而且直接替换会破坏原响应式对象。
   * 保留同一个 reactive 代理、只更新内部属性，模板才能继续追踪这个表单。
   */
  Object.assign(form, createEmptyForm());

  formRef.value?.clearValidate();
};

const openCreate = () => {
  resetForm();
  dialogVisible.value = true;
};

const openEdit = (pet: Pet) => {
  editingId.value = pet.id;

  Object.assign(form, {
    ownerId: pet.ownerId,
    typeId: pet.typeId,
    name: pet.name,
    gender: pet.gender,
    breed: pet.breed || "",
    birthDate: pet.birthDate,
    color: pet.color || "",
    microchipNo: pet.microchipNo || "",
    allergies: pet.allergies || "",
    photoUrl: pet.photoUrl || "",
  });

  dialogVisible.value = true;
};

/**
 * 把只有空格或空字符串的可选字段转换为 undefined，
 * 避免向后端发送没有意义的空白文本。
 */
const normalizedForm = (): PetForm => ({
  ownerId: form.ownerId,
  typeId: form.typeId,
  name: form.name.trim(),
  gender: form.gender,
  breed: form.breed?.trim() || undefined,
  birthDate: form.birthDate || undefined,
  color: form.color?.trim() || undefined,
  microchipNo: form.microchipNo?.trim() || undefined,
  allergies: form.allergies?.trim() || undefined,
  photoUrl: form.photoUrl?.trim() || undefined,
});

const save = async () => {
  const valid = await formRef.value
      ?.validate()
      .catch(() => false);

  if (!valid) {
    return;
  }

  saving.value = true;

  try {
    const data = normalizedForm();

    if (editingId.value === null) {
      await createPet(data);
      ElMessage.success("宠物档案创建成功");
    } else {
      await updatePet(editingId.value, data);
      ElMessage.success("宠物档案修改成功");
    }

    dialogVisible.value = false;
    await load();
  } finally {
    saving.value = false;
  }
};

const disable = async (pet: Pet) => {
  const confirmed = await ElMessageBox.confirm(
      `确定停用宠物“${pet.name}”吗？停用后不能创建预约。`,
      "停用宠物",
      {
        type: "warning",
        confirmButtonText: "确认停用",
        cancelButtonText: "取消",
      },
  ).catch(() => false);

  if (!confirmed) {
    return;
  }

  await disablePet(pet.id);
  ElMessage.success("宠物已停用");

  /*
   * 如果当前页最后一条记录被移除，就回到上一页，
   * 避免停留在一张空白的分页上。
   */
  if (rows.value.length === 1 && query.page && query.page > 1) {
    query.page -= 1;
  }

  await load();
};

const typeName = (typeId: number) =>
    petTypes.value.find((item) => item.id === typeId)?.name ||
    `类型 #${typeId}`;

const ownerName = (ownerId: number) => {
  if (auth.hasRole("OWNER")) {
    return "本人";
  }

  return (
      owners.value.find((item) => item.id === ownerId)?.name ||
      `主人 #${ownerId}`
  );
};

const genderName = (gender?: string) => {
  if (gender === "MALE") {
    return "公";
  }

  if (gender === "FEMALE") {
    return "母";
  }

  return "-";
};

const disableFutureDate = (date: Date) =>
    date.getTime() > Date.now();

/*
 * TODO【Vue 基础：onMounted 生命周期】
 * onMounted 表示组件第一次挂载到页面后执行。
 * 先加载下拉选项，再加载宠物列表。
 * 适合执行首次查询、注册第三方组件等依赖页面已经创建的初始化工作。
 * async 让回调可以使用 await；两个 await 表示先完成选项请求，再查询宠物列表。
 */
onMounted(async () => {
  await loadOptions();
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
  width: 240px;
}

.filter-bar .el-select {
  width: 180px;
}

.pet-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pet-cell strong,
.pet-cell small {
  display: block;
}

.pet-cell small {
  margin-top: 3px;
  color: #909399;
  font-size: 12px;
}

.pet-avatar {
  flex: none;
  color: #ffffff;
  background: linear-gradient(135deg, #23c7b8, #007f73);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 18px;
}

@media (max-width: 700px) {
  .filter-bar .el-input,
  .filter-bar .el-select {
    width: 100%;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
