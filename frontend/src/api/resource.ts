import http from "./http";
import type { ApiResponse, PageData, PageQuery } from "@/types";
export const listResource = <T>(
  endpoint: string,
  params: PageQuery & Record<string, unknown> = {},
) => http.get<any, ApiResponse<PageData<T> | T[]>>(endpoint, { params });
export const getResource = <T>(endpoint: string, id: number) =>
  http.get<any, ApiResponse<T>>(`${endpoint}/${id}`);
export const createResource = <T>(
  endpoint: string,
  data: Record<string, unknown>,
) => http.post<any, ApiResponse<T>>(endpoint, data);
export const updateResource = <T>(
  endpoint: string,
  id: number,
  data: Record<string, unknown>,
) => http.put<any, ApiResponse<T>>(`${endpoint}/${id}`, data);
export const deleteResource = (endpoint: string, id: number) =>
  http.delete<any, ApiResponse<void>>(`${endpoint}/${id}`);
export const postAction = <T>(
  endpoint: string,
  data?: Record<string, unknown>,
) => http.post<any, ApiResponse<T>>(endpoint, data || {});
