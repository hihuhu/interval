import { http } from '@/services/http';
import type {
  ChangePasswordRequest,
  LoginRequest,
  LoginResponseDto,
  RegisterRequest,
  RegisterResponseDto,
} from '@/types/auth';

export async function login(username: string, password: string): Promise<LoginResponseDto> {
  return http.post<never, LoginResponseDto>('/api/auth/login', { username, password } satisfies LoginRequest);
}

export async function register(username: string, password: string): Promise<RegisterResponseDto> {
  return http.post<never, RegisterResponseDto>('/api/auth/register', { username, password } satisfies RegisterRequest);
}

export async function changePassword(request: ChangePasswordRequest): Promise<void> {
  return http.post<never, void>('/api/auth/change-password', request);
}
