import { beforeEach, describe, expect, it, vi } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useAuthStore } from '@/stores/useAuthStore';
import * as authService from '@/services/authService';

vi.mock('@/services/authService');

const mockedAuthService = vi.mocked(authService);

describe('useAuthStore', () => {
  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
    vi.resetAllMocks();
  });

  it('restores session from localStorage', () => {
    localStorage.setItem('interval.auth.token', 'token-1');
    localStorage.setItem('interval.auth.username', 'alex');
    localStorage.setItem('interval.auth.accountType', 'ADMIN');
    localStorage.setItem('interval.auth.mustChangePassword', 'true');

    const store = useAuthStore();
    store.restoreSession();

    expect(store.token).toBe('token-1');
    expect(store.username).toBe('alex');
    expect(store.accountType).toBe('ADMIN');
    expect(store.mustChangePassword).toBe(true);
    expect(store.isAuthenticated).toBe(true);
  });

  it('stores token, username, account type and password state after login', async () => {
    mockedAuthService.login.mockResolvedValue({
      token: 'token-2',
      username: 'alex',
      accountType: 'USER',
      mustChangePassword: false,
    });

    const store = useAuthStore();
    await store.login('alex', 'SecurePass123!');

    expect(store.token).toBe('token-2');
    expect(store.accountType).toBe('USER');
    expect(store.mustChangePassword).toBe(false);
    expect(localStorage.getItem('interval.auth.token')).toBe('token-2');
    expect(localStorage.getItem('interval.auth.accountType')).toBe('USER');
    expect(localStorage.getItem('interval.auth.mustChangePassword')).toBe('false');
  });

  it('clears session on logout', () => {
    const store = useAuthStore();
    store.$patch({ token: 'token-3', username: 'alex', accountType: 'ADMIN', mustChangePassword: true });
    localStorage.setItem('interval.auth.token', 'token-3');
    localStorage.setItem('interval.auth.accountType', 'ADMIN');
    localStorage.setItem('interval.auth.mustChangePassword', 'true');

    store.logout();

    expect(store.token).toBeNull();
    expect(store.accountType).toBeNull();
    expect(store.mustChangePassword).toBe(false);
    expect(localStorage.getItem('interval.auth.token')).toBeNull();
    expect(localStorage.getItem('interval.auth.accountType')).toBeNull();
    expect(localStorage.getItem('interval.auth.mustChangePassword')).toBeNull();
  });
});
