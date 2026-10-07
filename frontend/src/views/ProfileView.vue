<template>
  <div>
    <PageHeader title="个人中心" description="查看你的账号身份与联系资料。" />
    <div class="profile-grid">
      <section class="panel profile-card">
        <div class="profile-avatar">
          {{ auth.user?.displayName?.slice(0, 1) }}
        </div>
        <h2>{{ auth.user?.displayName }}</h2>
        <el-tag type="success">{{
          accountName(auth.user?.accountType)
        }}</el-tag>
        <p>@{{ auth.user?.username }}</p>
      </section>
      <section class="panel">
        <div class="panel-head"><h2>账号资料</h2></div>
        <dl class="detail-grid">
          <div v-for="item in items" :key="item.label" class="detail-item">
            <dt>{{ item.label }}</dt>
            <dd>{{ item.value || "未记录" }}</dd>
          </div>
        </dl>
        <div class="profile-help">
          <ClinicIcon name="shield" />
          <div>
            <h3>你的诊所身份</h3>
            <p>
              {{
                auth.hasRole("OWNER")
                  ? "你可以查看自己的宠物和预约，通过诊疗助手整理预约安排。需要更正联系资料时，请联系诊所工作人员。"
                  : "你可以从左侧导航进入已授权的诊疗和管理功能。需要调整账号资料或访问范围时，请联系管理员。"
              }}
            </p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed } from "vue";
import PageHeader from "@/components/PageHeader.vue";
import ClinicIcon from "@/components/ClinicIcon.vue";
import { useAuthStore } from "@/stores/auth";
import { accountName } from "@/utils/clinic";
const auth = useAuthStore(),
  items = computed(() => [
    { label: "登录账号", value: auth.user?.username },
    { label: "显示姓名", value: auth.user?.displayName },
    { label: "联系电话", value: auth.user?.phone },
    { label: "电子邮箱", value: auth.user?.email },
  ]);
</script>
<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 24px;
}
.profile-card {
  text-align: center;
  padding: 32px 24px;
}
.profile-avatar {
  display: grid;
  place-items: center;
  width: 80px;
  height: 80px;
  margin: auto;
  border-radius: 50%;
  background: #eaf4ee;
  color: var(--clinic-primary);
  font-size: 28px;
  font-weight: 600;
}
.profile-card h2 {
  font-size: 20px;
  margin: 24px 0 12px;
}
.profile-card p {
  color: var(--clinic-muted);
  margin: 16px 0 0;
}
.profile-help {
  display: flex;
  gap: 16px;
  padding: 24px;
  background: var(--clinic-canvas);
  border-radius: 6px;
  margin-top: 32px;
}
.profile-help svg {
  flex: none;
  width: 24px;
  height: 24px;
  color: var(--clinic-primary);
}
.profile-help h3 {
  font-size: 16px;
  margin-bottom: 12px;
}
.profile-help p {
  margin: 0;
  color: var(--clinic-muted);
  line-height: 1.9;
}
@media (max-width: 767px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
  .profile-help {
    padding: 20px 16px;
  }
}
</style>
