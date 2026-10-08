import http from "./http";
import type { ApiResponse, VetSummary } from "@/types";

export const uploadVetAvatar = (id: number, file: File) => {
  const data = new FormData();
  data.append("file", file);
  return http.post<any, ApiResponse<VetSummary>>(`/vets/${id}/avatar`, data, {
    timeout: 60000,
  });
};

export const deleteVetAvatar = (id: number) =>
  http.delete<any, ApiResponse<void>>(`/vets/${id}/avatar`);
