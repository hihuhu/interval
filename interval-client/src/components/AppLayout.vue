<template>
  <div class="dashboard-shell">
    <header class="app-header">
      <div class="header-inner">
        <div class="brand-zone">
          <div class="brand-mark">
            <img class="logo-icon" :src="FAVICON_PATH" alt="" aria-hidden="true" />
            <span>Interval</span>
          </div>
          <nav aria-label="主导航">
            <template v-if="accountType === 'ADMIN'">
              <RouterLink class="nav-chip" :class="{ active: activeRoute === 'admin' }" to="/admin">管理</RouterLink>
            </template>
            <template v-else>
              <RouterLink class="nav-chip" :class="{ active: activeRoute === 'timeGrid' }" to="/time-grid">时间格</RouterLink>
              <RouterLink class="nav-chip" :class="{ active: activeRoute === 'stats' }" to="/stats">统计</RouterLink>
            </template>
          </nav>
        </div>
        <div class="user-menu">
          <button type="button" class="user-trigger" @click="menuOpen = !menuOpen">
            <span>{{ username }}</span>
            <ChevronDown :size="15" aria-hidden="true" />
          </button>
          <div v-if="menuOpen" class="dropdown">
            <p class="dropdown-note">{{ accountType === 'ADMIN' ? '管理员账号' : '普通用户账号' }}</p>
            <button type="button" class="change-password-menu-item" @click="openChangePassword">
              <KeyRound :size="15" aria-hidden="true" />
              修改密码
            </button>
            <button type="button" class="logout-menu-item" @click="$emit('logout')">
              <LogOut :size="15" aria-hidden="true" />
              退出登录
            </button>
          </div>
        </div>
      </div>
    </header>
    <main>
      <slot />
    </main>

    <div v-if="changePasswordOpen" class="modal-backdrop" @click.self="closeChangePassword">
      <article class="change-password-modal" role="dialog" aria-modal="true" aria-labelledby="self-password-title">
        <div class="modal-head">
          <div>
            <h2 id="self-password-title">修改密码</h2>
            <p>当前登录账号可在这里修改自己的密码。</p>
          </div>
          <button class="icon-btn" type="button" aria-label="关闭弹窗" @click="closeChangePassword">
            <X :size="17" aria-hidden="true" />
          </button>
        </div>

        <form class="modal-body" @submit.prevent="submitChangePassword">
          <label>
            <span>当前密码</span>
            <input v-model="currentPassword" name="currentPassword" type="password" autocomplete="current-password" />
          </label>
          <label>
            <span>新密码</span>
            <input v-model="newPassword" name="newPassword" type="password" autocomplete="new-password" />
          </label>
          <label>
            <span>确认新密码</span>
            <input v-model="confirmPassword" name="confirmPassword" type="password" autocomplete="new-password" />
          </label>
          <p class="password-rule">{{ PASSWORD_RULE_TEXT }}</p>
          <p v-if="changePasswordError" class="modal-message error" role="alert">{{ changePasswordError }}</p>
          <p v-if="changePasswordSuccess" class="modal-message success" role="status">
            <CircleCheck :size="15" aria-hidden="true" />
            {{ changePasswordSuccess }}
          </p>
        </form>

        <footer class="modal-foot">
          <button type="button" class="btn secondary" @click="closeChangePassword">取消</button>
          <button type="button" class="btn primary change-password-submit" :disabled="changePasswordLoading" @click="submitChangePassword">
            <KeyRound :size="16" aria-hidden="true" />
            {{ changePasswordLoading ? '提交中...' : '确认修改' }}
          </button>
        </footer>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ChevronDown, CircleCheck, KeyRound, LogOut, X } from '@lucide/vue';
import { FAVICON_PATH } from '@/constants/brand';
import * as authService from '@/services/authService';
import {
  PASSWORD_RULE_TEXT,
  getPasswordConfirmationMessage,
  getPasswordValidationMessage,
} from '@/utils/passwordRules';

interface AppLayoutProps {
  username: string;
  activeRoute?: 'timeGrid' | 'stats' | 'admin';
  accountType?: 'USER' | 'ADMIN';
}

withDefaults(defineProps<AppLayoutProps>(), {
  activeRoute: 'timeGrid',
  accountType: 'USER',
});
defineEmits<{ (e: 'logout'): void }>();

const menuOpen = ref(false);
const changePasswordOpen = ref(false);
const currentPassword = ref('');
const newPassword = ref('');
const confirmPassword = ref('');
const changePasswordLoading = ref(false);
const changePasswordError = ref('');
const changePasswordSuccess = ref('');

function openChangePassword() {
  menuOpen.value = false;
  changePasswordOpen.value = true;
  changePasswordError.value = '';
  changePasswordSuccess.value = '';
}

function closeChangePassword() {
  changePasswordOpen.value = false;
  resetChangePasswordForm();
}

function resetChangePasswordForm() {
  currentPassword.value = '';
  newPassword.value = '';
  confirmPassword.value = '';
  changePasswordError.value = '';
  changePasswordSuccess.value = '';
  changePasswordLoading.value = false;
}

async function submitChangePassword() {
  changePasswordError.value = '';
  changePasswordSuccess.value = '';

  if (!currentPassword.value) {
    changePasswordError.value = '请输入当前密码';
    return;
  }

  changePasswordError.value = getPasswordValidationMessage(newPassword.value)
    || getPasswordConfirmationMessage(newPassword.value, confirmPassword.value);
  if (changePasswordError.value) return;

  changePasswordLoading.value = true;
  try {
    await authService.changePassword({
      currentPassword: currentPassword.value,
      newPassword: newPassword.value,
    });
    currentPassword.value = '';
    newPassword.value = '';
    confirmPassword.value = '';
    changePasswordSuccess.value = '密码已修改';
  } catch (error) {
    changePasswordError.value = error instanceof Error ? error.message : '修改密码失败';
  } finally {
    changePasswordLoading.value = false;
  }
}
</script>

<style scoped>
.dashboard-shell {
  position: relative;
  min-height: 100vh;
}

.dashboard-shell::before,
.dashboard-shell::after {
  content: '';
  position: fixed;
  z-index: 0;
  border-radius: 999px;
  filter: blur(80px);
  pointer-events: none;
}

.dashboard-shell::before {
  top: 80px;
  left: -100px;
  width: 240px;
  height: 240px;
  background: rgba(99, 102, 241, 0.14);
}

.dashboard-shell::after {
  top: 260px;
  right: -80px;
  width: 220px;
  height: 220px;
  background: rgba(56, 189, 248, 0.12);
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 50;
  border-bottom: 1px solid rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.70);
  backdrop-filter: blur(20px);
}

.header-inner {
  position: relative;
  z-index: 1;
  max-width: 1280px;
  min-height: 64px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.brand-zone,
.brand-mark,
nav {
  display: flex;
  align-items: center;
}

.brand-zone {
  gap: 28px;
}

.brand-mark {
  gap: 10px;
  color: #4f46e5;
  font-size: 14px;
  font-weight: 900;
}

nav {
  gap: 8px;
}

.nav-chip {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 13px;
  border-radius: 999px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(255, 255, 255, 0.72);
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
  text-decoration: none;
}

.nav-chip.active {
  color: #4338ca;
  background: rgba(238, 242, 255, 0.86);
  border-color: rgba(129, 140, 248, 0.35);
}

.user-menu {
  position: relative;
}

.user-trigger {
  min-height: 36px;
  border: 1px solid rgba(203, 213, 225, 0.8);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.75);
  color: #475569;
  padding: 0 10px 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
}

.dropdown {
  position: absolute;
  right: 0;
  top: 44px;
  width: 190px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.14);
  padding: 8px;
}

.dropdown-note {
  margin: 0;
  padding: 8px 10px 10px;
  color: #94a3b8;
  font-size: 11px;
  line-height: 1.5;
}

.logout-menu-item,
.change-password-menu-item {
  width: 100%;
  min-height: 38px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #475569;
  padding: 0 10px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
}

.logout-menu-item:hover,
.change-password-menu-item:hover {
  background: #f8fafc;
  color: #4338ca;
}

main {
  position: relative;
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px 24px 48px;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 90;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.36);
}

.change-password-modal {
  width: min(440px, 100%);
  overflow: hidden;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.24);
}

.modal-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
}

.modal-head h2,
.modal-head p,
.modal-body p {
  margin: 0;
}

.modal-head h2 {
  color: #0f172a;
  font-size: 18px;
}

.modal-head p {
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  display: grid;
  place-items: center;
  cursor: pointer;
}

.icon-btn:hover {
  color: #4338ca;
  border-color: rgba(129, 140, 248, 0.45);
  background: #eef2ff;
}

.modal-body {
  padding: 18px;
  display: grid;
  gap: 16px;
}

.modal-body label {
  display: grid;
  gap: 8px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
}

.modal-body input {
  width: 100%;
  min-height: 40px;
  border: 0;
  border-bottom: 1.5px solid #e2e8f0;
  border-radius: 0;
  outline: none;
  background: transparent;
  color: #1e293b;
  padding: 8px 0;
}

.modal-body input:focus {
  border-bottom-color: #6366f1;
  box-shadow: 0 1px 0 0 #6366f1;
}

.password-rule {
  margin-top: -6px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.modal-message {
  border-radius: 8px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  line-height: 1.6;
}

.modal-message.error {
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.modal-message.success {
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
  color: #15803d;
  font-weight: 800;
}

.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 18px 18px;
}

.btn {
  min-height: 40px;
  border: 1px solid transparent;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 14px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 800;
}

.btn.primary {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  box-shadow: 0 16px 30px rgba(79, 70, 229, 0.25);
}

.btn.primary:hover {
  background: linear-gradient(135deg, #6366f1, #4338ca);
}

.btn.primary:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.btn.secondary {
  border-color: #e2e8f0;
  background: #fff;
  color: #334155;
}

.btn.secondary:hover {
  background: #f8fafc;
}

@media (max-width: 720px) {
  .header-inner {
    align-items: flex-start;
    flex-direction: column;
    padding: 14px 16px;
  }

  .brand-zone {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  main {
    padding: 20px 14px 36px;
  }

  .modal-foot {
    flex-direction: column-reverse;
  }

  .btn {
    width: 100%;
  }
}
</style>
