import http from "./http";
import type { ApiResponse, UserProfile } from "@/types";
export const login = (data: { username: string; password: string }) =>
  http.post<
    any,
    ApiResponse<{ token: string; expiresIn: number; user: UserProfile }>
  >("/auth/login", data);
export const getMe = () => http.get<any, ApiResponse<UserProfile>>("/auth/me");
