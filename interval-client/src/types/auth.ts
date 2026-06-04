export type AccountType = 'USER' | 'ADMIN';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
}

export interface LoginResponseDto {
  token: string;
  username: string;
  accountType: AccountType;
  mustChangePassword: boolean;
}

export interface RegisterResponseDto {
  userId: number;
  username: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
