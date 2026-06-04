import { http } from '@/services/http';
import type {
  AdminDashboardDto,
  AdminUserListItemDto,
  FetchUsersParams,
  ResetPasswordRequest,
  ResetPasswordResponseDto,
} from '@/types/admin';

export async function fetchDashboard(): Promise<AdminDashboardDto> {
  return http.get<never, AdminDashboardDto>('/api/admin/dashboard');
}

export async function fetchUsers(params: FetchUsersParams = {}): Promise<AdminUserListItemDto[]> {
  return http.get<never, AdminUserListItemDto[]>('/api/admin/users', { params });
}

export async function resetPassword(userId: number, request: ResetPasswordRequest): Promise<ResetPasswordResponseDto> {
  return http.post<never, ResetPasswordResponseDto>(`/api/admin/users/${userId}/reset-password`, request);
}

export async function disableUser(userId: number): Promise<void> {
  return http.post<never, void>(`/api/admin/users/${userId}/disable`);
}

export async function enableUser(userId: number): Promise<void> {
  return http.post<never, void>(`/api/admin/users/${userId}/enable`);
}
