<template>
  <div class="login-page">
    <div class="login-visual">
      <div class="visual-copy">
        <div class="eyebrow">SMART PET CLINIC</div>
        <h1>让每一次诊疗<br /><span>更安心、更高效</span></h1>
        <p>宠物档案、兽医排班、预约就诊与健康记录一站式管理。</p>
        <div class="feature-row">
          <div><b>24h</b><small>档案随时可查</small></div>
          <div><b>4+</b><small>核心诊疗专科</small></div>
          <div><b>100%</b><small>数据闭环管理</small></div>
        </div>
      </div>
      <div class="paw paw-one">●</div>
      <div class="paw paw-two">●</div>
    </div>
    <div class="login-panel">
      <div class="login-card">
        <div class="mobile-brand"><span>P</span>宠安诊所</div>
        <div class="eyebrow teal">WELCOME BACK</div>
        <h2>登录管理平台</h2>
        <p>使用演示账号进入宠物诊疗工作台</p>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @keyup.enter="submit"
          ><el-form-item label="账号" prop="username"
            ><el-input
              v-model="form.username"
              size="large"
              placeholder="请输入账号"
              :prefix-icon="User" /></el-form-item
          ><el-form-item label="密码" prop="password"
            ><el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              placeholder="请输入密码"
              :prefix-icon="Lock"
          /></el-form-item>
          <div class="demo-hint">
            <span>演示账号：admin / staff / owner_a</span
            ><span>密码：123456</span>
          </div>
          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            @click="submit"
            >进入系统</el-button
          ></el-form
        >
      </div>
      <div class="copyright">© 2026 宠安智能诊所 · 学习演示项目</div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { reactive, ref } from "vue";
import { User, Lock } from "@element-plus/icons-vue";
import type { FormInstance, FormRules } from "element-plus";
import { login } from "@/api/auth";
import { useAuthStore } from "@/stores/auth";
import { useRouter } from "vue-router";
const formRef = ref<FormInstance>();
const loading = ref(false);
const form = reactive({ username: "admin", password: "123456" });
const rules: FormRules = {
  username: [{ required: true, message: "请输入账号", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
};
const auth = useAuthStore();
const router = useRouter();
const submit = async () => {
  if (!(await formRef.value?.validate().catch(() => false))) return;
  loading.value = true;
  try {
    const res = await login(form);
    auth.setSession(res.data.token, res.data.user);
    router.push("/dashboard");
  } finally {
    loading.value = false;
  }
};
</script>
