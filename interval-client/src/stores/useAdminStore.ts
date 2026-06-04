import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import * as adminService from '@/services/adminService';
import type { AdminDashboardDto, AdminUserListItemDto, ResetPasswordRequest, UserStatus } from '@/types/admin';

export const useAdminStore = defineStore('admin', () => {
  const dashboard = ref<AdminDashboardDto | null>(null);
  const users = ref<AdminUserListItemDto[]>([]);
  const selectedUserId = ref<number | null>(null);
  const loading = ref(false);

  const selectedUser = computed(() => users.value.find((user) => user.userId === selectedUserId.value) ?? users.value[0] ?? null);

  async function loadDashboard() {
    dashboard.value = await adminService.fetchDashboard();
  }

  async function loadUsers(params: { keyword?: string; status?: UserStatus | '' } = {}) {
    users.value = await adminService.fetchUsers(params);
    if (!selectedUserId.value && users.value.length > 0) {
      selectedUserId.value = users.value[0].userId;
    }
  }

  async function loadAll() {
    loading.value = true;
    try {
      await Promise.all([loadDashboard(), loadUsers()]);
    } finally {
      loading.value = false;
    }
  }

  async function resetUserPassword(userId: number, request: ResetPasswordRequest) {
    return adminService.resetPassword(userId, request);
  }

  async function disableUser(userId: number) {
    await adminService.disableUser(userId);
    await loadUsers();
    await loadDashboard();
  }

  async function enableUser(userId: number) {
    await adminService.enableUser(userId);
    await loadUsers();
    await loadDashboard();
  }

  return {
    dashboard,
    users,
    selectedUserId,
    selectedUser,
    loading,
    loadDashboard,
    loadUsers,
    loadAll,
    resetUserPassword,
    disableUser,
    enableUser,
  };
});
