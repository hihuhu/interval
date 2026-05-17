<template>
  <main class="not-found">
    <h1>页面不存在</h1>
    <RouterLink to="/time-grid">返回 Time Grid</RouterLink>
  </main>
</template>

<style scoped>
.not-found { min-height: 100vh; display: grid; place-items: center; align-content: center; gap: 16px; }
a { color: #4f46e5; }
</style>
