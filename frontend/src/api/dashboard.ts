import http from "./http";
import type { ApiResponse } from "@/types";
export interface DashboardSummary {
  owners: number;
  pets: number;
  vets: number;
  scheduledVisits: number;
}
export interface MetricPoint {
  label: string;
  value: number;
}
export interface RecentVisit {
  id: number;
  petName: string;
  vetName: string;
  startTime: string;
  status: string;
}
export const getDashboardSummary = () =>
  http.get<any, ApiResponse<DashboardSummary>>("/dashboard/summary");
export const getVisitTrend = () =>
  http.get<any, ApiResponse<MetricPoint[]>>("/dashboard/visit-trend");
export const getPetDistribution = () =>
  http.get<any, ApiResponse<MetricPoint[]>>("/dashboard/pet-distribution");
export const getVetWorkload = () =>
  http.get<any, ApiResponse<MetricPoint[]>>("/dashboard/vet-workload");
export const getRecentVisits = () =>
  http.get<any, ApiResponse<RecentVisit[]>>("/dashboard/recent-visits");
