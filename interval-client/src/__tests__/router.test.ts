import { beforeEach, describe, expect, it } from 'vitest';
import { createRouter, createMemoryHistory } from 'vue-router';
import { setActivePinia, createPinia } from 'pinia';
import { routes, installAuthGuard } from '@/router';
import { AUTH_TOKEN_KEY, AUTH_USERNAME_KEY } from '@/services/http';
import { AUTH_ACCOUNT_TYPE_KEY, AUTH_MUST_CHANGE_PASSWORD_KEY } from '@/services/http';

describe('router guards', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('redirects unauthenticated users from time grid to login', async () => {
    setActivePinia(createPinia());
    const router = createRouter({ history: createMemoryHistory(), routes });
    installAuthGuard(router);

    await router.push('/time-grid');
    await router.isReady();

    expect(router.currentRoute.value.name).toBe('login');
  });

  it('redirects authenticated regular users from login to time grid', async () => {
    setActivePinia(createPinia());
    localStorage.setItem(AUTH_TOKEN_KEY, 'token');
    localStorage.setItem(AUTH_USERNAME_KEY, 'alex');
    localStorage.setItem(AUTH_ACCOUNT_TYPE_KEY, 'USER');
    const router = createRouter({ history: createMemoryHistory(), routes });
    installAuthGuard(router);

    await router.push('/login');
    await router.isReady();

    expect(router.currentRoute.value.name).toBe('timeGrid');
  });

  it('redirects authenticated admins from login to admin page', async () => {
    setActivePinia(createPinia());
    localStorage.setItem(AUTH_TOKEN_KEY, 'token');
    localStorage.setItem(AUTH_USERNAME_KEY, 'admin');
    localStorage.setItem(AUTH_ACCOUNT_TYPE_KEY, 'ADMIN');
    const router = createRouter({ history: createMemoryHistory(), routes });
    installAuthGuard(router);

    await router.push('/login');
    await router.isReady();

    expect(router.currentRoute.value.name).toBe('admin');
  });

  it('redirects users who must change password to change password page', async () => {
    setActivePinia(createPinia());
    localStorage.setItem(AUTH_TOKEN_KEY, 'token');
    localStorage.setItem(AUTH_USERNAME_KEY, 'alex');
    localStorage.setItem(AUTH_ACCOUNT_TYPE_KEY, 'USER');
    localStorage.setItem(AUTH_MUST_CHANGE_PASSWORD_KEY, 'true');
    const router = createRouter({ history: createMemoryHistory(), routes });
    installAuthGuard(router);

    await router.push('/time-grid');
    await router.isReady();

    expect(router.currentRoute.value.name).toBe('changePassword');
  });

  it('redirects regular users away from admin page', async () => {
    setActivePinia(createPinia());
    localStorage.setItem(AUTH_TOKEN_KEY, 'token');
    localStorage.setItem(AUTH_USERNAME_KEY, 'alex');
    localStorage.setItem(AUTH_ACCOUNT_TYPE_KEY, 'USER');
    const router = createRouter({ history: createMemoryHistory(), routes });
    installAuthGuard(router);

    await router.push('/admin');
    await router.isReady();

    expect(router.currentRoute.value.name).toBe('timeGrid');
  });

  it('redirects admins away from regular user pages', async () => {
    setActivePinia(createPinia());
    localStorage.setItem(AUTH_TOKEN_KEY, 'token');
    localStorage.setItem(AUTH_USERNAME_KEY, 'admin');
    localStorage.setItem(AUTH_ACCOUNT_TYPE_KEY, 'ADMIN');
    const router = createRouter({ history: createMemoryHistory(), routes });
    installAuthGuard(router);

    await router.push('/stats');
    await router.isReady();

    expect(router.currentRoute.value.name).toBe('admin');
  });
});
