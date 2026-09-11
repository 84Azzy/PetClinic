import {
  createRouter,
  createWebHistory,
  type RouteRecordRaw,
} from "vue-router";
import { useAuthStore } from "@/stores/auth";
const children: RouteRecordRaw[] = [
  {
    path: "dashboard",
    component: () => import("@/views/DashboardView.vue"),
    meta: { title: "数据看板", icon: "DataAnalysis" },
  },
  {
    path: "owners",
    component: () => import("@/views/OwnersView.vue"),
    meta: { title: "主人档案", icon: "User", accountTypes: ["ADMIN", "STAFF"] },
  },
  {
    path: "pets",
    component: () => import("@/views/PetsView.vue"),
    meta: { title: "宠物档案", icon: "MostlyCloudy" },
  },
  {
    path: "pet-types",
    component: () => import("@/views/PetTypesView.vue"),
    meta: {
      title: "宠物类型",
      icon: "CollectionTag",
      accountTypes: ["ADMIN", "STAFF"],
    },
  },
  {
    path: "vets",
    component: () => import("@/views/VetsView.vue"),
    meta: {
      title: "兽医管理",
      icon: "Avatar",
      accountTypes: ["ADMIN", "STAFF"],
    },
  },
  {
    path: "specialties",
    component: () => import("@/views/SpecialtiesView.vue"),
    meta: {
      title: "兽医专长",
      icon: "Medal",
      accountTypes: ["ADMIN", "STAFF"],
    },
  },
  {
    path: "schedules",
    component: () => import("@/views/SchedulesView.vue"),
    meta: { title: "排班管理", icon: "Calendar" },
  },
  {
    path: "visits",
    component: () => import("@/views/VisitsView.vue"),
    meta: { title: "预约就诊", icon: "Tickets" },
  },
  {
    path: "medical-records",
    component: () => import("@/views/MedicalRecordsView.vue"),
    meta: {
      title: "电子病历",
      icon: "Document",
      accountTypes: ["ADMIN", "STAFF"],
    },
  },
  {
    path: "vaccinations",
    component: () => import("@/views/VaccinationsView.vue"),
    meta: { title: "疫苗记录", icon: "FirstAidKit" },
  },
  {
    path: "notices",
    component: () => import("@/views/NoticesView.vue"),
    meta: { title: "公告管理", icon: "Bell" },
  },
  {
    path: "feedback",
    component: () => import("@/views/FeedbackView.vue"),
    meta: { title: "意见反馈", icon: "ChatLineSquare" },
  },
  {
    path: "system/users",
    component: () => import("@/views/SystemUsersView.vue"),
    meta: { title: "系统用户", icon: "UserFilled", accountTypes: ["ADMIN"] },
  },
  {
    path: "system/roles",
    component: () => import("@/views/SystemRolesView.vue"),
    meta: { title: "角色管理", icon: "Key", accountTypes: ["ADMIN"] },
  },
  {
    path: "system/permissions",
    component: () => import("@/views/SystemPermissionsView.vue"),
    meta: { title: "权限管理", icon: "Lock", accountTypes: ["ADMIN"] },
  },
  {
    path: "operation-logs",
    component: () => import("@/views/OperationLogsView.vue"),
    meta: { title: "操作日志", icon: "List", accountTypes: ["ADMIN"] },
  },
  {
    path: "ai",
    component: () => import("@/views/AiAssistantView.vue"),
    meta: {
      title: "AI 诊疗助手",
      icon: "MagicStick",
      accountTypes: ["ADMIN", "OWNER"],
    },
  },
  {
    path: "profile",
    component: () => import("@/views/ProfileView.vue"),
    meta: { title: "个人中心", icon: "User" },
  },
];
const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    component: () => import("@/views/LoginView.vue"),
    meta: { public: true, title: "登录" },
  },
  {
    path: "/",
    component: () => import("@/layout/AppLayout.vue"),
    redirect: "/dashboard",
    children,
  },
];
const router = createRouter({ history: createWebHistory(), routes });
router.beforeEach((to) => {
  document.title = `${String(to.meta.title || "管理平台")} · 宠安诊所`;
  const auth = useAuthStore();
  if (!to.meta.public && !auth.token) return "/login";
  if (to.path === "/login" && auth.token) return "/dashboard";
  return true;
});
export default router;
