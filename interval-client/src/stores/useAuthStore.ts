import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { AUTH_TOKEN_KEY, AUTH_USERNAME_KEY } from '@/services/http';
import * as authService from '@/services/authService';

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null);
  const username = ref<string | null>(null);
  const loading = ref(false);
  const errorMessage = ref<string | null>(null);
  const isAuthenticated = computed(() => Boolean(token.value));

  function restoreSession() {
    token.value = localStorage.getItem(AUTH_TOKEN_KEY);
    username.value = localStorage.getItem(AUTH_USERNAME_KEY);
  }

  async function login(name: string, password: string) {
    loading.value = true;
    errorMessage.value = null;
    try {
      const response = await authService.login(name, password);
      token.value = response.token;
      username.value = response.username;
      localStorage.setItem(AUTH_TOKEN_KEY, response.token);
      localStorage.setItem(AUTH_USERNAME_KEY, response.username);
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : 'Login failed';
      throw error;
    } finally {
      loading.value = false;
    }
  }

  async function register(name: string, password: string) {
    loading.value = true;
    errorMessage.value = null;
    try {
      return await authService.register(name, password);
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : 'Registration failed';
      throw error;
    } finally {
      loading.value = false;
    }
  }

  function logout() {
    token.value = null;
    username.value = null;
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(AUTH_USERNAME_KEY);
  }

  return { token, username, loading, errorMessage, isAuthenticated, restoreSession, login, register, logout };
});
