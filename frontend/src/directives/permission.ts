import type { DirectiveBinding, ObjectDirective } from "vue";
import { useAuthStore } from "@/stores/auth";
import {
  canAccess,
  type AccessRequirement,
} from "@/utils/permission";

export type PermissionValue = string | string[] | AccessRequirement;

const normalize = (value: PermissionValue): AccessRequirement => {
  if (typeof value === "string") return { authorities: [value] };
  if (Array.isArray(value)) return { authorities: value };
  return value;
};

const updateVisibility = (
  element: HTMLElement,
  binding: DirectiveBinding<PermissionValue>,
) => {
  const auth = useAuthStore();
  element.hidden = !canAccess(auth.permissions, normalize(binding.value));
};

const permissionDirective: ObjectDirective<HTMLElement, PermissionValue> = {
  mounted: updateVisibility,
  updated: updateVisibility,
};

export default permissionDirective;
