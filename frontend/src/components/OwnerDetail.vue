<template>
  <div>
    <div class="pet-identity">
      <span class="owner-avatar">{{ row.name?.slice(0, 1) }}</span>
      <div>
        <h2>{{ row.name }}</h2>
        <p>主人档案 #{{ row.id }}</p>
      </div>
      <StatusTag :status="row.status" />
    </div>
    <dl class="detail-grid">
      <div class="detail-item">
        <dt>联系电话</dt>
        <dd>
          <a :href="'tel:' + row.phone">{{ row.phone || "未记录" }}</a>
        </dd>
      </div>
      <div class="detail-item">
        <dt>电子邮箱</dt>
        <dd>{{ row.email || "未记录" }}</dd>
      </div>
      <div class="detail-item wide">
        <dt>联系地址</dt>
        <dd>{{ row.address || "未记录" }}</dd>
      </div>
    </dl>
    <section v-if="auth.hasAuthority('pet:manage')" class="detail-section">
      <h3>
        关联宠物 <small class="caption">{{ pets.length }} 只</small>
      </h3>
      <PageState v-if="loading" kind="loading" /><PageState
        v-else-if="error"
        kind="error"
        title="宠物加载失败"
        @retry="load"
      /><PageState
        v-else-if="!pets.length"
        kind="empty"
        title="尚无关联宠物"
        description="建立宠物档案时选择这位主人即可关联。"
      /><router-link
        v-for="pet in pets"
        v-else
        :key="pet.id"
        :to="'/pets?petId=' + pet.id"
        class="owner-pet"
        ><PetAvatar :src="pet.photoUrl" :size="48" />
        <div>
          <strong>{{ pet.name }}</strong
          ><small
            >{{ pet.breed || "品种未记录" }} ·
            {{ ageText(pet.birthDate) }}</small
          >
        </div>
        <span
          >查看档案<ClinicIcon
            class="direction-icon"
            name="chevron-right" /></span
      ></router-link>
    </section>
    <p class="caption">绑定账号 #{{ row.userId }}</p>
  </div>
</template>
<script setup lang="ts">
import { ref, watch } from "vue";
import PageState from "./PageState.vue";
import PetAvatar from "./PetAvatar.vue";
import StatusTag from "./StatusTag.vue";
import ClinicIcon from "./ClinicIcon.vue";
import { useAuthStore } from "@/stores/auth";
import { listAll, ageText, type ClinicRow } from "@/utils/clinic";
const props = defineProps<{ row: ClinicRow }>(),
  auth = useAuthStore(),
  pets = ref<ClinicRow[]>([]),
  loading = ref(false),
  error = ref(false);
let generation = 0;
async function load() {
  if (!auth.hasAuthority("pet:manage")) return;
  const current = ++generation;
  loading.value = true;
  error.value = false;
  pets.value = [];
  try {
    const result = await listAll("/pets", { ownerId: props.row.id });
    if (current === generation) pets.value = result;
  } catch {
    if (current === generation) error.value = true;
  } finally {
    if (current === generation) loading.value = false;
  }
}
watch(() => props.row.id, load, { immediate: true });
</script>
<style scoped>
.owner-avatar {
  width: 64px;
  height: 64px;
  flex: none;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: #eaf4ee;
  color: var(--clinic-primary);
  font-size: 24px;
}
.owner-pet {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid var(--clinic-border);
  text-decoration: none;
}
.owner-pet > div {
  flex: 1;
}
.owner-pet small {
  display: block;
  color: var(--clinic-muted);
  font-size: 12px;
  margin-top: 4px;
}
.owner-pet > span {
  font-size: 12px;
}
</style>
