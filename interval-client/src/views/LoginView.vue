<template>
  <main class="auth-page">
    <section class="auth-card surface-card" aria-labelledby="login-title">
      <div class="brand">
        <div class="logo-icon"></div>
        <span>Interval</span>
      </div>

      <div>
        <p class="eyebrow">WELCOME BACK</p>
        <h1 id="login-title">欢迎回来</h1>
        <p class="subtitle">用更轻盈的方式，继续记录今天的时间。</p>
      </div>

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
            autocomplete="current-password"
            required
          />
        </label>
        <p v-if="auth.errorMessage" class="error" role="alert">{{ auth.errorMessage }}</p>
        <button class="primary-action" type="submit" :disabled="auth.loading">
          <LogIn :size="16" />
          {{ auth.loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <p class="seed-hint">开发账号：admin / 123ABCdef*</p>
      <RouterLink class="register-link" to="/register">还没有账号？注册</RouterLink>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { LogIn } from '@lucide/vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/useAuthStore';

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();
const username = ref('admin');
const password = ref('123ABCdef*');

async function submit() {
  await auth.login(username.value, password.value);
  await router.push(typeof route.query.redirect === 'string' ? route.query.redirect : '/time-grid');
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
  letter-spacing: 0;
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
  letter-spacing: 0.08em;
}

input {
  min-height: 40px;
  padding: 8px 0;
  font-size: 14px;
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

.seed-hint,
.register-link {
  margin: 0;
  text-align: center;
  color: #64748b;
  font-size: 12px;
}

.register-link {
  color: #4f46e5;
  font-weight: 800;
  text-decoration: none;
}
</style>
