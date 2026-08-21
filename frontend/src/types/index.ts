export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: string;
}
export interface PageData<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
}
export interface UserProfile {
  id: number;
  username: string;
  displayName: string;
  phone?: string;
  email?: string;
  accountType: "ADMIN" | "STAFF" | "OWNER";
}
export interface PageQuery {
  page?: number;
  size?: number;
  keyword?: string;
  status?: string;
}
