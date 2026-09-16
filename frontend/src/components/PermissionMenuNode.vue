<template>
  <el-sub-menu
    v-if="menuChildren.length"
    :index="`permission-${node.id}`"
  >
    <template #title>
      <el-icon v-if="node.icon"><component :is="node.icon" /></el-icon>
      <span>{{ node.name }}</span>
    </template>

    <!--
      TODO【新知识：递归组件】
      一个菜单节点继续渲染自己的 MENU 子节点，因此能支持任意层级的后端权限树。
      当 menuChildren 为空时进入下面的叶子分支，递归自然结束。
    -->
    <PermissionMenuNode
      v-for="child in menuChildren"
      :key="child.id"
      :node="child"
    />
  </el-sub-menu>

  <el-menu-item v-else-if="node.path" :index="node.path">
    <el-icon v-if="node.icon"><component :is="node.icon" /></el-icon>
    <template #title>{{ node.name }}</template>
  </el-menu-item>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useAuthStore } from "@/stores/auth";
import type { PermissionNode } from "@/types";

const props = defineProps<{ node: PermissionNode }>();
const auth = useAuthStore();

const menuChildren = computed(() =>
  (props.node.children ?? []).filter((child) =>
    auth.isMenuNodeVisible(child),
  ),
);
</script>
