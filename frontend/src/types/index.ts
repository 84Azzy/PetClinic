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

  //后端返回ROLE_ADMIN，pet:manage的权限码
  authorities: string[];
}
export interface PageQuery {
  page?: number;
  size?: number;
  keyword?: string;
  status?: string;
}

export type PetStatus="ACTIVE" | "INACTIVE";
export type PetGender="MALE"|"FEMALE";

export interface Pet{
  id:number;
  ownerId:number;
  typeId:number;
  name:string;
  gender?:string;
  breed?: string;
  birthDate?: string;
  color?: string;
  microchipNo?: string;
  allergies?: string;
  photoUrl?: string;
  status: PetStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface PetType{
  id:number;
  name:string;
  description?:string;
  status:PetStatus;
}

export interface OwnerSummary {
  id: number;
  userId: number;
  name: string;
  phone: string;
  email?: string;
  status: PetStatus;
}

/**
 * 新增和修改宠物时提交给后端的数据。
 *
 * ownerId、typeId 在后端是必填字段，但表单刚打开时还没选择，
 * 所以前端暂时允许它们是 undefined，提交前再通过表单规则校验。
 */
export interface PetForm {
  ownerId?: number;
  typeId?: number;
  name: string;
  gender?: PetGender;
  breed?: string;
  birthDate?: string;
  color?: string;
  microchipNo?: string;
  allergies?: string;
  photoUrl?: string;
}

export type VisitStatus = "SCHEDULED" | "COMPLETED" | "CANCELLED";

export interface Visit {
  id: number;
  petId: number;
  slotId: number;
  vetId: number;
  createdBy: number;
  requestId: string;
  reason: string;
  status: VisitStatus;
  cancelledAt?: string;
  cancelReason?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface VetSummary {
  id: number;
  name: string;
  phone?: string;
  licenseNo?: string;
  status: string;
}

export interface ScheduleSlot {
  id: number;
  vetId: number;
  startTime: string;
  endTime: string;
  status: "AVAILABLE" | "BOOKED" | "CLOSED";
  note?: string;
}

export interface CreateVisitPayload {
  petId: number;
  slotId: number;
  requestId: string;
  reason: string;
}

export type PermissionType ="MENU"|"BUTTON"|"API";

export interface  PermissionNode{
  id: number;
  parentId?: number | null;
  code: string;
  name: string;
  type: PermissionType;
  path?: string | null;
  icon?: string | null;
  sortOrder: number;
  status: "ACTIVE" | "INACTIVE";
  children: PermissionNode[];
}