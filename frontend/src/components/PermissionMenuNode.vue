<template>
  <el-sub-menu v-if="menuChildren.length" :index="`permission-${node.id}`">
    <template #title>
      <el-icon><ClinicIcon :name="iconName" /></el-icon>
      <span>{{ displayName }}</span>
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
    <el-icon><ClinicIcon :name="iconName" /></el-icon>
    <template #title>{{ displayName }}</template>
  </el-menu-item>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useAuthStore } from "@/stores/auth";
import ClinicIcon from "./ClinicIcon.vue";
import type { PermissionNode } from "@/types";

const props = defineProps<{ node: PermissionNode }>();
const auth = useAuthStore();
const iconName = computed(() => props.node.path?.split("/")[1] || "settings");
const menuLabels: Record<string, string> = {
  "/dashboard": "诊所工作台",
  "/visits": "预约就诊",
  "/pets": "宠物档案",
  "/owners": "主人档案",
  "/medical-records": "电子病历",
  "/vaccinations": "疫苗记录",
  "/feedback": "意见反馈",
  "/notices": "诊所公告",
  "/ai": "AI 诊疗助手",
};
const displayName = computed(
  () => menuLabels[props.node.path || ""] || props.node.name,
);

const menuChildren = computed(() =>
  (props.node.children ?? []).filter((child) => auth.isMenuNodeVisible(child)),
);
</script>
