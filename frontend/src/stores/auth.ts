import { defineStore } from "pinia";
import type { UserProfile } from "@/types";
export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem("petclinic-token") || "",
    user: JSON.parse(
      localStorage.getItem("petclinic-user") || "null",
    ) as UserProfile | null,
    permissions: [] as string[],
  }),
  actions: {
    setSession(token: string, user: UserProfile) {
      this.token = token;
      this.user = user;
      localStorage.setItem("petclinic-token", token);
      localStorage.setItem("petclinic-user", JSON.stringify(user));
    },
    logout() {
      this.token = "";
      this.user = null;
      this.permissions = [];
      localStorage.removeItem("petclinic-token");
      localStorage.removeItem("petclinic-user");
    },
  },
});
