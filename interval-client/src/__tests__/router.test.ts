import { describe, expect, it } from 'vitest';
import { createRouter, createMemoryHistory } from 'vue-router';
import { setActivePinia, createPinia } from 'pinia';
import { routes, installAuthGuard } from '@/router';
import { AUTH_TOKEN_KEY, AUTH_USERNAME_KEY } from '@/services/http';

describe('router guards', () => {
  it('redirects unauthenticated users from time grid to login', async () => {
    setActivePinia(createPinia());
    const router = createRouter({ history: createMemoryHistory(), routes });
    installAuthGuard(router);

    await router.push('/time-grid');
    await router.isReady();

    expect(router.currentRoute.value.name).toBe('login');
  });

  it('redirects authenticated users from login to time grid', async () => {
    setActivePinia(createPinia());
    localStorage.setItem(AUTH_TOKEN_KEY, 'token');
    localStorage.setItem(AUTH_USERNAME_KEY, 'alex');
    const router = createRouter({ history: createMemoryHistory(), routes });
    installAuthGuard(router);

    await router.push('/login');
    await router.isReady();

    expect(router.currentRoute.value.name).toBe('timeGrid');
  });
});
