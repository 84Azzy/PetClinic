import { defineStore } from "pinia";
import { getMe } from "@/api/auth";
import { getPermissionTree } from "@/api/permissions";
import type { PermissionNode, UserProfile } from "@/types";
import {
  canAccess,
  type AccessRequirement,
  type Role,
} from "@/utils/permission";

/*
 * 菜单码表示“被分配了这个功能”，但个别页面的后端接口还附带角色条件。
 * 这里补上与 Controller 一致的页面级条件，避免菜单可见、进入页面后却立即 403。
 */
const PAGE_ACCESS: Record<string, AccessRequirement> = {
  "/medical-records": { roles: ["ADMIN", "STAFF"] },
  "/vaccinations": { roles: ["ADMIN", "STAFF"] },
};

function readStoredUser(): UserProfile | null {
  try {
    const value = localStorage.getItem("petclinic-user");
    return value ? JSON.parse(value) : null;
  } catch {
    localStorage.removeItem("petclinic-user");
    return null;
  }
}

/*
 * TODO【新知识：递归展开树】
 * 每个节点先收集自己的 code，再递归收集 children，最终把树变成一维权限码数组。
 * MENU 用于菜单和路由，BUTTON 用于按钮，API 仍交给后端做最终安全校验。
 */
function flattenCodes(nodes: PermissionNode[]): string[] {
  return nodes.flatMap((node) => [
    node.code,
    ...flattenCodes(node.children ?? []),
  ]);
}

function normalizePath(path: string): string {
  return path.length > 1 && path.endsWith("/") ? path.slice(0, -1) : path;
}

function pathMeetsPageAccess(permissions: string[], path: string): boolean {
  return canAccess(permissions, PAGE_ACCESS[normalizePath(path)] ?? {});
}

function isVisibleMenuNode(
  node: PermissionNode,
  permissions: string[],
): boolean {
  if (node.type !== "MENU" || node.status !== "ACTIVE") return false;

  const ownPathVisible =
    !!node.path && pathMeetsPageAccess(permissions, node.path);
  const hasVisibleChild = (node.children ?? []).some((child) =>
    isVisibleMenuNode(child, permissions),
  );

  return ownPathVisible || hasVisibleChild;
}

function containsMenuPath(
  nodes: PermissionNode[],
  path: string,
  permissions: string[],
): boolean {
  const target = normalizePath(path);
  return nodes.some(
    (node) =>
      (node.type === "MENU" &&
        node.status === "ACTIVE" &&
        !!node.path &&
        normalizePath(node.path) === target &&
        pathMeetsPageAccess(permissions, target)) ||
      containsMenuPath(node.children ?? [], target, permissions),
  );
}

function findFirstMenuPath(
  nodes: PermissionNode[],
  permissions: string[],
): string | undefined {
  for (const node of nodes) {
    if (
      node.type === "MENU" &&
      node.status === "ACTIVE" &&
      node.path &&
      pathMeetsPageAccess(permissions, node.path)
    )
      return node.path;
    const childPath = findFirstMenuPath(node.children ?? [], permissions);
    if (childPath) return childPath;
  }
  return undefined;
}

export const useAuthStore = defineStore("auth", {
  state: () => {
    const user = readStoredUser();
    return {
      token: localStorage.getItem("petclinic-token") || "",
      user,
      permissions: user?.authorities ?? [],
      permissionTree: [] as PermissionNode[],
      restored: false,
      menuLoaded: false,
      menuLoading: false,
    };
  },

  getters: {
    hasAuthority: (state) => (code: string) =>
      state.permissions.includes(code),
    hasRole: (state) => (role: Role) =>
      state.permissions.includes(`ROLE_${role}`),
    firstMenuPath: (state) =>
      findFirstMenuPath(state.permissionTree, state.permissions),
    canVisitPath: (state) => (path: string) =>
      containsMenuPath(state.permissionTree, path, state.permissions),
    isMenuNodeVisible: (state) => (node: PermissionNode) =>
      isVisibleMenuNode(node, state.permissions),
  },

  actions: {
    setSession(token: string, user: UserProfile) {
      const normalizedUser: UserProfile = {
        ...user,
        authorities: user.authorities ?? [],
      };

      this.token = token;
      this.user = normalizedUser;
      this.permissions = normalizedUser.authorities;
      this.permissionTree = [];
      this.menuLoaded = false;
      this.restored = false;

      localStorage.setItem("petclinic-token", token);
      localStorage.setItem("petclinic-user", JSON.stringify(normalizedUser));
    },

    async startSession(token: string, user: UserProfile) {
      this.setSession(token, user);
      try {
        await this.loadPermissionTree();
        this.restored = true;
      } catch (error) {
        // 权限树不可用时不保留半登录状态，遵循 fail-closed。
        this.logout();
        throw error;
      }
    },

    async loadPermissionTree() {
      if (!this.token) return;

      this.menuLoading = true;
      try {
        const response = await getPermissionTree();
        this.permissionTree = response.data ?? [];

        const roles =
          this.user?.authorities.filter((code) => code.startsWith("ROLE_")) ?? [];
        const treeCodes = flattenCodes(this.permissionTree);

        // Set 去重后再展开为数组：角色码来自 /auth/me，权限码来自最新权限树。
        this.permissions = [...new Set([...roles, ...treeCodes])];
        this.menuLoaded = true;
      } finally {
        this.menuLoading = false;
      }
    },

    async restoreSession() {
      if (!this.token) {
        this.restored = true;
        return;
      }

      try {
        const response = await getMe();
        this.setSession(this.token, response.data);
        await this.loadPermissionTree();
      } catch {
        this.logout();
      } finally {
        this.restored = true;
      }
    },

    logout() {
      this.token = "";
      this.user = null;
      this.permissions = [];
      this.permissionTree = [];
      this.restored = true;
      this.menuLoaded = false;
      this.menuLoading = false;
      localStorage.removeItem("petclinic-token");
      localStorage.removeItem("petclinic-user");
    },
  },
});
