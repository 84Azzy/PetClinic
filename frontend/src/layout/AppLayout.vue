<template>
  <div class="app-shell">
    <aside :class="['sidebar', { collapsed }]">
      <div class="brand">
        <div class="brand-mark">P</div>
        <div v-if="!collapsed">
          <strong>黑心诊所</strong><small>ZZY PET CLINIC</small>
        </div>
      </div>

      <div v-if="auth.menuLoading" class="menu-state">正在加载菜单...</div>
      <div
        v-else-if="auth.menuLoaded && menuNodes.length === 0"
        class="menu-state"
      >
        当前账号暂无菜单权限
      </div>

      <el-menu
        v-else
        :collapse="collapsed"
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#cbd5e1"
        active-text-color="#5eead4"
      >
        <PermissionMenuNode
          v-for="node in menuNodes"
          :key="node.id"
          :node="node"
        />
      </el-menu>

      <div v-if="!collapsed" class="sidebar-tip">健康相伴，安心托付</div>
    </aside>

    <main class="workspace">
      <header class="topbar">
        <div class="topbar-left">
          <el-button text circle @click="collapsed = !collapsed">
            <el-icon><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
          </el-button>
          <div>
            <strong>{{ String($route.meta.title || "管理平台") }}</strong>
            <span> / 智能宠物诊疗系统</span>
          </div>
        </div>

        <el-dropdown>
          <div class="user-chip">
            <span class="avatar">
              {{ auth.user?.displayName?.slice(0, 1) || "宠" }}
            </span>
            <div>
              <strong>{{ auth.user?.displayName || "演示用户" }}</strong>
              <small>{{ accountLabel }}</small>
            </div>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="$router.push('/profile')">
                个人中心
              </el-dropdown-item>
              <el-dropdown-item divided @click="signOut">
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <div class="tabbar">
        <span class="tab active">
          <i></i>{{ String($route.meta.title || "首页") }}
        </span>
      </div>

      <section class="page-container"><router-view /></section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import PermissionMenuNode from "@/components/PermissionMenuNode.vue";
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

/*
 * TODO【新知识：后端驱动的计算属性】
 * permissionTree 由 /system/permissions/mine 返回；这里只留下有效 MENU 根节点。
 * 子菜单继续由 PermissionMenuNode 递归处理，BUTTON/API 永远不会混进侧边栏。
 */
const menuNodes = computed(() =>
  auth.permissionTree.filter((node) => auth.isMenuNodeVisible(node)),
);

const signOut = () => {
  auth.logout();
  router.push("/login");
};
</script>

<style scoped>
.menu-state {
  padding: 24px 14px;
  color: #9fb2c4;
  font-size: 13px;
  line-height: 1.6;
  text-align: center;
}
</style>
