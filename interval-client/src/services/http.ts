import axios, { AxiosError } from 'axios';
import type { ApiResponse } from '@/types/api';

export class ApiError extends Error {
  constructor(message: string, public readonly status?: number) {
    super(message);
    this.name = 'ApiError';
  }
}

export const AUTH_TOKEN_KEY = 'interval.auth.token';
export const AUTH_USERNAME_KEY = 'interval.auth.username';

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>;
    if (body.result === 'SUCCESS') {
      return body.data as typeof response.data;
    }
    throw new ApiError(body.message, response.status);
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(AUTH_TOKEN_KEY);
      localStorage.removeItem(AUTH_USERNAME_KEY);
      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        window.location.assign('/login');
      }
    }
    const message = error.response?.data?.message ?? error.message;
    throw new ApiError(message, error.response?.status);
  },
);
