import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import type { AxiosAdapter } from 'axios';
import {
  AUTH_ACCOUNT_TYPE_KEY,
  AUTH_MUST_CHANGE_PASSWORD_KEY,
  AUTH_TOKEN_KEY,
  AUTH_USERNAME_KEY,
  http,
} from '@/services/http';

describe('http service', () => {
  const originalAdapter = http.defaults.adapter;

  beforeEach(() => {
    localStorage.clear();
    window.history.replaceState({}, '', '/login');
  });

  afterEach(() => {
    http.defaults.adapter = originalAdapter;
  });

  it('clears all auth storage keys on unauthorized responses', async () => {
    localStorage.setItem(AUTH_TOKEN_KEY, 'token');
    localStorage.setItem(AUTH_USERNAME_KEY, 'alex');
    localStorage.setItem(AUTH_ACCOUNT_TYPE_KEY, 'ADMIN');
    localStorage.setItem(AUTH_MUST_CHANGE_PASSWORD_KEY, 'true');
    http.defaults.adapter = (async (config) => Promise.reject({
      config,
      message: 'Unauthorized',
      response: {
        config,
        data: { result: 'ERROR', message: 'Expired' },
        headers: {},
        status: 401,
        statusText: 'Unauthorized',
      },
    })) as AxiosAdapter;

    await expect(http.get('/api/categories')).rejects.toThrow('Expired');

    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBeNull();
    expect(localStorage.getItem(AUTH_USERNAME_KEY)).toBeNull();
    expect(localStorage.getItem(AUTH_ACCOUNT_TYPE_KEY)).toBeNull();
    expect(localStorage.getItem(AUTH_MUST_CHANGE_PASSWORD_KEY)).toBeNull();
  });
});
