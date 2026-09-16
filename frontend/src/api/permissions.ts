import http from "./http"
import type{
    ApiResponse,PermissionNode
} from "@/types";

export const getPermissionTree=()=>
    http.get<any,ApiResponse<PermissionNode[]>>("/system/permissions/mine");