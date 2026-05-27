import { beforeEach, describe, expect, it, vi } from 'vitest';
import { http, ApiError } from '@/services/http';

vi.mock('axios', () => {
  const handlers = {
    request: undefined as ((config: any) => any) | undefined,
    responseSuccess: undefined as ((response: any) => any) | undefined,
    responseError: undefined as ((error: any) => any) | undefined,
  };
  const instance = {
    interceptors: {
      request: { use: vi.fn((handler) => { handlers.request = handler; }) },
      response: { use: vi.fn((success, error) => { handlers.responseSuccess = success; handlers.responseError = error; }) },
    },
    __handlers: handlers,
  };
  return { default: { create: vi.fn(() => instance) } };
});

describe('http service', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('attaches bearer token when present', () => {
    localStorage.setItem('interval.auth.token', 'abc');
    const config = (http as any).__handlers.request({ headers: {} });
    expect(config.headers.Authorization).toBe('Bearer abc');
  });

  it('unwraps ApiResponse data on success', () => {
    const result = (http as any).__handlers.responseSuccess({
      data: { result: 'SUCCESS', message: 'ok', data: { id: 1 } },
    });
    expect(result).toEqual({ id: 1 });
  });

  it('throws ApiError on backend error response', () => {
    expect(() => (http as any).__handlers.responseSuccess({
      data: { result: 'ERROR', message: 'bad', data: null },
    })).toThrow(ApiError);
  });
});
