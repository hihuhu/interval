<template>
  <main class="auth-page">
    <section class="auth-card surface-card">
      <div class="brand">
        <img class="logo-icon" :src="FAVICON_PATH" alt="" aria-hidden="true" />
        <span>Interval</span>
      </div>
      <h1>创建账号</h1>
      <form @submit.prevent="submit">
        <label>
          <span>用户名</span>
          <input v-model="username" class="input-underline" name="username" autocomplete="username" required />
        </label>
        <label>
          <span>密码</span>
          <input
            v-model="password"
            class="input-underline"
            name="password"
            type="password"
            autocomplete="new-password"
            required
          />
        </label>
        <label>
          <span>确认密码</span>
          <input
            v-model="confirmPassword"
            class="input-underline"
            name="confirmPassword"
            type="password"
            autocomplete="new-password"
            required
          />
        </label>
        <p class="password-rule">{{ PASSWORD_RULE_TEXT }}</p>
        <p v-if="formErrorMessage" class="error" role="alert">{{ formErrorMessage }}</p>
        <p v-if="auth.errorMessage" class="error">{{ auth.errorMessage }}</p>
        <p v-if="successMessage" class="success">{{ successMessage }}</p>
        <button class="primary-action" type="submit" :disabled="auth.loading">
          {{ auth.loading ? '注册中...' : '注册' }}
        </button>
      </form>
      <RouterLink class="register-link" to="/login">已有账号？登录</RouterLink>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { FAVICON_PATH } from '@/constants/brand';
import { useAuthStore } from '@/stores/useAuthStore';
import {
  PASSWORD_RULE_TEXT,
  getPasswordConfirmationMessage,
  getPasswordValidationMessage,
} from '@/utils/passwordRules';

const auth = useAuthStore();
const router = useRouter();
const username = ref('');
const password = ref('');
const confirmPassword = ref('');
const successMessage = ref('');
const formErrorMessage = ref('');

async function submit() {
  formErrorMessage.value = getPasswordValidationMessage(password.value)
    || getPasswordConfirmationMessage(password.value, confirmPassword.value);
  if (formErrorMessage.value) return;

  await auth.register(username.value, password.value);
  successMessage.value = '注册成功，请登录';
  setTimeout(() => router.push('/login'), 500);
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: linear-gradient(135deg, rgba(238, 242, 255, 0.62), rgba(255, 255, 255, 0.92), rgba(240, 249, 255, 0.62));
}

.auth-card {
  width: min(392px, 100%);
  padding: 32px;
  display: grid;
  gap: 22px;
}

.brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #4f46e5;
  font-size: 20px;
  font-weight: 900;
}

h1 {
  margin: 0;
  color: #1e293b;
  font-size: 22px;
}

form,
label {
  display: grid;
  gap: 14px;
}

label span {
  color: #64748b;
  font-size: 11px;
  font-weight: 800;
}

input {
  min-height: 40px;
  padding: 8px 0;
}

.password-rule {
  margin: -4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.primary-action {
  min-height: 48px;
  width: 100%;
}

.error,
.success {
  margin: -4px 0 0;
  border-radius: 12px;
  padding: 10px 12px;
  font-size: 12px;
}

.error {
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.success {
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
}

.register-link {
  color: #4f46e5;
  font-size: 12px;
  font-weight: 800;
  text-align: center;
  text-decoration: none;
}
</style>
