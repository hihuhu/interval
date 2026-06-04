<template>
  <div class="dashboard-shell">
    <header class="app-header">
      <div class="header-inner">
        <div class="brand-zone">
          <div class="brand-mark">
            <div class="logo-icon"></div>
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
            <ChevronDown :size="15" />
          </button>
          <div v-if="menuOpen" class="dropdown">
            <p class="dropdown-note">{{ accountType === 'ADMIN' ? '管理员账号' : '普通用户账号' }}</p>
            <button type="button" class="logout-menu-item" @click="$emit('logout')">
              <LogOut :size="15" />
              退出登录
            </button>
          </div>
        </div>
      </div>
    </header>
    <main>
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ChevronDown, LogOut } from '@lucide/vue';

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

.brand-mark .logo-icon {
  width: 20px;
  height: 20px;
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
  width: 180px;
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

.logout-menu-item {
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

.logout-menu-item:hover {
  background: #f8fafc;
  color: #4338ca;
}

main {
  position: relative;
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px 24px 48px;
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
}
</style>
