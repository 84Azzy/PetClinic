<template>
  <div>
    <PageHeader title="个人中心" description="查看当前登录账号与诊所身份" />
    <div class="profile-grid">
      <div class="panel profile-card">
        <div class="profile-avatar">
          {{ auth.user?.displayName?.slice(0, 1) }}
        </div>
        <h3>{{ auth.user?.displayName }}</h3>
        <el-tag type="success">{{ auth.user?.accountType }}</el-tag>
        <p>@{{ auth.user?.username }}</p>
      </div>
      <div class="panel">
        <h3>账号资料</h3>
        <el-descriptions :column="1" border
          ><el-descriptions-item label="账号">{{
            auth.user?.username
          }}</el-descriptions-item
          ><el-descriptions-item label="姓名">{{
            auth.user?.displayName
          }}</el-descriptions-item
          ><el-descriptions-item label="手机号">{{
            auth.user?.phone || "-"
          }}</el-descriptions-item
          ><el-descriptions-item label="邮箱">{{
            auth.user?.email || "-"
          }}</el-descriptions-item
          ><el-descriptions-item label="授权说明"
            >菜单与按钮由后端 RBAC 权限树动态生成，业务接口权限仍由后端最终兜底。</el-descriptions-item
          ></el-descriptions
        >
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import PageHeader from "@/components/PageHeader.vue";
import { useAuthStore } from "@/stores/auth";
const auth = useAuthStore();
</script>
<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 18px;
}
.profile-card {
  text-align: center;
  padding: 35px;
}
.profile-avatar {
  display: grid;
  place-items: center;
  width: 84px;
  height: 84px;
  border-radius: 24px;
  margin: auto;
  background: linear-gradient(135deg, #23c7b8, #007f73);
  color: #fff;
  font-size: 34px;
  font-weight: 700;
}
.profile-card h3 {
  margin: 16px 0 8px;
}
.profile-card p {
  color: #93a0aa;
}
@media (max-width: 700px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
