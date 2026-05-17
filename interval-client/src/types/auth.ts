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
}

export interface RegisterResponseDto {
  userId: number;
  username: string;
}
