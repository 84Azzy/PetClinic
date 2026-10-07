<template>
  <div>
    <nav class="management-tabs" aria-label="系统管理页面">
      <router-link
        v-for="item in sections"
        :key="item.key"
        :to="{
          path: '/system/users',
          query: item.key === 'users' ? {} : { section: item.key },
        }"
        :class="{ active: section === item.key }"
        :aria-current="section === item.key ? 'page' : undefined"
        >{{ item.label }}</router-link
      >
    </nav>
    <component :is="currentView" :key="section" />
  </div>
</template>
<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import SystemUsersView from "./SystemUsersView.vue";
import SystemRolesView from "./SystemRolesView.vue";
import SystemPermissionsView from "./SystemPermissionsView.vue";
import OperationLogsView from "./OperationLogsView.vue";
import PetTypesView from "./PetTypesView.vue";
import SpecialtiesView from "./SpecialtiesView.vue";
const route = useRoute();
const sections = [
  { key: "users", label: "系统用户" },
  { key: "roles", label: "角色管理" },
  { key: "permissions", label: "权限管理" },
  { key: "logs", label: "操作日志" },
  { key: "pet-types", label: "宠物类型" },
  { key: "specialties", label: "兽医专长" },
];
const views = {
  users: SystemUsersView,
  roles: SystemRolesView,
  permissions: SystemPermissionsView,
  logs: OperationLogsView,
  "pet-types": PetTypesView,
  specialties: SpecialtiesView,
};
const section = computed(() => {
  const requested = String(route.query.section || "users");
  return Object.prototype.hasOwnProperty.call(views, requested)
    ? (requested as keyof typeof views)
    : "users";
});
const currentView = computed(() => views[section.value]);
</script>
<style scoped>
.management-tabs {
  display: flex;
  gap: 24px;
  overflow-x: auto;
  white-space: nowrap;
  border-bottom: 1px solid var(--clinic-border);
  margin-bottom: 24px;
  padding: 0 4px;
  scrollbar-width: thin;
}
.management-tabs a {
  padding: 12px 0;
  display: block;
  text-decoration: none;
  color: var(--clinic-muted);
  border-bottom: 2px solid transparent;
  flex: none;
}
.management-tabs a.active {
  color: var(--clinic-primary);
  font-weight: 600;
  border-bottom-color: var(--clinic-primary);
}
@media (max-width: 639px) {
  .management-tabs {
    gap: 20px;
    margin-bottom: 20px;
  }
}
</style>
