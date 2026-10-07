<template>
  <div class="app-shell" @keydown.esc="closeNav">
    <a href="#main-content" class="skip-link">跳到页面内容</a>
    <button
      v-if="mobile && navOpen"
      class="nav-backdrop"
      aria-label="关闭导航"
      @click="closeNav"
    />
    <aside
      ref="sidebarRef"
      :class="[
        'sidebar',
        { collapsed: !mobile && collapsed, 'mobile-open': mobile && navOpen },
      ]"
      :inert="mobile && !navOpen ? true : undefined"
      aria-label="主导航"
      @keydown="trapFocus"
    >
      <ClinicBrand :compact="!mobile && collapsed" />
      <div class="sidebar-nav">
        <PageState v-if="auth.menuLoading" kind="loading" />
        <p v-else-if="auth.menuLoaded && !menuNodes.length" class="muted">
          当前账号暂无菜单权限
        </p>
        <div v-for="group in groups" :key="group.label" class="nav-group">
          <p v-if="!collapsed || mobile" class="nav-group-label">
            {{ group.label }}
          </p>
          <el-menu
            :collapse="!mobile && collapsed"
            :default-active="$route.path"
            router
            @select="closeNav"
          >
            <PermissionMenuNode
              v-for="node in group.nodes"
              :key="node.id"
              :node="node"
            />
          </el-menu>
        </div>
      </div>
      <div v-if="!collapsed || mobile" class="sidebar-footer">
        <span>宠安智能诊所</span>
      </div>
    </aside>
    <div class="workspace">
      <header class="topbar">
        <div class="topbar-left">
          <el-button
            ref="menuButton"
            text
            circle
            :aria-label="
              mobile
                ? navOpen
                  ? '关闭导航'
                  : '打开导航'
                : collapsed
                  ? '展开导航'
                  : '收起导航'
            "
            :aria-expanded="mobile ? navOpen : !collapsed"
            @click="toggleNav"
            ><el-icon
              ><Expand v-if="collapsed || mobile" /><Fold v-else /></el-icon
          ></el-button>
          <div class="breadcrumb">
            <span>宠安诊所</span
            ><ClinicIcon class="direction-icon" name="chevron-right" />
            <strong>{{ title }}</strong>
          </div>
        </div>
        <el-dropdown>
          <div
            class="user-chip"
            tabindex="0"
            role="button"
            aria-label="账户菜单"
          >
            <span class="avatar">{{
              auth.user?.displayName?.slice(0, 1) || "宠"
            }}</span
            ><span class="user-label"
              >{{ auth.user?.displayName
              }}<small>{{ accountLabel }}</small></span
            ><el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown
            ><el-dropdown-menu
              ><el-dropdown-item @click="$router.push('/profile')"
                >个人中心</el-dropdown-item
              ><el-dropdown-item divided @click="signOut"
                >退出登录</el-dropdown-item
              ></el-dropdown-menu
            ></template
          >
        </el-dropdown>
      </header>
      <main id="main-content" class="page-container" tabindex="-1">
        <router-view />
      </main>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from "vue";
import { ArrowDown, Expand, Fold } from "@element-plus/icons-vue";
import { useRoute, useRouter } from "vue-router";
import ClinicBrand from "@/components/ClinicBrand.vue";
import ClinicIcon from "@/components/ClinicIcon.vue";
import PageState from "@/components/PageState.vue";
import PermissionMenuNode from "@/components/PermissionMenuNode.vue";
import { useAuthStore } from "@/stores/auth";
import type { PermissionNode } from "@/types";
const auth = useAuthStore();
const router = useRouter();
const route = useRoute();
const media = window.matchMedia("(max-width: 899px)");
const mobile = ref(media.matches),
  navOpen = ref(false),
  collapsed = ref(false);
const sidebarRef = ref<HTMLElement>();
const menuButton = ref<{ $el: HTMLElement }>();
const title = computed(() => String(route.meta.title || "诊疗管理"));
const accountLabel = computed(
  () =>
    ({ ADMIN: "管理员", STAFF: "诊所员工", OWNER: "宠物主人" })[
      auth.user?.accountType || "OWNER"
    ],
);
const menuNodes = computed(() =>
  auth.permissionTree.filter((node) => auth.isMenuNodeVisible(node)),
);
function firstPath(node: PermissionNode): string {
  return node.path || (node.children || []).map(firstPath).find(Boolean) || "";
}
const groups = computed(() => {
  const definitions = [
    {
      label: "诊疗工作",
      paths: [
        "/dashboard",
        "/visits",
        "/schedules",
        "/medical-records",
        "/vaccinations",
        "/vets",
      ],
    },
    {
      label: "档案与沟通",
      paths: ["/owners", "/pets", "/notices", "/feedback", "/ai"],
    },
    {
      label: "设置与审计",
      paths: ["/pet-types", "/specialties", "/system", "/operation-logs"],
    },
  ];
  return definitions
    .map((group, index) => ({
      label: group.label,
      nodes: menuNodes.value.filter((node) => {
        const path = firstPath(node);
        const match = definitions.findIndex((item) =>
          item.paths.some(
            (prefix) => path === prefix || path.startsWith(prefix + "/"),
          ),
        );
        return match === index || (match < 0 && index === 2);
      }),
    }))
    .filter((group) => group.nodes.length);
});
function changed(event: MediaQueryListEvent) {
  mobile.value = event.matches;
  navOpen.value = false;
}
media.addEventListener("change", changed);
onBeforeUnmount(() => media.removeEventListener("change", changed));
function closeNav() {
  if (mobile.value && navOpen.value) {
    navOpen.value = false;
    nextTick(() => menuButton.value?.$el.focus());
  }
}
async function toggleNav() {
  if (!mobile.value) {
    collapsed.value = !collapsed.value;
    return;
  }
  navOpen.value = !navOpen.value;
  if (navOpen.value) {
    await nextTick();
    sidebarRef.value
      ?.querySelector<HTMLElement>('[tabindex="0"],button,a')
      ?.focus();
  }
}
function trapFocus(event: KeyboardEvent) {
  if (!mobile.value || !navOpen.value || event.key !== "Tab") return;
  const nodes = Array.from(
    sidebarRef.value?.querySelectorAll<HTMLElement>(
      'button,a,[tabindex="0"]',
    ) || [],
  ).filter((node) => node.offsetParent !== null);
  const first = nodes[0],
    last = nodes[nodes.length - 1];
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last?.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first?.focus();
  }
}
watch(() => route.path, closeNav);
function signOut() {
  auth.logout();
  router.push("/login");
}
</script>
