import http from "./http"
import type{
    ApiResponse,
    OwnerSummary,
    PageData,
    PageQuery,
    Pet,
    PetForm,
    PetType,
} from "@/types";

export interface PetQuery extends PageQuery{
    ownerId?: number;
}

//http.get<响应内容类型, 最终返回类型>()
export const listPets=(params:PetQuery)=>
    http.get<any,ApiResponse<PageData<Pet>>>("/pets",{params});

export const listPetTypes=()=>
    http.get<any,ApiResponse<PetType[]>>("/pet-types");

export const listOwners=()=>
    http.get<any,ApiResponse<PageData<OwnerSummary>>>("/owners",{
        params:{
            page:1,
            size:100,
            status:"ACTIVE",
        },
    });

export const createPet=(data: PetForm)=>
    http.post<any,ApiResponse<Pet>>("/pets",data);

export const updatePet=(id:number,data:PetForm)=>
    http.put<any,ApiResponse<Pet>>(`/pets/${id}`,data);

export const disablePet =(id:number)=>
    http.delete<any,ApiResponse<void>>(`/pets/${id}`);