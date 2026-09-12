import {
  createRouter,
  createWebHistory,
  type RouteRecordRaw,
} from "vue-router";
import { useAuthStore } from "@/stores/auth";
import type { UserProfile } from "@/types";

/**
 * ==================== Vue Router 学习笔记 ====================
 *
 * Vue Router主要负责：
 * 1. 根据浏览器 URL 选择需要显示的 Vue 组件；
 * 2. 在页面跳转前，通过导航守卫检查登录状态和访问权限。
 *
 * 本项目采用嵌套路由：
 *
 * AppLayout
 * ├── 左侧菜单
 * ├── 顶部导航
 * └── <router-view>
 *     └── DashboardView、PetsView、VisitsView 等子页面
 *
 * children 中的 path 没有以 "/" 开头，表示相对于父路由：
 * 父路由 "/" + 子路由 "pets" = "/pets"。
 */
const children: RouteRecordRaw[] = [
  {
    path: "dashboard",
    // 动态 import 实现路由懒加载：真正访问页面时才加载组件代码。
    component: () => import("@/views/DashboardView.vue"),
    // meta 保存自定义路由信息，由标题、菜单和路由守卫主动读取。
    meta: { title: "数据看板", icon: "DataAnalysis" },
  },
  {
    path: "owners",
    component: () => import("@/views/OwnersView.vue"),
    // accountTypes 声明允许进入该页面的账号类型。
    meta: { title: "主人档案", icon: "User", accountTypes: ["ADMIN", "STAFF"] },
  },
  {
    path: "pets",
    component: () => import("@/views/PetsView.vue"),
    // authority 声明进入页面所需的具体权限码。
    meta: { title: "宠物档案", icon: "MostlyCloudy", authority: "pet:manage" },
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
    meta: { title: "预约就诊", icon: "Tickets", authority: "visit:manage" },
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

/**
 * 顶层路由：
 * - /login 不使用 AppLayout，因为登录页不需要菜单和顶栏；
 * - 业务页面作为 "/" 的 children，显示在 AppLayout 的 <router-view> 中。
 */
const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    component: () => import("@/views/LoginView.vue"),
    // public 是项目自定义标记，表示该页面无需登录。
    meta: { public: true, title: "登录" },
  },
  {
    path: "/",
    component: () => import("@/layout/AppLayout.vue"),
    // 访问根地址时，默认跳转到数据看板。
    redirect: "/dashboard",
    children,
  },
];

/**
 * createWebHistory() 使用 /pets 形式的正常 URL，
 * 而不是旧式的 /#/pets。
 */
const router = createRouter({
  history: createWebHistory(),
  routes,
});

/**
 * UserProfile["accountType"] 是从 UserProfile 中取出 accountType 属性的类型，
 * 结果为 "ADMIN" | "STAFF" | "OWNER"。
 * AccountType[] 才表示由这些账号类型组成的数组，并不是二维数组。
 */
type AccountType = UserProfile["accountType"];

/**
 * ==================== 全局前置导航守卫 ====================
 *
 * beforeEach 会在每次路由跳转前执行。
 * to 是准备前往的路由；from 是当前离开的路由，本项目暂时不需要。
 *
 * 常见返回值：
 * - return true：允许跳转；
 * - return false：取消跳转；
 * - return "/login"：改为跳转到登录页；
 * - return { ... }：使用完整的路由对象跳转。
 *
 * 检查顺序：恢复登录状态 → 处理公开页 → 检查登录 → 检查账号类型
 * → 检查权限码 → 放行。
 */
router.beforeEach(async (to) => {
  document.title = `${String(to.meta.title || "管理平台")} · zzy的黑心诊所`;

  const auth = useAuthStore();

  /**
   * async/await 总结：
   * restoreSession() 会请求 GET /api/auth/me，因此返回 Promise。
   * 外层使用 await，守卫才会等身份恢复完成后再继续鉴权。
   * restoreSession() 内部的 await 只保证它自己的执行顺序，调用者不会自动等待。
   * 因为当前守卫使用了 await，所以守卫函数前必须加 async。
   * await 只暂停当前守卫，不会阻塞整个浏览器。
   */
  if (auth.token && !auth.restored) {
    await auth.restoreSession();
  }

  /**
   * 登录页是公开页面：
   * - 已登录时访问 /login，转到 /dashboard；
   * - 未登录时返回 true，允许进入登录页。
   */
  if (to.meta.public) {
    return auth.token ? "/dashboard" : true;
  }

  /**
   * 非公开页面必须登录。
   * redirect 保存用户原本想访问的完整地址，登录后可以跳回去。
   */
  if (!auth.token) {
    return {
      path: "/login",
      query: { redirect: to.fullPath },
    };
  }

  /**
   * to.meta.accountTypes 可能是账号类型数组，也可能没有配置。
   * as 是 TypeScript 类型断言，只影响类型检查，不会转换运行时数据。
   */
  const accountTypes = to.meta.accountTypes as AccountType[] | undefined;

  // 配置了账号类型限制，并且当前用户不在允许范围内，则拒绝访问。
  if (
    accountTypes &&
    (!auth.user || !accountTypes.includes(auth.user.accountType))
  ) {
    return "/dashboard";
  }

  // authority 可能是 pet:manage、visit:manage，也可能没有配置。
  const authority = to.meta.authority as string | undefined;

  // 配置了权限码，但当前用户不拥有该权限，则拒绝访问。
  if (authority && !auth.hasAuthority(authority)) {
    return "/dashboard";
  }

  // 登录状态、账号类型和权限码检查全部通过。
  return true;
});

// 导出路由器实例，供 main.ts 通过 app.use(router) 注册。
export default router;

/**
 * 用/Pets走一遍完整过程
 * 1. el-menu 请求跳转到 /pets
 * 2. Vue Router找到 pets 路由
 * 3. 执行 beforeEach
 * 4. token 存在，不去登录页
 * 5. 读取 to.meta.authority
 * 6. 得到 pet:manage
 * 7. 调用 auth.hasAuthority("pet:manage")
 * 8. 权限数组中存在该权限
 * 9. 守卫返回 true
 * 10. Router加载 PetsView.vue
 * 11. PetsView显示在 AppLayout的 router-view 中
 */
