import http from "./http";
import type { ApiResponse, UserProfile } from "@/types";

export interface AuthResult {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: UserProfile;
}

export interface RegisterPayload {
  username: string;
  password: string;
  confirmPassword: string;
  displayName: string;
  phone: string;
  email?: string;
  address?: string;
}

export const login = (data: { username: string; password: string }) =>
  http.post<any, ApiResponse<AuthResult>>("/auth/login", data);

export const register = (data: RegisterPayload) =>
  http.post<any, ApiResponse<AuthResult>>("/auth/register", data);

export const getMe = () => http.get<any, ApiResponse<UserProfile>>("/auth/me");
