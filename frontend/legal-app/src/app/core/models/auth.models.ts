export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'LAWYER' | 'ACCOUNTANT' | 'USER' | 'VIEWER';
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION';

export interface UserInfo {
  id: number;
  name: string;
  username: string;
  email: string;
  role: UserRole;
  avatarUrl?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
  deviceId?: string;
  deviceName?: string;
  deviceType?: string;
}

export interface RegisterRequest {
  name: string;
  username: string;
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  deviceId: string;
  user: UserInfo;
}

export interface TokenRefreshResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface ApiError {
  status: number;
  message: string;
  timestamp: string;
  errors?: Record<string, string>;
}

export interface AuthState {
  user: UserInfo | null;
  accessToken: string | null;
  refreshToken: string | null;
  deviceId: string | null;
  loading: boolean;
  error: string | null;
  isAuthenticated: boolean;
}
