import http from "./http";
import type {
  ApiResponse,
  CreateVisitPayload,
  PageData,
  PageQuery,
  ScheduleSlot,
  VetSummary,
  Visit,
} from "@/types";

export interface VisitQuery extends PageQuery {
  petId?: number;
  vetId?: number;
}

export const listVisits = (params: VisitQuery) =>
  http.get<any, ApiResponse<PageData<Visit>>>("/visits", { params });

export const listVetsForVisit = () =>
  http.get<any, ApiResponse<PageData<VetSummary>>>("/vets", {
    params: { page: 1, size: 100, status: "ACTIVE" },
  });

export const listAvailableSlots = (vetId: number, date: string) =>
  http.get<any, ApiResponse<ScheduleSlot[]>>("/slots/available", {
    params: { vetId, date },
  });

export const getSlot = (id: number) =>
  http.get<any, ApiResponse<ScheduleSlot>>(`/slots/${id}`);

export const createVisit = (data: CreateVisitPayload) =>
  http.post<any, ApiResponse<Visit>>("/visits", data);

export const cancelVisit = (id: number, reason: string) =>
  http.post<any, ApiResponse<Visit>>(`/visits/${id}/cancel`, { reason });

export const completeVisit = (id: number) =>
  http.post<any, ApiResponse<Visit>>(`/visits/${id}/complete`, {});
