<template>
  <main class="auth-page">
    <section class="card">
      <h1>Interval</h1>
      <p class="subtitle">时间记录从这里开始</p>
      <form @submit.prevent="submit">
        <label>用户名<input v-model="username" name="username" required /></label>
        <label>密码<input v-model="password" name="password" type="password" required /></label>
        <p v-if="auth.errorMessage" class="error">{{ auth.errorMessage }}</p>
        <button type="submit" :disabled="auth.loading">{{ auth.loading ? '登录中...' : '登录' }}</button>
      </form>
      <RouterLink to="/register">还没有账号？注册</RouterLink>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/useAuthStore';

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();
const username = ref('');
const password = ref('');

async function submit() {
  await auth.login(username.value, password.value);
  await router.push(typeof route.query.redirect === 'string' ? route.query.redirect : '/time-grid');
}
</script>

<style scoped>
.auth-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; background: linear-gradient(135deg, #eef2ff, #fff, #faf5ff); }
.card { width: min(420px, 100%); background: white; border-radius: 24px; padding: 32px; box-shadow: 0 24px 70px rgba(79, 70, 229, 0.14); display: grid; gap: 18px; }
h1 { margin: 0; text-align: center; color: #4f46e5; font-size: 44px; }
.subtitle { margin: 0; text-align: center; color: #6b7280; }
form, label { display: grid; gap: 10px; }
input { border: 1px solid #d1d5db; border-radius: 12px; padding: 12px; }
button { border: 0; background: #4f46e5; color: white; border-radius: 12px; padding: 12px; font-weight: 700; cursor: pointer; }
.error { color: #dc2626; margin: 0; }
a { text-align: center; color: #4f46e5; }
</style>
