<template>
  <main class="auth-page">
    <section class="auth-card surface-card" aria-labelledby="change-password-title">
      <div class="brand">
        <img class="logo-icon" :src="FAVICON_PATH" alt="" aria-hidden="true" />
        <span>Interval</span>
      </div>
      <div>
        <p class="eyebrow">PASSWORD REQUIRED</p>
        <h1 id="change-password-title">修改密码</h1>
        <p class="subtitle">首次登录或密码重置后需要设置新密码。</p>
      </div>
      <form @submit.prevent="submit">
        <label>
          <span>当前密码</span>
          <input v-model="currentPassword" class="input-underline" name="currentPassword" type="password" required />
        </label>
        <label>
          <span>新密码</span>
          <input v-model="newPassword" class="input-underline" name="newPassword" type="password" required />
        </label>
        <label>
          <span>确认新密码</span>
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
        <p v-if="errorMessage" class="error" role="alert">{{ errorMessage }}</p>
        <button class="primary-action" type="submit" :disabled="loading">
          {{ loading ? '提交中...' : '确认修改' }}
        </button>
      </form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { FAVICON_PATH } from '@/constants/brand';
import * as authService from '@/services/authService';
import { AUTH_MUST_CHANGE_PASSWORD_KEY } from '@/services/http';
import { useAuthStore } from '@/stores/useAuthStore';
import {
  PASSWORD_RULE_TEXT,
  getPasswordConfirmationMessage,
  getPasswordValidationMessage,
} from '@/utils/passwordRules';

const auth = useAuthStore();
const router = useRouter();
const currentPassword = ref('');
const newPassword = ref('');
const confirmPassword = ref('');
const loading = ref(false);
const errorMessage = ref<string | null>(null);

async function submit() {
  errorMessage.value = getPasswordValidationMessage(newPassword.value)
    || getPasswordConfirmationMessage(newPassword.value, confirmPassword.value);
  if (errorMessage.value) return;

  loading.value = true;
  errorMessage.value = null;
  try {
    await authService.changePassword({
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
    });
    auth.mustChangePassword = false;
    localStorage.setItem(AUTH_MUST_CHANGE_PASSWORD_KEY, 'false');
    await router.push(auth.accountType === 'ADMIN' ? '/admin' : '/time-grid');
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '修改密码失败';
    throw error;
  } finally {
    loading.value = false;
  }
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
  gap: 26px;
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

.eyebrow {
  margin: 0 0 8px;
  color: #6366f1;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

h1 {
  margin: 0;
  color: #1e293b;
  font-size: 22px;
}

.subtitle {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

form,
label {
  display: grid;
  gap: 18px;
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
  margin: -6px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.primary-action {
  min-height: 48px;
  width: 100%;
}

.error {
  margin: -4px 0 0;
  border: 1px solid #fecaca;
  border-radius: 12px;
  background: #fef2f2;
  color: #b91c1c;
  padding: 10px 12px;
  font-size: 12px;
}
</style>
