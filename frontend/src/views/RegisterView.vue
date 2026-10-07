<template>
  <div class="login-page register-page">
    <AuthVisual
      heading="为爱宠建立健康档案"
      description="从这里开始，安排就诊、维护基础资料，让照护更有条理。"
    />
    <div class="login-panel register-panel">
      <div class="login-card register-card">
        <ClinicBrand class="mobile-brand" />

        <h2>注册宠物主人账号</h2>
        <p>填写基础资料，创建专属的宠物主人账号</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="submit"
        >
          <div class="form-grid">
            <h3 class="form-section-title">主人与联系资料</h3>
            <el-form-item label="姓名" prop="displayName">
              <el-input
                v-model="form.displayName"
                size="large"
                maxlength="50"
                placeholder="请输入姓名"
                :prefix-icon="User"
              />
            </el-form-item>

            <el-form-item label="手机号" prop="phone">
              <el-input
                v-model="form.phone"
                size="large"
                maxlength="11"
                placeholder="请输入手机号"
                :prefix-icon="Phone"
              />
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
              <el-input
                v-model="form.email"
                size="large"
                maxlength="100"
                placeholder="选填"
                :prefix-icon="Message"
              />
            </el-form-item>

            <el-form-item class="full-row" label="联系地址" prop="address">
              <el-input
                v-model="form.address"
                size="large"
                maxlength="255"
                placeholder="选填"
                :prefix-icon="Location"
              />
            </el-form-item>

            <h3 class="form-section-title">登录账号与密码</h3>
            <el-form-item label="登录账号" prop="username">
              <el-input
                v-model="form.username"
                size="large"
                maxlength="20"
                placeholder="4-20 位字母、数字或下划线"
                :prefix-icon="User"
              />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                size="large"
                type="password"
                maxlength="64"
                show-password
                placeholder="至少 6 位"
                :prefix-icon="Lock"
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                size="large"
                type="password"
                maxlength="64"
                show-password
                placeholder="再次输入密码"
                :prefix-icon="Lock"
              />
            </el-form-item>
          </div>

          <el-button
            type="primary"
            native-type="submit"
            size="large"
            class="login-button"
            :loading="loading"
            :disabled="loading"
          >
            {{ loading ? "正在注册..." : "注册并进入系统" }}
          </el-button>

          <div class="auth-switch">
            已有账号？<router-link to="/login">返回登录</router-link>
          </div>
        </el-form>
      </div>

      <div class="copyright">© 2026 宠安智能诊所 · 作品集演示</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import AuthVisual from "@/components/AuthVisual.vue";
import ClinicBrand from "@/components/ClinicBrand.vue";
import { reactive, ref } from "vue";
import { Location, Lock, Message, Phone, User } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { register, type RegisterPayload } from "@/api/auth";
import { useAuthStore } from "@/stores/auth";
import { useRouter } from "vue-router";

const formRef = ref<FormInstance>();
const loading = ref(false);
const form = reactive<RegisterPayload>({
  username: "",
  password: "",
  confirmPassword: "",
  displayName: "",
  phone: "",
  email: "",
  address: "",
});

const rules: FormRules<RegisterPayload> = {
  displayName: [
    { required: true, message: "请输入姓名", trigger: "blur" },
    { max: 50, message: "姓名不能超过 50 个字符", trigger: "blur" },
  ],
  username: [
    { required: true, message: "请输入登录账号", trigger: "blur" },
    { min: 4, max: 20, message: "账号长度必须为 4-20 位", trigger: "blur" },
    {
      pattern: /^[A-Za-z0-9_]+$/,
      message: "账号只能包含字母、数字和下划线",
      trigger: "blur",
    },
  ],
  phone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    { pattern: /^1\d{10}$/, message: "手机号格式不正确", trigger: "blur" },
  ],
  email: [{ type: "email", message: "邮箱格式不正确", trigger: "blur" }],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 64, message: "密码长度必须为 6-64 位", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请再次输入密码", trigger: "blur" },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error("两次输入的密码不一致"));
          return;
        }
        callback();
      },
      trigger: ["blur", "change"],
    },
  ],
};

const auth = useAuthStore();
const router = useRouter();

const submit = async () => {
  if (loading.value) return;
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  if (loading.value) return;
  loading.value = true;
  try {
    const response = await register({
      ...form,
      username: form.username.trim(),
      displayName: form.displayName.trim(),
      phone: form.phone.trim(),
      email: form.email?.trim() || undefined,
      address: form.address?.trim() || undefined,
    });
    await auth.startSession(response.data.token, response.data.user);
    ElMessage.success("注册成功");
    await router.replace(auth.firstMenuPath || "/profile");
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  height: auto;
}

.register-panel {
  min-height: 100vh;
  overflow-y: auto;
  padding-top: 24px;
  padding-bottom: 24px;
}

.register-card {
  width: min(560px, 100%);
  padding: 24px 0;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 16px;
}

.full-row {
  grid-column: 1 / -1;
}

@media (max-width: 640px) {
  .register-panel {
    justify-content: flex-start;
  }

  .register-card {
    padding: 0;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .full-row {
    grid-column: auto;
  }
}
</style>
