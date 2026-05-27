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

    const store = useAuthStore();
    store.restoreSession();

    expect(store.token).toBe('token-1');
    expect(store.username).toBe('alex');
    expect(store.isAuthenticated).toBe(true);
  });

  it('stores token and username after login', async () => {
    mockedAuthService.login.mockResolvedValue({ token: 'token-2', username: 'alex' });

    const store = useAuthStore();
    await store.login('alex', 'SecurePass123!');

    expect(store.token).toBe('token-2');
    expect(localStorage.getItem('interval.auth.token')).toBe('token-2');
  });

  it('clears session on logout', () => {
    const store = useAuthStore();
    store.$patch({ token: 'token-3', username: 'alex' });
    localStorage.setItem('interval.auth.token', 'token-3');

    store.logout();

    expect(store.token).toBeNull();
    expect(localStorage.getItem('interval.auth.token')).toBeNull();
  });
});
