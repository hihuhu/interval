export type UserStatus = 'ACTIVE' | 'DISABLED';
export type ResetPasswordMode = 'AUTO' | 'MANUAL';

export interface AdminDashboardDto {
  totalUsers: number;
  activeUsers: number;
  disabledUsers: number;
  activeToday: number;
  activeLast7Days: number;
}

export interface AdminUserListItemDto {
  userId: number;
  username: string;
  status: UserStatus;
  createdAt: string;
  lastLoginAt: string | null;
  lastActiveAt: string | null;
  mustChangePassword: boolean;
}

export interface ResetPasswordRequest {
  mode: ResetPasswordMode;
  temporaryPassword: string | null;
}

export interface ResetPasswordResponseDto {
  temporaryPassword: string;
  mustChangePassword: boolean;
}

export interface FetchUsersParams {
  keyword?: string;
  status?: UserStatus | '';
}
