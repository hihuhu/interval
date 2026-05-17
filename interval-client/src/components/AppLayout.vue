<template>
  <div class="app-layout">
    <header class="app-header">
      <div>
        <h1>Time Grid</h1>
        <p>以 15 分钟为单位记录今天做了什么</p>
      </div>
      <div class="header-actions">
        <span>当前用户：<strong>{{ username }}</strong></span>
        <button type="button" @click="$emit('logout')">退出登录</button>
      </div>
    </header>
    <main>
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
interface AppLayoutProps {
  username: string;
}

defineProps<AppLayoutProps>();
defineEmits<{ (e: 'logout'): void }>();
</script>

<style scoped>
.app-layout { min-height: 100vh; background: #f8fafc; padding: 32px; }
.app-header { max-width: 1180px; margin: 0 auto 24px; display: flex; justify-content: space-between; gap: 16px; align-items: center; }
h1 { margin: 0; font-size: 32px; color: #111827; }
p { margin: 6px 0 0; color: #6b7280; }
.header-actions { display: flex; align-items: center; gap: 12px; color: #4b5563; }
button { border: 1px solid #d1d5db; background: #fff; border-radius: 10px; padding: 9px 14px; cursor: pointer; }
main { max-width: 1180px; margin: 0 auto; }
@media (max-width: 720px) { .app-layout { padding: 16px; } .app-header { align-items: flex-start; flex-direction: column; } }
</style>
