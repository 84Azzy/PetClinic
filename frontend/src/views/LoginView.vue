<template>
  <div class="login-page">
    <AuthVisual />
    <section class="login-panel">
      <div class="login-card">
        <ClinicBrand class="mobile-brand" />
        <h2>欢迎回到宠安</h2>
        <p>登录账号，继续你的诊疗工作。</p>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="submit"
        >
          <el-form-item label="账号" prop="username"
            ><el-input
              v-model="form.username"
              size="large"
              placeholder="请输入账号"
              autocomplete="username"
              :prefix-icon="User"
          /></el-form-item>
          <el-form-item label="密码" prop="password"
            ><el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              placeholder="请输入密码"
              autocomplete="current-password"
              :prefix-icon="Lock"
          /></el-form-item>
          <div class="demo-hint">
            <span>演示账号：admin / staff / owner_a</span
            ><span>演示密码：123456</span>
          </div>
          <el-alert
            v-if="submitError"
            :title="submitError"
            type="error"
            :closable="false"
            show-icon
            class="form-alert"
          />
          <el-button
            native-type="submit"
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            :disabled="loading"
            >{{ loading ? "正在登录…" : "进入诊所工作台" }}</el-button
          >
          <div class="auth-switch">
            第一次使用？<router-link to="/register"
              >注册宠物主人账号</router-link
            >
          </div>
        </el-form>
      </div>
      <p class="copyright">© 2026 宠安智能诊所 · 作品集演示</p>
    </section>
  </div>
</template>
<script setup lang="ts">
import AuthVisual from "@/components/AuthVisual.vue";
import ClinicBrand from "@/components/ClinicBrand.vue";
import { reactive, ref } from "vue";
import { User, Lock } from "@element-plus/icons-vue";
import type { FormInstance, FormRules } from "element-plus";
import { login } from "@/api/auth";
import { useAuthStore } from "@/stores/auth";
import { useRouter } from "vue-router";
const formRef = ref<FormInstance>();
const loading = ref(false);
const submitError = ref("");
const form = reactive({ username: "admin", password: "123456" });
const rules: FormRules = {
  username: [{ required: true, message: "请输入账号", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
};
const auth = useAuthStore();
const router = useRouter();
const submit = async () => {
  if (loading.value) return;
  if (!(await formRef.value?.validate().catch(() => false))) return;
  if (loading.value) return;
  loading.value = true;
  submitError.value = "";
  try {
    const res = await login(form);
    // 登录成功后继续加载后端权限树，菜单和按钮准备好以后再进入业务页。
    await auth.startSession(res.data.token, res.data.user);
    /**
     * 访问 /pets
     * → 发现没登录
     * → 跳到 /login?redirect=/pets
     * → 登录成功
     * → 回到 /pets
     */
    const redirect =
      typeof router.currentRoute.value.query.redirect === "string"
        ? router.currentRoute.value.query.redirect
        : auth.firstMenuPath || "/profile";

    await router.push(redirect);
  } catch (error) {
    submitError.value =
      (error as { response?: { data?: { message?: string } } }).response?.data
        ?.message || "暂时无法登录，请检查网络后重试";
  } finally {
    loading.value = false;
  }
};
</script>
