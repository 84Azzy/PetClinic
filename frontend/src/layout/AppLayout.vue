<template>
  <div class="app-shell">
    <aside :class="['sidebar', { collapsed }]">
      <div class="brand">
        <div class="brand-mark">P</div>
        <div v-if="!collapsed">
          <strong>宠安诊所</strong><small>SMART PET CLINIC</small>
        </div>
      </div>
      <el-menu
        :collapse="collapsed"
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#cbd5e1"
        active-text-color="#5eead4"
        ><template v-for="group in visibleGroups" :key="group.title"
          ><el-menu-item
            v-if="group.items.length === 1 && !group.grouped"
            :index="group.items[0].path"
            ><el-icon><component :is="group.items[0].icon" /></el-icon
            ><template #title>{{
              group.items[0].title
            }}</template></el-menu-item
          ><el-sub-menu v-else :index="group.title"
            ><template #title
              ><el-icon><component :is="group.icon" /></el-icon
              ><span>{{ group.title }}</span></template
            ><el-menu-item
              v-for="item in group.items"
              :key="item.path"
              :index="item.path"
              ><el-icon><component :is="item.icon" /></el-icon
              ><template #title>{{ item.title }}</template></el-menu-item
            ></el-sub-menu
          ></template
        ></el-menu
      >
      <div class="sidebar-tip" v-if="!collapsed">健康相伴，安心托付</div>
    </aside>
    <main class="workspace">
      <header class="topbar">
        <div class="topbar-left">
          <el-button text circle @click="collapsed = !collapsed"
            ><el-icon><Fold v-if="!collapsed" /><Expand v-else /></el-icon
          ></el-button>
          <div>
            <strong>{{ String($route.meta.title || "管理平台") }}</strong
            ><span> / 智能宠物诊疗系统</span>
          </div>
        </div>
        <el-dropdown
          ><div class="user-chip">
            <span class="avatar">{{
              auth.user?.displayName?.slice(0, 1) || "宠"
            }}</span>
            <div>
              <strong>{{ auth.user?.displayName || "演示用户" }}</strong
              ><small>{{ accountLabel }}</small>
            </div>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown
            ><el-dropdown-menu
              ><el-dropdown-item @click="$router.push('/profile')"
                >个人中心</el-dropdown-item
              ><el-dropdown-item divided @click="signOut"
                >退出登录</el-dropdown-item
              ></el-dropdown-menu
            ></template
          ></el-dropdown
        >
      </header>
      <div class="tabbar">
        <span class="tab active"
          ><i></i>{{ String($route.meta.title || "首页") }}</span
        >
      </div>
      <section class="page-container"><router-view /></section>
    </main>
  </div>
</template>
<script setup lang="ts">
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
const collapsed = ref(false);
const auth = useAuthStore();
const router = useRouter();
const accountLabel = computed(
  () =>
    ({ ADMIN: "管理员", STAFF: "诊所员工", OWNER: "宠物主人" })[
      auth.user?.accountType || "OWNER"
    ],
);
type Item = { title: string; path: string; icon: string; types?: string[] };
type Group = { title: string; icon: string; grouped?: boolean; items: Item[] };
const groups: Group[] = [
  {
    title: "数据看板",
    icon: "DataAnalysis",
    items: [{ title: "数据看板", path: "/dashboard", icon: "DataAnalysis" }],
  },
  {
    title: "档案中心",
    icon: "Files",
    grouped: true,
    items: [
      {
        title: "主人档案",
        path: "/owners",
        icon: "User",
        types: ["ADMIN", "STAFF"],
      },
      { title: "宠物档案", path: "/pets", icon: "MostlyCloudy" },
      {
        title: "宠物类型",
        path: "/pet-types",
        icon: "CollectionTag",
        types: ["ADMIN", "STAFF"],
      },
    ],
  },
  {
    title: "诊疗管理",
    icon: "FirstAidKit",
    grouped: true,
    items: [
      {
        title: "兽医管理",
        path: "/vets",
        icon: "Avatar",
        types: ["ADMIN", "STAFF"],
      },
      {
        title: "兽医专长",
        path: "/specialties",
        icon: "Medal",
        types: ["ADMIN", "STAFF"],
      },
      { title: "排班管理", path: "/schedules", icon: "Calendar" },
      { title: "预约就诊", path: "/visits", icon: "Tickets" },
      {
        title: "电子病历",
        path: "/medical-records",
        icon: "Document",
        types: ["ADMIN", "STAFF"],
      },
      { title: "疫苗记录", path: "/vaccinations", icon: "FirstAidKit" },
    ],
  },
  {
    title: "运营服务",
    icon: "Service",
    grouped: true,
    items: [
      { title: "公告管理", path: "/notices", icon: "Bell" },
      { title: "意见反馈", path: "/feedback", icon: "ChatLineSquare" },
    ],
  },
  {
    title: "系统管理",
    icon: "Setting",
    grouped: true,
    items: [
      {
        title: "系统用户",
        path: "/system/users",
        icon: "UserFilled",
        types: ["ADMIN"],
      },
      {
        title: "角色管理",
        path: "/system/roles",
        icon: "Key",
        types: ["ADMIN"],
      },
      {
        title: "权限管理",
        path: "/system/permissions",
        icon: "Lock",
        types: ["ADMIN"],
      },
      {
        title: "操作日志",
        path: "/operation-logs",
        icon: "List",
        types: ["ADMIN"],
      },
    ],
  },
  {
    title: "AI 诊疗助手",
    icon: "MagicStick",
    items: [
      {
        title: "AI 诊疗助手",
        path: "/ai",
        icon: "MagicStick",
        types: ["ADMIN", "OWNER"],
      },
    ],
  },
];
const visibleGroups = computed(() =>
  groups
    .map((g) => ({
      ...g,
      items: g.items.filter(
        (i) => !i.types || i.types.includes(auth.user?.accountType || "OWNER"),
      ),
    }))
    .filter((g) => g.items.length),
);
const signOut = () => {
  auth.logout();
  router.push("/login");
};
</script>
