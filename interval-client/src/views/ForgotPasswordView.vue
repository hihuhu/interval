<template>
  <main class="auth-page">
    <section class="auth-card surface-card" aria-labelledby="forgot-password-title">
      <div class="brand">
        <img class="logo-icon" :src="FAVICON_PATH" alt="" aria-hidden="true" />
        <span>Interval</span>
      </div>
      <div>
        <p class="eyebrow">PASSWORD HELP</p>
        <h1 id="forgot-password-title">忘记密码</h1>
        <p class="subtitle">当前版本请联系管理员重置密码。</p>
      </div>
      <RouterLink class="register-link" to="/login">返回登录</RouterLink>
    </section>
  </main>
</template>

<script setup lang="ts">
import { FAVICON_PATH } from '@/constants/brand';
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

.register-link {
  color: #4f46e5;
  font-size: 12px;
  font-weight: 800;
  text-align: center;
  text-decoration: none;
}
</style>
