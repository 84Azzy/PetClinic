import {
  createRouter,
  createWebHistory,
  type RouteRecordRaw,
} from "vue-router";
import { useAuthStore } from "@/stores/auth";

/*
 * 路由表仍是前端允许加载的组件白名单；后端权限树只决定当前用户能看到、访问哪些 path。
 * 这样不能通过数据库中的恶意组件名让浏览器加载任意文件。
 */
const children: RouteRecordRaw[] = [
  {
    path: "dashboard",
    component: () => import("@/views/DashboardView.vue"),
    meta: { title: "数据看板", icon: "DataAnalysis" },
  },
  {
    path: "owners",
    component: () => import("@/views/OwnersView.vue"),
    meta: { title: "主人档案", icon: "User" },
  },
  {
    path: "pets",
    component: () => import("@/views/PetsView.vue"),
    meta: { title: "宠物档案", icon: "MostlyCloudy" },
  },
  {
    path: "pet-types",
    component: () => import("@/views/PetTypesView.vue"),
    meta: { title: "宠物类型", icon: "CollectionTag" },
  },
  {
    path: "vets",
    component: () => import("@/views/VetsView.vue"),
    meta: { title: "兽医管理", icon: "Avatar" },
  },
  {
    path: "specialties",
    component: () => import("@/views/SpecialtiesView.vue"),
    meta: { title: "兽医专长", icon: "Medal" },
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
    meta: { title: "电子病历", icon: "Document" },
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
    meta: { title: "系统用户", icon: "UserFilled" },
  },
  {
    path: "system/roles",
    component: () => import("@/views/SystemRolesView.vue"),
    meta: { title: "角色管理", icon: "Key" },
  },
  {
    path: "system/permissions",
    component: () => import("@/views/SystemPermissionsView.vue"),
    meta: { title: "权限管理", icon: "Lock" },
  },
  {
    path: "operation-logs",
    component: () => import("@/views/OperationLogsView.vue"),
    meta: { title: "操作日志", icon: "List" },
  },
  {
    path: "ai",
    component: () => import("@/views/AiAssistantView.vue"),
    meta: { title: "AI 诊疗助手", icon: "MagicStick" },
  },
  {
    path: "profile",
    component: () => import("@/views/ProfileView.vue"),
    meta: { title: "个人中心", permissionFree: true },
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
    children,
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to) => {
  document.title = `${String(to.meta.title || "管理平台")} · zzy的黑心诊所`;
  const auth = useAuthStore();

  if (auth.token && !auth.restored) await auth.restoreSession();

  if (to.meta.public) {
    return auth.token ? auth.firstMenuPath || "/profile" : true;
  }

  if (!auth.token) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }

  if (to.path === "/") return auth.firstMenuPath || "/profile";
  if (to.meta.permissionFree) return true;

  /*
   * TODO【新知识：权限树驱动路由守卫】
   * path 必须既存在于前端静态路由表，也存在于后端下发的 MENU 树中。
   * 菜单隐藏和 URL 直接访问因此使用同一份后端权限数据。
   */
  if (!auth.canVisitPath(to.path)) {
    const fallback = auth.firstMenuPath || "/profile";
    return to.path === fallback ? "/profile" : fallback;
  }

  return true;
});

export default router;
